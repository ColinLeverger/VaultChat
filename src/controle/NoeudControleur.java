/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.util.LinkedList;

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
		this.listeAttenteSectionCritique = new LinkedList<>();
		this.urlNoeud = url;
		this.used = false;
	}

	@Override
	// renvoie vrai si la SC est immédiatement disponible
	public synchronized boolean demanderSectionCritique(final String urlDemandeur)
	{
		assert urlDemandeur != null;
		System.out.println("@@@@@@@@ appel de demander Section Critique avec pour urldemandeur --> " + urlDemandeur);

		if ( !this.used ) {
			this.used = true;
			setUrlEnSC(urlDemandeur);
		} else {
			if ( !listeAttenteSectionCritique.contains(urlDemandeur) ) { // On n'ajoute dans la liste d'attente qu'une seule fois, on garde la date de la première demande (ordre dans la liste).
				this.listeAttenteSectionCritique.add(urlDemandeur);
				System.out.println(this.urlNoeud + ": \t  SC pas dispo, mise en attente.");
			}
		}
		return used;
	}

	@Override
	public String quitterSectionCritique(final String url) throws IllegalAccessException
	{
		// TODO: Trouver pourquoi urlEnSC est à null en entrée de la méthode pour un 2eme appel. Ne doit pas l'être !! (initialisé dans "demanderSC"
		System.out.println("ON VEUT QUITTER LA SECTION CRITIQUE --> ON EST : " + url + " ET EN SC IL Y A : " + this.urlEnSC);
		if ( !urlEnSC.equals(url) ) {
			throw new IllegalAccessException("Pas le bon abris qui demande à quitter la SC");
		}

		String prochain = null;
		if ( !listeAttenteSectionCritique.isEmpty() ) {
			prochain = listeAttenteSectionCritique.getFirst();
		}

		// Libère la SC
		System.out.println("@@@ on libère la sc dont on met à null, c'est normal");
		setUrlEnSC(null);

		return prochain;
	}

	public void setUrlEnSC(final String urlEnSC)
	{
		System.out.println("@@@@@@@@ appel de setUrlEnSC avec pour valeur --> " + urlEnSC);
		this.urlEnSC = urlEnSC;
	}

}
