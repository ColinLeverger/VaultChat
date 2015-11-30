/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import modele.AbriException;
import modele.Message;
import modele.NoeudCentralException;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public interface NoeudCentralRemoteInterface extends Remote
{
	void modifierAiguillage(String depuisUrl, ArrayList<String> versListeUrl) throws RemoteException, NoeudCentralException;

	void transmettreMessage(Message message) throws RemoteException, AbriException, NoeudCentralException;

	void creerAbri(String url) throws RemoteException, NotBoundException, MalformedURLException;

	void demanderSectionCritique(String url) throws RemoteException;

	void quitterSectionCritique(String url) throws RemoteException;

	void deconnecterAbri(String url) throws RemoteException;

}
