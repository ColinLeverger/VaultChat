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
import java.util.List;

import modele.AbriException;
import modele.Message;
import modele.NoeudCentralException;

/**
 * Classe modélisant les appels pouvant être éffectués sur le noeud central
 * depuis un abri du réseau via Java RMI.
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public interface NoeudCentralRemoteInterface extends Remote
{
	void connexionAbri(String urlNouveau, String groupeNouveau) throws RemoteException, NotBoundException, MalformedURLException, AbriException, NoeudCentralException, IllegalAccessException;

	void deconnecterAbri(String urlAbriDeconnecte) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException;

	void demanderSectionCritique(String urlAbriDemandeur) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException;

	void modifierAiguillage(String depuisUrl, List<String> versListeUrl) throws RemoteException, NoeudCentralException;

	void quitterSectionCritique(String urlAbriDemandeur) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException;

	void transmettreMessage(Message message) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException;

}
