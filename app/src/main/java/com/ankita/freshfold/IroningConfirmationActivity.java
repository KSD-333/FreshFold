package com.ankita.freshfold;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.ui.home.MainActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class IroningConfirmationActivity extends AppCompatActivity {

    private static final String PRESSMAN_STATS_PATH = "freshfold/data/pressman/stats";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        setContentView(R.layout.activity_ironing_confirmation);

        String pickupDate = getIntent().getStringExtra("pickup_date");
        int itemCount = getIntent().getIntExtra("item_count", 0);

        TextView tvClothesValue    = findViewById(R.id.tvClothesValue);
        TextView tvPickupDateValue = findViewById(R.id.tvPickupDateValue);
        TextView tvDeliveryDate    = findViewById(R.id.tvDeliveryDate);
        TextView tvInfoMessage     = findViewById(R.id.tvInfoMessage);

        tvClothesValue.setText(itemCount + " Items");
        tvPickupDateValue.setText(pickupDate);

        // Read current pressman points from Firestore to decide delivery message
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference statsRef = db.document(PRESSMAN_STATS_PATH);

        statsRef.get().addOnSuccessListener(doc -> {
            long currentPoints = 0;
            if (doc.exists() && doc.getLong("totalPoints") != null) {
                currentPoints = doc.getLong("totalPoints");
            }

            if (currentPoints < 100) {
                tvDeliveryDate.setText("Tomorrow");
                tvInfoMessage.setText("Your order will be delivered on time.");
            } else {
                tvDeliveryDate.setText("Day After Tomorrow");
                tvInfoMessage.setText("Due to high workload, delivery is scheduled for the next available day.");
            }
        }).addOnFailureListener(e -> {
            // Fallback if Firestore unreachable
            tvDeliveryDate.setText("Tomorrow");
            tvInfoMessage.setText("Your order will be delivered on time.");
        });

        // Calculate points from cart items BEFORE btnDone clears the cart
        final int calculatedPoints = calculatePoints();

        findViewById(R.id.btnDone).setOnClickListener(v -> {
            // Write order to Firestore: increment pressman stats
            statsRef.get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // Document exists — increment fields
                    statsRef.update(
                        "totalPoints",  FieldValue.increment(calculatedPoints),
                        "pendingTasks", FieldValue.increment(1),
                        "pressedToday", FieldValue.increment(0)   // pressman marks this himself
                    );
                } else {
                    // First order ever — create document
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("totalPoints",  (long) calculatedPoints);
                    data.put("pendingTasks", 1L);
                    data.put("pressedToday", 0L);
                    statsRef.set(data);
                }

                // Order persists in global history now
                android.widget.Toast.makeText(this, "Ironing Order Placed!", android.widget.Toast.LENGTH_SHORT).show();

                String orderId = getIntent().getStringExtra("order_id_to_clear");
                Intent intent = new Intent(IroningConfirmationActivity.this, com.ankita.freshfold.ui.cart.OrderConfirmationActivity.class);
                intent.putExtra("order_id", orderId);
                intent.putExtra("pickup_date", pickupDate);
                startActivity(intent);
                finish();
            }).addOnFailureListener(e -> {
                // Even if Firestore fails, navigate home
                // Order persists in global history now
                android.widget.Toast.makeText(this, "Order Confirmed", android.widget.Toast.LENGTH_SHORT).show();

                String orderId = getIntent().getStringExtra("order_id_to_clear");
                Intent intent = new Intent(IroningConfirmationActivity.this, com.ankita.freshfold.ui.cart.OrderConfirmationActivity.class);
                intent.putExtra("order_id", orderId);
                intent.putExtra("pickup_date", pickupDate);
                startActivity(intent);
                finish();
            });
        });
    }

    private int calculatePoints() {
        // Points per cloth type — keys must match cloth type strings (case-insensitive)
        java.util.Map<String, Integer> pointsMap = new java.util.HashMap<>();
        pointsMap.put("shirt", 2);
        pointsMap.put("t-shirt", 1);
        pointsMap.put("tshirt", 1);
        pointsMap.put("saree", 3);
        pointsMap.put("trousers", 2);
        pointsMap.put("trouser", 2);
        pointsMap.put("kurta", 2);
        pointsMap.put("jacket", 3);
        pointsMap.put("suit", 5);
        pointsMap.put("jeans", 2);
        pointsMap.put("bed sheet", 3);
        pointsMap.put("curtain", 4);
        pointsMap.put("sneakers", 2);
        pointsMap.put("formal shoes", 2);
        pointsMap.put("sports shoes", 2);
        pointsMap.put("leather shoes", 2);

        int totalPoints = 0;
        List<CartItem> items = CartManager.getInstance().getGlobalItems();

        android.util.Log.d("POINTS_DEBUG", "Cart has " + items.size() + " item types");

        for (CartItem item : items) {
            String clothType = item.getDescription();
            int qty = item.getQuantity();
            int pointsPerItem = 1; // default if type not found

            if (clothType != null) {
                String key = clothType.trim().toLowerCase();
                Integer mapped = pointsMap.get(key);
                if (mapped != null) {
                    pointsPerItem = mapped;
                }
                android.util.Log.d("POINTS_DEBUG",
                    "Type='" + clothType + "' key='" + key + "' qty=" + qty
                    + " pts/item=" + pointsPerItem + " subtotal=" + (pointsPerItem * qty));
            }
            totalPoints += pointsPerItem * qty;
        }

        android.util.Log.d("POINTS_DEBUG", "Total calculated points = " + totalPoints);
        return totalPoints;
    }
}
