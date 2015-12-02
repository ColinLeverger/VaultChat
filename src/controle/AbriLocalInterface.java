/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import modele.AbriException;
import modele.AnnuaireAbri;
import modele.NoeudCentralException;

/**
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public interface AbriLocalInterface
{
	public String getUrl();

	public boolean estConnecte();

	public AnnuaireAbri getAnnuaire();

	public void connecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException;

	public void deconnecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException;

	public void emettreMessageDanger(String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException;

	public void attribuerGroupe(String groupe);

}
