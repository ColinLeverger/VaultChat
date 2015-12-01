/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class NoeudControleur implements ControleurInterface
{
	protected String notreURL;
	private LinkedList<String> listeAttenteSectionCritique; // On stock l'url des abris en attente
	private String urlEnSC; // Indique l'url de l'abri qui est acctuellement en section critique
	private AtomicBoolean used; // Indique si la section critique est acctuellement utilisée. Reviens à évaluer 'urlEnSC == null'

	public NoeudControleur(final String url)
	{
		this.listeAttenteSectionCritique = new LinkedList<>(); // Stucture FIFO
		this.notreURL = url;
		this.used = new AtomicBoolean(false);
	}

	@Override
	// renvoie vrai si la SC est immédiatement disponible
	public boolean demanderSectionCritique(final String urlDemandeur)
	{
		assert urlDemandeur != null;
		System.out.println("@@@@@@@@ appel de demander Section Critique avec pour urldemandeur --> " + urlDemandeur);

		if ( !used.get() ) { // Section critique est acctuellement libre
			used.set(true);
			setUrlEnSC(urlDemandeur);
		} else {
			synchronized ( listeAttenteSectionCritique ) {
				if ( !listeAttenteSectionCritique.contains(urlDemandeur) ) { // On n'ajoute dans la liste d'attente qu'une seule fois, on garde la date de la première demande (ordre dans la liste).
					this.listeAttenteSectionCritique.addFirst(urlDemandeur); // Ajout en tête de liste (FIFO)
				}
			}
		}
		System.out.println("@@@ retour de la methode Controleur#demanderSectionCritique --> " + used.get());
		return used.get();
	}

	@Override
	public String quitterSectionCritique(final String url) throws IllegalAccessException
	{
		// TODO: Trouver pourquoi urlEnSC est à null en entrée de la méthode pour un 2eme appel. Ne doit pas l'être !! (initialisé dans "demanderSC"
		System.out.println("ON VEUT QUITTER LA SECTION CRITIQUE --> ON EST : " + url + " ET EN SC IL Y A : " + this.urlEnSC);
		// On s'assure que la demande viens bien du bon abri (par acqui de conscience, ce cas ne doit pas être possible !)
		if ( !urlEnSC.equals(url) ) {
			throw new IllegalAccessException("Pas le bon abris qui demande à quitter la SC");
		}

		// Libère la SC
		System.out.println("@@@ on libère la sc dont on met à null, c'est normal");
		setUrlEnSC(null);

		// Regarde si quelqu'un attends la SC
		String prochain = null;
		synchronized ( listeAttenteSectionCritique ) {
			if ( !listeAttenteSectionCritique.isEmpty() ) {
				prochain = listeAttenteSectionCritique.getLast(); // On prends la fin de liste (FIFO)
			}
		}
		return prochain; // url du prochain abris à qui on doit donner la SC. Null si personne.
	}

	public void setUrlEnSC(final String urlEnSC)
	{
		System.out.println("@@@@@@@@ appel de setUrlEnSC avec pour valeur --> " + urlEnSC);
		this.urlEnSC = urlEnSC;
	}

}
