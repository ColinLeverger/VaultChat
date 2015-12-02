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
 * @author Maelig Nantel
 * @author Colin Leverger
 * 
 *         Classe permettant de communiquer de manière locale avec notre abri.
 *         Utilisée pour la gestion des vues et donc non liée à RMI. La vue gère
 *         les intéractions avec l'utilisateur. <br>
 *         Pour la documentation des méthodes, se référer à la classe
 *         d'implémentation.
 */
public interface AbriLocalInterface
{
	public String getUrl();

	public boolean estConnecte();

	public AnnuaireAbri getAnnuaire();

	public void connecterAbri() throws AbriException, RemoteException, MalformedURLException, NotBoundException, NoeudCentralException;

	public void demanderDeconexion() throws AbriException, RemoteException, MalformedURLException, NotBoundException, NoeudCentralException;

	public void emettreMessageDanger(String message) throws InterruptedException, RemoteException, AbriException, NoeudCentralException;

	public void attribuerGroupe(String groupe);

}
