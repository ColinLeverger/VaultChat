/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contrôleur chargé de gérer la section critique sur le noeud central. Ce
 * controleur est unique dans le système et se situe sur le noeud central. Il
 * est chargé d'éxécuté notre algorithme d'exclusion mutuelle. Nous avons
 * choisis d'implémenter la gestion de la section critique sous la forme d'une
 * file FIFO.
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class SectionCritiqueControleurFIFO implements ControleurInterface
{
	private final String noeudURL;

	/**
	 * Liste modélisant une structure FIFO pour gérer les demandes de section
	 * critique. On ajoute en tête (first) et on prends en queu (last).
	 */
	private LinkedList<String> listeAttente;

	/**
	 * Mémorise l'adresse de l'abri qui est acctuellement dans la section
	 * critique. Null si la section critique est libre.
	 */
	private String urlEnSC;

	/**
	 * La classe AtomicBoolean permet de modéliser un type booleen sécurisé
	 * quand aux accès concurents. Ici on mémorise l'état de la section critique
	 * (utilisée ou non à un instant donné). Reviens également à savoir si
	 * 'urlEnSC == null'.
	 */
	private AtomicBoolean used;

	// ===========
	// CONSTRUCTOR
	// ===========

	public SectionCritiqueControleurFIFO(final String noeudURL)
	{
		this.listeAttente = new LinkedList<>();
		this.noeudURL = noeudURL;
		this.used = new AtomicBoolean(false); // Au lancement, la section critique est disponible.
	}

	/**
	 * Retourne vrai si la section critique est immédiatement disponible, faux
	 * sinon. Si la section critique est disponible, on la passe en non-libre et
	 * on mémorise l'abri utilisateur. Si elle n'est pas disponible, on ajoute
	 * l'abri demandeur dans la liste d'attente (si il ne l'est pas déjà).
	 */
	@Override
	public synchronized boolean demanderSectionCritique(final String urlDemandeur)
	{
		assert urlDemandeur != null;
		if ( !this.used.get() ) { // Section critique est acctuellement libre
			this.used.set(true);
			setUrlEnSC(urlDemandeur);
		} else {
			//	synchronized ( listeAttente ) { // Protège d'accès concurents sur la liste d'attente
			// Maximum 1 fois dans la liste d'attente. Ne doit pas arriver car cas déjà géré dans l'abris (boolean demandeSC)
			if ( !this.listeAttente.contains(urlDemandeur) ) {
				this.listeAttente.addFirst(urlDemandeur);
			}
			//	}
		}
		return this.used.get();
	}

	public synchronized String getNoeudURL()
	{
		return this.noeudURL;
	}

	// =================
	// GETTERS / SETTERS
	// =================

	/**
	 * Libère la section critique et retourne l'adresse du prochain abri à qui
	 * il faut donner la section critique. Retourne 'null' si personne n'est en
	 * attente de la section critique au moment où la quitte.
	 */
	@Override
	public synchronized String quitterSectionCritique(final String urlDemandeur) throws IllegalAccessException
	{
		// On s'assure que la demande viens bien du bon abri (par acquis de conscience, ce cas ne doit pas être possible !)
		if ( !this.urlEnSC.equals(urlDemandeur) ) {
			throw new IllegalAccessException("Pas le bon abris qui demande à quitter la SC");
		}

		setUrlEnSC(null);

		// Regarde si quelqu'un attends la SC
		String prochain = null;
		//synchronized ( listeAttente ) {
		if ( !this.listeAttente.isEmpty() ) {
			prochain = this.listeAttente.removeLast(); // removeLast pour récupérer tout en retirant de la liste
			//	}
		}
		return prochain;
	}

	public synchronized void setUrlEnSC(final String urlEnSC)
	{
		this.urlEnSC = urlEnSC;
	}

}
