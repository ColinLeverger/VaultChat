/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modele;

import java.util.List;
import java.util.Observable;

/**
 * Classe qui simule le noeud central du r�seau via l'abri entrants et les abris
 * sortants
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class NoeudCentral extends Observable
{
	protected String depuisUrl;
	protected List<String> versUrl; // Passage d'une ArrayList à une List (en gardant l'implémentation d'une ArrayList) (bonnes pratiques Java)
	protected boolean transmissionEnCours;

	public void demarrerTransmission()
	{
		this.transmissionEnCours = true;
		notifierObservateurs();
	}

	public String getDepuisUrl() throws NoeudCentralException
	{
		if ( this.depuisUrl == null ) {
			throw new NoeudCentralException("Le noeud central n'est configure pour aucun emetteur.");
		}
		return this.depuisUrl;
	}

	public List<String> getVersUrl() throws NoeudCentralException
	{
		if ( this.versUrl == null ) {
			throw new NoeudCentralException("Le noeud central n'est configure pour aucun destinataire.");
		}
		return this.versUrl;
	}

	protected void notifierObservateurs()
	{
		super.setChanged();
		notifyObservers();
	}

	public void reconfigurerAiguillage(final String _depuisUrl, final List<String> _versUrl) throws NoeudCentralException
	{
		if ( this.transmissionEnCours ) {
			throw new NoeudCentralException("Impossible de modifier la configuration du noeud central lorsqu'une transmission est en cours");
		}
		this.depuisUrl = _depuisUrl;
		this.versUrl = _versUrl;
		notifierObservateurs();
	}

	public void stopperTransmission()
	{
		this.transmissionEnCours = false;
		notifierObservateurs();
	}

	public boolean tranmissionEnCours()
	{
		return this.transmissionEnCours;
	}

}
