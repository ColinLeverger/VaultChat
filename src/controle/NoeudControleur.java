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
import java.util.LinkedList;

import modele.AbriException;
import modele.NoeudCentralException;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class NoeudControleur implements ControleurInterface
{

	protected String urlNoeud;
	private LinkedList<String> listeAttenteSectionCritique; // On stock l'url des abris en attente
	private String urlEnSC;
	private boolean used;

	public NoeudControleur(final String url)
	{
		this.urlNoeud = url;
		this.used = false;
	}

	@Override
	// renvoie vrai si la SC est immédiatement disponible
	public synchronized boolean demanderSectionCritique(final String urlDemandeur)
	{
		System.out.println(this.urlNoeud + ": \tDemande de section critique enregistree");

		if ( !this.used ) {
			this.used = true;
			this.urlEnSC = urlDemandeur;
		} else {
			this.listeAttenteSectionCritique.add(urlDemandeur);
		}
		return used;
	}

	@Override
	public synchronized void signalerAutorisation(final String urlDemandeur) throws MalformedURLException, RemoteException, NotBoundException
	{
		// Récupérer l'interface de l'abris que l'on veut contacter
		AbriRemoteInterface abriRemote = (AbriRemoteInterface) Naming.lookup(urlDemandeur); // @@@ mais on l'avais déjà dans NoeudBackend :(

		// Lui indiquer qu'il a accès à la section critique
		try {
			abriRemote.recevoirSC();
		} catch ( AbriException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( NoeudCentralException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(this.urlNoeud + ": \tSignalement de l'autorisation");
	}

	@Override
	public String quitterSectionCritique(final String url) throws IllegalAccessException
	{
		String prochain = null;
		if ( !urlEnSC.equals(url) ) {
			throw new IllegalAccessException("Pas le bon abris qui demande à quitter la SC");
		}

		// Libère la SC
		used = false;
		urlEnSC = null;

		if ( !listeAttenteSectionCritique.isEmpty() ) {
			prochain = listeAttenteSectionCritique.getFirst();
		}
		return prochain;
	}

	@Override
	public void enregistrerControleur(final String urlDistant, final String groupe)
	{
		System.out.println(this.urlNoeud + ": \tEnregistrement du controleur " + urlDistant);
	}

	@Override
	public void supprimerControleur(final String urlDistant)
	{
		System.out.println(this.urlNoeud + ": \tSuppression du controleur " + urlDistant);
	}

}
