package controle;

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
	 * Receptionne la notification du processus metier a sa sortie de la section
	 * critique
	 * 
	 * @return
	 * 
	 * @throws IllegalAccessException
	 */
	String quitterSectionCritique(String url) throws IllegalAccessException;

}
