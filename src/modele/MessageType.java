package modele;

/**
 *
 * Type énuméré permettant de décrire les différents types de messages échangés
 * sur le système. Cette classe ne prend pas en compte les messages liés à la
 * demande et à la sortie de section critique. Nous avons commencé à essayer de
 * gérer des priorités sur les messages, mais nous n'avons pas eu le temps
 * d'aboutir cette réalisation. Il faudrait que les messages de connections ou de
 * déconnexion soit plus prioritaire que les messages de signalement de danger
 * par exemple.
 *
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public enum MessageType
{
	SIGNALEMENT_DANGER(6),
	SIGNALEMENT_EXISTENCE(9),
	SIGNALEMENT_CONNECTION(8),
	SIGNALEMENT_DECONNECTION(10),

	SIGNALEMENT_AUTORISATION_SC(-1); // Message du noeud vers un abri, donc pas de priorite.

	private final int priorite;

	/**
	 * Plus la valeur de 'priorite' est élevée, plus le message est prioritaire
	 * dans la file d'envoie
	 **/
	MessageType(final int priorite)
	{
		this.priorite = priorite;
	}

	public int getPriorite()
	{
		return this.priorite;
	}
}
