/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import modele.*;

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

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class AbriBackend extends UnicastRemoteObject implements AbriLocalInterface, AbriRemoteInterface
{
	private static final long serialVersionUID = -7291203652359910179L;

	protected final String notreURL; // URL de notre Abri
	protected String noeudCentralUrl;
	protected Abri abri;
	protected NoeudCentralRemoteInterface noeudCentral;
	protected AnnuaireAbri abrisDistants; // Map<Url,Groupe>
	protected ArrayList<String> copains; // Les urls des autres membres du groupe de l'abri courant // Pas dans l'annuaire -> imposer la gestion d'une liste locale aux abris pour les groupes
	protected List<Message> messagesEnAttente;

	public AbriBackend(final String _url, final Abri _abri) throws RemoteException
	{
		this.notreURL = _url;
		System.out.println("CONSTRUCTION DE ABRI BACKEND AVEC POUR URL : " + _url + " OK.");
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
			Naming.unbind(notreURL);
		} finally {
			super.finalize();
		}
	}

	@Override
	public String getUrl()
	{
		return notreURL;
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
		Naming.rebind(notreURL, this);

		// Enregistrement de tous les autres abris
		// et notification a tous les autres abris
		for ( String name : Naming.list(Adresses.archetypeAdresseAbri()) ) {
			name = "rmi:" + name;
			if ( !name.equals(notreURL) ) {
				Remote o = Naming.lookup(name);
				if ( o instanceof AbriRemoteInterface ) {
					// Enregistrement de l'abri courant
					System.out.println(notreURL + ": \tEnregistrement aupres de " + name);
					// Enregistrement d'un abri distant
					AbriBackend.this.enregistrerAbri(name, ((AbriRemoteInterface) o).signalerGroupe(), (AbriRemoteInterface) o);
				} else if ( o instanceof NoeudCentralRemoteInterface ) {
					if ( noeudCentral == null ) {
						this.noeudCentralUrl = name;
						noeudCentral = (NoeudCentralRemoteInterface) o;
						noeudCentral.creerAbri(notreURL);
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
		noeudCentral.deconnecterAbri(this.notreURL);

		noeudCentralUrl = "";
		noeudCentral = null;

		this.abrisDistants.vider();
		this.abri.deconnecter(); // Abri
		Naming.unbind(this.notreURL); // Annuaire RMI
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
		System.out.println(notreURL + ": \tMessage recu de " + message.getUrlEmetteur() + " \"" + message.getContenu() + "\"");
		abri.ajouterMessage(message); // Ajout à la liste des messages reçus par l'abri
	}

	@Override
	public void recevoirSC() throws AbriException, RemoteException, NoeudCentralException
	{
		System.out.println("Abris backend Recevoir SC --> url : " + this.notreURL + " viens de recevoir la SC");
		// On envoi tout les messages en attentes
		System.out.println("Les messages en attente sont : " + this.messagesEnAttente);
		for ( Message message : this.messagesEnAttente ) {
			System.out.println("Envoie d'un message depuis " + message.getUrlEmetteur() + "vers " + message.getUrlDestinataire() + " avec pour contenu: " + message.getContenu());
			this.noeudCentral.transmettreMessage(message);
		}

		// On rends la section critique
		this.noeudCentral.quitterSectionCritique(notreURL);
	}

	@Override
	public void ajouterMessageAuTampon(final String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException
	{
		calculCopains();
		messagesEnAttente.add(new Message(this.notreURL, this.copains, message, MessageType.SIGNALEMENT_DANGER));
		System.out.println("@@@ Ajout d'un message en attente et demande de la SC");
		noeudCentral.demanderSectionCritique(notreURL);
	}

	private void calculCopains()
	{
		for ( Entry<String, String> entry : this.abrisDistants.getAbrisDistants().entrySet() ) {
			if ( entry.getValue().equals(this.abri.donnerGroupe()) && !this.notreURL.equals(entry.getKey()) ) { // c'est un copain de notre zone mais que c'est pas nous
				this.copains.add(entry.getKey());
			}
		}
	}

	@Override
	public void enregistrerAbri(final String urlDistant, final String groupe, final AbriRemoteInterface distant)
	{
		System.out.println("Ajout de " + urlDistant + " dans la liste des abris distants de " + notreURL);
		abrisDistants.ajouterAbriDistant(urlDistant, groupe);

		if ( groupe.equals(abri.donnerGroupe()) ) {
			this.copains.add(urlDistant);
			System.out.println("Ajout de " + urlDistant + " dans la liste des copains de " + notreURL);
		}
	}

	@Override
	public synchronized void enregistrerAbri(final String urlAbriDistant, final String groupe)
	{
		try {
			AbriRemoteInterface o = (AbriRemoteInterface) Naming.lookup(urlAbriDistant);
			AbriBackend.this.enregistrerAbri(urlAbriDistant, groupe, o);
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
		System.out.println(notreURL + ": \tOubli de l'abri " + urlDistant);
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
		} catch ( NotBoundException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		} catch ( MalformedURLException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		} catch ( RemoteException ex ) {
			Logger.getLogger(AbriBackend.class.getName()).log(Level.SEVERE, null, ex);
		}
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
