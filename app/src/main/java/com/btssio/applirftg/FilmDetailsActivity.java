package com.btssio.applirftg;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

// -----------------------------------------
// FilmDetailsActivity
// -----------------------------------------
public class FilmDetailsActivity extends AppCompatActivity {

    private TextView titleTextView, descriptionTextView, releaseYearTextView, ratingTextView, lengthTextView;
    private Button btnRetour, btnAjouterPanier;
    private int filmId;
    private String selectedURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_details);

        // Association des composants UI
        titleTextView = findViewById(R.id.tvTitle);
        descriptionTextView = findViewById(R.id.tvDescription);
        releaseYearTextView = findViewById(R.id.tvReleaseYear);
        ratingTextView = findViewById(R.id.tvRating);
        lengthTextView = findViewById(R.id.tvLength);
        btnRetour = findViewById(R.id.btnRetour);
        btnAjouterPanier = findViewById(R.id.btnAjouterPanier);

        // Récupération de l'ID du film et de l'URL
        filmId = getIntent().getIntExtra("filmId", -1);
        selectedURL = getIntent().getStringExtra("selectedURL");

        if (selectedURL == null || selectedURL.isEmpty() || filmId == -1) {
            Toast.makeText(this, "Erreur : données manquantes", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Appel asynchrone pour récupérer les détails du film
        String apiUrl = selectedURL + "/toad/film/getById?id=" + filmId;
        new GetFilmDetailsTask().execute(apiUrl);

        // Bouton "Retour"
        btnRetour.setOnClickListener(v -> finish());

        // Bouton "Ajouter au panier" -> vérification de la disponibilité et ajout de l'inventoryId
        btnAjouterPanier.setOnClickListener(v -> {
            new Thread(() -> {
                // Récupère l'inventoryId disponible pour ce film
                String inventoryId = getInventoryIdDisponible(filmId);
                runOnUiThread(() -> {
                    if (inventoryId == null) {
                        Toast.makeText(FilmDetailsActivity.this, "Ce film n'est pas disponible", Toast.LENGTH_SHORT).show();
                    } else {
                        // Ajouter l'inventoryId au panier (converti en int)
                        Panier.ajouterFilm(Integer.parseInt(inventoryId));
                        Toast.makeText(FilmDetailsActivity.this, "Film ajouté au panier", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
    }

    /**
     * Vérifie la disponibilité et retourne l'inventoryId disponible pour le film.
     * Renvoie null si aucun n'est disponible.
     */
    private String getInventoryIdDisponible(int filmId) {
        try {
            String urlCheck = selectedURL + "/toad/inventory/available/getById?id=" + filmId;
            HttpURLConnection connection = (HttpURLConnection) new URL(urlCheck).openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inventoryId = reader.readLine(); // Exemple : "15" ou "null"
            reader.close();
            connection.disconnect();

            return (inventoryId != null && !inventoryId.equals("null")) ? inventoryId : null;
        } catch (Exception e) {
            Log.e("FilmDetailsActivity", "Erreur lors de la vérification de la disponibilité", e);
            return null;
        }
    }

    /**
     * Tâche asynchrone pour récupérer et afficher les détails du film.
     */
    private class GetFilmDetailsTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() != 200) return null;

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String result = reader.readLine();
                reader.close();
                connection.disconnect();

                return new JSONObject(result);

            } catch (Exception e) {
                Log.e("FilmDetailsActivity", "Erreur API", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject film) {
            if (film == null) {
                Toast.makeText(FilmDetailsActivity.this, "Erreur lors de la récupération du film", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            try {
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
