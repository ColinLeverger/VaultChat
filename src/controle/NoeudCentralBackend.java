/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map.Entry;

import modele.AbriException;
import modele.AnnuaireNoeudCentral;
import modele.Message;
import modele.NoeudCentral;
import modele.NoeudCentralException;

/**
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class NoeudCentralBackend extends UnicastRemoteObject implements NoeudCentralRemoteInterface
{
	private static final long serialVersionUID = -5192481389891341422L;

	protected String url;
	protected NoeudCentral noeudCentral;
	protected SectionCritiqueNoeudControleur noeudControleur;
	protected AnnuaireNoeudCentral abris;

	public NoeudCentralBackend(final String _url) throws RemoteException, MalformedURLException
	{
		this.url = _url;
		noeudCentral = new NoeudCentral();
		noeudControleur = new SectionCritiqueNoeudControleur(this.url);
		abris = new AnnuaireNoeudCentral();
		Naming.rebind(url, this);
	}

	@Override
	public void finalize() throws RemoteException, NotBoundException, MalformedURLException, Throwable
	{
		try {
			Naming.unbind(url);
		} finally {
			super.finalize();
		}
	}

	@Override
	public synchronized void deconnecterAbri(final String url)
	{
		// Faire un broadcast pour les autres
		for ( Entry<String, AbriRemoteInterface> autreAbri : abris.getAbrisDistants().entrySet() ) {
			if ( !autreAbri.getKey().equals(url) ) {
				try {
					autreAbri.getValue().supprimerAbri(autreAbri.getKey());
				} catch ( RemoteException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// Mettre à jour son annuaire
		abris.retirerAbriDistant(url);
	}

	public NoeudCentral getNoeudCentral()
	{
		return noeudCentral;
	}

	public AnnuaireNoeudCentral getAnnuaire()
	{
		return abris;
	}

	@Override
	public void modifierAiguillage(final String depuisUrl, final ArrayList<String> versUrl) throws RemoteException, NoeudCentralException
	{
		System.out.print(url + ": \tReconfiguration du r�seau de " + depuisUrl + " vers " + versUrl);
		noeudCentral.reconfigurerAiguillage(depuisUrl, versUrl);
	}

	@Override
	// On doit avoir la section critique quand on est dans cette méthode. On ne doit pas quitter la SC dans cette méthode.
	public void transmettreMessage(final Message message) throws RemoteException, AbriException, NoeudCentralException
	{
		System.out.println("@@@ ENTREE METHODE TRANSMETTRE MESSAGE DU NOEUD CENTRAL BACKEND");
		modifierAiguillage(message.getUrlEmetteur(), message.getUrlDestinataire());
		try {
			noeudCentral.demarrerTransmission();
			// Tous les destinataires recoivent
			for ( String abri : noeudCentral.getVersUrl() ) {
				abris.chercherUrl(abri).recevoirMessage(message);
			}
			System.out.println("@@@ ENVOIE DES MESSAGES TERMINES");
		} finally {
			noeudCentral.stopperTransmission();
		}
	}

	@Override
	// le noeud centrale recoit un message signalant qu'un nouvel abri a rejoint le réseau
	public void creerAbri(final String urlAbriDistant, final String groupeAbri) throws RemoteException, NotBoundException, MalformedURLException
	{
		// On mémorise dans l'annuaire du noeud central
		System.out.println(url + ": \tEnregistrement de l'abri dans l'annuaire du noeud central " + urlAbriDistant);
		AbriRemoteInterface abriDistant = (AbriRemoteInterface) Naming.lookup(urlAbriDistant);
		abris.ajouterAbriDistant(urlAbriDistant, abriDistant);

		// On envoie un broadcast à tout les abris éxistants pour signaler la présence du nouveau.
		for ( Entry<String, AbriRemoteInterface> autreAbri : abris.getAbrisDistants().entrySet() ) {
			if ( !autreAbri.getKey().equals(urlAbriDistant) ) {
				System.out.println("@@@ L'abris distant " + autreAbri.getKey() + " a été par le noeud central que l'abris " + urlAbriDistant + " viens de se connecter.");
				autreAbri.getValue().enregistrerAbri(urlAbriDistant, groupeAbri);
			}
		}
	}

	@Override
	public synchronized void demanderSectionCritique(final String url) throws RemoteException
	{
		boolean available = noeudControleur.demanderSectionCritique(url);
		if ( available ) { // SC dispo immédiatement
			try {
				System.out.println("SC dispo on donne l'autorisaiton");
				noeudControleur.setUrlEnSC(url);
				abris.chercherUrl(url).recevoirSC();
			} catch ( AbriException | NoeudCentralException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void quitterSectionCritique(final String url) throws RemoteException
	{
		System.out.println("Abri demande à quitter la SC");
		// On enregistre le fait que l'abris actuel libère la SC
		String prochain = null;
		try {
			prochain = noeudControleur.quitterSectionCritique(url);
		} catch ( IllegalAccessException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Un abri était en attente pour avoir la SC, on la lui donne
		if ( prochain != null ) {
			try {
				noeudControleur.setUrlEnSC(prochain);
				abris.chercherUrl(prochain).recevoirSC();
			} catch ( AbriException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch ( NoeudCentralException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
