package com.ankita.freshfold;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.ui.address.PickupSlotActivity;
import com.ankita.freshfold.CartAdapter;
import com.ankita.freshfold.Order;

public class OrderDetailActivity extends AppCompatActivity implements CartAdapter.OnCartChangeListener {

    private Order currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        currentOrder = (Order) getIntent().getSerializableExtra("order");

        if (currentOrder == null) {
            finish();
            return;
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvTitle = findViewById(R.id.tvOrderTitle);
        tvTitle.setText(currentOrder.getCombinedNames());

        RecyclerView rvItems = findViewById(R.id.rvOrderItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        
        // We use the existing CartAdapter but it works with items list
        CartAdapter adapter = new CartAdapter(currentOrder.getItems(), this);
        rvItems.setAdapter(adapter);

        TextView tvTotal = findViewById(R.id.tvTotalAmount);
        tvTotal.setText("₹" + currentOrder.getTotalPrice());

        findViewById(R.id.btnProceed).setOnClickListener(v -> {
            // According to flow: Order Detail -> Proceed to Checkout -> PickupSlot
            // Since we are checking out this SPECIFIC order, we should probably 
            // make sure PickupSlot knows which one to clear? 
            // The user said: "Only clear cart when order is successfully completed".
            // So we'll just go to PickupSlot.
            Intent intent = new Intent(this, PickupSlotActivity.class);
            // We pass the order ID so PickupSlot can clear it later if needed
            intent.putExtra("order_id_to_clear", currentOrder.getId());
            startActivity(intent);
        });
    }

    @Override public void onCartUpdated() {}
    @Override public void onItemDeleted(int pos) {}
}
