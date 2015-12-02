package modele;

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
		return priorite;
	}
}
