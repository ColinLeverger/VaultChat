/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modele;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import controle.AbriRemoteInterface;

/**
 * Anciennement nommé "Annuaire". Modélise l'annuaire du noeud central. L'accès
 * à cet annuaire est protégé contre les accès concurents à l'aide de blocs
 * 'synchronized'.
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class AnnuaireNoeudCentral extends Observable
{
	// Map<URL,AbriRemoteInterface>
	protected Map<String, AbriRemoteInterface> abrisDistants;

	public AnnuaireNoeudCentral()
	{
		this.abrisDistants = new HashMap<>();
	}

	public void ajouterAbriDistant(final String url, final AbriRemoteInterface abri)
	{
		this.abrisDistants.put(url, abri);
		notifierObservateurs();
	}

	public AbriRemoteInterface chercherUrl(final String urlDistant) throws AbriException
	{
		AbriRemoteInterface abri = this.abrisDistants.get(urlDistant);
		if ( abri == null ) {
			throw new AbriException("Abri " + urlDistant + " introuvable dans l'annuaire local.");
		} else {
			return abri;
		}
	}

	public Map<String, AbriRemoteInterface> getAbrisDistants()
	{
		return this.abrisDistants;
	}

	protected void notifierObservateurs()
	{
		super.setChanged();
		notifyObservers();
	}

	public void retirerAbriDistant(final String url)
	{
		this.abrisDistants.remove(url);
		notifierObservateurs();
	}

	public void vider()
	{
		this.abrisDistants.clear();
		notifierObservateurs();
	}

}
