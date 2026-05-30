package com.ankita.freshfold.ui.services;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.R;
import com.google.android.material.card.MaterialCardView;

public class SteamIronInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_steam_iron_info);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Home button
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.ankita.freshfold.ui.home.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("open_tab", "HOME");
            startActivity(intent);
            finish();
        });

        // Book Now → navigate to ServiceDetailActivity with Steam Iron
        MaterialCardView btnBookNow = findViewById(R.id.btnBookNow);
        btnBookNow.setOnClickListener(v -> {
            if (!new com.ankita.freshfold.SessionManager(this).isApproved()) {
                android.widget.Toast.makeText(this, "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ServiceDetailActivity.class);
            intent.putExtra("service_name",     "Steam Iron");
            intent.putExtra("service_subtitle", "Perfectly pressed clothes.");
            intent.putExtra("service_image_res", R.drawable.img_ironing_new);
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
