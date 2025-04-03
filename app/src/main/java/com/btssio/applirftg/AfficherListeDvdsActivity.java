package com.btssio.applirftg;

import android.content.Intent;
import android.database.MatrixCursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Activité qui affiche la liste des DVD disponibles.
 *
 * Fonctionnement :
 * - L'activité récupère l'URL du serveur (API) passée via l'intent.
 * - Un bouton permet d'accéder au panier (PanierActivity).
 * - Un MatrixCursor est créé pour stocker temporairement les données des films (id, titre, année de sortie).
 * - Un SimpleCursorAdapter lie ces données au layout d'un item personnalisé (activity_afficherlisteitemsdvds.xml).
 * - La ListView affiche la liste des DVD.
 * - Un clic sur un item de la liste redirige l'utilisateur vers FilmDetailsActivity,
 *   en transmettant l'id du film et l'URL du serveur.
 * - Une AsyncTask est lancée pour effectuer un appel GET sur l'API et récupérer la liste complète des films,
 *   que l'on ajoute ensuite au MatrixCursor et met à jour l'adaptateur.
 */
public class AfficherListeDvdsActivity extends AppCompatActivity {

    // Adaptateur permettant de lier le MatrixCursor à la ListView
    private SimpleCursorAdapter adapter;
    // MatrixCursor qui stockera temporairement les données (id, titre, année) des DVD
    private MatrixCursor dvdCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Définition du layout pour cette activité
        setContentView(R.layout.activity_afficherlistedvds);

        // Récupération de l'URL du serveur passé par l'activité précédente via l'intent
        String selectedURL = getIntent().getStringExtra("selectedURL");

        // Configuration du bouton qui permet d'accéder au panier
        Button btnPanier = findViewById(R.id.btnNavigate);
        btnPanier.setOnClickListener(v -> {
            // Lancement de l'activité PanierActivity en passant l'URL du serveur
            Intent intent = new Intent(this, PanierActivity.class);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
        });

        // Initialisation du MatrixCursor avec les colonnes attendues (_id, title, releaseYear)
        dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear"});
        // Création du SimpleCursorAdapter qui liera le cursor au layout de chaque item de la liste
        adapter = new SimpleCursorAdapter(
                this,
                R.layout.activity_afficherlisteitemsdvds, // Layout de chaque item
                dvdCursor,
                new String[]{"title", "releaseYear"}, // Colonnes à afficher
                new int[]{R.id.filmName, R.id.filmDate}, // ID des TextView dans le layout d'item
                0
        );

        // Récupération de la ListView du layout et affectation de l'adaptateur
        ListView listviewDvds = findViewById(R.id.listView);
        listviewDvds.setAdapter(adapter);

        // Gestion du clic sur un élément de la liste
        listviewDvds.setOnItemClickListener((parent, view, position, id) -> {
            // Déplacement du cursor à la position cliquée
            dvdCursor.moveToPosition(position);
            // Récupération de l'identifiant du film à partir de la colonne "_id"
            int filmId = dvdCursor.getInt(dvdCursor.getColumnIndex("_id"));

            // Lancement de FilmDetailsActivity en passant l'identifiant du film et l'URL du serveur
            Intent intent = new Intent(this, FilmDetailsActivity.class);
            intent.putExtra("filmId", filmId);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
        });

        // Lancement de la tâche asynchrone pour récupérer la liste complète des DVD via l'API REST
        new AppelerServiceRestGETAfficherListeDvdsTask().execute(selectedURL + "/toad/film/all");
    }

    /**
     * AsyncTask qui se charge d'appeler l'API REST pour récupérer la liste complète des DVD.
     * La réponse est lue sous forme de chaîne, convertie en JSONArray, puis chaque film est ajouté dans le MatrixCursor.
     * Enfin, l'adaptateur est mis à jour pour afficher la liste dans la ListView.
     */
    private class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String... urls) {
            String urlString = urls[0];
            StringBuilder result = new StringBuilder();

            try {
                // Création de l'objet URL à partir de l'URL passée en paramètre
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                Log.d("AppelerServiceRestGETAfficherListeDvdsTask", "Connexion à l'API établie avec succès");

                // Lecture de la réponse de l'API ligne par ligne
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                Log.d("AppelerServiceRestGETAfficherListeDvdsTask", "Réponse reçue : " + result.toString());

                // Conversion de la chaîne en JSONArray et retour du résultat
                return new JSONArray(result.toString());

            } catch (Exception e) {
                Log.e("AppelerServiceRestGETAfficherListeDvdsTask", "Erreur de connexion ou de lecture : ", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray films) {
            if (films == null) {
                Log.e("AppelerServiceRestGETAfficherListeDvdsTask", "Erreur : la liste films récupérée est null");
                Toast.makeText(AfficherListeDvdsActivity.this, "Erreur lors de la récupération de la liste des films.", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Fermeture de l'ancien cursor et création d'un nouveau
                dvdCursor.close();
                dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear"});

                // Parcours du JSONArray pour extraire les informations de chaque film
                for (int i = 0; i < films.length(); i++) {
                    JSONObject film = films.getJSONObject(i);
                    int filmId = film.getInt("filmId");
                    String title = film.getString("title");
                    String annee = film.getString("releaseYear");

                    // Ajout des données dans le MatrixCursor
                    dvdCursor.addRow(new Object[]{filmId, title, annee});
                    Log.d("AppelerServiceRestGETAfficherListeDvdsTask", "Ajout du film -> filmId: " + filmId + ", title: " + title + ", releaseYear: " + annee);
                }

                // Mise à jour de l'adaptateur avec le nouveau cursor
                adapter.changeCursor(dvdCursor);
                Log.d("AppelerServiceRestGETAfficherListeDvdsTask", "Liste mise à jour avec succès");

            } catch (JSONException e) {
                Log.e("AppelerServiceRestGETAfficherListeDvdsTask", "Erreur de parsing du JSON : ", e);
            }
        }
    }
}
