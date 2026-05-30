package com.ankita.freshfold.ui.cart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.Order;
import com.ankita.freshfold.R;
import com.ankita.freshfold.ui.home.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class OrderSuccessActivity extends AppCompatActivity {

    private boolean isDetailsVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().setStatusBarColor(Color.parseColor("#F8F9FF"));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        
        setContentView(R.layout.activity_order_success);

        String orderId = getIntent().getStringExtra("order_id");
        String pickupDate = getIntent().getStringExtra("pickup_date");

        TextView tvDate = findViewById(R.id.tvDate);
        TextView tvTotal = findViewById(R.id.tvTotalAmount);
        View btnHome = findViewById(R.id.btnBackHome);
        
        View btnToggle = findViewById(R.id.btnToggleDetails);
        TextView tvToggleText = findViewById(R.id.tvToggleText);
        ImageView ivToggleArrow = findViewById(R.id.ivToggleArrow);
        RecyclerView rvItems = findViewById(R.id.rvOrderItems);

        // Display pickup date
        tvDate.setText(pickupDate != null ? pickupDate : "Today");

        List<CartItem> orderItems = new ArrayList<>();
        int totalPrice = 0;

        if (orderId != null) {
            for (Order o : CartManager.getInstance().getOrders()) {
                if (orderId.equals(o.getId())) {
                    orderItems = o.getItems();
                    break;
                }
            }
        }

        for (CartItem item : orderItems) {
            totalPrice += item.getTotalPrice();
        }
        tvTotal.setText("₹" + totalPrice);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(new OrderSummaryAdapter(orderItems));

        // Toggle details logic
        btnToggle.setOnClickListener(v -> {
            isDetailsVisible = !isDetailsVisible;
            rvItems.setVisibility(isDetailsVisible ? View.VISIBLE : View.GONE);
            tvToggleText.setText(isDetailsVisible ? "Hide details" : "View details");
            ivToggleArrow.setRotation(isDetailsVisible ? 180 : 0);
        });

        // Navigation back to home
        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(OrderSuccessActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
