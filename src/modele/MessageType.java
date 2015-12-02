package modele;

public enum MessageType
{
	ENVOYER_SIGNALEMENT_DANGER(6),
	RECEVOIR_SIGNALEMENT_DANGER(6),

	RECEVOIR_SIGNALEMENT_EXISTENCE(9),
	ENVOYER_SIGNALEMENT_EXISTENCE(9),

	ENVOYER_SIGNALEMENT_CONNECTION(8),
	RECEVOIR_SIGNALEMENT_CONNECTION(8),

	RECEVOIR_SIGNALEMENT_DECONNECTION(10),
	ENVOYER_SIGNALEMENT_DECONNECTION(10); // Message du noeud vers un abri, donc pas de priorite.

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
