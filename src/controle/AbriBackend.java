
package controle;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
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
 * @author Colin Leverger
 * @author Maelig Nantel
 *
 *         Class permettant de gérer un abri dans un réseau totalement
 *         centralisé autour d'un noeud central. Les seuls messages pouvant
 *         arriver sur cette classe proviennent d'un unique noeud central. Il
 *         n'est donc pas nécéssaire de gérer la concurence des accès à l'aide
 *         de blocs ou de méthode 'synchronized' ici.
 */
public class AbriBackend extends UnicastRemoteObject implements AbriLocalInterface, AbriRemoteInterface
{
	private static final long serialVersionUID = -7291203652359910179L;

	private static final String SPLIT_CHAR = "#"; // Ne pas utiliser le caractère réservé '|'
	private static final String RMI_ADDRESS_PREFIX = "rmi:";

	/**
	 * Notre adresse permettant de nous contacter depuis d'autre sites. C'est
	 * l'adresse utilisée pour publier notre AbriRemoteInterface dans l'annuaire
	 * RMI.
	 */
	private final String notreURL;

	private Abri notreModele;

	/**
	 * Annuaire associant à l'adresse d'un abri son groupe (cette class a donc
	 * été modifié par nos soins pour ne pas mémoriser de AbriRemoteInterface
	 * d'un abri distant (nous somme en mode totalement centralisé)
	 * L'intégralité des abris du connectés au réseau sont présent dans cette
	 * liste.
	 */
	private AnnuaireAbri abrisDistants;

	/**
	 * Un 'copain' est un abri se trouvant dans le même groupe que nous. Chaque
	 * instance de 'copains' est forcément présente dans 'abrisDistants'.
	 */
	private ArrayList<String> copains;

	/**
	 * Pour l'envoie de message, il est nécéssaire de disposer de la section
	 * critique. Nous avons choisis de ne pas "attendre" de disposer de celle ci
	 * avant de pouvoir prévoir un envoie. A chaque instant, il est possible de
	 * demander l'envoi d'un message. Ce dernier sera alors stocké dans une
	 * mémoire tampon. Dès que nous disposerons de la section critique,
	 * l'enssemble des messages en attente seront envoyés à leur destinataires
	 * respectifs.
	 */
	private List<Message> messagesEnAttente;

	/**
	 * Adresse permettant de récupérer la RemoteInterface du noeud dans
	 * l'annuaire RMI.
	 */
	private String noeudURL;

	/**
	 * RemoteInterface permettant d'appeler des méthodes distantes sur le noeud
	 * central via Java RMI.
	 */
	private NoeudCentralRemoteInterface noeudRemote;

	/**
	 * Variable indiquant si la section critique a déjà été demandée. Un même
	 * abri ne peut pas être plusieurs fois en attente d'accès à la section
	 * critique.
	 */
	private boolean demandeSC;

	// ===========
	// CONSTRUCTOR
	// ===========

	public AbriBackend(final String url, final Abri abri) throws RemoteException // Obligé de throws due à la super class
	{
		this.notreURL = url;
		this.notreModele = abri;
		this.abrisDistants = new AnnuaireAbri(); // Chaque abris connait tout les abris éxistant (peut importe la zone)
		this.copains = new ArrayList<>(); // Abris présent dans abrisDistants et étant aussi dans notre zone de danger
		this.messagesEnAttente = new ArrayList<>();
		this.demandeSC = false;
		System.out.println("CONSTRUCTION DE ABRI BACKEND AVEC POUR URL : " + url + " OK.");
	}

	/**
	 * Ajout d'un message dans un tampon en attendant leur envoi effectif. Lors
	 * d'un ajout, si nous n'étions pas déjà en attente de la section critique,
	 * on le devient.
	 */
	private void ajouterMesageTampon(final Message message) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		getMessagesEnAttente().add(message);
		if ( !this.demandeSC ) { // Si on est pas déjà en attente pour avoir la SC, on la demande car on a un mesage qui veut être envoyé !
			this.demandeSC = true;
			this.noeudRemote.demanderSectionCritique(this.notreURL);
		}
	}

	@Override
	public void attribuerGroupe(final String groupe)
	{
		this.notreModele.definirGroupe(groupe);
	}

	/**
	 * Méthode utilisée pour se connecter au réseau. Nous sommes un abri et nous
	 * demandons à rejoindre le réseau. Nous devons donc: <br>
	 * - Publier notre adresse dans l'annuaire RMI pour que les autres sites
	 * puissent nous trouver <br>
	 * - Initialiser nos informations sur l'unique noeud central en parcourant
	 * l'annuaire RMI <br>
	 * - Envoyer un message au noeud central pour lui indiquer que nous avons
	 * rejoins le réseau
	 */
	@Override
	public void demanderConnection() throws AbriException, RemoteException, MalformedURLException, NotBoundException, NoeudCentralException, IllegalAccessException
	{
		// Annuaire RMI
		Naming.rebind(this.notreURL, this);

		// Initialisation du noeud central pour notre abri (les autres abri seront initialisé lors d'une réception d'un message du noeud central, on est en décentralisé).
		for ( String name : Naming.list(Adresses.archetypeAdresseNoeudCentral()) ) { // Itère sur tout ce qui est publié dans l'annuaire RMI
			name = RMI_ADDRESS_PREFIX + name;
			Remote remote = Naming.lookup(name);
			if ( remote instanceof NoeudCentralRemoteInterface ) { // Aucun traitement sur les AbriRemoteInterface rencontrées
				if ( this.noeudRemote == null ) { // Le noeud central est unique
					System.out.println("LE NOEUD CENTRAL VIENT D'ETRE INITIALISE SUR L'URL -> " + name + " POUR L'ABRI " + this.notreURL);
					this.noeudURL = name;
					this.noeudRemote = (NoeudCentralRemoteInterface) remote;
				} else {
					throw new AbriException("Plusieurs noeuds centraux semblent exister.");
				}
			}
		}
		assert this.noeudRemote != null; // Il doit forcément être initialisé une fois connecté pour que notre système fonctionne.
		this.noeudRemote.connexionAbri(this.notreURL, this.notreModele.donnerGroupe()); // On indique qu'on se connecte, il va faire un broadcast pour prévenir les autres abris.
		this.notreModele.connecter();
	}

	/**
	 * Méthode utilisée pour demander à se déconnecter du réseau. Nous sommes un
	 * abri et nous demandons à quitter le réseau. Nous devons donc: <br>
	 * - Prévenir le noeud central que nous n'éxistons plus <br>
	 * - Vider nos paramètres (listes d'abris éxistants, de copains et
	 * informations sur le noeud central. Si on souhaite à nouveau rejoindre le
	 * réseau, ces informations auront potentiellement évoluée et afin de ne pas
	 * avoir d'incohérence, il faudra les redemander. <br>
	 * - Se retirer de l'annuaire RMI
	 */
	@Override
	public void demanderDeconexion() throws AbriException, RemoteException, MalformedURLException, NotBoundException, NoeudCentralException, IllegalAccessException
	{
		this.noeudRemote.deconnecterAbri(this.notreURL);

		this.noeudURL = null;
		this.noeudRemote = null;
		this.abrisDistants.vider(); // Pattern observer va mettre à jour la vue.
		this.copains.clear();

		this.notreModele.deconnecter();
		Naming.unbind(this.notreURL);

		System.out.println("ABRI " + this.notreURL + " DECONNECTE");
	}

	/**
	 * Envoie d'un message indiquant un danger pour l'enssemble du groupe auquel
	 * on appartient. L'ajout d'un message consiste à ajouter le message dans un
	 * tampon (ce qui provoquera une demande de section critique). L'envoie sera
	 * réellement éffectué que lorsque la section critique sera accordée à notre
	 * abri.
	 */
	@Override
	public void emettreMessageDanger(final String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		System.out.println("AJOUT DE MESSAGE DANGER DS FILE D'ATTENDE DE " + this.notreURL);
		ajouterMesageTampon(new Message(this.notreURL, this.copains, message, MessageType.SIGNALEMENT_DANGER));
	}

	/**
	 * Méthode éxécutée lorsque le noeud central nous informe qu'un nouvel abri
	 * a rejoins le réseau. Nous devons donc:<br>
	 * - Ajouter ce nouvel abri à notre annuaire et si besoin à notre liste de
	 * copains <br>
	 * - Renvoyer un message "d'acquitement" à ce nouvel abris (à travers le
	 * noeud central) permettant ainsi que lui signaler notre éxistence dans le
	 * réseau.
	 */
	private void enregistrerNouvelAbri(final String urlNouveau, final String groupeNouveau) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		System.out.println("AJOUT DE " + urlNouveau + " DANS LISTE ABRIS DISTANTS DE " + this.notreURL);
		this.abrisDistants.ajouterAbriDistant(urlNouveau, groupeNouveau);
		if ( groupeNouveau.equals(this.notreModele.donnerGroupe()) ) {
			this.copains.add(urlNouveau);
		}

		/*
		 * Pour signaler notre éxistance, on doit envoyer notre adresse et notre
		 * groupe. La classe Message actuelle ne permet pas de passer des
		 * paramètres multiples. Pour éviter cette modification du modèle, nous
		 * avons décidé de regrouper les deux informations dans le champ
		 * contenu. Le destinataire pourra simplement récupérer les deux
		 * paramètres en éffectuant un split sur SPLIT_CHAR
		 */
		String contenu = this.notreURL + SPLIT_CHAR + this.notreModele.donnerGroupe();
		ajouterMesageTampon(new Message(this.notreURL, urlNouveau, contenu, MessageType.SIGNALEMENT_EXISTENCE));
	}

	@Override
	public boolean estConnecte()
	{
		return this.notreModele.estConnecte();
	}

	@Override
	public void finalize() throws Throwable
	{
		try {
			demanderDeconexion();
			Naming.unbind(this.notreURL); // Retrait de l'annuaire RMI
		} finally {
			super.finalize();
		}
	}

	@Override
	/**
	 * Retourne l'enssemble des abris du réseau connus. Ne retourne pas
	 * uniquement nos copains, donc dans les vues c'est l'enssemble des abris
	 * qui seront affichés (fonctionalité déjà proposé dans le code fournis pour
	 * ce projet).
	 */
	public AnnuaireAbri getAnnuaire()
	{
		return this.abrisDistants;
	}

	private List<Message> getMessagesEnAttente()
	{
		return this.messagesEnAttente;
	}

	public String getNoeudURL()
	{
		return this.noeudURL;
	}

	@Override
	public String getUrl()
	{
		return this.notreURL;
	}

	/**
	 * Méthode éxécutée lorsque le noeud central nous informe qu'un abri
	 * éxistant dans le réseau s'est déconnecté. Nous devons donc le retirer de
	 * notre annuaire et si besoin, de notre liste de copains.
	 */
	private void oublierAbriExistant(final String urlAOublier)
	{
		System.out.println("RETRAIT DE " + urlAOublier + " DANS LISTE ABRIS DISTANTS DE " + this.notreURL);
		this.abrisDistants.retirerAbriDistant(urlAOublier);
		this.copains.remove(urlAOublier); // Ne fait rien si l'abri que l'on déconnecte n'étais pas un copain donc pas besoin de faire de vérifications.
	}

	/**
	 * Méthode permettant de recevoir un message ayant transité par le noeud
	 * central. Un message peut être de plusieurs types (voir le type énuméré
	 * MessageType). On doit: <br>
	 * - Vérifier que nous sommes bien supposer recevoir le message (on ne doit
	 * pas recevoir un message d'un autre groupe) <br>
	 * - Traiter le message en fonction de son type. Voir la documentation des
	 * méthodes associées.
	 */
	@Override
	public void recevoirMessage(final modele.Message messageRecu) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		if ( !messageRecu.getUrlDestinataire().contains(this.notreURL) ) {
			throw new AbriException("Message recu par le mauvais destinataire (" + messageRecu.getUrlDestinataire().toString() + " != " + this.notreURL + ")");
		}

		switch ( messageRecu.getType() ) {
			case SIGNALEMENT_DANGER : // Le noeud cental nous indique qu'un abri de notre groupe nous signale un danger
				this.notreModele.memoriserMessageRecu(messageRecu); // Ajout à la liste des messages que l'on a reçues
				break;
			case SIGNALEMENT_EXISTENCE : // On a rejoins le réseau récemment et un autre abri du réseau nous signale son éxistance.
				// Voir méthode 'enregistrerNouvelAbri' pour cette partie.
				String[] parts = messageRecu.getContenu().split(SPLIT_CHAR);
				String url = parts[0];
				String groupe = parts[1];

				/*
				 * On l'ajoute à notre liste d'abris connus et si besoin à notre
				 * liste de copains. Différent de la méthode
				 * enregistrerNouvelAbri car ici on est récepteur de la réponse
				 * d'acquitement.
				 */
				this.abrisDistants.ajouterAbriDistant(url, groupe);
				if ( groupe.equals(this.notreModele.donnerGroupe()) ) {
					this.copains.add(url);
				}
				break;
			case SIGNALEMENT_AUTORISATION_SC : // Le noeud central nous indique qu'on a l'autorisation d'utiliser la section critique
				recevoirSC();
				break;
			case SIGNALEMENT_CONNECTION : // Le noeud central nous indique qu'un nouvel abri a rejoins le réseau
				enregistrerNouvelAbri(messageRecu.getUrlEmetteur(), messageRecu.getContenu());
				break;
			case SIGNALEMENT_DECONNECTION : // Le noeud central nous indique qu'un abri a quitté le réseau
				oublierAbriExistant(messageRecu.getUrlEmetteur());
				break;
			default :
				break;
		}
	}

	/**
	 * Méthode éxécutée lorsque l'algorithme d'exclusion mutuelle tournant sur
	 * le noeud central nous indique que nous pouvons utiliser la section
	 * critique. Nous devons donc: <br>
	 * - Réaliser l'envoi de tous les messages de notre tampon (on les transmet
	 * au noeud central qui fera le transfert vers les destinataires). <br>
	 * - Libérer la section critique une fois les envois terminés (on garde la
	 * section critique le temps le plus court possible)<br>
	 * <br>
	 * Limitation possible: Si nous souhaitons envoyer un très grand nombre de
	 * messages et que nous devons attendre un certains temps pour avoir la
	 * section critique, il est possible que le tampon soit surchargé. Ce cas
	 * reste cependant très peu probable.
	 */
	private void recevoirSC() throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException
	{
		System.out.println("ENTREE EN SC DE : " + this.notreURL);
		/*
		 * Block try-finally afin de garantir que quoique il se passe pendant
		 * l'accès à la section critique, elle sera libérée (sauf une
		 * RemoteException liée à une coupure réseau sur l'abri... c'est
		 * l'inconvénient du centralisé !)
		 */
		try {
			this.demandeSC = false;
			// On transmet tout les messages en attentes... pour le moment aucune priorité n'est gérée
			for ( Message message : getMessagesEnAttente() ) {
				System.err.println("ENVOIE D'UN MESSAGE DEPUIS " + message.getUrlEmetteur() + " VERS " + message.getUrlDestinataire() + " AVEC POUR CONTENU: " + message.getContenu());
				this.noeudRemote.transmettreMessage(message);
			}
			// Ne pas supprimer le message envoyé dans la boucle, cela cause une ConcurentModificationException sur la liste.
			getMessagesEnAttente().clear(); // Tout a été envoyé, alors on vide le tampon

			System.out.println("DEMANDE DE SORTIE SC DE : " + this.notreURL);
		} finally {
			this.noeudRemote.quitterSectionCritique(this.notreURL);
		}
	}
}
