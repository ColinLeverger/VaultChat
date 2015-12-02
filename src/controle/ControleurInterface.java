package controle;

/**
 * Interface definissant les fonctionnalites nécéssaires à la gestion de la
 * section critique. Notre conception ne permet que de demander ou de libérer la
 * section critique. Le signalement de l'autorisation est directement éffectué
 * dans le NoeudCentralBackend étant donné qu'il a déjà un accès vers les
 * AbriRemoteInterface. On évite de ce fait de faire un accès l'annuaire RMI.
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public interface ControleurInterface
{
	/*
	 * Nous avons été obligés de modifier les prototypes du sujet afin d'adapter
	 * le code fournis à notre conception. Nous avons rajouté l'adresse URL de
	 * l'abri emetteur de la requête.
	 */

	boolean demanderSectionCritique(final String url);

	String quitterSectionCritique(String url) throws IllegalAccessException;

}
