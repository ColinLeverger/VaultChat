/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vue;

import controle.AbriLocalInterface;
import modele.Abri;
import modele.AnnuaireAbri;
import modele.Message;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

/**
 *
 * @author Gwenole Lecorve
 * @author David Guennec
 * @author Maelig Nantel
 * @author Colin Leverger
 */
public class AbriVue extends JFrame implements Observer
{

	protected class EmissionThread extends Thread
	{

		JFrame parent;

		EmissionThread(final JFrame parent)
		{
			this.parent = parent;
		}

		@Override
		public void run()
		{
			while ( AbriVue.this.emettreEnBoucle ) {
				try {
					AbriVue.this.backend.emettreMessageDanger(AbriVue.this.emissionTextArea.getText());
				} catch ( Exception ex ) {
					//ex.printStackTrace();
					//new ErrorDialog(this.parent, "Erreur lors de l'emission du message", ex.getMessage());//TODO Maëlig
				}
			}
		}
	}

	/**
	 *
	 */
	private static final long serialVersionUID = -585462256022303529L;
	private AbriLocalInterface backend;

	private boolean emettreEnBoucle;

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton connectionBouton;

	private javax.swing.JToggleButton demarrerToggleBouton;

	private javax.swing.JList destinataireList;

	private javax.swing.JButton emettreBouton;

	private javax.swing.JPanel emissionPanel;

	private javax.swing.JScrollPane emissionScrollPane;

	private javax.swing.JTextArea emissionTextArea;

	private javax.swing.JLabel etatLabel;

	private javax.swing.JList groupsList;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JScrollPane listeScrollPane;
	private javax.swing.JPanel receptionPanel;
	private javax.swing.JScrollPane receptionScrollPane;
	private javax.swing.JTextPane receptionTextPane;
	private javax.swing.JPanel statutPanel;
	private javax.swing.JButton stopperBouton;
	private javax.swing.JTextField urlTextField;

	// End of variables declaration//GEN-END:variables
	/**
	 * Creates new form AbriFenetre
	 *
	 * @param backend
	 *            Traitements en arri�re-plan
	 * @param abri
	 *            Abri
	 */
	public AbriVue(final AbriLocalInterface backend, final Abri abri)
	{
		this.backend = backend;
		this.emettreEnBoucle = false;

		initComponents();

		setResizable(false);
		setTitle("Abri - " + backend.getUrl());
		this.urlTextField.setText(backend.getUrl());
		this.etatLabel.setText("<html><a color=red>Deconnecte</a></html>");

		this.emettreBouton.setEnabled(false);
		this.demarrerToggleBouton.setEnabled(false);
		this.stopperBouton.setEnabled(false);
		this.destinataireList.setEnabled(false);

		DefaultCaret caret = (DefaultCaret) this.receptionTextPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		abri.addObserver(this);
		backend.getAnnuaire().addObserver(this);

		abri.definirGroupe((String) this.groupsList.getSelectedValue());

		setVisible(true);
	}

	/**
	 * Affiche une bo�te de dialogue correspondant � une erreur
	 *
	 * @param titre
	 *            Titre de la bo�te de dialogue
	 * @param contenu
	 *            D�tail du message d'erreur
	 */
	public void afficherErreur(final String titre, final String contenu)
	{
		new ErrorDialog(this, titre, contenu);
	}

	protected void ajouterTexteRecu(final String texte)
	{
		try {
			HTMLDocument doc = (HTMLDocument) this.receptionTextPane.getDocument();
			((HTMLEditorKit) this.receptionTextPane.getEditorKitForContentType("text/html")).insertHTML(doc, doc.getLength(), texte, 0, 0, null);
		} catch ( Exception ex ) {
			ex.printStackTrace();
			afficherErreur("Erreur lors de l'affichage d'un message recu", ex.getMessage());
		}
	}

	protected void ajusterEtatBoutons()
	{
		// Non connecte ou message a envoyer vide ou aucun destinataire existant
		if ( !this.backend.estConnecte() || this.emissionTextArea.getText().equals("") || this.destinataireList.getModel().getSize() == 0 ) {
			this.emettreBouton.setEnabled(false);
			this.demarrerToggleBouton.setSelected(false);
			this.demarrerToggleBouton.setEnabled(false);
			this.stopperBouton.setEnabled(false);
			this.destinataireList.setEnabled(false);
		} // Connecte et message a envoyer non vide et au moins un destinataire et envoi en boucle actif
		else if ( this.emettreEnBoucle ) {
			this.emettreBouton.setEnabled(false);
			this.demarrerToggleBouton.setSelected(true);
			this.demarrerToggleBouton.setEnabled(false);
			this.stopperBouton.setEnabled(true);
			this.destinataireList.setEnabled(false);
		} // Connecte et message a envoyer non vide et au moins un destinataire et envoi en boucle non actif
		else {
			this.emettreBouton.setEnabled(true);
			this.demarrerToggleBouton.setSelected(false);
			this.demarrerToggleBouton.setEnabled(true);
			this.stopperBouton.setEnabled(false);
			this.destinataireList.setEnabled(true);
		}
	}

	private void connectionBoutonActionPerformed(final java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_connectionBoutonActionPerformed
		if ( this.backend.estConnecte() ) {
			try {
				this.backend.demanderDeconexion();
				this.etatLabel.setText("<html><a color=red>Deconnecte</a></html>");
				this.connectionBouton.setText("Connecter l'abri au reseau");
				if ( this.emettreEnBoucle ) {
					this.emettreEnBoucle = false;
					afficherErreur("Arret de l'emission", "L'envoi en boucle d'un message a ete interrompu en raison d'une deconnexion.");
				}
				ajusterEtatBoutons();
			} catch ( Exception ex ) {
				ex.printStackTrace();
				afficherErreur("Erreur de deconnexion", ex.getMessage());
			}
		} else {
			try {
				this.backend.attribuerGroupe((String) this.groupsList.getSelectedValue());
				this.backend.demanderConnection();
				this.etatLabel.setText("<html><a color=green>Connecte</a></html>");
				this.connectionBouton.setText("Deconnecter l'abri du reseau");
				ajusterEtatBoutons();
			} catch ( Exception ex ) {
				ex.printStackTrace();
				afficherErreur("Erreur de connexion", ex.getMessage());
			}
		}
	}//GEN-LAST:event_connectionBoutonActionPerformed

	private void demarrerToggleBoutonActionPerformed(final java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_demarrerToggleBoutonActionPerformed
		this.emettreEnBoucle = true;
		Thread t;
		t = new EmissionThread(this);
		t.start();
		ajusterEtatBoutons();
	}//GEN-LAST:event_demarrerToggleBoutonActionPerformed

	private void emettreBoutonActionPerformed(final java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_emettreBoutonActionPerformed
		try {
			this.backend.emettreMessageDanger(this.emissionTextArea.getText());
		} catch ( Exception ex ) {
			//ex.printStackTrace();
			//afficherErreur("Erreur lors de l'emission du message", ex.getMessage()); //TODO Maëlig
		}
	}//GEN-LAST:event_emettreBoutonActionPerformed

	private void emissionTextAreaKeyReleased(final java.awt.event.KeyEvent evt)
	{//GEN-FIRST:event_emissionTextAreaKeyReleased
		if ( this.emissionTextArea.getText().equals("") ) {
			afficherErreur("Arret de l'emission", "L'envoi en boucle d'un message a ete interrompu en raison d'un message vide.");
			this.emettreEnBoucle = false;
		}
		ajusterEtatBoutons();
	}//GEN-LAST:event_emissionTextAreaKeyReleased

	private void formWindowClosing(final java.awt.event.WindowEvent evt)
	{//GEN-FIRST:event_formWindowClosing
		try {
			this.backend.demanderDeconexion();
		} catch ( Exception ex ) {
			ex.printStackTrace();
			afficherErreur("Erreur lors de la fermeture", ex.getMessage());
		}
	}//GEN-LAST:event_formWindowClosing

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
	private void initComponents()
	{

		this.receptionPanel = new javax.swing.JPanel();
		this.receptionScrollPane = new javax.swing.JScrollPane();
		this.receptionTextPane = new javax.swing.JTextPane();
		this.emissionPanel = new javax.swing.JPanel();
		this.emettreBouton = new javax.swing.JButton();
		this.emissionScrollPane = new javax.swing.JScrollPane();
		this.emissionTextArea = new javax.swing.JTextArea();
		this.listeScrollPane = new javax.swing.JScrollPane();
		this.destinataireList = new javax.swing.JList();
		this.destinataireList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		this.demarrerToggleBouton = new javax.swing.JToggleButton();
		this.stopperBouton = new javax.swing.JButton();
		this.statutPanel = new javax.swing.JPanel();
		this.etatLabel = new javax.swing.JLabel();
		this.connectionBouton = new javax.swing.JButton();
		this.urlTextField = new javax.swing.JTextField();
		this.jScrollPane1 = new javax.swing.JScrollPane();
		this.groupsList = new javax.swing.JList();
		this.jSeparator1 = new javax.swing.JSeparator();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("Abri");
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(final java.awt.event.WindowEvent evt)
			{
				formWindowClosing(evt);
			}
		});

		this.receptionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Reception", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

		this.receptionTextPane.setContentType("text/html"); // NOI18N
		this.receptionTextPane.setDocument(new HTMLDocument());
		this.receptionScrollPane.setViewportView(this.receptionTextPane);

		javax.swing.GroupLayout receptionPanelLayout = new javax.swing.GroupLayout(this.receptionPanel);
		this.receptionPanel.setLayout(receptionPanelLayout);
		receptionPanelLayout.setHorizontalGroup(receptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(this.receptionScrollPane, javax.swing.GroupLayout.Alignment.TRAILING));
		receptionPanelLayout.setVerticalGroup(receptionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(this.receptionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE));

		this.emissionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Emission", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

		this.emettreBouton.setText("Emettre une fois");
		this.emettreBouton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt)
			{
				emettreBoutonActionPerformed(evt);
			}
		});

		this.emissionTextArea.setColumns(20);
		this.emissionTextArea.setRows(5);
		this.emissionTextArea.setText("Message (ex : Attaque de radcafards)");
		this.emissionTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
			@Override
			public void keyReleased(final java.awt.event.KeyEvent evt)
			{
				emissionTextAreaKeyReleased(evt);
			}
		});
		this.emissionScrollPane.setViewportView(this.emissionTextArea);

		this.destinataireList.setModel(new SortedListModel());
		this.destinataireList.setEnabled(false);
		this.listeScrollPane.setViewportView(this.destinataireList);

		this.demarrerToggleBouton.setText("Demarrer l'emission");
		this.demarrerToggleBouton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt)
			{
				demarrerToggleBoutonActionPerformed(evt);
			}
		});

		this.stopperBouton.setText("Stopper l'emission");
		this.stopperBouton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt)
			{
				stopperBoutonActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout emissionPanelLayout = new javax.swing.GroupLayout(this.emissionPanel);
		this.emissionPanel.setLayout(emissionPanelLayout);
		emissionPanelLayout.setHorizontalGroup(emissionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		        .addGroup(emissionPanelLayout.createSequentialGroup().addComponent(this.listeScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
		                .addGroup(emissionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(this.emettreBouton, javax.swing.GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
		                        .addComponent(this.demarrerToggleBouton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		                        .addComponent(this.stopperBouton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
		        .addComponent(this.emissionScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 461, Short.MAX_VALUE));
		emissionPanelLayout.setVerticalGroup(emissionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
		        emissionPanelLayout.createSequentialGroup().addComponent(this.emissionScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
		                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		                .addGroup(emissionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
		                        .addGroup(emissionPanelLayout.createSequentialGroup().addComponent(this.emettreBouton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(this.demarrerToggleBouton)
		                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(this.stopperBouton))
		                .addComponent(this.listeScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(18, 18, 18)));

		this.statutPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Statut", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.DEFAULT_POSITION));

		this.etatLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		this.etatLabel.setText("Non connecte");

		this.connectionBouton.setText("Connecter l'abri au reseau");
		this.connectionBouton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt)
			{
				connectionBoutonActionPerformed(evt);
			}
		});

		this.urlTextField.setEditable(false);
		this.urlTextField.setHorizontalAlignment(SwingConstants.CENTER);
		this.urlTextField.setText("rmi://url");

		this.groupsList.setModel(new javax.swing.AbstractListModel() {
			/**
			 *
			 */
			private static final long serialVersionUID = -2523435975157370141L;
			String[] strings = { "Groupe 1", "Groupe 2" };

			@Override
			public Object getElementAt(final int i)
			{
				return this.strings[i];
			}

			@Override
			public int getSize()
			{
				return this.strings.length;
			}
		});
		this.groupsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		this.groupsList.setToolTipText("");
		this.groupsList.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
		this.groupsList.setSelectedIndex(0);
		this.jScrollPane1.setViewportView(this.groupsList);

		this.jSeparator1.setToolTipText("Groupe de danger");
		this.jSeparator1.setName("Groupe de dangers"); // NOI18N

		javax.swing.GroupLayout statutPanelLayout = new javax.swing.GroupLayout(this.statutPanel);
		this.statutPanel.setLayout(statutPanelLayout);
		statutPanelLayout.setHorizontalGroup(statutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		        .addGroup(statutPanelLayout.createSequentialGroup().addContainerGap()
		                .addGroup(statutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(this.jSeparator1).addComponent(this.jScrollPane1)
		                        .addComponent(this.urlTextField, javax.swing.GroupLayout.Alignment.TRAILING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
		                                statutPanelLayout.createSequentialGroup().addGap(12, 12, 12).addComponent(this.etatLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 169, Short.MAX_VALUE)
		                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(this.connectionBouton, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)))
		        .addContainerGap()));
		statutPanelLayout.setVerticalGroup(statutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
		        .addGroup(statutPanelLayout.createSequentialGroup().addComponent(this.urlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
		                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(this.jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
		                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(this.jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18)
		                .addGroup(statutPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(this.etatLabel).addComponent(this.connectionBouton))
		                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(this.receptionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		        .addComponent(this.emissionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
		        .addComponent(this.statutPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
		        layout.createSequentialGroup().addComponent(this.statutPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
		                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(this.receptionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
		                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(this.emissionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)));

		this.receptionPanel.getAccessibleContext().setAccessibleName("Messages recus");

		pack();
	}// </editor-fold>//GEN-END:initComponents

	protected void remplirListe(final AnnuaireAbri annuaire)
	{

		ArrayList<String> selection = new ArrayList<String>();

		if ( !this.destinataireList.isSelectionEmpty() ) {
			selection = (ArrayList<String>) this.destinataireList.getSelectedValuesList();
		}

		this.destinataireList.removeAll();
		SortedListModel listModel = (SortedListModel) this.destinataireList.getModel();
		listModel.clear();
		for ( String url : annuaire.getAbrisDistants().keySet() ) {
			listModel.add(url);
		}
		this.destinataireList.setModel(listModel);

		try {
			int[] tabSelectedIndices = new int[selection.size()];
			for ( int i = 0; i < selection.size(); i++ ) {
				tabSelectedIndices[i] = listModel.getElementIndex(selection.get(i));

			}
			this.destinataireList.setSelectedIndices(tabSelectedIndices);
		} catch ( Exception ex ) {
			if ( selection == null || listModel.getSize() > 0 ) {
				this.destinataireList.setSelectedIndex(0);
			}
		}
		ajusterEtatBoutons();
	}

	private void stopperBoutonActionPerformed(final java.awt.event.ActionEvent evt)
	{//GEN-FIRST:event_stopperBoutonActionPerformed
		this.emettreEnBoucle = false;
		ajusterEtatBoutons();
	}//GEN-LAST:event_stopperBoutonActionPerformed

	@Override
	public void update(final Observable o, final Object arg)
	{
		if ( o instanceof Abri ) {
			for ( Message message : ((Abri) o).lireTampon() ) {
				ajouterTexteRecu(message.toHTML());
			}
			((Abri) o).viderTampon();
		} else if ( o instanceof AnnuaireAbri ) {
			remplirListe((AnnuaireAbri) o);
		}
		repaint();
	}
}
