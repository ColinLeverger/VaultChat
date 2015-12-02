/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.rmi.Remote;
import java.rmi.RemoteException;

import modele.AbriException;
import modele.Message;
import modele.NoeudCentralException;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public interface NoeudCentralRemoteInterface extends Remote
{
	//void modifierAiguillage(String depuisUrl, List<String> versListeUrl) throws RemoteException, NoeudCentralException;

	void transmettreMessage(Message message) throws RemoteException, AbriException, NoeudCentralException;

	//void connexionAbri(String urlAbri, String groupeAbri) throws RemoteException, NotBoundException, MalformedURLException, AbriException, NoeudCentralException;

	void demanderSectionCritique(String url) throws RemoteException, AbriException, NoeudCentralException;

	void quitterSectionCritique(String url) throws RemoteException, AbriException, NoeudCentralException;

	//void deconnecterAbri(String url) throws RemoteException, AbriException, NoeudCentralException;

}
