package controle;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Interface definissant les fonctionnalites attendues d'un controleur pour le
 * projet VaultChat
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public interface ControleurInterface
{

	/**
	 * Receptionne du demande d'entree en section critique de la part du
	 * processus metier
	 */
	boolean demanderSectionCritique(final String url);

	/**
	 * Signale l'autorisation d'entrer en section critique aupres du processus
	 * metier
	 */
	void signalerAutorisation(final String url) throws MalformedURLException, RemoteException, NotBoundException;

	/**
	 * Receptionne la notification du processus metier a sa sortie de la section
	 * critique
	 * 
	 * @return
	 * 
	 * @throws IllegalAccessException
	 */
	String quitterSectionCritique(String url) throws IllegalAccessException;

	/**
	 * Enregistre l'URL d'un controleur distant
	 *
	 * @param urlDistant
	 *            l'URL a memoriser
	 * @param groupe
	 *            le groupe auquel appartient l'abri
	 */
	void enregistrerControleur(String urlDistant, String groupe);

	/**
	 * Oublie l'URL d'un controleur distant
	 *
	 * @param urlDistant
	 *            l'URL a oublier
	 */
	void supprimerControleur(String urlDistant);

}
