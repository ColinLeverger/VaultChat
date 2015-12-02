/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.rmi.Remote;
import java.rmi.RemoteException;

import modele.AbriException;
import modele.NoeudCentralException;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public interface AbriRemoteInterface extends Remote
{
	void recevoirMessage(modele.Message transmission) throws RemoteException, AbriException, NoeudCentralException;
}
