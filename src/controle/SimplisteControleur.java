/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * @author Gwenole Lecorve
 * @author David Guennec
 */
public class SimplisteControleur implements ControleurInterface {

    protected String url;
    protected AbriLocalInterface abri;

    public SimplisteControleur(String url, AbriLocalInterface abri) {
        this.url = url;
        this.abri = abri;
    }


    public void demanderSectionCritique() {
        System.out.println(this.url + ": \tDemande de section critique enregistr�e");
        signalerAutorisation();
    }


    public synchronized void signalerAutorisation() {
        System.out.println(this.url + ": \tSignalement de l'autorisation");
        abri.recevoirAutorisation();
    }

    @Override
    public void demanderSectionCritique(String url) {

    }

    @Override
    public void signalerAutorisation(String url) throws MalformedURLException, RemoteException, NotBoundException {

    }

    @Override
    public void quitterSectionCritique() {
        System.out.println(this.url + ": \tFin de section critique");
    }

    @Override
    public void enregistrerControleur(String urlDistant, String groupe) {
        System.out.println(this.url + ": \tEnregistrement du controleur " + urlDistant);
    }

    @Override
    public void supprimerControleur(String urlDistant) {
        System.out.println(this.url + ": \tSuppression du controleur " + urlDistant);
    }

}
