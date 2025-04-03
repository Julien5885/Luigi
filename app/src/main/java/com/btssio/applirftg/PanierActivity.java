package com.btssio.applirftg;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activité qui gère le panier de locations.
 * Pour chaque film présent dans le panier (stocké sous forme d'inventoryId) :
 * - Récupère l'objet Inventory pour obtenir le filmId.
 * - Récupère la durée de location (rental_duration) du film.
 * - Calcule la date de retour en fonction de la durée.
 * - Crée la location via un appel POST (le backend fixe return_date à null).
 * - Récupère le rental_id de la location créée et effectue un appel PUT pour mettre à jour le return_date.
 */
public class PanierActivity extends AppCompatActivity {

    private ListView listViewPanier;
    private Button btnRetour, btnValiderPanier;
    private TextView tvPanierTitre;
    private String selectedURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        selectedURL = getIntent().getStringExtra("selectedURL");
        if (selectedURL == null || selectedURL.isEmpty()) {
            Toast.makeText(this, "URL non spécifiée, veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        listViewPanier = findViewById(R.id.listViewPanier);
        btnRetour = findViewById(R.id.btnRetour);
        btnValiderPanier = findViewById(R.id.btnValiderPanier);
        tvPanierTitre = findViewById(R.id.tvPanierTitre);

        afficherNomsFilms();

        btnRetour.setOnClickListener(v -> {
            Intent intent = new Intent(PanierActivity.this, AfficherListeDvdsActivity.class);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
            finish();
        });

        btnValiderPanier.setOnClickListener(v -> {
            Log.i("PANIER", "Bouton Valider du panier cliqué");
            validerPanier();
        });
    }

    /**
     * Affiche les titres des films présents dans le panier.
     * Le panier contient des inventoryId. Pour chaque inventoryId,
     * on récupère l'objet Inventory afin d'extraire le filmId,
     * puis on récupère le titre du film via l'API /toad/film/getById.
     */
    private void afficherNomsFilms() {
        new Thread(() -> {
            List<Integer> inventoryIds = Panier.getFilms();
            ArrayList<String> affichage = new ArrayList<>();
            if (inventoryIds.isEmpty()) {
                affichage.add("Aucun film dans le panier.");
            } else {
                for (int invId : inventoryIds) {
                    try {
                        JSONObject inventoryObj = getInventoryById(invId);
                        if (inventoryObj == null) {
                            affichage.add("Inventory ID: " + invId + " (introuvable)");
                            continue;
                        }
                        int filmId = inventoryObj.getInt("filmId");
                        String titre = getFilmTitle(filmId);
                        affichage.add(titre);
                        Log.d("PANIER", "Film affiché: " + titre + " (inventoryId: " + invId + ")");
                    } catch (Exception e) {
                        Log.e("PANIER", "Erreur lors de l'affichage pour inventoryId=" + invId, e);
                        affichage.add("Erreur Inventory ID: " + invId);
                    }
                }
            }
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, affichage);
                listViewPanier.setAdapter(adapter);
            });
        }).start();
    }

    /**
     * Récupère le titre du film à partir de son filmId.
     */
    private String getFilmTitle(int filmId) throws Exception {
        URL url = new URL(selectedURL + "/toad/film/getById?id=" + filmId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String result = reader.readLine();
        reader.close();
        connection.disconnect();
        JSONObject json = new JSONObject(result);
        return json.getString("title");
    }

    /**
     * Récupère l'objet Inventory en JSON pour un inventoryId donné.
     */
    private JSONObject getInventoryById(int inventoryId) throws Exception {
        URL url = new URL(selectedURL + "/toad/inventory/getById?id=" + inventoryId);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        if (connection.getResponseCode() != 200) {
            connection.disconnect();
            Log.e("PANIER", "getInventoryById - Code réponse != 200 pour inventoryId=" + inventoryId);
            return null;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String result = reader.readLine();
        reader.close();
        connection.disconnect();
        if (result == null || result.equals("null")) {
            Log.e("PANIER", "getInventoryById - Résultat null pour inventoryId=" + inventoryId);
            return null;
        }
        return new JSONObject(result);
    }

    /**
     * Valide le panier en créant une location pour chaque inventoryId.
     * Pour chaque location :
     *  - Récupération de l'objet Inventory pour obtenir le filmId.
     *  - Récupération de la durée de location (rental_duration) via l'API film.
     *  - Calcul de la date de retour en fonction de la durée.
     *  - Appel POST pour créer la location (le backend fixe return_date à null).
     *  - Récupération du rental_id créé et appel PUT pour mettre à jour le return_date.
     */
    private void validerPanier() {
        int customerId = 1;
        // Calcul de la date de location au format "yyyy-MM-dd"
        String dateLocation = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(System.currentTimeMillis());
        Log.d("PANIER", "Date de location calculée: " + dateLocation);

        new Thread(() -> {
            for (int inventoryId : Panier.getFilms()) {
                try {
                    // Récupérer l'objet Inventory pour obtenir le filmId
                    JSONObject inventoryObj = getInventoryById(inventoryId);
                    if (inventoryObj == null) {
                        Log.e("PANIER", "Inventory introuvable pour inventoryId=" + inventoryId);
                        continue;
                    }
                    int filmId = inventoryObj.getInt("filmId");
                    Log.d("PANIER", "Traitement de inventoryId=" + inventoryId + " pour filmId=" + filmId);

                    // Récupérer la durée de location (rental_duration) du film
                    int rentalDuration = getFilmRentalDuration(filmId);
                    if (rentalDuration <= 0) {
                        rentalDuration = 7; // Valeur par défaut si non spécifiée
                    }
                    Log.d("PANIER", "Rental duration pour filmId=" + filmId + " : " + rentalDuration + " jours");

                    // Calculer la date de retour en ajoutant rentalDuration jours à la date actuelle
                    long now = System.currentTimeMillis();
                    long returnMillis = now + rentalDuration * 24L * 60 * 60 * 1000;
                    String computedReturnDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(returnMillis);
                    Log.d("PANIER", "Date de retour calculée pour inventoryId=" + inventoryId + " : " + computedReturnDate);

                    // Créer la location (appel POST) et mettre à jour le return_date
                    insererLocation(inventoryId, customerId, dateLocation, computedReturnDate);

                } catch (Exception e) {
                    Log.e("PANIER", "Erreur lors du traitement de inventoryId=" + inventoryId, e);
                }
            }
            Panier.vider();
            runOnUiThread(() -> Toast.makeText(this, "Enregistrements réalisés !", Toast.LENGTH_LONG).show());
        }).start();
    }

    /**
     * Récupère la durée de location (rental_duration) pour un film donné via l'API /toad/film/getById.
     * On suppose que le JSON retourné contient un champ "rental_duration" (en jours).
     */
    private int getFilmRentalDuration(int filmId) {
        try {
            URL url = new URL(selectedURL + "/toad/film/getById?id=" + filmId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = reader.readLine();
            reader.close();
            connection.disconnect();
            JSONObject json = new JSONObject(result);
            int duration = json.getInt("rental_duration");
            Log.d("PANIER", "Durée récupérée pour filmId=" + filmId + " : " + duration);
            return duration;
        } catch (Exception e) {
            Log.e("PANIER", "Erreur lors de la récupération de rental_duration pour filmId=" + filmId, e);
            return -1;
        }
    }

    /**
     * Crée la location via POST, récupère le rental_id créé et effectue une mise à jour (PUT)
     * pour renseigner le return_date calculé.
     */
    private void insererLocation(int inventoryId, int customerId, String rentalDate, String returnDate) throws Exception {
        // Appel POST pour créer la location (le backend fixe return_date à null)
        String urlFinal = selectedURL
                + "/toad/rental/add?rental_date=" + rentalDate
                + "&inventory_id=" + inventoryId
                + "&customer_id=" + customerId
                + "&return_date=" + returnDate
                + "&staff_id=1"
                + "&last_update=" + rentalDate;
        Log.d("PANIER", "Envoi POST pour créer la location: " + urlFinal);
        URL url = new URL(urlFinal);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        int postResponseCode = connection.getResponseCode();
        Log.d("PANIER", "Code réponse POST: " + postResponseCode);
        connection.disconnect();
        Log.i("PANIER", "Location insérée pour inventory ID: " + inventoryId);

        // Récupérer le rental_id de la location créée
        int rentalId = getRentalIdFor(inventoryId, customerId, rentalDate);
        if (rentalId != -1) {
            Log.d("PANIER", "Rental_id trouvé: " + rentalId + " pour inventoryId=" + inventoryId);
            // Appel PUT pour mettre à jour le return_date
            updateRental(rentalId, rentalDate, inventoryId, customerId, returnDate, 1, rentalDate);
        } else {
            Log.e("PANIER", "Impossible de trouver le rental record pour inventory ID: " + inventoryId);
        }
    }

    /**
     * Récupère le rental_id correspondant à une location créée en filtrant via
     * inventoryId, customerId et rentalDate.
     * Ici, on utilise les clés JSON telles que définies par le backend : "inventoryId", "customerId", "rentalDate", etc.
     * On compare la date de location en coupant la chaîne à 10 caractères (format "yyyy-MM-dd").
     */
    private int getRentalIdFor(int inventoryId, int customerId, String rentalDate) {
        try {
            URL url = new URL(selectedURL + "/toad/rental/all");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder responseStr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseStr.append(line);
            }
            reader.close();
            connection.disconnect();

            Log.d("PANIER", "Réponse GET /toad/rental/all: " + responseStr.toString());

            JSONArray array = new JSONArray(responseStr.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject rental = array.getJSONObject(i);

                // Utilisation des clés telles que fournies par le backend
                int invId = rental.getInt("inventoryId");
                int custId = rental.getInt("customerId");
                String rentalDateInDb = rental.getString("rentalDate");
                // Exemple : "2025-04-04 00:00:00.0"

                // Conserver uniquement la partie date (yyyy-MM-dd)
                if (rentalDateInDb != null && rentalDateInDb.length() >= 10) {
                    rentalDateInDb = rentalDateInDb.substring(0, 10);
                }
                Log.d("PANIER", "Vérification rental_id candidate: inventoryId=" + invId + ", customerId=" + custId + ", rentalDate=" + rentalDateInDb);

                if (invId == inventoryId && custId == customerId && rentalDateInDb.equals(rentalDate)) {
                    Log.d("PANIER", "Match trouvé pour rental_id: " + rental.getInt("rentalId"));
                    return rental.getInt("rentalId");
                }
            }
        } catch (Exception e) {
            Log.e("PANIER", "Erreur lors de la récupération du rental_id", e);
        }
        return -1;
    }

    /**
     * Appelle l'endpoint PUT pour mettre à jour la location et renseigner le return_date.
     */
    private void updateRental(int rentalId, String rentalDate, int inventoryId, int customerId,
                              String returnDate, int staffId, String lastUpdate) throws Exception {
        String urlFinal = selectedURL + "/toad/rental/update/" + rentalId
                + "?rental_date=" + rentalDate
                + "&inventory_id=" + inventoryId
                + "&customer_id=" + customerId
                + "&return_date=" + returnDate
                + "&staff_id=" + staffId
                + "&last_update=" + lastUpdate;
        Log.d("PANIER", "Envoi PUT pour updateRental: " + urlFinal);
        URL url = new URL(urlFinal);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        Log.i("PANIER", "Rental updated for rentalId: " + rentalId + ", response code: " + responseCode);
    }
}
