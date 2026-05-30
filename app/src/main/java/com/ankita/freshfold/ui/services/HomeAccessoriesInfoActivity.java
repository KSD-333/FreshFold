package com.ankita.freshfold.ui.services;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.R;
import com.google.android.material.card.MaterialCardView;

public class HomeAccessoriesInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(R.layout.activity_home_accessories_info);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.ankita.freshfold.ui.home.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("open_tab", "HOME");
            startActivity(intent);
            finish();
        });

        MaterialCardView btnBookNow = findViewById(R.id.btnBookNow);
        btnBookNow.setOnClickListener(v -> {
            if (!new com.ankita.freshfold.SessionManager(this).isApproved()) {
                android.widget.Toast.makeText(this, "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, ServiceDetailActivity.class);
            intent.putExtra("service_name",      "Home Accessories");
            intent.putExtra("service_subtitle",  "Clean your home accessories.");
            intent.putExtra("service_image_res", R.drawable.img_blanket_new);
            startActivity(intent);
        });
    }
}
