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
import java.util.Iterator;

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
	protected NoeudControleur noeudControleur;
	protected AnnuaireNoeudCentral abris;

	public NoeudCentralBackend(final String _url) throws RemoteException, MalformedURLException
	{
		this.url = _url;
		noeudCentral = new NoeudCentral();
		noeudControleur = new NoeudControleur(this.url);
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
		System.out.println("Deconnection de l'abri");
		// Faire un broadcast pour les autres

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
	public synchronized void modifierAiguillage(final String depuisUrl, final ArrayList<String> versUrl) throws RemoteException, NoeudCentralException
	{
		System.out.print(url + ": \tReconfiguration du r�seau de " + depuisUrl + " vers ");
		Iterator<String> itr = versUrl.iterator();
		while ( itr.hasNext() ) {
			System.out.print(itr.next());
		}
		System.out.print("\n");

		noeudCentral.reconfigurerAiguillage(depuisUrl, versUrl);
	}

	@Override
	public synchronized void transmettreMessage(final Message message) throws RemoteException, AbriException, NoeudCentralException
	{
		System.out.println("@@@ ENTREE METHODE TRANSMETTRE MESSAGE DU NOEUD CENTRAL BACKEND");
		try {
			noeudCentral.demarrerTransmission();
			System.out.println(url + ": \tTransmission du message \"" + message.toString() + "\"");

			ArrayList<String> abrisCible = noeudCentral.getVersUrl();
			Iterator<String> itr = abrisCible.iterator();

			while ( itr.hasNext() ) {
				AbriRemoteInterface c = abris.chercherUrl(itr.next());
				c.recevoirMessage(message);
			}
		} catch ( RemoteException ex ) {
			throw ex;
		} catch ( AbriException ex ) {
			throw ex;
		} finally {
			noeudCentral.stopperTransmission();
		}
	}

	@Override
	public synchronized void creerAbri(final String urlAbriDistant) throws RemoteException, NotBoundException, MalformedURLException
	{
		System.out.println(url + ": \tEnregistrement de l'abri dans l'annuaire " + urlAbriDistant);
		AbriRemoteInterface abriDistant = (AbriRemoteInterface) Naming.lookup(urlAbriDistant);
		abris.ajouterAbriDistant(urlAbriDistant, abriDistant);
	}

	@Override
	public synchronized void demanderSectionCritique(final String url) throws RemoteException
	{
		boolean available = noeudControleur.demanderSectionCritique(url);
		if ( available ) { // SC dispo immédiatement
			try {
				System.out.println("SC dispo on donne l'autorisaiton");
				abris.getAbrisDistants().get(url).recevoirSC();
			} catch ( AbriException | NoeudCentralException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void quitterSectionCritique(final String url) throws RemoteException
	{
		String prochain = null;
		try {
			prochain = noeudControleur.quitterSectionCritique(url);
		} catch ( IllegalAccessException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ( prochain != null ) { // On donne l'accès au prochain
			try {
				abris.getAbrisDistants().get(prochain).recevoirSC();
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
