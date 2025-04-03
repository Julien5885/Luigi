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
 * Les films sont récupérés via une API REST, placés dans un MatrixCursor,
 * et affichés dans une ListView via un SimpleCursorAdapter.
 * En cliquant sur un élément de la liste, l'utilisateur accède aux détails du film.
 */
public class AfficherListeDvdsActivity extends AppCompatActivity {

    // Adaptateur qui lie le MatrixCursor aux vues de la ListView
    private SimpleCursorAdapter adapter;
    // MatrixCursor qui contiendra les données à afficher (_id, title, releaseYear)
    private MatrixCursor dvdCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afficherlistedvds);

        // Récupération de l'URL de l'API (ou du serveur) depuis l'intent
        String selectedURL = getIntent().getStringExtra("selectedURL");

        // Bouton pour accéder au panier
        Button btnPanier = findViewById(R.id.btnNavigate);
        btnPanier.setOnClickListener(v -> {
            // Lancement de l'activité PanierActivity en passant l'URL sélectionnée
            Intent intent = new Intent(this, PanierActivity.class);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
        });

        // Initialisation du MatrixCursor avec les colonnes attendues
        dvdCursor = new MatrixCursor(new String[]{"_id", "title", "releaseYear"});
        // Création de l'adaptateur pour lier le MatrixCursor au layout d'affichage des items
        adapter = new SimpleCursorAdapter(this, R.layout.activity_afficherlisteitemsdvds,
                dvdCursor, new String[]{"title", "releaseYear"}, new int[]{R.id.filmName, R.id.filmDate}, 0);

        // Récupération de la ListView et affectation de l'adaptateur
        ListView listviewDvds = findViewById(R.id.listView);
        listviewDvds.setAdapter(adapter);

        // Définition du comportement lors du clic sur un élément de la liste
        listviewDvds.setOnItemClickListener((parent, view, position, id) -> {
            dvdCursor.moveToPosition(position);
            // Récupération de l'identifiant du film à partir de la colonne "_id"
            int filmId = dvdCursor.getInt(dvdCursor.getColumnIndex("_id"));

            // Lancement de FilmDetailsActivity avec le filmId et l'URL
            Intent intent = new Intent(this, FilmDetailsActivity.class);
            intent.putExtra("filmId", filmId);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
        });

        // Lancement de la tâche asynchrone pour récupérer la liste complète des films
        new AppelerServiceRestGETAfficherListeDvdsTask().execute(selectedURL + "/toad/film/all");
    }

    /**
     * Tâche asynchrone pour appeler l'API REST et récupérer la liste de tous les films.
     * Le résultat est traité sous forme de JSONArray.
     */
    private class AppelerServiceRestGETAfficherListeDvdsTask extends AsyncTask<String, Void, JSONArray> {

        @Override
        protected JSONArray doInBackground(String... urls) {
            String urlString = urls[0];
            StringBuilder result = new StringBuilder();

            try {
                // Création de l'objet URL à partir de l'URL fournie
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

                // Conversion du résultat en JSONArray
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

                    // Ajout de la ligne dans le cursor
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
