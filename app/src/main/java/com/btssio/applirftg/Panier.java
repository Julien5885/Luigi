package com.btssio.applirftg;

import java.util.ArrayList;
import java.util.List;

public class Panier {
    private static final List<Integer> films = new ArrayList<>();

    public static void ajouterFilm(int filmId) {
        if (!films.contains(filmId)) {
            films.add(filmId);
        }
    }


    public static List<Integer> getFilms() {
        return new ArrayList<>(films);
    }


    public static void vider() {
        films.clear();
    }

    public static boolean estVide() {
        return films.isEmpty();
    }
}