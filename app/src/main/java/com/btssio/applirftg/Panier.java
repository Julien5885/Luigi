package com.btssio.applirftg;

import java.util.ArrayList;
import java.util.List;
public class Panier {
    private static final List<String> films = new ArrayList<>();

    public static void ajouterFilm(String titre) {
        if (!films.contains(titre)) {
            films.add(titre);
        }
    }

    public static List<String> getFilms() {
        return new ArrayList<>(films);
    }

    public static void vider() {
        films.clear();
    }

    public static boolean estVide() {
        return films.isEmpty();
    }
}