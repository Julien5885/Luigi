package com.btssio.applirftg;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FilmDetailsActivity extends AppCompatActivity {

    private TextView titleTextView, descriptionTextView, releaseYearTextView, ratingTextView, lengthTextView;
    private Button btnRetour, btnAjouterPanier;
    private int filmId;

    // Liste statique pour stocker les films ajoutés au panier


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_film_details);

        // Initialisation UI
        titleTextView = findViewById(R.id.tvTitle);
        descriptionTextView = findViewById(R.id.tvDescription);
        releaseYearTextView = findViewById(R.id.tvReleaseYear);
        ratingTextView = findViewById(R.id.tvRating);
        lengthTextView = findViewById(R.id.tvLength);
        btnRetour = findViewById(R.id.btnRetour);
        btnAjouterPanier = findViewById(R.id.btnAjouterPanier);

        // Récupération de l'ID transmis
        filmId = getIntent().getIntExtra("filmId", -1);
        Log.d("DEBUG_FILM_ID", "Film ID reçu : " + filmId);

        if (filmId != -1) {
            String apiUrl = "http://10.0.2.2:8080/toad/film/getById?id=" + filmId;
            Log.d("DEBUG_API", "URL appelée : " + apiUrl);
            new GetFilmDetailsTask().execute(apiUrl);
        } else {
            Log.e("DEBUG_FILM_ID", "filmId invalide reçu !");
        }

        // Retour
        btnRetour.setOnClickListener(v -> finish());

        // Ajouter au panier
        btnAjouterPanier.setOnClickListener(v -> {
            String filmTitle = titleTextView.getText().toString();
            if (!filmTitle.equals("Titre du film")) {
                Panier.ajouterFilm(filmTitle);
                Log.d("DEBUG_PANIER", "Ajout : " + filmTitle);
            }
        });
    }

    private class GetFilmDetailsTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            StringBuilder result = new StringBuilder();
            try {
                String urlStr = urls[0];
                Log.d("DEBUG_URL", "URL appelée : " + urlStr);

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                Log.d("DEBUG_HTTP", "Code réponse HTTP : " + responseCode);

                if (responseCode != 200) {
                    Log.e("DEBUG_HTTP", "Erreur HTTP : " + responseCode);
                    return null;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                Log.d("DEBUG_JSON", "Réponse JSON brute : " + result.toString());

                return new JSONObject(result.toString());

            } catch (Exception e) {
                Log.e("DEBUG_CONNEXION", "Erreur de connexion ou de lecture", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject film) {
            if (film == null) {
                Log.e("GetFilmDetailsTask", "Erreur : le film récupéré est null");
                return;
            }

            try {
                titleTextView.setText(film.getString("title"));
                descriptionTextView.setText(film.getString("description"));
                releaseYearTextView.setText(film.getString("releaseYear"));
                ratingTextView.setText(film.getString("rating"));
                lengthTextView.setText(film.getString("length") + " min");
            } catch (Exception e) {
                Log.e("GetFilmDetailsTask", "Erreur de parsing du JSON", e);
            }
        }
    }
}
