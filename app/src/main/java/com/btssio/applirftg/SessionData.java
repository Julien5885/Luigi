package com.btssio.applirftg;

import java.util.ArrayList;

// Cette classe stocke les données de session partagées pour l'application.
// Ici, elle contient une liste statique (recentRentalIds) qui garde en mémoire
// les identifiants de location (rental_id) des films récemment loués pendant la session en cours.
public class SessionData {
    // ArrayList statique pour stocker les rental_ids créés lors de la session.
    // Cette liste permet de filtrer les locations récentes dans l'activité récapitulative.
    public static ArrayList<Integer> recentRentalIds = new ArrayList<>();
}
