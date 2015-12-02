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
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

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

	protected final String notreURL; // URL de notre Abri (nous)
	protected Abri abri;

	protected AnnuaireAbri abrisDistants; // Dans le noeud on mémorise l'url de l'abri et
	protected ArrayList<String> copains; // Les urls des autres membres du groupe de l'abri courant // Pas dans l'annuaire -> imposer la gestion d'une liste locale aux abris pour les groupes
	protected List<Message> messagesEnAttente;

	protected String noeudURL; // URL du noeud central
	protected NoeudCentralRemoteInterface noeudRemote; // Remote pour appeler une méthode sur le noeud central (communications)

	protected AtomicBoolean demandeSC; // Indique si on a déjà demandé la SC

	//
	// CONSTRUCTOR
	//

	public AbriBackend(final String url, final Abri abri) throws RemoteException // Obligé de throws due à la super class
	{
		System.out.println("CONSTRUCTION DE ABRI BACKEND AVEC POUR URL : " + url + " OK.");
		this.notreURL = url;
		this.abri = abri;
		this.abrisDistants = new AnnuaireAbri(); // Chaque abris connait tout les abris éxistant (peut importe la zone)
		this.copains = new ArrayList<>(); // Abris présent dans abrisDistants et étant aussi dans notre zone de danger
		this.messagesEnAttente = new ArrayList<>();
		this.demandeSC = new AtomicBoolean(false);
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
		return abri.donnerGroupe();
	}

	@Override
	public boolean estConnecte()
	{
		return abri.estConnecte();
	}

	@Override
	public AnnuaireAbri getAnnuaire()
	{
		return abrisDistants;
	}

	private synchronized List<Message> getMessagesEnAttente()
	{
		synchronized ( messagesEnAttente ) {
			return this.messagesEnAttente;
		}
	}

	@Override
	public void attribuerGroupe(final String groupe)
	{
		abri.definirGroupe(groupe);
	}

	//
	// CONNECTION / DECONNECTION D'ABRI
	//

	@Override
	public void connecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException
	{
		// Enregistrer dans l'annuaire RMI, notre AbriRemoteInterface sera disponible à partir de notreURL
		Naming.rebind(notreURL, this);

		// Initialisation du noeud central
		for ( String name : Naming.list(Adresses.archetypeAdresseNoeudCentral()) ) {
			name = "rmi:" + name;
			if ( !name.equals(notreURL) ) {
				Remote remote = Naming.lookup(name);
				if ( remote instanceof NoeudCentralRemoteInterface ) {
					if ( noeudRemote == null ) { // Le noeud central est unique
						System.out.println("@@@ Initialisation du noeud central --> " + name);
						noeudURL = name;
						noeudRemote = (NoeudCentralRemoteInterface) remote;
					} else {
						throw new AbriException("Plusieurs noeuds centraux semblent exister.");
					}
				}
			}
		}
		assert noeudRemote != null; // Il doit forcément éxister pour que notre système fonctionne.
		noeudRemote.creerAbri(notreURL, abri.donnerGroupe()); // On lui indique qu'on se connecte, il va faire un broadcast aux autres.
		abri.connecter();
	}

	/**
	 * @throws AbriException
	 * @throws RemoteException
	 * @throws MalformedURLException
	 * @throws NotBoundException
	 */
	@Override
	public void deconnecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException
	{
		noeudRemote.deconnecterAbri(notreURL);

		noeudURL = "";
		noeudRemote = null;
		abrisDistants.vider();
		copains.clear();

		abri.deconnecter();
		Naming.unbind(notreURL); // Retrait de l'annuaire RMI
	}

	@Override
	// On recoit un message du noeud central signalant l'éxistence d'un nouvel abri dans le réseau
	public void enregistrerAbri(final String urlDistant, final String groupe) throws RemoteException
	{
		System.out.println("Ajout de " + urlDistant + " dans la liste des abris distants de " + notreURL);
		// On commence par le rajouter dans notre liste d'abris et de mettre à jour notre liste de copains...
		abrisDistants.ajouterAbriDistant(urlDistant, groupe);
		calculCopains();

		// ... puis on renvoie un message indiquant qu'on a reçu et que nous on éxiste (pour que le nouveau nous connaisse)
		ArrayList<String> destinataires = new ArrayList<>();
		destinataires.add(urlDistant);
		String contenu = notreURL + "|" + abri.donnerGroupe(); // Le destinataire devra split sur "|" pour récupérer les deux infos
		ajouterMesageTampon(new Message(notreURL, destinataires, contenu, MessageType.SIGNALEMENT_EXISTENCE));
	}

	@Override
	public synchronized void supprimerAbri(final String urlDistant)
	{
		System.out.println(notreURL + ": \tOubli de l'abri " + urlDistant);
		abrisDistants.retirerAbriDistant(urlDistant);
		calculCopains();
	}

	//
	// EMISSION DE MESSAGES
	//

	@Override
	public void emettreMessageDanger(final String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException
	{
		System.out.println("@@@ Ajout d'un message en attente et demande de la SC");
		calculCopains(); // On s'assure que notre liste de copains est à jour
		ajouterMesageTampon(new Message(notreURL, copains, message, MessageType.SIGNALEMENT_DANGER));
	}

	/**
	 * @param message
	 * @throws RemoteException
	 * @throws AbriException
	 */
	@Override
	public synchronized void recevoirMessage(final modele.Message message) throws RemoteException, AbriException
	{
		if ( !message.getUrlDestinataire().contains(notreURL) ) {
			throw new AbriException("Message recu par le mauvais destinataire (" + message.getUrlDestinataire().toString() + " != " + notreURL + ")");
		}
		switch ( message.getType() ) {
			case SIGNALEMENT_DANGER :
				System.out.println(notreURL + ": \tMessage recu de " + message.getUrlEmetteur() + " \"" + message.getContenu() + "\"");
				abri.ajouterMessage(message); // Ajout à la liste des messages reçus par l'abri
				break;
			case SIGNALEMENT_EXISTENCE : // On met à jour notre liste quand quelqu'on nous réponds qu'il éxiste
				String[] parts = message.getContenu().split("|");
				String url = parts[0];
				String groupe = parts[1];
				System.out.println("@@@ Recevoir signalement de danger on a récupéré via split : url -> " + url + " et groupe -> " + groupe);
				abrisDistants.ajouterAbriDistant(url, groupe);
				calculCopains();
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
		System.out.println("Abris backend Recevoir SC --> url : " + notreURL + " viens de recevoir la SC");
		// On transmet tout les messages en attentes...
		for ( Message message : getMessagesEnAttente() ) {
			System.out.println("Envoie d'un message depuis " + message.getUrlEmetteur() + "vers " + message.getUrlDestinataire() + " avec pour contenu: " + message.getContenu());
			noeudRemote.transmettreMessage(message);
		}
		getMessagesEnAttente().clear(); // Tout a été envoyé, alors on vide le tampon

		// ... puis on libère directement la section critique.
		System.out.println("@@@ on a finis de faire ce qu'on veut en SC donc on la libère");
		demandeSC.set(false); // On a reçu la SC donc on ne la demande plus immédiatement
		noeudRemote.quitterSectionCritique(notreURL);
	}

	//
	// UTILITAIRE
	//

	private void ajouterMesageTampon(final Message message) throws RemoteException
	{
		getMessagesEnAttente().add(message);
		if ( !demandeSC.get() ) { // Si on est pas déjà en attente pour avoir la SC, on la demande car on a un mesage qui veut être envoyé !
			demandeSC.set(true);
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
			deconnecterAbri();
			Naming.unbind(notreURL); // Retrait de l'annuaire RMI
		} finally {
			super.finalize();
		}
	}

	private void calculCopains()
	{
		synchronized ( copains ) {
			copains.clear();
			for ( Entry<String, String> entry : abrisDistants.getAbrisDistants().entrySet() ) {
				if ( entry.getValue().equals(abri.donnerGroupe()) && !notreURL.equals(entry.getKey()) ) { // c'est un copain de notre zone mais  c'est pas nous
					copains.add(entry.getKey());
				}
			}
		}
	}
}
