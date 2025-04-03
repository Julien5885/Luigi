package com.btssio.applirftg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.AdapterView;

public class LoginActivity extends AppCompatActivity {

    // Déclaration des variables d'interface (UI)
    private EditText editTextEmail, editTextPassword, editTextCustomURL; // Champs pour l'email, le mot de passe et l'URL personnalisée
    private Spinner spinnerURLs; // Spinner pour sélectionner une URL prédéfinie ou personnalisée
    private Button buttonLogin;  // Bouton de connexion

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Associe l'activité au layout activity_login.xml
        setContentView(R.layout.activity_login);

        // Liaison des composants UI avec leur identifiant dans le layout
        spinnerURLs = findViewById(R.id.spinnerURLs);
        editTextCustomURL = findViewById(R.id.editTextCustomURL);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        // Création d'un ArrayAdapter pour le Spinner, en utilisant une ressource de chaîne (liste d'URLs)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.listeURLs,  // Référence au tableau défini dans strings.xml
                android.R.layout.simple_spinner_item // Layout par défaut pour les items du spinner
        );
        // Définir le layout utilisé pour afficher la liste déroulante du spinner
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapter); // Affecter l'adapter au spinner

        // Gestion de la sélection du spinner
        spinnerURLs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            // Méthode appelée quand un item est sélectionné
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Récupère la chaîne sélectionnée
                String selection = spinnerURLs.getSelectedItem().toString();
                // Si l'utilisateur choisit "Autre (personnalisée)", on affiche le champ editTextCustomURL pour saisir une URL manuellement
                // Sinon, on masque ce champ
                editTextCustomURL.setVisibility(selection.equals("Autre (personnalisée)") ? View.VISIBLE : View.GONE);
            }

            // Méthode appelée quand aucune sélection n'est faite (non utilisée ici)
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                editTextCustomURL.setVisibility(View.GONE); // Masquer le champ si aucune sélection
            }
        });

        // Définition du comportement du bouton de connexion
        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Méthode qui vérifie les saisies et tente de connecter l'utilisateur.
     */
    private void attemptLogin() {
        // Récupération des valeurs saisies par l'utilisateur et suppression des espaces superflus
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String selectedURL = spinnerURLs.getSelectedItem().toString();

        // Si l'utilisateur a choisi "Autre (personnalisée)", on prend l'URL saisie manuellement
        String finalURL = selectedURL.equals("Autre (personnalisée)")
                ? editTextCustomURL.getText().toString().trim()
                : selectedURL;

        // Vérifier que tous les champs sont remplis, sinon afficher un message d'erreur
        if (email.isEmpty() || password.isEmpty() || finalURL.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lancer la procédure de connexion en passant l'URL finale, l'email et le mot de passe
        performLogin(finalURL, email, password);
    }

    /**
     * Effectue la connexion en appelant le service web qui retourne les informations du client à partir de son email.
     * Si la réponse contient le mot de passe attendu, l'utilisateur est connecté.
     * Sinon, un message d'erreur est affiché.
     *
     * @param url L'URL de base pour l'appel au service web.
     * @param email L'adresse email saisie.
     * @param password Le mot de passe saisi.
     */
    private void performLogin(String url, String email, String password) {
        new Thread(() -> {
            try {
                // Construction de l'URL de la requête GET pour récupérer le client via son email
                String requestUrl = url + "/toad/customer/getByEmail?email=" + email;
                java.net.URL finalURL = new java.net.URL(requestUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) finalURL.openConnection();
                connection.setRequestMethod("GET");

                // Récupération du code de réponse HTTP
                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    // Lecture de la réponse de l'API
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    String response = reader.lines().collect(java.util.stream.Collectors.joining());
                    reader.close();

                    // Vérification de la présence du mot de passe dans la réponse JSON (vérification simplifiée)
                    if (response.contains("\"password\":" + password)) {
                        // Si la connexion réussit, lancer l'activité AfficherListeDvdsActivity
                        runOnUiThread(() -> {
                            Intent intent = new Intent(this, AfficherListeDvdsActivity.class);
                            intent.putExtra("selectedURL", url);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        // Si le mot de passe ne correspond pas, afficher un message d'erreur
                        runOnUiThread(() -> Toast.makeText(this, "Identifiants incorrects.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    // Si la réponse HTTP n'est pas 200, afficher un message d'erreur avec le code
                    runOnUiThread(() -> Toast.makeText(this, "Erreur réseau : " + responseCode, Toast.LENGTH_SHORT).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                // En cas d'exception, afficher un message d'erreur avec le détail
                runOnUiThread(() -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
