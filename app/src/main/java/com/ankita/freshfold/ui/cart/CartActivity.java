package com.ankita.freshfold.ui.cart;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.CartAdapter;
import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.R;
import com.ankita.freshfold.ui.address.PickupSlotActivity;
import com.ankita.freshfold.ui.home.MainActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartChangeListener {

    private RecyclerView rvCartItems;
    private CartAdapter adapter;
    private TextView tvGrandTotal;

    /**
     * true  → opened from bottom nav  → show pendingCartItems (all services ever added)
     * false → opened from service flow → show sessionItems (current flow only)
     */
    private boolean isNavCart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(0);

        // Detect launch source
        isNavCart = getIntent().getBooleanExtra("from_nav", false);

        tvGrandTotal = findViewById(R.id.tvGrandTotal);

        // ── Back button ──
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // ── Home button ──
        findViewById(R.id.btnHome).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("open_tab", "HOME");
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });




        updateCartBadge();

        // ── Checkout button always visible ──
        MaterialCardView btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setVisibility(View.VISIBLE);

        if (isNavCart) {
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
                setupBottomNavigation();
            }
            View bottomSection = findViewById(R.id.fixedBottomSection);
            if (bottomSection != null && bottomSection.getLayoutParams() instanceof androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams params =
                        (androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) bottomSection.getLayoutParams();
                params.bottomMargin = (int) (86 * getResources().getDisplayMetrics().density);
                bottomSection.setLayoutParams(params);
            }
            View scrollView = findViewById(R.id.nestedScrollView);
            if (scrollView != null) {
                scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(),
                        scrollView.getPaddingRight(), (int) (190 * getResources().getDisplayMetrics().density));
            }
        }

        // ── Add More Services → go to home services section ──
        View btnAddMore = findViewById(R.id.btnAddMore);
        btnAddMore.setOnClickListener(v -> {
            CartManager.getInstance().setAddMoreSession(true);
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("scroll_to_services", true);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // ── Proceed to Checkout ──
        btnContinue.setOnClickListener(v -> {
            if (getDisplayItems().isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            int total = getDisplayTotal();
            if (total < 80) {
                Toast.makeText(this,
                        "Minimum order amount is \u20B980. Please add more items.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            // Commit displayed items → global order, clears both session + pending
            String newOrderId = CartManager.getInstance().commitToGlobal(isNavCart);

            Intent intent = new Intent(this, PickupSlotActivity.class);
            intent.putExtra("order_id_to_clear", newOrderId);
            intent.putExtra("from_nav", isNavCart);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // ── Clear Cart button ──
        View btnClearCart = findViewById(R.id.btnClearCart);
        if (btnClearCart != null) {
            btnClearCart.setOnClickListener(v -> {
                if (getDisplayItems().isEmpty()) return;
                CartManager.getInstance().clearSession();
                CartManager.getInstance().clearPendingCart();
                onCartUpdated();
                Toast.makeText(this, "Cart cleared", Toast.LENGTH_SHORT).show();
            });
        }

        // ── RecyclerView ──
        rvCartItems = findViewById(R.id.rvCartItems);
        rvCartItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(getDisplayItems(), this);
        rvCartItems.setAdapter(adapter);

        updateTotals();
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Nav cart → pendingCartItems (A + B + C — all services)
     * Service flow → sessionItems (current flow only — B + C)
     */
    private List<CartItem> getDisplayItems() {
        return isNavCart
                ? CartManager.getInstance().getPendingCartItems()
                : CartManager.getInstance().getSessionItems();
    }

    private int getDisplayTotal() {
        return isNavCart
                ? CartManager.getInstance().getPendingCartTotalPrice()
                : CartManager.getInstance().getSessionTotalPrice();
    }

    private void updateTotals() {
        List<CartItem> items = getDisplayItems();
        int total = getDisplayTotal();
        int count = items.size();

        tvGrandTotal.setText("₹" + total);

        TextView tvSubtotal = findViewById(R.id.tvSubtotal);
        if (tvSubtotal != null) tvSubtotal.setText("₹" + total);

        TextView tvBottomTotal = findViewById(R.id.tvBottomTotal);
        if (tvBottomTotal != null) tvBottomTotal.setText("₹" + total);

        TextView tvCartItemsTitle = findViewById(R.id.tvCartItemsTitle);
        if (tvCartItemsTitle != null) {
            tvCartItemsTitle.setText("Cart Items (" + count + ")");
        }

        View sectionCartItems = findViewById(R.id.sectionCartItems);
        View layoutEmptyCart = findViewById(R.id.layoutEmptyCart);
        
        if (sectionCartItems != null) {
            sectionCartItems.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
        if (layoutEmptyCart != null) {
            layoutEmptyCart.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        }

        // Hide checkout button if empty
        View btnContinue = findViewById(R.id.btnContinue);
        if (btnContinue != null) {
            btnContinue.setEnabled(count > 0);
            btnContinue.setAlpha(count > 0 ? 1.0f : 0.5f);
        }
    }

    private void updateCartBadge() {
        int count = getDisplayItems().size();
        TextView tvCartTopBadge = findViewById(R.id.tvCartTopBadge);
        if (tvCartTopBadge != null) {
            tvCartTopBadge.setText(String.valueOf(count));
        }
    }

    @Override
    public void onCartUpdated() {
        adapter.updateItems(getDisplayItems());
        updateTotals();
        updateCartBadge();
    }

    @Override
    public void onItemDeleted(int position) {
        List<CartItem> items = getDisplayItems();
        if (position >= 0 && position < items.size()) {
            CartItem removedItem = items.get(position);
            CartManager.getInstance().getSessionItems().removeIf(item -> 
                item.getName().equals(removedItem.getName()) && 
                item.getDescription().equals(removedItem.getDescription()));
            CartManager.getInstance().getPendingCartItems().removeIf(item -> 
                item.getName().equals(removedItem.getName()) && 
                item.getDescription().equals(removedItem.getDescription()));
            CartManager.getInstance().notifyItemChanged();
        }
        onCartUpdated();
    }

    private void setupBottomNavigation() {
        View navHome = findViewById(R.id.navHome);
        View navServices = findViewById(R.id.navServices);
        View navCart = findViewById(R.id.navCart);
        View navWallet = findViewById(R.id.navWallet);
        View navProfile = findViewById(R.id.navProfile);

        ImageView ivHome = findViewById(R.id.ivHome);
        ImageView ivServices = findViewById(R.id.ivServices);
        ImageView ivCart = findViewById(R.id.ivCart);
        ImageView ivWallet = findViewById(R.id.ivWallet);
        ImageView ivProfile = findViewById(R.id.ivProfile);

        TextView tvHome = findViewById(R.id.tvHome);
        TextView tvServices = findViewById(R.id.tvServices);
        TextView tvCart = findViewById(R.id.tvCart);
        TextView tvWallet = findViewById(R.id.tvWallet);
        TextView tvProfile = findViewById(R.id.tvProfile);

        int inactiveColor = Color.parseColor("#A0A8C0");
        int activeColor = ContextCompat.getColor(this, R.color.brand_indigo);

        resetNavItem(ivHome, tvHome, inactiveColor);
        resetNavItem(ivServices, tvServices, inactiveColor);
        resetNavItem(ivCart, tvCart, inactiveColor);
        resetNavItem(ivWallet, tvWallet, inactiveColor);
        resetNavItem(ivProfile, tvProfile, inactiveColor);

        if (ivCart != null) ivCart.setColorFilter(activeColor);
        if (tvCart != null) tvCart.setTextColor(activeColor);

        if (navHome != null) navHome.setOnClickListener(v -> navigateToTab("HOME"));
        if (navServices != null) navServices.setOnClickListener(v -> navigateToTab("SERVICES"));
        // Cart tab already active
        if (navWallet != null) navWallet.setOnClickListener(v -> navigateToTab("WALLET"));
        if (navProfile != null) navProfile.setOnClickListener(v -> navigateToTab("PROFILE"));
    }

    private void resetNavItem(ImageView icon, TextView text, int color) {
        if (icon != null) icon.setColorFilter(color);
        if (text != null) text.setTextColor(color);
    }

    private void navigateToTab(String tab) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("open_tab", tab);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
