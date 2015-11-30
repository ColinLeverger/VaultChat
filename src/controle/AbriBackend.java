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
import java.util.logging.Level;
import java.util.logging.Logger;

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

	protected final String url;
	protected String controleurUrl;
	protected String noeudCentralUrl;
	protected Abri abri;
	protected NoeudCentralRemoteInterface noeudCentral;
	protected AnnuaireAbri abrisDistants; // Map<Url,Groupe>
	protected ArrayList<String> copains; // Les urls des autres membres du groupe de l'abri courant // Pas dans l'annuaire -> imposer la gestion d'une liste locale aux abris pour les groupes
	protected List<Message> messagesEnAttente;

	protected boolean enSC;

	public AbriBackend(final String _url, final Abri _abri) throws RemoteException
	{
		this.url = _url;
		System.out.println("CONSTRUCITON DE ABRI BACKEND AVEC POUR URL : " + _url + " OK.");
		this.controleurUrl = _url + "/controleur";
		this.noeudCentralUrl = "";
		this.abri = _abri;
		this.abrisDistants = new AnnuaireAbri();
		this.copains = new ArrayList<>();
		this.messagesEnAttente = new ArrayList<>();
	}

	/**
	 * @throws AbriException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws MalformedURLException
	 * @throws Throwable
	 */
	@Override
	public void finalize() throws AbriException, RemoteException, NotBoundException, MalformedURLException, Throwable
	{
		try {
			deconnecterAbri();
			Naming.unbind(url);
		} finally {
			super.finalize();
		}
	}

	@Override
	public String getUrl()
	{
		return url;
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

	@Override
	public void connecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException
	{
		// Enregistrer dans l'annuaire RMI
		Naming.rebind(url, this);

		// Enregistrement de tous les autres abris
		// et notification a tous les autres abris
		for ( String name : Naming.list(Adresses.archetypeAdresseAbri()) ) {
			name = "rmi:" + name;
			if ( !name.equals(url) ) {
				Remote o = Naming.lookup(name);
				if ( o instanceof AbriRemoteInterface ) {
					// Enregistrement de l'abri courant
					System.out.println(url + ": \tEnregistrement aupres de " + name);
					((AbriRemoteInterface) o).enregistrerAbri(url, abri.donnerGroupe(), controleurUrl);
					// Enregistrement d'un abri distant
					AbriBackend.this.enregistrerAbri(name, ((AbriRemoteInterface) o).signalerGroupe(), (AbriRemoteInterface) o);
				} else if ( o instanceof NoeudCentralRemoteInterface ) {
					if ( noeudCentral == null ) {
						this.noeudCentralUrl = name;
						noeudCentral = (NoeudCentralRemoteInterface) o;
						noeudCentral.creerAbri(url);
					} else {
						throw new AbriException("Plusieurs noeuds centraux semblent exister.");
					}
				}
			}
		}
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
		noeudCentral.deconnecterAbri(this.url);

		noeudCentralUrl = "";
		noeudCentral = null;

		this.abrisDistants.vider();
		this.abri.deconnecter(); // Abri
		Naming.unbind(this.url); // Annuaire RMI
	}

	/**
	 * @param message
	 * @throws RemoteException
	 * @throws AbriException
	 */
	@Override
	public synchronized void recevoirMessage(final modele.Message message) throws RemoteException, AbriException
	{
		if ( !message.getUrlDestinataire().contains(url) ) {
			throw new AbriException("Message recu par le mauvais destinataire (" + message.getUrlDestinataire().toString() + " != " + url + ")");
		}
		System.out.println(url + ": \tMessage recu de " + message.getUrlEmetteur() + " \"" + message.getContenu() + "\"");
		abri.ajouterMessage(message);
	}

	@Override
	public synchronized void recevoirSC() throws AbriException, RemoteException, NoeudCentralException
	{
		System.out.println("Abris backend Recevoir SC --> url: " + this.url + " viens de recevoir la SC");
		//	semaphore.acquire();
		// On envoi tout les messages en attentes
		System.out.println("TAILLE DE MESSAGES EN ATTENTE --> " + messagesEnAttente.size());
		for ( Message message : this.messagesEnAttente ) {
			System.out.println("Envoie d'un message depuis " + message.getUrlEmetteur() + "vers " + message.getUrlDestinataire() + " avec pour contenu: " + message.getContenu());
			noeudCentral.modifierAiguillage(this.url, message.getUrlDestinataire());
			this.noeudCentral.transmettreMessage(message);
		}
		// On rends la section critique
		this.noeudCentral.quitterSectionCritique(url);
	}

	@Override
	public void ajouterMessageAuTampon(final String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException
	{
		ArrayList<String> copaings = calculCopains();
		messagesEnAttente.add(new Message(this.url, copaings, message, MessageType.SIGNALEMENT_DANGER));
		noeudCentral.demanderSectionCritique(url);
	}

	private ArrayList<String> calculCopains()
	{
		ArrayList<String> copaings = new ArrayList<>();
		for ( Entry<String, String> entry : this.abrisDistants.getAbrisDistants().entrySet() ) {
			if ( entry.getValue().equals(this.abri.donnerGroupe()) && !this.url.equals(entry.getKey()) ) { // c'est un copain de notre zone mais que c'est pas nous
				copaings.add(entry.getKey());
			}
		}
		return copaings;
	}

	@Override
	public void enregistrerAbri(final String urlDistant, final String groupe, final AbriRemoteInterface distant)
	{
		abrisDistants.ajouterAbriDistant(urlDistant, groupe);

		if ( groupe.equals(abri.donnerGroupe()) ) {
			this.copains.add(urlDistant);
		}

		System.out.println(url + ": \tEnregistrement de l'abri " + urlDistant);
	}

	@Override
	public synchronized void enregistrerAbri(final String urlAbriDistant, final String groupe, final String urlControleurDistant)
	{
		try {
			AbriRemoteInterface o = (AbriRemoteInterface) Naming.lookup(urlAbriDistant);
			AbriBackend.this.enregistrerAbri(urlAbriDistant, groupe, o);
			//       enregistrerControleurNoeud(urlControleurDistant, groupe);
			//       o.enregistrerControleurNoeud(controleurUrl, groupe);
		} catch ( NotBoundException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		} catch ( MalformedURLException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		} catch ( RemoteException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public synchronized void supprimerAbri(final String urlDistant)
	{
		System.out.println(url + ": \tOubli de l'abri " + urlDistant);
		abrisDistants.retirerAbriDistant(urlDistant);
		if ( copains.contains(urlDistant) ) {
			copains.remove(urlDistant);
		}
	}

	@Override
	public synchronized void supprimerAbri(final String urlAbriDistant, final String urlControleurDistant)
	{
		try {
			AbriRemoteInterface o = (AbriRemoteInterface) Naming.lookup(urlAbriDistant);
			AbriBackend.this.supprimerAbri(urlAbriDistant);
			//    supprimerControleurNoeud(urlControleurDistant);
			//     o.supprimerControleurNoeud(controleurUrl);
		} catch ( NotBoundException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		} catch ( MalformedURLException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		} catch ( RemoteException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//	@Override
	//	public synchronized void enregistrerControleurNoeud(final String urlDistante, final String groupe)
	//	{
	//		noeudControleur.enregistrerControleur(urlDistante, groupe);
	//	}

	/*
	 * @Override public synchronized void supprimerControleurNoeud(String
	 * urlDistante) { controleur.supprimerControleur(urlDistante); }
	 */

	@Override
	public void recevoirAutorisation()
	{
		//	semaphore.release();
	}

	@Override
	public void changerGroupe(final String groupe)
	{
		abri.definirGroupe(groupe);
	}

	@Override
	public String signalerGroupe() throws RemoteException
	{
		return abri.donnerGroupe();
	}

}
