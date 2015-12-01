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
 * @author Gwenole Lecorve
 * @author David Guennec
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
		if ( !abrisDistants.containsKey(url) ) { // Association 1-1
			this.abrisDistants.put(url, groupe);
			notifierObservateurs();
		}
	}

	public Map<String, String> getAbrisDistants()
	{
		return this.abrisDistants;
	}

	protected void notifierObservateurs()
	{
		super.setChanged();
		notifyObservers();
	}

	//TODO jamais mise à jour...
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
