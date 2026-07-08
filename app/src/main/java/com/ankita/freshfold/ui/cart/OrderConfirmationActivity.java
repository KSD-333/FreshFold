package com.ankita.freshfold.ui.cart;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.ankita.freshfold.R;
import com.ankita.freshfold.ui.home.MainActivity;

public class OrderConfirmationActivity extends AppCompatActivity {

    private ProgressBar ringProgress;
    private ImageView ivCheckmark;
    private TextView tvTitle, tvSubtitle;
    private CardView cardSummary;
    private View btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Transparent status bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        
        setContentView(R.layout.activity_order_confirmation);

        ringProgress = findViewById(R.id.ringProgress);
        ivCheckmark  = findViewById(R.id.ivCheckmark);
        tvTitle      = findViewById(R.id.tvTitle);
        tvSubtitle   = findViewById(R.id.tvSubtitle);
        cardSummary  = findViewById(R.id.cardSummary);
        btnDone      = findViewById(R.id.btnDone);

        // Dynamic Data Binding
        bindOrderData();

        // Trigger custom in-app and system status bar notification
        String pickupDateNotif = getIntent().getStringExtra("pickup_date");
        String servicesTextNotif = getIntent().getStringExtra("services_text");
        int totalPriceNotif = getIntent().getIntExtra("total_price", 0);
        String orderId = getIntent().getStringExtra("order_id");
        String addressDocId = getIntent().getStringExtra("address_doc_id");
        
        if (pickupDateNotif == null || pickupDateNotif.isEmpty()) {
            pickupDateNotif = "scheduled date";
        }
        if (servicesTextNotif == null || servicesTextNotif.isEmpty()) {
            servicesTextNotif = "laundry services";
        }
        com.ankita.freshfold.NotificationHelper.triggerOrderSuccessNotification(
                this, pickupDateNotif, servicesTextNotif, totalPriceNotif, orderId, addressDocId
        );

        startAnimations();

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(OrderConfirmationActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void bindOrderData() {
        String pickupDate    = getIntent().getStringExtra("pickup_date");
        String servicesText  = getIntent().getStringExtra("services_text");
        int    totalPrice    = getIntent().getIntExtra("total_price", 0);

        com.ankita.freshfold.SessionManager session = new com.ankita.freshfold.SessionManager(this);

        // Services
        if (servicesText != null && !servicesText.isEmpty()) {
            ((TextView) findViewById(R.id.tvSummaryServices)).setText(servicesText);
        }

        // Total
        if (totalPrice > 0) {
            ((TextView) findViewById(R.id.tvSummaryTotal)).setText("₹" + totalPrice);
        }

        // Date
        if (pickupDate != null) {
            ((TextView) findViewById(R.id.tvSummaryDate)).setText(pickupDate);
        }

        // Pickup time — fixed slot
        ((TextView) findViewById(R.id.tvSummaryTime)).setText("6:00 AM – 8:00 AM");

        // Address
        String address = session.getUserAddress();
        if (address != null && !address.isEmpty()) {
            ((TextView) findViewById(R.id.tvSummaryLocation)).setText(address);
        }
    }

    private void startAnimations() {
        Handler handler = new Handler();

        // Step 1 (0ms): Draw ring animation 1000ms
        handler.postDelayed(() -> {
            ObjectAnimator animator = ObjectAnimator.ofInt(ringProgress, "progress", 0, 100);
            animator.setDuration(1000);
            animator.start();
        }, 0);

        // Step 2 (1200ms): Checkmark Scale In
        handler.postDelayed(() -> {
            ivCheckmark.setVisibility(View.VISIBLE);
            ivCheckmark.setScaleX(0f);
            ivCheckmark.setScaleY(0f);
            
            ivCheckmark.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        }, 1200);

        // Step 3 (1400ms): Fade in TextViews
        handler.postDelayed(() -> {
            ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f).setDuration(500).start();
            ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f).setDuration(500).start();
        }, 1400);

        handler.postDelayed(() -> {
            cardSummary.setScaleX(0.95f);
            cardSummary.setScaleY(0.95f);
            cardSummary.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator(0.8f))
                    .start();

            btnDone.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(200)
                    .start();
        }, 2000);
    }
}
