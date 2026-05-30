package com.ankita.freshfold.ui.services;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.R;
import com.ankita.freshfold.ui.cart.CartActivity;

import java.util.List;

public class ServiceDetailActivity extends AppCompatActivity {

    private String[] currentTypes;
    private int[] currentIcons;
    private java.util.Map<String, Integer> currentPriceMap;

    // ── MEN'S APPAREL ──
    private static final String[] MEN_TYPES   = {"Shirt", "T-Shirt", "Trousers", "Jeans", "Jacket", "Kurta"};
    private static final int[]    MEN_ICONS   = {R.drawable.shirt, R.drawable.tshirt, R.drawable.pants, R.drawable.clothes, R.drawable.jacket, R.drawable.kurta};

    // ── WOMEN'S APPAREL ──
    private static final String[] WOMEN_TYPES = {"Saree", "Salwar Suit", "Dupatta", "Kurti", "Lehenga", "Blouse"};
    private static final int[]    WOMEN_ICONS = {R.drawable.saree, R.drawable.salwarsuit, R.drawable.dupatta, R.drawable.kurti, R.drawable.lehenga, R.drawable.blouse};

    // ── LAUNDRY CONFIG ──
    private static final String[] LAUNDRY_TYPES = {"Shirt", "T-Shirt", "Trousers", "Saree", "Kurta", "Jacket", "Suit"};
    private static final int[] LAUNDRY_ICONS = {R.drawable.shirt, R.drawable.tshirt, R.drawable.pants, R.drawable.saree, R.drawable.kurta, R.drawable.jacket, R.drawable.ic_cloth_suit_photo};
    private static final java.util.Map<String, Integer> LAUNDRY_PRICES = new java.util.HashMap<>();
    static {
        LAUNDRY_PRICES.put("Shirt", 40);
        LAUNDRY_PRICES.put("T-Shirt", 30);
        LAUNDRY_PRICES.put("Trousers", 50);
        LAUNDRY_PRICES.put("Saree", 100);
        LAUNDRY_PRICES.put("Kurta", 60);
        LAUNDRY_PRICES.put("Jacket", 120);
        LAUNDRY_PRICES.put("Suit", 200);
    }

    // ── IRONING CONFIG ──
    private static final String[] IRONING_TYPES = {"Shirt", "T-Shirt", "Trousers", "Saree", "Kurta", "Jacket", "Suit"};
    private static final int[] IRONING_ICONS = {R.drawable.shirt, R.drawable.tshirt, R.drawable.pants, R.drawable.saree, R.drawable.kurta, R.drawable.jacket, R.drawable.ic_cloth_suit_photo};
    private static final java.util.Map<String, Integer> IRONING_PRICES = new java.util.HashMap<>();
    static {
        IRONING_PRICES.put("Shirt", 12);
        IRONING_PRICES.put("T-Shirt", 10);
        IRONING_PRICES.put("Trousers", 15);
        IRONING_PRICES.put("Saree", 40);
        IRONING_PRICES.put("Kurta", 20);
        IRONING_PRICES.put("Jacket", 50);
        IRONING_PRICES.put("Suit", 80);
    }

    // ── DRY CLEANING CONFIG ──
    private static final String[] DRY_CLEAN_TYPES = {"Shirt", "T-Shirt", "Trousers", "Saree", "Kurta", "Jacket", "Suit"};
    private static final int[] DRY_CLEAN_ICONS = {R.drawable.shirt, R.drawable.tshirt, R.drawable.pants, R.drawable.saree, R.drawable.kurta, R.drawable.jacket, R.drawable.ic_cloth_suit_photo};
    private static final java.util.Map<String, Integer> DRY_CLEAN_PRICES = new java.util.HashMap<>();
    static {
        DRY_CLEAN_PRICES.put("Shirt", 120);
        DRY_CLEAN_PRICES.put("T-Shirt", 100);
        DRY_CLEAN_PRICES.put("Trousers", 150);
        DRY_CLEAN_PRICES.put("Saree", 350);
        DRY_CLEAN_PRICES.put("Kurta", 200);
        DRY_CLEAN_PRICES.put("Jacket", 500);
        DRY_CLEAN_PRICES.put("Suit", 800);
    }

    private TextView tvTotalPrice, tvBottomTotal, tvPriceBreakdown, tvCartBadge, tvTypeLabel;
    private int quantity = 0;
    private java.util.Map<String, Integer> itemQuantities = new java.util.HashMap<>();
    private java.util.Map<String, Integer> menQuantities   = new java.util.HashMap<>();
    private java.util.Map<String, Integer> womenQuantities = new java.util.HashMap<>();
    private boolean isMenSelected = true;
    private boolean isApparelService = false;
    private LinearLayout itemsContainer;
    private String currentServiceName;
    private int currentServiceImageRes;

    private static final String[] SHOE_TYPES = {"Sneakers", "Formal Shoes", "Sports Shoes", "Leather Shoes"};
    private static final int[] SHOE_ICONS = {R.drawable.sneakers, R.drawable.formal_shoe, R.drawable.sport_shoe, R.drawable.leather};
    private static final java.util.Map<String, Integer> SHOE_PRICES = new java.util.HashMap<>();
    static {
        SHOE_PRICES.put("Sneakers", 50);
        SHOE_PRICES.put("Formal Shoes", 100);
        SHOE_PRICES.put("Sports Shoes", 60);
        SHOE_PRICES.put("Leather Shoes", 150);
    }

    // ── HOME ACCESSORIES CONFIG ──
    private static final String[] HOME_ACC_TYPES = {"Bedsheet", "Pillow Cover", "Blanket", "Curtain", "Sofa Cover", "Carpet/Rug", "Table Cloth", "Towel"};
    private static final int[] HOME_ACC_ICONS = {
            R.drawable.bedsheet,
            R.drawable.pellow_cover,
            R.drawable.blanket,
            R.drawable.curtain,
            R.drawable.sofa_cover,
            R.drawable.carpet,
            R.drawable.table_cover,
            R.drawable.towel
    };
    private static final java.util.Map<String, Integer> HOME_ACC_PRICES = new java.util.HashMap<>();
    static {
        HOME_ACC_PRICES.put("Bedsheet", 80);
        HOME_ACC_PRICES.put("Pillow Cover", 40);
        HOME_ACC_PRICES.put("Blanket", 200);
        HOME_ACC_PRICES.put("Curtain", 150);
        HOME_ACC_PRICES.put("Sofa Cover", 250);
        HOME_ACC_PRICES.put("Carpet/Rug", 300);
        HOME_ACC_PRICES.put("Table Cloth", 60);
        HOME_ACC_PRICES.put("Towel", 30);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(0); // white icons on gradient

        // Get service details from intent
        String serviceName    = getIntent().getStringExtra("service_name");
        String serviceSubtitle = getIntent().getStringExtra("service_subtitle");
        String serviceEmoji   = getIntent().getStringExtra("service_emoji");
        int serviceImageRes   = getIntent().getIntExtra("service_image_res", R.drawable.img_dry_clean_new);
        boolean addMoreFlow   = getIntent().getBooleanExtra("add_more_flow", false);
        currentServiceName    = serviceName;
        currentServiceImageRes = serviceImageRes;

        // Bind views
        TextView tvServiceName     = findViewById(R.id.tvServiceName);
        TextView tvServiceSubtitle = findViewById(R.id.tvServiceSubtitle);
        ImageView ivServiceImage   = findViewById(R.id.ivServiceImage);
        tvTypeLabel                = findViewById(R.id.tvTypeLabel);
        itemsContainer             = findViewById(R.id.itemsContainer);

        if (serviceName != null) tvServiceName.setText(serviceName);
        if (serviceSubtitle != null) tvServiceSubtitle.setText(serviceSubtitle);
        if (ivServiceImage != null) ivServiceImage.setImageResource(serviceImageRes);

        tvTotalPrice    = findViewById(R.id.tvTotalPrice);
        tvBottomTotal   = findViewById(R.id.tvBottomTotal);
        tvPriceBreakdown = findViewById(R.id.tvPriceBreakdown);
        tvCartBadge     = findViewById(R.id.tvCartBadge);

        // ── CUSTOMIZATION FOR DIFFERENT SERVICES ──
        String normalizedName = serviceName != null ? serviceName.trim() : "";

        // Services that use Men/Women apparel toggle
        boolean isLaundry   = normalizedName.equalsIgnoreCase("Wash & Fold") || normalizedName.equalsIgnoreCase("Laundry");
        boolean isWashIron  = normalizedName.equalsIgnoreCase("Wash & Iron");
        boolean isSteamIron = normalizedName.equalsIgnoreCase("Steam Iron") || normalizedName.equalsIgnoreCase("Ironing");
        boolean isDryClean  = normalizedName.equalsIgnoreCase("Dry Cleaning") || normalizedName.toLowerCase().startsWith("dry");
        isApparelService    = isLaundry || isWashIron || isSteamIron || isDryClean;

        android.view.View layoutGenderToggle = findViewById(R.id.layoutGenderToggle);
        LinearLayout btnMen   = findViewById(R.id.btnMen);
        LinearLayout btnWomen = findViewById(R.id.btnWomen);

        if ("Shoe Care".equalsIgnoreCase(normalizedName)) {
            if (tvTypeLabel != null) tvTypeLabel.setText("Select Shoes");
            currentTypes = SHOE_TYPES;
            currentIcons = SHOE_ICONS;
            currentPriceMap = SHOE_PRICES;
            for (String type : currentTypes) itemQuantities.put(type, 0);
        } else if ("Home Accessories".equalsIgnoreCase(normalizedName)) {
            if (tvTypeLabel != null) tvTypeLabel.setText("Select Items");
            currentTypes = HOME_ACC_TYPES;
            currentIcons = HOME_ACC_ICONS;
            currentPriceMap = HOME_ACC_PRICES;
            for (String type : currentTypes) itemQuantities.put(type, 0);
        } else if (isApparelService) {
            if (tvTypeLabel != null) tvTypeLabel.setText("Select Clothes");
            if (layoutGenderToggle != null) layoutGenderToggle.setVisibility(android.view.View.VISIBLE);
            if (tvPriceBreakdown != null) tvPriceBreakdown.setVisibility(android.view.View.GONE);

            // Build price maps for Men & Women based on service
            buildGenderPriceMaps(normalizedName);

            // Init quantities
            for (String t : MEN_TYPES)   menQuantities.put(t, 0);
            for (String t : WOMEN_TYPES) womenQuantities.put(t, 0);

            // Start with Men selected
            isMenSelected   = true;
            currentTypes    = MEN_TYPES;
            currentIcons    = MEN_ICONS;
            currentPriceMap = menPriceMap;
            itemQuantities  = menQuantities;
        } else {
            if (tvTypeLabel != null) tvTypeLabel.setText("Select Clothes");
            currentTypes = LAUNDRY_TYPES;
            currentIcons = LAUNDRY_ICONS;
            currentPriceMap = LAUNDRY_PRICES;
            for (String type : currentTypes) itemQuantities.put(type, 0);
        }

        loadExistingQuantities();
        populateItemRows();
        updatePriceDisplay();
        updateCartBadge();

        // ── Gender toggle clicks ──
        if (btnMen != null && btnWomen != null) {
            btnMen.setOnClickListener(v -> {
                if (!isMenSelected) {
                    isMenSelected   = true;
                    currentTypes    = MEN_TYPES;
                    currentIcons    = MEN_ICONS;
                    currentPriceMap = menPriceMap;
                    itemQuantities  = menQuantities;
                    btnMen.setBackgroundResource(R.drawable.bg_gender_btn_active);
                    // Update Men text color to white
                    for (int ci = 0; ci < btnMen.getChildCount(); ci++) {
                        if (btnMen.getChildAt(ci) instanceof TextView) {
                            ((TextView) btnMen.getChildAt(ci)).setTextColor(android.graphics.Color.WHITE);
                        }
                    }
                    btnWomen.setBackgroundResource(R.drawable.bg_gender_btn_inactive);
                    // Update Women text color to teal
                    for (int ci = 0; ci < btnWomen.getChildCount(); ci++) {
                        if (btnWomen.getChildAt(ci) instanceof TextView) {
                            ((TextView) btnWomen.getChildAt(ci)).setTextColor(android.graphics.Color.parseColor("#1a9e8f"));
                        }
                    }
                    populateItemRows();
                    updatePriceDisplay();
                }
            });
            btnWomen.setOnClickListener(v -> {
                if (isMenSelected) {
                    isMenSelected   = false;
                    currentTypes    = WOMEN_TYPES;
                    currentIcons    = WOMEN_ICONS;
                    currentPriceMap = womenPriceMap;
                    itemQuantities  = womenQuantities;
                    btnWomen.setBackgroundResource(R.drawable.bg_gender_btn_active);
                    // Update Women text color to white
                    for (int ci = 0; ci < btnWomen.getChildCount(); ci++) {
                        if (btnWomen.getChildAt(ci) instanceof TextView) {
                            ((TextView) btnWomen.getChildAt(ci)).setTextColor(android.graphics.Color.WHITE);
                        }
                    }
                    btnMen.setBackgroundResource(R.drawable.bg_gender_btn_inactive);
                    // Update Men text color to teal
                    for (int ci = 0; ci < btnMen.getChildCount(); ci++) {
                        if (btnMen.getChildAt(ci) instanceof TextView) {
                            ((TextView) btnMen.getChildAt(ci)).setTextColor(android.graphics.Color.parseColor("#1a9e8f"));
                        }
                    }
                    populateItemRows();
                    updatePriceDisplay();
                }
            });
        }

        // Back button
        View btnBack = findViewById(R.id.btnBack);
        applyScaleTouch(btnBack);
        btnBack.setOnClickListener(v -> {
            CartManager.getInstance().setAddMoreSession(false);
            finish();
        });

        // Home button
        View btnHome = findViewById(R.id.btnHome);
        if (btnHome != null) {
            applyScaleTouch(btnHome);
            btnHome.setOnClickListener(v -> {
                CartManager.getInstance().setAddMoreSession(false);
                Intent intent = new Intent(this, com.ankita.freshfold.ui.home.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("open_tab", "HOME");
                startActivity(intent);
                finish();
            });
        }

        // Cart button — removed from this screen
        // View btnCart = findViewById(R.id.btnCart);
        tvCartBadge = null; // no badge on this screen

        // ── Add More Services button ──
        findViewById(R.id.btnAddMoreServices).setOnClickListener(v -> {
            if (!new com.ankita.freshfold.SessionManager(this).isApproved()) {
                android.widget.Toast.makeText(this, "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (saveCurrentItemsToCart()) {
                CartManager.getInstance().setAddMoreSession(true);
                Intent intent = new Intent(this, com.ankita.freshfold.ui.home.MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("open_tab", "HOME"); // Go back to home dashboard
                intent.putExtra("scroll_to_services", true); // Scroll to services section if needed
                startActivity(intent);
                finish();
            }
        });

        // Next button (Add to Cart)
        findViewById(R.id.btnNext).setOnClickListener(v -> {
            if (!new com.ankita.freshfold.SessionManager(this).isApproved()) {
                android.widget.Toast.makeText(this, "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (saveCurrentItemsToCart()) {
                Intent intent = new Intent(this, CartActivity.class);
                startActivity(intent);
            }
        });
    }

    private java.util.Map<String, Integer> menPriceMap   = new java.util.HashMap<>();
    private java.util.Map<String, Integer> womenPriceMap = new java.util.HashMap<>();

    private void buildGenderPriceMaps(String serviceName) {
        menPriceMap.clear();
        womenPriceMap.clear();

        boolean isDry   = serviceName.equalsIgnoreCase("Dry Cleaning") || serviceName.toLowerCase().startsWith("dry");
        boolean isIron  = serviceName.equalsIgnoreCase("Steam Iron") || serviceName.equalsIgnoreCase("Ironing") || serviceName.equalsIgnoreCase("Wash & Iron");
        boolean isWash  = serviceName.equalsIgnoreCase("Wash & Fold") || serviceName.equalsIgnoreCase("Laundry");

        // Men prices
        if (isDry) {
            menPriceMap.put("Shirt", 120); menPriceMap.put("T-Shirt", 100);
            menPriceMap.put("Trousers", 150); menPriceMap.put("Jeans", 160);
            menPriceMap.put("Jacket", 500); menPriceMap.put("Kurta", 200);
        } else if (isIron) {
            menPriceMap.put("Shirt", 12); menPriceMap.put("T-Shirt", 10);
            menPriceMap.put("Trousers", 15); menPriceMap.put("Jeans", 18);
            menPriceMap.put("Jacket", 50); menPriceMap.put("Kurta", 20);
        } else {
            menPriceMap.put("Shirt", 40); menPriceMap.put("T-Shirt", 30);
            menPriceMap.put("Trousers", 50); menPriceMap.put("Jeans", 55);
            menPriceMap.put("Jacket", 120); menPriceMap.put("Kurta", 60);
        }

        // Women prices
        if (isDry) {
            womenPriceMap.put("Saree", 350); womenPriceMap.put("Salwar Suit", 280);
            womenPriceMap.put("Dupatta", 100); womenPriceMap.put("Kurti", 180);
            womenPriceMap.put("Lehenga", 600); womenPriceMap.put("Blouse", 120);
        } else if (isIron) {
            womenPriceMap.put("Saree", 40); womenPriceMap.put("Salwar Suit", 30);
            womenPriceMap.put("Dupatta", 10); womenPriceMap.put("Kurti", 20);
            womenPriceMap.put("Lehenga", 60); womenPriceMap.put("Blouse", 12);
        } else {
            womenPriceMap.put("Saree", 100); womenPriceMap.put("Salwar Suit", 80);
            womenPriceMap.put("Dupatta", 30); womenPriceMap.put("Kurti", 60);
            womenPriceMap.put("Lehenga", 150); womenPriceMap.put("Blouse", 40);
        }
    }

    private void populateItemRows() {
        if (itemsContainer == null) return;
        itemsContainer.removeAllViews();

        for (int i = 0; i < currentTypes.length; i++) {
            final String type = currentTypes[i];
            final int iconRes = currentIcons[i];
            final int price = currentPriceMap.get(type);

            View rowView = getLayoutInflater().inflate(R.layout.item_service_selection, itemsContainer, false);

            com.google.android.material.card.MaterialCardView card =
                    rowView.findViewById(R.id.cardItemRow);
            ImageView ivIcon  = rowView.findViewById(R.id.ivItemIcon);
            TextView tvName   = rowView.findViewById(R.id.tvItemName);
            TextView tvPrice  = rowView.findViewById(R.id.tvItemPrice);
            TextView tvQty    = rowView.findViewById(R.id.tvRowQuantity);
            View btnMinus     = rowView.findViewById(R.id.btnRowMinus);
            View btnPlus      = rowView.findViewById(R.id.btnRowPlus);
            TextView tvAmount = rowView.findViewById(R.id.tvRowAmount);

            ivIcon.setImageResource(iconRes);
            tvName.setText(type);
            tvPrice.setText("₹" + price + " / piece");
            tvQty.setText(String.valueOf(itemQuantities.get(type)));
            updateRowHighlight(card, itemQuantities.get(type));
            updateRowAmount(tvAmount, itemQuantities.get(type), price);

            btnPlus.setOnClickListener(v -> {
                int q = itemQuantities.get(type) + 1;
                itemQuantities.put(type, q);
                tvQty.setText(String.valueOf(q));
                updateRowHighlight(card, q);
                updateRowAmount(tvAmount, q, price);
                animateBounce(v);
                syncCartWithCurrentSelections();
                updatePriceDisplay();
                updateCartBadge();
            });

            btnMinus.setOnClickListener(v -> {
                int q = itemQuantities.get(type);
                if (q > 0) {
                    q--;
                    itemQuantities.put(type, q);
                    tvQty.setText(String.valueOf(q));
                    updateRowHighlight(card, q);
                    updateRowAmount(tvAmount, q, price);
                    animateBounce(v);
                    syncCartWithCurrentSelections();
                    updatePriceDisplay();
                    updateCartBadge();
                }
            });

            itemsContainer.addView(rowView);
        }
    }

    /** Show ₹amount next to + button when count > 0, hide when 0 */
    private void updateRowAmount(TextView tvAmount, int qty, int price) {
        if (tvAmount == null) return;
        if (qty > 0) {
            tvAmount.setVisibility(View.VISIBLE);
            tvAmount.setText("₹" + (qty * price));
        } else {
            tvAmount.setVisibility(View.GONE);
        }
    }

    /** Highlight card border + tint when qty > 0 */
    private void updateRowHighlight(com.google.android.material.card.MaterialCardView card, int qty) {
        if (card == null) return;
        if (qty > 0) {
            card.setStrokeColor(getResources().getColor(R.color.brand_primary, getTheme()));
            card.setStrokeWidth(Math.round(getResources().getDisplayMetrics().density * 1.5f));
            card.setCardBackgroundColor(getResources().getColor(R.color.tint_primary, getTheme()));
        } else {
            card.setStrokeColor(getResources().getColor(R.color.stroke_light, getTheme()));
            card.setStrokeWidth(Math.round(getResources().getDisplayMetrics().density * 1f));
            card.setCardBackgroundColor(getResources().getColor(android.R.color.white, getTheme()));
        }
    }

    /** Quick scale-bounce on +/- tap */
    private void animateBounce(View v) {
        v.animate().scaleX(0.82f).scaleY(0.82f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80).start())
                .start();
    }

    @Override
    public void onBackPressed() {
        // Reset add-more session flag when going back
        CartManager.getInstance().setAddMoreSession(false);
        super.onBackPressed();
    }

    private void updatePriceDisplay() {
        int grandTotal = 0;
        int totalItems = 0;

        if (isApparelService) {
            // Count both Men and Women selections
            for (String type : MEN_TYPES) {
                int q = menQuantities.containsKey(type) ? menQuantities.get(type) : 0;
                totalItems += q;
                if (menPriceMap.containsKey(type)) grandTotal += q * menPriceMap.get(type);
            }
            for (String type : WOMEN_TYPES) {
                int q = womenQuantities.containsKey(type) ? womenQuantities.get(type) : 0;
                totalItems += q;
                if (womenPriceMap.containsKey(type)) grandTotal += q * womenPriceMap.get(type);
            }
        } else {
            for (String type : currentTypes) {
                int q = itemQuantities.get(type);
                totalItems += q;
                grandTotal += q * currentPriceMap.get(type);
            }
        }

        tvTotalPrice.setText("₹" + grandTotal);
        tvBottomTotal.setText("₹" + grandTotal);

        if (totalItems == 0) {
            tvPriceBreakdown.setText("0 items");
        } else {
            tvPriceBreakdown.setText(totalItems + (totalItems == 1 ? " item" : " items"));
        }
    }

    private void updateCartBadge() {
        int count = CartManager.getInstance().getPendingCartItems().size();
        if (tvCartBadge != null) {
            tvCartBadge.setText(count > 0 ? String.valueOf(count) : "0");
        }
    }

    private void loadExistingQuantities() {
        List<CartItem> existing = CartManager.getInstance().getPendingCartItems();
        for (CartItem item : existing) {
            if (item.getName().equalsIgnoreCase(currentServiceName)) {
                String desc = item.getDescription();
                if (isApparelService) {
                    if (desc.startsWith("Men - ")) {
                        String type = desc.replace("Men - ", "");
                        menQuantities.put(type, item.getQuantity());
                    } else if (desc.startsWith("Women - ")) {
                        String type = desc.replace("Women - ", "");
                        womenQuantities.put(type, item.getQuantity());
                    }
                } else {
                    itemQuantities.put(desc, item.getQuantity());
                }
            }
        }
    }

    private void syncCartWithCurrentSelections() {
        // Always remove old items for THIS service first (to replace with new selection)
        CartManager.getInstance().removeItemsByService(currentServiceName);

        List<CartItem> newItems = new java.util.ArrayList<>();
        String instructions = "";

        if (isApparelService) {
            for (int i = 0; i < MEN_TYPES.length; i++) {
                String t = MEN_TYPES[i];
                int q = menQuantities.getOrDefault(t, 0);
                if (q > 0) newItems.add(new CartItem(currentServiceName, "Men - " + t, q, menPriceMap.get(t), "item", "🧺", instructions, MEN_ICONS[i]));
            }
            for (int i = 0; i < WOMEN_TYPES.length; i++) {
                String t = WOMEN_TYPES[i];
                int q = womenQuantities.getOrDefault(t, 0);
                if (q > 0) newItems.add(new CartItem(currentServiceName, "Women - " + t, q, womenPriceMap.get(t), "item", "🧺", instructions, WOMEN_ICONS[i]));
            }
        } else {
            for (int i = 0; i < currentTypes.length; i++) {
                String t = currentTypes[i];
                int q = itemQuantities.getOrDefault(t, 0);
                if (q > 0) newItems.add(new CartItem(currentServiceName, t, q, currentPriceMap.get(t), "item", "🧺", instructions, currentIcons[i]));
            }
        }

        for (CartItem item : newItems) CartManager.getInstance().addSessionItem(item);
        CartManager.getInstance().updatePendingCartForService(currentServiceName, newItems);
    }

    private boolean saveCurrentItemsToCart() {
        boolean hasItems = false;
        if (isApparelService) {
            for (int q : menQuantities.values()) if (q > 0) { hasItems = true; break; }
            if (!hasItems) for (int q : womenQuantities.values()) if (q > 0) { hasItems = true; break; }
        } else {
            for (int q : itemQuantities.values()) if (q > 0) { hasItems = true; break; }
        }

        if (!hasItems) {
            android.widget.Toast.makeText(this, "Please select at least one item.", android.widget.Toast.LENGTH_SHORT).show();
            return false;
        }

        // Items are already synced in real-time, just return true
        return true;
    }



    @SuppressLint("ClickableViewAccessibility")
    private void applyScaleTouch(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press_scale_down));
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.press_scale_up));
            }
            return false;
        });
    }
}
