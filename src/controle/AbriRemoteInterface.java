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
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 *
 *         Classe représentant les appels distants pouvant être effectués sur un
 *         abri à travers Java RMI. Dans notre conception, nous partons du
 *         principe que la seule interaction entrante possible est la réception
 *         d'un 'message'. Le traitement à appliquer va alors différer en
 *         fonction du type de message recu (voir implémentation). <br>
 *         Cette interface doit être publiée dans l'annuaire RMI à l'aide d'une
 *         adresse unique afin d'être récupérée par d'autres sites distants.
 *         C'est Java RMI qui s'occupent de gérer l'instance de cette interface.
 *         Pour la documentation des méthodes, se référer à la classe
 *         d'implémentation.
 */
public interface AbriRemoteInterface extends Remote
{
	void recevoirMessage(modele.Message transmission) throws RemoteException, AbriException, NoeudCentralException, IllegalAccessException;
}
