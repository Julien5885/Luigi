package com.btssio.applirftg;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * FilmDetailsActivity est l'activité qui affiche les détails d'un film sélectionné.
 * Elle récupère l'ID du film et l'URL du serveur via l'intent, puis effectue un appel
 * asynchrone pour obtenir les informations détaillées du film depuis l'API.
 * L'utilisateur peut ensuite ajouter le film au panier (si disponible) ou revenir en arrière.
 */
public class FilmDetailsActivity extends AppCompatActivity {

    // Composants UI pour afficher les informations du film
    private TextView titleTextView, descriptionTextView, releaseYearTextView, ratingTextView, lengthTextView;
    // Boutons pour naviguer dans l'application
    private Button btnRetour, btnAjouterPanier;
    // Variables pour stocker l'ID du film et l'URL du serveur
    private int filmId;
    private String selectedURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Associer l'activité au layout activity_film_details.xml
        setContentView(R.layout.activity_film_details);

        // Liaison des composants de l'interface utilisateur avec les éléments du layout
        titleTextView = findViewById(R.id.tvTitle);
        descriptionTextView = findViewById(R.id.tvDescription);
        releaseYearTextView = findViewById(R.id.tvReleaseYear);
        ratingTextView = findViewById(R.id.tvRating);
        lengthTextView = findViewById(R.id.tvLength);
        btnRetour = findViewById(R.id.btnRetour);
        btnAjouterPanier = findViewById(R.id.btnAjouterPanier);

        // Récupération de l'ID du film et de l'URL depuis l'intent
        filmId = getIntent().getIntExtra("filmId", -1);
        selectedURL = getIntent().getStringExtra("selectedURL");

        // Vérification des données essentielles. Si elles sont manquantes, on affiche une erreur et on termine l'activité.
        if (selectedURL == null || selectedURL.isEmpty() || filmId == -1) {
            Toast.makeText(this, "Erreur : données manquantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Construction de l'URL de l'API pour récupérer les détails du film
        String apiUrl = selectedURL + "/toad/film/getById?id=" + filmId;
        // Lancement d'une AsyncTask pour effectuer l'appel API de manière asynchrone
        new GetFilmDetailsTask().execute(apiUrl);

        // Définition de l'action pour le bouton "Retour"
        btnRetour.setOnClickListener(v -> {
            // Finir l'activité pour revenir à l'écran précédent
            finish();
        });

        // Définition de l'action pour le bouton "Ajouter au panier"
        btnAjouterPanier.setOnClickListener(v -> {
            // Lancer un nouveau thread pour vérifier la disponibilité du film sans bloquer l'UI
            new Thread(() -> {
                // Appel de la méthode getInventoryIdDisponible pour obtenir l'ID d'inventaire disponible pour ce film
                String inventoryId = getInventoryIdDisponible(filmId);
                // Retour sur le thread principal pour mettre à jour l'interface utilisateur
                runOnUiThread(() -> {
                    if (inventoryId == null) {
                        // Si aucun inventaire n'est disponible, afficher un message d'erreur
                        Toast.makeText(FilmDetailsActivity.this, "Ce film n'est pas disponible", Toast.LENGTH_SHORT).show();
                    } else {
                        // Sinon, ajouter le film au panier en passant l'inventoryId (converti en int)
                        Panier.ajouterFilm(Integer.parseInt(inventoryId));
                        Toast.makeText(FilmDetailsActivity.this, "Film ajouté au panier", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
    }

    /**
     * Méthode qui vérifie la disponibilité du film.
     * Elle effectue un appel GET vers l'API /toad/inventory/available/getById avec l'ID du film.
     * @param filmId L'identifiant du film dont on veut vérifier la disponibilité.
     * @return La chaîne représentant l'inventoryId disponible ou null si le film n'est pas disponible.
     */
    private String getInventoryIdDisponible(int filmId) {
        try {
            // Construction de l'URL pour vérifier la disponibilité du film
            String urlCheck = selectedURL + "/toad/inventory/available/getById?id=" + filmId;
            HttpURLConnection connection = (HttpURLConnection) new URL(urlCheck).openConnection();
            connection.setRequestMethod("GET");

            // Lecture de la réponse de l'API
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inventoryId = reader.readLine(); // Par exemple "15" ou "null"
            reader.close();
            connection.disconnect();

            // Retourner l'inventoryId s'il est valide, sinon null
            return (inventoryId != null && !inventoryId.equals("null")) ? inventoryId : null;
        } catch (Exception e) {
            Log.e("FilmDetailsActivity", "Erreur lors de la vérification de la disponibilité", e);
            return null;
        }
    }

    /**
     * AsyncTask pour récupérer les détails du film via l'API.
     * Le résultat est un JSONObject contenant les informations du film.
     */
    private class GetFilmDetailsTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            try {
                // Création de l'objet URL à partir de l'URL fournie
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                // Vérification du code de réponse HTTP
                if (connection.getResponseCode() != 200) return null;

                // Lecture de la réponse de l'API
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String result = reader.readLine();
                reader.close();
                connection.disconnect();

                // Conversion du résultat en JSONObject et retour
                return new JSONObject(result);

            } catch (Exception e) {
                Log.e("FilmDetailsActivity", "Erreur API", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject film) {
            if (film == null) {
                // Si aucune donnée n'est reçue, afficher un message et terminer l'activité
                Toast.makeText(FilmDetailsActivity.this, "Erreur lors de la récupération du film", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
                // Remplissage des TextViews avec les informations du film récupérées
                titleTextView.setText(film.getString("title"));
                descriptionTextView.setText(film.getString("description"));
                releaseYearTextView.setText(film.getString("releaseYear"));
                ratingTextView.setText(film.getString("rating"));
                lengthTextView.setText(film.getString("length") + " min");
            } catch (Exception e) {
                Log.e("FilmDetailsActivity", "Erreur parsing JSON", e);
            }
        }
    }
}
