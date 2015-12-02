package controle;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map.Entry;

import modele.AbriException;
import modele.AnnuaireNoeudCentral;
import modele.Message;
import modele.MessageType;
import modele.NoeudCentral;
import modele.NoeudCentralException;

/**
 * Classe permettant de gérer un noeud central dans un réseau totalement
 * centralisé. Il a deux objectifs: <br>
 * - Gérer l'accès à la section critique, c'est à dire qu'un seul noeud à la
 * fois peut avoir un plein accès au noeud central <br>
 * - Faire le "passe-plat" pour transmettre des messages entre différents abris.
 * Le noeud central connais l'enssemble des abris connectés au réseau mais n'a
 * pas d'informations concernant les groupe de dangers.
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class NoeudCentralBackend extends UnicastRemoteObject implements NoeudCentralRemoteInterface
{
	private static final long serialVersionUID = -5192481389891341422L;

	private final String noeudURL;
	private NoeudCentral noeudCentralModele;

	/**
	 * Controleur unique dont l'objectif sera de gérer l'accès à la section
	 * critique. C'est ce contrôleur qui contiendra notre algorithme d'exclusion
	 * mutuelle, il suffit de modifier cette classe si on veut changer
	 * d'algorithme.
	 */
	private final SectionCritiqueControleurFIFO sectionCritiqueControleur;

	/**
	 * Annuaire associat pour chaque adresse URL une instance
	 * d'AbriRemoteInterface afin de pouvoir communiquer avec les abris. Tous
	 * les abris connectés au réseau doivent se trouver dans cet annuaire.
	 */
	private AnnuaireNoeudCentral abris;

	public NoeudCentralBackend(final String url) throws RemoteException, MalformedURLException
	{
		this.noeudURL = url;
		this.noeudCentralModele = new NoeudCentral();
		this.sectionCritiqueControleur = new SectionCritiqueControleurFIFO(this.noeudURL);
		this.abris = new AnnuaireNoeudCentral();
		Naming.rebind(this.noeudURL, this); // Publication dans l'annuaire RMI
	}

	/**
	 * Le noeud central recoit un message de la part d'un nouvel abri signalant
	 * qu'il viens de rejoindre le réseau. Il doit l'ajouter à son annuaire puis
	 * signaler l'éxistance du nouveau à tous les autres abris du réseau.
	 */
	@Override
	public void connexionAbri(final String urlNouveau, final String groupeNouveau) throws RemoteException, NotBoundException, MalformedURLException, AbriException, NoeudCentralException, IllegalAccessException
	{
		System.out.println("JE SUIS LE NOEUD, ABRI " + urlNouveau + " VIENS DE S'AJOUTER, JE DOIS PREVENIR " + this.abris.getAbrisDistants().size() + " AUTRES ABRIS.");
		// Il faut récupérer la remote interface de ce nouvel abris dans l'annuaire RMI
		AbriRemoteInterface remoteNouvelAbri = (AbriRemoteInterface) Naming.lookup(urlNouveau);
		this.abris.ajouterAbriDistant(urlNouveau, remoteNouvelAbri);

		// On envoie un broadcast à tout les abris éxistants pour signaler la présence du nouveau.
		for ( Entry<String, AbriRemoteInterface> autreAbri : this.abris.getAbrisDistants().entrySet() ) {
			if ( !autreAbri.getKey().equals(urlNouveau) ) { // On n'envoi rien à celui qui viens de se créer
				System.out.println(autreAbri.getKey() + " RECOIS " + urlNouveau);
				autreAbri.getValue().recevoirMessage(new Message(urlNouveau, autreAbri.getKey(), groupeNouveau, MessageType.SIGNALEMENT_CONNECTION));
			}
		}
	}

	/**
	 * Le noeud central est informé qu'un abri se déconnecte du réseau.Il doit
	 * alors prévenir l'enssemble des autres abris pour qu'ils puissent
	 * maintenir leur conaissance du réseau à jour, puis mettre à son tour à
	 * jour son annuaire.
	 */
	@Override
	public void deconnecterAbri(final String urlAbriDeconecte) throws AbriException, NoeudCentralException, RemoteException, IllegalAccessException
	{
		// Faire un broadcast pour prévenir les autres qu'il y a un abri en moins
		for ( Entry<String, AbriRemoteInterface> autreAbri : this.abris.getAbrisDistants().entrySet() ) {
			if ( !autreAbri.getKey().equals(urlAbriDeconecte) ) {
				autreAbri.getValue().recevoirMessage(new Message(urlAbriDeconecte, autreAbri.getKey(), MessageType.SIGNALEMENT_DECONNECTION));
			}
		}
		// Mettre à jour son annuaire
		this.abris.retirerAbriDistant(urlAbriDeconecte);
	}

	/**
	 * Méthode éxécutée lorsque le noeud central recoit une demande d'un abri
	 * pour entrer en section critique. On doit: <br>
	 * Déléguer la gestion au contrôleur qui nous répondra si l'abris peut
	 * immédiatement ou non entrer en SC. Si c'est le cas, on doit informer
	 * l'abris.
	 */
	@Override
	public synchronized void demanderSectionCritique(final String urlAbriDemandeur) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		System.out.println("JE SUIS " + urlAbriDemandeur + " ET JE VIENS D'ARRIVER AU NOEUD POUR UNE DEMANDE DE SC");
		boolean available = this.sectionCritiqueControleur.demanderSectionCritique(urlAbriDemandeur);
		if ( available ) { // SC dispo immédiatement
			this.sectionCritiqueControleur.setUrlEnSC(urlAbriDemandeur);
			this.abris.chercherUrl(urlAbriDemandeur).recevoirMessage(new Message(this.noeudURL, urlAbriDemandeur, MessageType.SIGNALEMENT_AUTORISATION_SC));
		}
	}

	@Override
	public void finalize() throws RemoteException, NotBoundException, MalformedURLException, Throwable
	{
		try {
			Naming.unbind(this.noeudURL);
		} finally {
			super.finalize();
		}
	}

	// =================================
	// CONNECTION ET DECONECTION D'ABRIS
	// =================================

	public AnnuaireNoeudCentral getAnnuaire()
	{
		return this.abris;
	}

	public NoeudCentral getNoeudCentral()
	{
		return this.noeudCentralModele;
	}

	// ================
	// SECTION CRITIQUE
	// ================
	/**
	 * La logique de getion de la SC est proposée dans
	 * SectionCritiqueControleur. Cependant, les demandes des abris arrivent
	 * dans la classe NoeudCentralBackend qui doit se charger de faire le relai
	 * vers le controleur. Il doit également se charger d'informer l'abri qui
	 * dispose d'un accès à la section critique.
	 */

	/**
	 * Méthode permettant de définir l'emetteur et le(s) destinataires d'un
	 * message avant envoi. Doit être appelé au bon moment pour éviter qu'un
	 * message n'arrive à un mauvais destinataire ! L'emeteur est forcément
	 * unique alors que les destinataires peuvent être nombreux.
	 */
	@Override
	public void modifierAiguillage(final String depuisUrl, final List<String> versUrl) throws RemoteException, NoeudCentralException
	{
		System.out.println("JE SUIS LE NOEUD CENTRAL, JE ME RECONFIGURE POUR ENVOYER DEPUIS " + depuisUrl + " VERS " + versUrl);
		this.noeudCentralModele.reconfigurerAiguillage(depuisUrl, versUrl);
	}

	/**
	 * Méthode éxétuée lorsque le noeud central recoit une demande d'un abri
	 * pour quitter la section critique. On doit: <br>
	 * - Déléguer la gestion au controleur qui nous informera si un autre abri
	 * était en attente de la section critiquie l'obtient à son tour. Si c'est
	 * le cas, nous devons prévenir cet abri.
	 */
	@Override
	public void quitterSectionCritique(final String urlAbriDemandeur) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		System.out.println("JE SUIS " + urlAbriDemandeur + " ET JE VIENS D'ARRIVER AU NOEUD POUR QUITTER LA SC");
		String prochain = this.sectionCritiqueControleur.quitterSectionCritique(urlAbriDemandeur);

		if ( prochain != null ) {
			System.out.println("JE SUIS LE NOEUD, UN ABRI A QUITTE LA SC ET JE LA DONNE MAINTENANT A " + prochain);
			this.sectionCritiqueControleur.setUrlEnSC(prochain);
			this.abris.chercherUrl(prochain).recevoirMessage(new Message(this.noeudURL, prochain, MessageType.SIGNALEMENT_AUTORISATION_SC));
		} else {
			System.out.println("JE SUIS LE NOEUD, UN ABRI A QUITTE LA SC ET PERSONNE EN ATTENTE");
		}
	}

	/**
	 * Effectue le passe plat pour la transmission d'un message. L'aiguillage
	 * est tout d'abord modifié directement dans le noed à partir des
	 * informations présente dans le corps du message à transmettre (nous avons
	 * choisis de placer l'appel ici et donc de ne pas demander à l'abri de
	 * faire la demande de modification d'aiguillage). L'envoi d'un message
	 * consiste en fait à appeler la méthode "recevoirMessage" d'un abri via
	 * l'instance d'AbriRemoteInterface. C'est le principe fondamentale de Java
	 * RMI.
	 */
	@Override
	public void transmettreMessage(final Message message) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		modifierAiguillage(message.getUrlEmetteur(), message.getUrlDestinataire());
		try {
			this.noeudCentralModele.demarrerTransmission();
			for ( String abri : this.noeudCentralModele.getVersUrl() ) {
				this.abris.chercherUrl(abri).recevoirMessage(message);
				System.out.println("ABRI '" + abri + "' VIENS DE RECEVOIR LE MESSAGE '" + message + "'");
			}
		} finally { // Assure que le noeud central ne se considèrera plus comme en train d'emmetre en cas de problème (évite donc un blocage du système).
			this.noeudCentralModele.stopperTransmission();
		}
	}

}
