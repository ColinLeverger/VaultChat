/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.LinkedList;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class NoeudControleur implements ControleurInterface {

    protected String urlNoeud;
    protected AbriLocalInterface abri;
    private LinkedList<String> listeAttenteSectionCritique; // On stock l'url des abris en attente
    private String urlEnSC;
    private boolean used;


    public NoeudControleur(String url, AbriLocalInterface abri) {
        this.urlNoeud = url;
        this.abri = abri;
    }

    @Override
    public void demanderSectionCritique(final String urlDemandeur) {
        System.out.println(this.urlNoeud + ": \tDemande de section critique enregistree");

        if (!this.used) {
            this.used = true;
            this.urlEnSC = urlDemandeur;
            try {
                signalerAutorisation(urlDemandeur);
                //TODO
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        } else {
            this.listeAttenteSectionCritique.add(urlDemandeur);
        }
    }

    public synchronized void signalerAutorisation(final String urlDemandeur) throws MalformedURLException, RemoteException, NotBoundException {
        // Récupérer l'interface de l'abris que l'on veut contacter
        AbriRemoteInterface abriRemote = (AbriRemoteInterface) Naming.lookup(urlDemandeur); // @@@ mais on l'avais déjà dans NoeudBackend :(

        // Lui indiquer qu'il a accès à la section critique
        abriRemote.recevoirSC();

        System.out.println(this.urlNoeud + ": \tSignalement de l'autorisation");
    }

    @Override
    public void quitterSectionCritique() {
        System.out.println(this.urlNoeud + ": \tFin de section critique");
    }

    @Override
    public void enregistrerControleur(String urlDistant, String groupe) {
        System.out.println(this.urlNoeud + ": \tEnregistrement du controleur " + urlDistant);
    }

    @Override
    public void supprimerControleur(String urlDistant) {
        System.out.println(this.urlNoeud + ": \tSuppression du controleur " + urlDistant);
    }

}
