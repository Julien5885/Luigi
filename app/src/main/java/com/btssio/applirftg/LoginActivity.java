package com.btssio.applirftg;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;

    private static final String BASE_URL = "http://10.0.2.2:8080/toad/customer/getByEmail?email=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = editTextEmail.getText().toString().trim();
        String passwordInput = editTextPassword.getText().toString().trim();

        // Vérifie que les champs ne sont pas vides
        if (email.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        // Login fictif avec des valeurs prédéfinies
        if (email.equals("MARY.SMITH@peachcustomer.org") && passwordInput.equals("1234")) {
            Toast.makeText(this, "Connexion réussie (factice)", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, AfficherListeDvdsActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_SHORT).show();
        }
    }

}
