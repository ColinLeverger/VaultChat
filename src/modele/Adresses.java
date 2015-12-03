/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modele;

/**
 * Classe contenant les arch�types des adresses RMI
 *
 * @author Gw�nol� Lecorv�
 * @author David Guennec
 */
public class Adresses
{

	private static final String noeud = "localhost"; // localhost // Par d�faut, le simulateur lance tous les sites sur la boucle locale
	private static final String abri = "localhost"; // localhost
	private static final int port = 2020;

	public static String archetypeAdresseAbri()
	{
		return "rmi://" + Adresses.abri + ":" + Adresses.port + "/";
	}

	public static String archetypeAdresseNoeudCentral()
	{
		return "rmi://" + Adresses.noeud + ":" + Adresses.port + "/";
	}

	Adresses()
	{
	}

}
