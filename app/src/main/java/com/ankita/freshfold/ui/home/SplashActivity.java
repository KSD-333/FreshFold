package com.ankita.freshfold.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.DeliveryDashboardActivity;
import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_splash);

        // Find views for animation
        View logoContainer = findViewById(R.id.logoContainer);
        View tvAppTagline = findViewById(R.id.tvAppTagline);
        View accentLine = findViewById(R.id.accentLine);
        View bottomInfoContainer = findViewById(R.id.bottomInfoContainer);

        // Set initial states
        logoContainer.setAlpha(0f);
        logoContainer.setScaleX(0.7f);
        logoContainer.setScaleY(0.7f);
        
        tvAppTagline.setAlpha(0f);
        tvAppTagline.setTranslationY(30f);
        
        accentLine.setAlpha(0f);
        accentLine.setScaleX(0f);
        
        bottomInfoContainer.setAlpha(0f);

        // Run animations
        logoContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        tvAppTagline.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(500)
                .start();

        accentLine.animate()
                .alpha(1f)
                .scaleX(1f)
                .setDuration(600)
                .setStartDelay(800)
                .start();

        bottomInfoContainer.animate()
                .alpha(1f)
                .setDuration(1200)
                .setStartDelay(1000)
                .start();

        // After splash animation, request location permission before navigating
        new Handler().postDelayed(this::checkAndRequestLocationPermission, 2500);
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted — ask the user
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Already granted — proceed
            navigateToNextScreen();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            // Proceed regardless of whether the user granted or denied
            navigateToNextScreen();
        }
    }

    private void navigateToNextScreen() {
        // Initialize CartManager with context so it loads persisted cart from SharedPreferences
        CartManager.getInstance(SplashActivity.this);

        SessionManager sessionManager = new SessionManager(SplashActivity.this);
        Intent intent;

        if (sessionManager.isLoggedIn()) {
            String role = sessionManager.getUserRole();
            if ("delivery".equalsIgnoreCase(role)) {
                intent = new Intent(SplashActivity.this, DeliveryDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            }
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
