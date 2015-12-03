/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modele;

import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

/**
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class Abri extends Observable
{

	protected boolean connecte;
	protected List<Message> tampon;
	protected String groupe;

	public Abri()
	{
		this.connecte = false;
		this.tampon = new LinkedList<>();
		this.groupe = "";
	}

	public void connecter()
	{
		this.connecte = true;
	}

	public void deconnecter()
	{
		this.connecte = false;
	}

	public void definirGroupe(final String groupe)
	{
		this.groupe = groupe;
	}

	public String donnerGroupe()
	{
		return this.groupe;
	}

	public boolean estConnecte()
	{
		return this.connecte;
	}

	public List<Message> lireTampon()
	{
		return this.tampon;
	}

	public void memoriserMessageRecu(final Message message)
	{
		this.tampon.add(message);
		notifierObservateurs();
	}

	protected void notifierObservateurs()
	{
		super.setChanged();
		notifyObservers();
	}

	public void viderTampon()
	{
		this.tampon.clear();
	}

}
