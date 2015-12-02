/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import modele.Abri;
import modele.AbriException;
import modele.Adresses;
import modele.AnnuaireAbri;
import modele.Message;
import modele.MessageType;
import modele.NoeudCentralException;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class AbriBackend extends UnicastRemoteObject implements AbriLocalInterface, AbriRemoteInterface
{
	private static final long serialVersionUID = -7291203652359910179L;

	private static final String SPLIT_CHAR = "#";
	private static final String RMI_ADDRESS_PREFIX = "rmi:";

	protected final String notreURL; // URL de notre Abri (nous)
	protected Abri nous;

	protected AnnuaireAbri abrisDistants; // Tous les abris distants
	protected ArrayList<String> copains; // Les urls des autres membres du groupe de l'abri courant // Pas dans l'annuaire -> imposer la gestion d'une liste locale aux abris pour les groupes
	protected List<Message> messagesEnAttente;

	protected String noeudURL; // URL du noeud central
	protected NoeudCentralRemoteInterface noeudRemote; // Remote pour appeler une méthode sur le noeud central (communications)

	protected boolean demandeSC;

	//
	// CONSTRUCTOR
	//

	public AbriBackend(final String url, final Abri abri) throws RemoteException // Obligé de throws due à la super class
	{
		this.notreURL = url;
		this.nous = abri;
		this.abrisDistants = new AnnuaireAbri(); // Chaque abris connait tout les abris éxistant (peut importe la zone)
		this.copains = new ArrayList<>(); // Abris présent dans abrisDistants et étant aussi dans notre zone de danger
		this.messagesEnAttente = new ArrayList<>();
		this.demandeSC = false;
		System.out.println("CONSTRUCTION DE ABRI BACKEND AVEC POUR URL : " + url + " OK.");
	}

	//
	// GETTERS / SETTERS
	//

	@Override
	public String getUrl()
	{
		return notreURL;
	}

	@Override
	public String signalerGroupe() throws RemoteException
	{
		return nous.donnerGroupe();
	}

	@Override
	public boolean estConnecte()
	{
		return nous.estConnecte();
	}

	@Override
	public AnnuaireAbri getAnnuaire()
	{
		return abrisDistants;
		//TODO
		// On veut seulement afficher les abris qui sont dans notre groupe, donc on reconstruit un abris composé uniquement de nos copains.
		//		calculCopains();
		//		AnnuaireAbri annuaireCopains = new AnnuaireAbri();
		//		for ( String copain : copains ) {
		//			annuaireCopains.ajouterAbriDistant(copain, nous.donnerGroupe());
		//		}
		//		return annuaireCopains;
	}

	private List<Message> getMessagesEnAttente()
	{
		return this.messagesEnAttente;
	}

	@Override
	public void attribuerGroupe(final String groupe)
	{
		nous.definirGroupe(groupe);
	}

	//
	// CONNECTION / DECONNECTION D'ABRI
	//

	@Override
	// On se connecte
	public void connecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException, NoeudCentralException
	{
		// Enregistrer dans l'annuaire RMI, notre AbriRemoteInterface sera disponible à partir de notreURL
		Naming.rebind(notreURL, this);

		// Initialisation du noeud central pour notre abri (les autres abri seront initialisé lors d'une réception d'un message du noeud central, on est en décentralisé).
		for ( String name : Naming.list(Adresses.archetypeAdresseNoeudCentral()) ) { // Itère sur tout ce qui est publié dans l'annuaire RMI
			name = RMI_ADDRESS_PREFIX + name;
			Remote remote = Naming.lookup(name);
			if ( remote instanceof NoeudCentralRemoteInterface ) {
				if ( noeudRemote == null ) { // Le noeud central est unique
					System.out.println("LE NOEUD CENTRAL VIENT D'ETRE INITIALISE SUR L'URL -> " + name + " POUR L'ABRI " + notreURL);
					noeudURL = name;
					noeudRemote = (NoeudCentralRemoteInterface) remote;
				} else {
					throw new AbriException("Plusieurs noeuds centraux semblent exister.");
				}
			}
		}
		assert noeudRemote != null; // Il doit forcément éxister pour que notre système fonctionne.
		//noeudRemote.connexionAbri(notreURL, nous.donnerGroupe()); // On indique qu'on se connecte, il va faire un broadcast aux autres.
		ajouterMesageTampon(new Message(notreURL, Arrays.asList(noeudURL), MessageType.ENVOYER_SIGNALEMENT_CONNECTION));
		nous.connecter();
	}

	@Override
	public void demanderDeconexion() throws AbriException, RemoteException, MalformedURLException, NotBoundException, NoeudCentralException
	{
		//	noeudRemote.deconnecterAbri(notreURL);
		ajouterMesageTampon(new Message(notreURL, Arrays.asList(noeudURL), MessageType.ENVOYER_SIGNALEMENT_DECONNECTION));
		// On oublie tout ce que l'on connait du réseau
		noeudURL = null;
		noeudRemote = null;
		abrisDistants.vider();
		copains.clear();

		nous.deconnecter();
		System.out.println("ABRI " + notreURL + " DECONNECTE");
		Naming.unbind(notreURL); // Retrait de l'annuaire RMI
	}

	// On recoit un message du noeud central signalant l'éxistence d'un nouvel abri dans le réseau
	private void enregistrerAbri(final String urlDistant, final String groupe) throws RemoteException, AbriException, NoeudCentralException
	{
		System.out.println("AJOUT DE " + urlDistant + " DANS LISTE ABRIS DISTANTS DE " + notreURL);
		// On commence par le rajouter dans notre liste d'abris et de mettre à jour notre liste de copains...
		abrisDistants.ajouterAbriDistant(urlDistant, groupe);
		if ( groupe.equals(nous.donnerGroupe()) ) {
			copains.add(urlDistant);
		}

		// ... puis on renvoie un message indiquant qu'on a reçu et que nous on éxiste (pour que le nouveau nous connaisse)
		String contenu = notreURL + SPLIT_CHAR + nous.donnerGroupe(); // Le destinataire devra split pour récupérer les deux infos
		ajouterMesageTampon(new Message(notreURL, Arrays.asList(urlDistant), contenu, MessageType.RECEVOIR_SIGNALEMENT_EXISTENCE));
	}

	private void supprimerAbri(final String urlDistant)
	{
		System.out.println("RETRAIT DE " + urlDistant + " DANS LISTE ABRIS DISTANTS DE " + notreURL);
		abrisDistants.retirerAbriDistant(urlDistant);
		copains.remove(urlDistant); // Ne fait rien si l'abri que l'on déconnecte n'étais pas un copain donc pas besoin de faire de vérifications.
	}

	//
	// EMISSION DE MESSAGES
	//

	@Override
	public void emettreMessageDanger(final String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException
	{
		System.out.println("AJOUT DE MESSAGE DANGER DS FILE D'ATTENDE DE " + notreURL);
		ajouterMesageTampon(new Message(notreURL, copains, message, MessageType.ENVOYER_SIGNALEMENT_DANGER));
	}

	/**
	 * @param message
	 * @throws RemoteException
	 * @throws AbriException
	 * @throws NoeudCentralException
	 */
	@SuppressWarnings("incomplete-switch")
	@Override
	public void recevoirMessage(final modele.Message message) throws RemoteException, AbriException, NoeudCentralException
	{
		if ( !message.getUrlDestinataire().contains(notreURL) ) {
			throw new AbriException("Message recu par le mauvais destinataire (" + message.getUrlDestinataire().toString() + " != " + notreURL + ")");
		}

		switch ( message.getType() ) {
			case RECEVOIR_SIGNALEMENT_DANGER :
				nous.memoriserMessageRecu(message); // Ajout à la liste des messages reçus par l'abri
				break;
			case RECEVOIR_SIGNALEMENT_EXISTENCE : // On met à jour notre liste quand quelqu'on nous réponds qu'il éxiste
				String[] parts = message.getContenu().split(SPLIT_CHAR);
				String url = parts[0];
				String groupe = parts[1];
				abrisDistants.ajouterAbriDistant(url, groupe);
				if ( groupe.equals(nous.donnerGroupe()) ) {
					copains.add(url);
				}
				break;
			case RECEVOIR_SIGNALEMENT_CONNECTION :
				enregistrerAbri(message.getUrlEmetteur(), message.getContenu());
				break;
			case RECEVOIR_SIGNALEMENT_DECONNECTION :
				supprimerAbri(message.getUrlEmetteur());
				break;
			default :
				break;
		}
	}

	//
	// SECTION CRITIQUE
	//

	@Override
	public void recevoirSC() throws RemoteException, AbriException, NoeudCentralException
	{
		System.out.println("ENTREE EN SC DE : " + notreURL);
		// On transmet tout les messages en attentes...
		for ( Message message : getMessagesEnAttente() ) {
			System.err.println("ENVOIE D'UN MESSAGE DEPUIS " + message.getUrlEmetteur() + " VERS " + message.getUrlDestinataire() + " AVEC POUR CONTENU: " + message.getContenu());
			noeudRemote.transmettreMessage(message);
		}
		getMessagesEnAttente().clear(); // Tout a été envoyé, alors on vide le tampon

		// ... puis on libère directement la section critique.
		System.out.println("DEMANDE DE SORTIE SC DE : " + notreURL);
		demandeSC = false;
		noeudRemote.quitterSectionCritique(notreURL);
	}

	//
	// UTILITAIRE
	//

	private void ajouterMesageTampon(final Message message) throws RemoteException, AbriException, NoeudCentralException
	{
		getMessagesEnAttente().add(message);
		if ( !demandeSC ) { // Si on est pas déjà en attente pour avoir la SC, on la demande car on a un mesage qui veut être envoyé !
			demandeSC = true;
			noeudRemote.demanderSectionCritique(notreURL);
		}
	}

	/**
	 * @throws AbriException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws MalformedURLException
	 * @throws Throwable
	 */
	@Override
	public void finalize() throws Throwable
	{
		try {
			demanderDeconexion();
			Naming.unbind(notreURL); // Retrait de l'annuaire RMI
		} finally {
			super.finalize();
		}
	}
}
