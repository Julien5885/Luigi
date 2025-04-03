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

    private EditText editTextEmail, editTextPassword, editTextCustomURL;
    private Spinner spinnerURLs;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        spinnerURLs = findViewById(R.id.spinnerURLs);
        editTextCustomURL = findViewById(R.id.editTextCustomURL);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.listeURLs,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerURLs.setAdapter(adapter);

        spinnerURLs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = spinnerURLs.getSelectedItem().toString();
                editTextCustomURL.setVisibility(selection.equals("Autre (personnalisée)") ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                editTextCustomURL.setVisibility(View.GONE);
            }
        });

        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String selectedURL = spinnerURLs.getSelectedItem().toString();

        String finalURL = selectedURL.equals("Autre (personnalisée)")
                ? editTextCustomURL.getText().toString().trim()
                : selectedURL;

        if (email.isEmpty() || password.isEmpty() || finalURL.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires.", Toast.LENGTH_SHORT).show();
            return;
        }

        performLogin(finalURL, email, password);
    }

    private void performLogin(String url, String email, String password) {
        new Thread(() -> {
            try {
                String requestUrl = url + "/toad/customer/getByEmail?email=" + email;
                java.net.URL finalURL = new java.net.URL(requestUrl);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) finalURL.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();

                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                    String response = reader.lines().collect(java.util.stream.Collectors.joining());
                    reader.close();

                    if (response.contains("\"password\":" + password)) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(this, AfficherListeDvdsActivity.class);
                            intent.putExtra("selectedURL", url);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Identifiants incorrects.", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Erreur réseau : " + responseCode, Toast.LENGTH_SHORT).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
