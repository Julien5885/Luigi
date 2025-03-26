package com.btssio.applirftg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PanierActivity extends AppCompatActivity {

    private ListView listViewPanier;
    private Button btnRetour, btnValiderPanier;
    private TextView tvPanierTitre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panier);

        listViewPanier = findViewById(R.id.listViewPanier);
        btnRetour = findViewById(R.id.btnRetour);
        btnValiderPanier = findViewById(R.id.btnValiderPanier);
        tvPanierTitre = findViewById(R.id.tvPanierTitre);

        // Récupération des films dans le panier
        List<String> filmsDansPanier = Panier.getFilms();

        ArrayList<String> filmsAffiches = new ArrayList<>();
        if (filmsDansPanier == null || filmsDansPanier.isEmpty()) {
            filmsAffiches.add("Aucun film dans le panier.");
        } else {
            filmsAffiches.addAll(filmsDansPanier);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filmsAffiches);
        listViewPanier.setAdapter(adapter);

        // Bouton Retour vers la liste des DVDs
        btnRetour.setOnClickListener(v -> {
            Intent intent = new Intent(PanierActivity.this, AfficherListeDvdsActivity.class);
            startActivity(intent);
            finish(); // Termine cette activité pour éviter la pile d'activités inutiles
        });

        // Bouton Valider le Panier
        btnValiderPanier.setOnClickListener(v -> {
            // Logique pour valider le panier
            validerPanier();
        });
    }

    // Méthode pour valider le panier
    private void validerPanier() {
        int customerId = 1; // client fictif temporairement
        String rentalDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis());
        String returnDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000));

        List<String> filmsDansPanier = Panier.getFilms();

        for (String film : filmsDansPanier) {
            new Thread(() -> {
                try {
                    String inventoryId = getFilmInventoryId(film);
                    createRental(rentalDate, inventoryId, customerId, returnDate);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(PanierActivity.this, "Échec de la validation du panier", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        }

        // Une fois les locations simulées, on affiche directement la vue validerpanier.xml
        runOnUiThread(() -> {
            setContentView(R.layout.validerpanier);

            ListView listViewFilmsLoue = findViewById(R.id.listViewFilmsLoue);
            TextView tvDateLocation = findViewById(R.id.tvDateLocation);
            TextView tvDateRetour = findViewById(R.id.tvDateRetour);

            tvDateLocation.setText("Date de location : " + rentalDate);
            tvDateRetour.setText("Date de retour prévue : " + returnDate);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(PanierActivity.this, android.R.layout.simple_list_item_1, filmsDansPanier);
            listViewFilmsLoue.setAdapter(adapter);

            // Bouton retour à la liste principale des films (si tu en as un dans validerpanier.xml)
            Button btnRetourAccueil = findViewById(R.id.btnRetourAccueil);
            btnRetourAccueil.setOnClickListener(v -> {
                Intent intent = new Intent(PanierActivity.this, AfficherListeDvdsActivity.class);
                startActivity(intent);
                finish();
            });
        });

        Panier.vider(); // vide le panier après validation
    }

    // Méthode simulée temporairement pour récupérer un ID d'inventaire fictif
    private String getFilmInventoryId(String film) {
        // Retourne un ID fictif temporairement (par exemple toujours "1")
        return "1";
    }

    // Méthode simulée temporairement pour créer une location
    private void createRental(String rentalDate, String inventoryId, int customerId, String returnDate) {
        // Simule une opération de location (ne fait rien pour le moment)
        runOnUiThread(() -> {
            Toast.makeText(PanierActivity.this, "Location créée fictivement pour l'inventaire " + inventoryId, Toast.LENGTH_SHORT).show();
        });
    }


}
