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
 * PanierActivity gère le processus d'enregistrement des locations pour les films ajoutés au panier.
 *
 * Pour chaque film (identifié par son inventoryId) présent dans le panier :
 *  - On récupère l'objet Inventory afin d'obtenir le filmId associé.
 *  - On récupère ensuite la durée de location (rental_duration) du film via une API.
 *  - On calcule la date de retour en fonction de la durée de location.
 *  - On crée la location en effectuant un appel POST à l'API.
 *    (Le backend crée la location en fixant return_date à null lors de la création.)
 *  - Ensuite, on récupère le rental_id généré et on effectue un appel PUT pour mettre à jour le return_date.
 *  - Enfin, on relit la location (via GET) pour afficher un compte rendu avec la date de retour réellement enregistrée.
 */
public class PanierActivity extends AppCompatActivity {

    // Déclaration des éléments de l'interface
    private ListView listViewPanier; // Liste pour afficher les titres des films dans le panier
    private Button btnRetour, btnValiderPanier; // Boutons pour retourner à la liste des DVD et pour valider le panier
    private TextView tvPanierTitre; // Titre affiché en haut de la vue
    private String selectedURL; // URL de base du serveur/API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Association de l'activité au layout activity_panier.xml
        setContentView(R.layout.activity_panier);

        // Récupération de l'URL passée par l'activité précédente
        selectedURL = getIntent().getStringExtra("selectedURL");
        if (selectedURL == null || selectedURL.isEmpty()) {
            Toast.makeText(this, "URL non spécifiée, veuillez vous reconnecter.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Liaison des composants UI aux vues définies dans le layout
        listViewPanier = findViewById(R.id.listViewPanier);
        btnRetour = findViewById(R.id.btnRetour);
        btnValiderPanier = findViewById(R.id.btnValiderPanier);
        tvPanierTitre = findViewById(R.id.tvPanierTitre);

        // Afficher les titres des films présents dans le panier
        afficherNomsFilms();

        // Bouton "Retour" : permet de revenir à l'activité affichant la liste des DVD
        btnRetour.setOnClickListener(v -> {
            Intent intent = new Intent(PanierActivity.this, AfficherListeDvdsActivity.class);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
            finish();
        });

        // Bouton "Valider" : déclenche le processus d'enregistrement des locations
        btnValiderPanier.setOnClickListener(v -> {
            Log.i("PANIER", "Bouton Valider du panier cliqué");
            validerPanier();
        });
    }

    /**
     * Affiche les titres des films présents dans le panier.
     * Pour chaque inventoryId, on récupère l'objet Inventory afin d'extraire le filmId,
     * puis on interroge l'API pour obtenir le titre du film.
     */
    private void afficherNomsFilms() {
        new Thread(() -> {
            // Récupération de la liste des inventoryIds contenus dans le panier
            List<Integer> inventoryIds = Panier.getFilms();
            ArrayList<String> affichage = new ArrayList<>();
            if (inventoryIds.isEmpty()) {
                affichage.add("Aucun film dans le panier.");
            } else {
                // Pour chaque inventoryId, on récupère l'objet Inventory et le titre du film
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
            // Mise à jour de la ListView sur le thread principal
            runOnUiThread(() -> {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, affichage);
                listViewPanier.setAdapter(adapter);
            });
        }).start();
    }

    /**
     * Interroge l'API pour récupérer le titre du film à partir de son filmId.
     * @param filmId l'identifiant du film.
     * @return le titre du film.
     * @throws Exception en cas d'erreur réseau ou de parsing.
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
     * Interroge l'API pour récupérer l'objet Inventory associé à un inventoryId.
     * @param inventoryId l'identifiant d'inventaire.
     * @return un JSONObject représentant l'inventaire, ou null si introuvable.
     * @throws Exception en cas d'erreur.
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
     * Valide le panier en créant et mettant à jour les locations pour chaque film du panier.
     * Pour chaque inventoryId :
     *  - Récupère l'objet Inventory pour obtenir le filmId.
     *  - Récupère la durée de location (rental_duration) via l'API du film.
     *  - Calcule la date de retour en fonction de la durée.
     *  - Effectue un POST pour créer la location (le backend fixe return_date à null).
     *  - Récupère le rental_id créé et effectue un PUT pour mettre à jour le return_date.
     *  - Affiche ensuite un compte rendu en relisant la location mise à jour.
     */
    private void validerPanier() {
        int customerId = 1;
        // Calcul de la date de location au format "yyyy-MM-dd"
        String dateLocation = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(System.currentTimeMillis());
        Log.d("PANIER", "Date de location: " + dateLocation);

        new Thread(() -> {
            // Pour chaque film présent dans le panier
            for (int inventoryId : Panier.getFilms()) {
                try {
                    // Récupération de l'objet Inventory pour obtenir le filmId
                    JSONObject inventoryObj = getInventoryById(inventoryId);
                    if (inventoryObj == null) {
                        Log.e("PANIER", "Inventory introuvable pour inventoryId=" + inventoryId);
                        continue;
                    }
                    int filmId = inventoryObj.getInt("filmId");
                    Log.d("PANIER", "Traitement inventoryId=" + inventoryId + " pour filmId=" + filmId);

                    // Récupération de la durée de location pour ce film via l'API (rental_duration)
                    int rentalDuration = getFilmRentalDuration(filmId);
                    if (rentalDuration <= 0) {
                        rentalDuration = 7; // Valeur par défaut en cas d'erreur
                    }
                    Log.d("PANIER", "Rental duration filmId=" + filmId + ": " + rentalDuration + " jours");

                    // Calcul de la date de retour en ajoutant rentalDuration jours à la date actuelle
                    long now = System.currentTimeMillis();
                    long returnMillis = now + rentalDuration * 24L * 60 * 60 * 1000;
                    String computedReturnDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(returnMillis);
                    Log.d("PANIER", "Date de retour calculée: " + computedReturnDate);

                    // Crée la location et met à jour return_date via POST et PUT
                    insererLocation(inventoryId, customerId, dateLocation, computedReturnDate);
                } catch (Exception e) {
                    Log.e("PANIER", "Erreur pour inventoryId=" + inventoryId, e);
                }
            }
            // Vider le panier après traitement
            Panier.vider();
            // Une fois le traitement terminé, afficher un Toast et lancer l'activité récapitulative
            runOnUiThread(() -> {
                Toast.makeText(PanierActivity.this, "Enregistrements réalisés !", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(PanierActivity.this, RecapitulatifActivity.class);
                intent.putExtra("selectedURL", selectedURL);
                startActivity(intent);
            });
        }).start();
    }

    /**
     * Récupère la durée de location (rental_duration) d'un film via l'API /toad/film/getById.
     * @param filmId l'identifiant du film.
     * @return la durée de location en jours.
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
            Log.d("PANIER", "Durée filmId=" + filmId + ": " + duration);
            return duration;
        } catch (Exception e) {
            Log.e("PANIER", "Erreur rental_duration filmId=" + filmId, e);
            return -1;
        }
    }

    /**
     * Crée la location via un POST, récupère le rental_id créé, effectue un PUT pour mettre à jour return_date,
     * puis relit la location pour afficher le compte rendu.
     *
     * @param inventoryId L'identifiant d'inventaire du film.
     * @param customerId L'identifiant du client.
     * @param rentalDate La date de location (format yyyy-MM-dd).
     * @param returnDate La date de retour calculée (format yyyy-MM-dd).
     */
    private void insererLocation(int inventoryId, int customerId, String rentalDate, String returnDate) throws Exception {
        // Construction de l'URL pour l'appel POST ; le backend fixe return_date à null lors de la création
        String urlFinal = selectedURL
                + "/toad/rental/add?rental_date=" + rentalDate
                + "&inventory_id=" + inventoryId
                + "&customer_id=" + customerId
                + "&return_date=" + returnDate
                + "&staff_id=1"
                + "&last_update=" + rentalDate;
        Log.d("PANIER", "POST location: " + urlFinal);
        HttpURLConnection connection = (HttpURLConnection) new URL(urlFinal).openConnection();
        connection.setRequestMethod("POST");
        int postResponseCode = connection.getResponseCode();
        connection.disconnect();
        Log.i("PANIER", "Location insérée pour inventoryId=" + inventoryId + ", code=" + postResponseCode);

        // Récupération du rental_id de la location créée via un GET sur l'ensemble des locations
        int rentalId = getRentalIdFor(inventoryId, customerId, rentalDate);
        if (rentalId != -1) {
            Log.d("PANIER", "Rental_id trouvé: " + rentalId);
            // Mise à jour du record avec la date de retour via un appel PUT
            updateRental(rentalId, rentalDate, inventoryId, customerId, returnDate, 1, rentalDate);

            // Ajout du rentalId aux locations récentes de la session
            SessionData.recentRentalIds.add(rentalId);

            // Pause pour permettre au backend de finaliser la mise à jour
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Lancer une AsyncTask pour relire la location mise à jour et afficher le compte rendu
            new AsyncTask<Integer, Void, String>() {
                @Override
                protected String doInBackground(Integer... params) {
                    return getRentalById(params[0]);
                }
                @Override
                protected void onPostExecute(String rentalInfo) {
                    Toast.makeText(PanierActivity.this,
                            "Location mise à jour : " + rentalInfo,
                            Toast.LENGTH_LONG).show();
                }
            }.execute(rentalId);
        } else {
            Log.e("PANIER", "Impossible de trouver la location pour inventoryId=" + inventoryId);
        }
    }

    /**
     * Recherche le rental_id d'une location en filtrant par inventoryId, customerId et rentalDate.
     * La date de location est comparée sur la partie "yyyy-MM-dd" pour correspondre au format.
     *
     * @param inventoryId L'identifiant d'inventaire.
     * @param customerId L'identifiant du client.
     * @param rentalDate La date de location.
     * @return Le rental_id trouvé, ou -1 si non trouvé.
     */
    private int getRentalIdFor(int inventoryId, int customerId, String rentalDate) {
        try {
            URL url = new URL(selectedURL + "/toad/rental/all");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            connection.disconnect();

            Log.d("PANIER", "Réponse GET /toad/rental/all: " + sb.toString());

            JSONArray array = new JSONArray(sb.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject rental = array.getJSONObject(i);
                int invId = rental.getInt("inventoryId");
                int custId = rental.getInt("customerId");
                String rentalDateInDb = rental.getString("rentalDate");
                if (rentalDateInDb != null && rentalDateInDb.length() >= 10) {
                    rentalDateInDb = rentalDateInDb.substring(0, 10);
                }
                if (invId == inventoryId && custId == customerId && rentalDateInDb.equals(rentalDate)) {
                    return rental.getInt("rentalId");
                }
            }
        } catch (Exception e) {
            Log.e("PANIER", "Erreur getRentalIdFor", e);
        }
        return -1;
    }

    /**
     * Effectue un appel PUT pour mettre à jour la location (notamment le return_date).
     *
     * @param rentalId L'identifiant de la location à mettre à jour.
     * @param rentalDate La date de location.
     * @param inventoryId L'identifiant d'inventaire.
     * @param customerId L'identifiant du client.
     * @param returnDate La date de retour à mettre à jour.
     * @param staffId L'identifiant du staff.
     * @param lastUpdate La date de dernière mise à jour.
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
        Log.d("PANIER", "PUT location: " + urlFinal);
        HttpURLConnection connection = (HttpURLConnection) new URL(urlFinal).openConnection();
        connection.setRequestMethod("PUT");
        int responseCode = connection.getResponseCode();
        connection.disconnect();
        Log.i("PANIER", "PUT rentalId=" + rentalId + ", code=" + responseCode);
    }

    /**
     * Relit la location via l'API et retourne un résumé contenant rentalId, rentalDate et returnDate.
     *
     * @param rentalId L'identifiant de la location.
     * @return Une chaîne résumant la location ou un message d'erreur.
     */
    private String getRentalById(int rentalId) {
        try {
            URL url = new URL(selectedURL + "/toad/rental/getById?id=" + rentalId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String result = reader.readLine();
                reader.close();
                connection.disconnect();
                JSONObject json = new JSONObject(result);
                String dateRetour = json.optString("returnDate", "null");
                return "RentalID=" + json.getInt("rentalId")
                        + ", Date location=" + json.getString("rentalDate")
                        + ", Date retour=" + dateRetour;
            } else {
                connection.disconnect();
                return "Erreur GET rentalId=" + rentalId + ", code=" + code;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Exception GET rentalId=" + rentalId + " : " + e.getMessage();
        }
    }
}
