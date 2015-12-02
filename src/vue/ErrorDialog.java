/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vue;

import java.awt.Component;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JOptionPane;

/**
 * Classe interne pour l'affichage d'un message d'erreur
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 */
class ErrorDialog
{

	protected static Lock verrou = new ReentrantLock();

	protected static synchronized void singletonRun(Component _parentComponent, String _title, String _content)
	{
		JOptionPane.showMessageDialog(_parentComponent, _content, _title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Constructeur
	 *
	 * @param _parentComponent
	 *            Composant parent de la bo�te de dialogue
	 * @param _title
	 *            Titre
	 * @param _content
	 *            D�tail du message d'erreur
	 */
	protected ErrorDialog(final Component _parentComponent, final String _title, final String _content)
	{
		Thread t = new Thread(new Runnable() {
			@Override
			public void run()
			{
				singletonRun(_parentComponent, _title, _content);
				System.exit(1);
			}
		});
		t.start();
	}
}
