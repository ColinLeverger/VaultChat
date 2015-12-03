/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modele;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Classe représentant une information envoyée par un abri vers un autre
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class Message implements Serializable
{
	private static final long serialVersionUID = 528762786790366521L;
	protected String urlEmetteur;
	protected List<String> urlDestinataire;
	protected String contenu; // N'a de sens que pour un message de type "SIGNALEMENT_DANGER"
	protected String timestamp;
	protected final MessageType type;

	public Message(final String _urlEmetteur, final List<String> _urlDestinataire, final MessageType type)
	{
		this(_urlEmetteur, _urlDestinataire, null, type);
	}

	public Message(final String _urlEmetteur, final List<String> _urlDestinataire, final String _contenu, final MessageType type)
	{
		this.urlEmetteur = _urlEmetteur;
		this.urlDestinataire = _urlDestinataire;
		this.type = type;
		this.timestamp = new Timestamp(System.currentTimeMillis()).toString();
		this.contenu = _contenu;
	}

	/**
	 * Constructeur permettant d'envoyer un message à un seul destinataire, sans
	 * contenu.
	 *
	 * @param _urlEmetteur
	 * @param _urlDestinataire
	 * @param type
	 */
	public Message(final String _urlEmetteur, final String _urlDestinataire, final MessageType type)
	{
		this(_urlEmetteur, Arrays.asList(_urlDestinataire), type);

	}

	/**
	 * Constructeur permettant d'envoyer un message à un seul destinataire, avec
	 * contenu.
	 *
	 * @param _urlEmetteur
	 * @param _urlDestinataire
	 * @param contenu
	 * @param type
	 */
	public Message(final String _urlEmetteur, final String _urlDestinataire, final String contenu, final MessageType type)
	{
		this(_urlEmetteur, Arrays.asList(_urlDestinataire), contenu, type);
	}

	public String getContenu()
	{
		return this.contenu;
	}

	public String getTimestamp()
	{
		return this.timestamp;
	}

	public MessageType getType()
	{
		return this.type;
	}

	public List<String> getUrlDestinataire()
	{
		return this.urlDestinataire;
	}

	public String getUrlEmetteur()
	{
		return this.urlEmetteur;
	}

	public String toHTML()
	{
		return "<u>A " + this.timestamp + "<br>De <b>" + this.urlEmetteur + "</b><br>Pour " + this.urlDestinataire + "</u>:<br>" + this.contenu + "<hr>";
	}

	@Override
	public String toString()
	{
		return "A " + this.timestamp + ", de " + this.urlEmetteur + " pour " + this.urlDestinataire + ": " + this.contenu;
	}

}
