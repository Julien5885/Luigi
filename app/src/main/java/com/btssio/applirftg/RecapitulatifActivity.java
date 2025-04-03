package com.btssio.applirftg;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * RecapitulatifActivity affiche un compte rendu des locations récentes effectuées lors de la session.
 *
 * Fonctionnement :
 *  - Récupère l'URL du serveur (API) passée via l'intent.
 *  - Configure le bouton "Retour à la liste des DVD" qui renvoie à l'activité AfficherListeDvdsActivity.
 *  - Lance une AsyncTask (FetchRentalInfoTask) pour récupérer toutes les locations via l'API.
 *  - Filtre le JSONArray reçu pour ne conserver que les locations dont le rental_id est présent dans SessionData.recentRentalIds.
 *  - Affiche dans la ListView un résumé pour chaque location (titre du film et date de retour prévue).
 */
public class RecapitulatifActivity extends AppCompatActivity {

    // Liste pour afficher le récapitulatif des locations
    private ListView lvRecap;
    // Bouton pour revenir à l'accueil (liste des DVD)
    private Button btnRetourAccueil;
    // URL du serveur/API récupérée depuis l'intent
    private String selectedURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Association au layout activity_recapitulatif.xml
        setContentView(R.layout.activity_recapitulatif);

        // Récupération de l'URL transmise par l'activité précédente
        selectedURL = getIntent().getStringExtra("selectedURL");
        if (selectedURL == null || selectedURL.isEmpty()){
            Toast.makeText(this, "URL non spécifiée", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Liaison des vues du layout aux variables
        lvRecap = findViewById(R.id.lvRecap);
        btnRetourAccueil = findViewById(R.id.btnRetourAccueil);

        // Configuration du bouton pour revenir à l'activité AfficherListeDvdsActivity
        btnRetourAccueil.setOnClickListener(v -> {
            Intent intent = new Intent(RecapitulatifActivity.this, AfficherListeDvdsActivity.class);
            intent.putExtra("selectedURL", selectedURL);
            startActivity(intent);
            finish();
        });

        // Lancement de l'AsyncTask pour récupérer le récapitulatif des locations depuis l'API
        new FetchRentalInfoTask().execute(selectedURL + "/toad/rental/getInformations");
    }

    /**
     * FetchRentalInfoTask est une AsyncTask qui interroge l'API via GET pour récupérer
     * l'ensemble des locations, puis filtre pour ne conserver que celles dont le rental_id
     * est présent dans SessionData.recentRentalIds (locations créées durant la session).
     * Le résultat final (une ArrayList de chaînes) est utilisé pour alimenter la ListView.
     */
    private class FetchRentalInfoTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(String... urls) {
            String urlStr = urls[0];
            ArrayList<String> results = new ArrayList<>();
            try {
                // Création de l'objet URL et ouverture de la connexion HTTP
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                int code = connection.getResponseCode();
                if (code == 200) {
                    // Lecture de la réponse de l'API
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    connection.disconnect();

                    // Conversion de la réponse en JSONArray
                    JSONArray array = new JSONArray(sb.toString());
                    // Parcours de chaque record du tableau JSON
                    for (int i = 0; i < array.length(); i++){
                        JSONObject record = array.getJSONObject(i);
                        // Récupération du rental_id
                        int rentalId = record.getInt("rental_id");
                        // Ne retenir que les locations dont le rental_id est présent dans la session
                        if (SessionData.recentRentalIds.contains(rentalId)) {
                            // Récupération du titre et de la date de retour
                            String title = record.optString("title", "Titre inconnu");
                            String returnDate = record.optString("return_date", "Non défini");
                            // Construction d'une chaîne de résumé
                            String lineInfo = title + " - Retour prévu le " + returnDate;
                            results.add(lineInfo);
                        }
                    }
                } else {
                    connection.disconnect();
                    Log.e("RECAP", "Erreur GET, code = " + code);
                }
            } catch (Exception e) {
                Log.e("RECAP", "Erreur lors du GET des locations", e);
            }
            return results;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if(result == null || result.isEmpty()){
                // Si aucune location récente n'est trouvée, on affiche un message
                Toast.makeText(RecapitulatifActivity.this, "Aucune location récente trouvée.", Toast.LENGTH_SHORT).show();
            } else {
                // Création d'un ArrayAdapter pour afficher les résumés dans la ListView
                // Ici, on utilise un layout personnalisé pour forcer la couleur en blanc (voir item_location.xml)
                ArrayAdapter<String> adapter = new ArrayAdapter<>(RecapitulatifActivity.this, R.layout.item_location, result);
                lvRecap.setAdapter(adapter);
            }
        }
    }
}
