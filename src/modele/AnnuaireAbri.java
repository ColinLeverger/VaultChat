/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modele;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

/**
 * Classse permettant de modéliser un annuaire dans un abri. On ne doit pas
 * mémoriser d'accès aux autres AbriRemoteInterface comme c'était précédement le
 * cas, car nous voulons fonctionner en totalement centralisé. Nous avons
 * cependant besoin de mémoriser le groupe de chaque abri.
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class AnnuaireAbri extends Observable
{
	// Map<URL,Groupe>
	protected Map<String, String> abrisDistants;

	public AnnuaireAbri()
	{
		this.abrisDistants = new HashMap<>();
	}

	public void ajouterAbriDistant(final String url, final String groupe)
	{
		if ( !this.abrisDistants.containsKey(url) ) { // Association 1-1
			this.abrisDistants.put(url, groupe);
			notifierObservateurs();
		}
	}

	public Map<String, String> getAbrisDistants()
	{
		synchronized ( this.abrisDistants ) {
			return this.abrisDistants;
		}
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
