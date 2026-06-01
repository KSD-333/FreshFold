package com.ankita.freshfold.ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;


import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.DeliveryDashboardActivity;
import com.ankita.freshfold.ui.cart.CartActivity;
import com.ankita.freshfold.ui.cart.CartPreviewFragment;
import com.ankita.freshfold.ui.profile.ProfileFragment;
import com.ankita.freshfold.R;
import com.ankita.freshfold.ui.services.ServicesFragment;
import com.ankita.freshfold.ui.wallet.WalletFragment;

import com.razorpay.PaymentResultListener;
import com.ankita.freshfold.viewmodel.WalletViewModel;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity implements CartPreviewFragment.CartBadgeUpdater, PaymentResultListener {

    private LinearLayout navHome, navServices, navCart, navWallet, navProfile;
    private ImageView ivHome, ivServices, ivCart, ivWallet, ivProfile;
    private TextView tvHome, tvServices, tvCart, tvWallet, tvProfile;
    private TextView btnProceedCheckout;
    private long backPressedTime;
    private android.widget.Toast backToast;
    private String currentTab = "HOME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(android.graphics.Color.WHITE);

        navHome = findViewById(R.id.navHome);
        navServices = findViewById(R.id.navServices);
        navCart = findViewById(R.id.navCart);
        navWallet = findViewById(R.id.navWallet);
        navProfile = findViewById(R.id.navProfile);

        ivHome = findViewById(R.id.ivHome);
        ivServices = findViewById(R.id.ivServices);
        ivCart = findViewById(R.id.ivCart);
        ivWallet = findViewById(R.id.ivWallet);
        ivProfile = findViewById(R.id.ivProfile);

        tvHome = findViewById(R.id.tvHome);
        tvServices = findViewById(R.id.tvServices);
        tvCart = findViewById(R.id.tvCart);
        tvWallet = findViewById(R.id.tvWallet);
        tvProfile = findViewById(R.id.tvProfile);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "HOME");
            updateNavUI("HOME");
        }

        // Handle Add More Services navigation from CartActivity
        handleIntent(getIntent());

        // Proceed to Checkout button — visible only when cart has items
        btnProceedCheckout = findViewById(R.id.btnProceedCheckout);
        btnProceedCheckout.setOnClickListener(v -> {
            Intent cartIntent = new Intent(this, CartActivity.class);
            cartIntent.putExtra("from_nav", true);
            startActivity(cartIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment(), "HOME");
            updateNavUI("HOME");
        });

        navServices.setOnClickListener(v -> {
            loadFragment(new ServicesFragment(), "SERVICES");
            updateNavUI("SERVICES");
        });

        navCart.setOnClickListener(v -> {
            if (!new com.ankita.freshfold.SessionManager(this).isApproved()) {
                android.widget.Toast.makeText(this, "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent cartIntent = new Intent(this, CartActivity.class);
            cartIntent.putExtra("from_nav", true);
            startActivity(cartIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        navWallet.setOnClickListener(v -> {
            if (!new com.ankita.freshfold.SessionManager(this).isApproved()) {
                android.widget.Toast.makeText(this, "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            loadFragment(new WalletFragment(), "WALLET");
            updateNavUI("WALLET");
        });

        navProfile.setOnClickListener(v -> {
            loadFragment(new ProfileFragment(), "PROFILE");
            updateNavUI("PROFILE");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCheckoutButtonVisibility();
    }

    private void updateCheckoutButtonVisibility() {
        if (btnProceedCheckout != null) {
            boolean show = CartManager.getInstance().isAddMoreSession()
                        && !CartManager.getInstance().isEmpty();
            
            // Hide on specific tabs
            if ("WALLET".equals(currentTab) || "PROFILE".equals(currentTab)) {
                show = false;
            }
            
            btnProceedCheckout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void updateCartBadge() {
        // Badge removed as per user request
    }

    @Override
    protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(android.content.Intent intent) {
        if (intent == null) return;



        if (intent.getBooleanExtra("scroll_to_services", false)) {
            // Force reload HomeFragment with add_more_flow=true (bypass isVisible check)
            HomeFragment homeFragment = new HomeFragment();
            android.os.Bundle args = new android.os.Bundle();
            args.putBoolean("scroll_to_services", true);
            args.putBoolean("add_more_flow", true);
            homeFragment.setArguments(args);

            // Force replace — don't use loadFragment (it skips if already visible)
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragmentContainer, homeFragment, "HOME")
                    .commit();
            this.currentTab = "HOME";
            updateNavUI("HOME");
        } else if (intent.hasExtra("open_tab")) {
            String tab = intent.getStringExtra("open_tab");
            if ("HOME".equals(tab)) {
                loadFragment(new HomeFragment(), "HOME");
                updateNavUI("HOME");
            } else if ("SERVICES".equals(tab)) {
                loadFragment(new ServicesFragment(), "SERVICES");
                updateNavUI("SERVICES");
            } else if ("WALLET".equals(tab)) {
                loadFragment(new WalletFragment(), "WALLET");
                updateNavUI("WALLET");
            } else if ("PROFILE".equals(tab)) {
                loadFragment(new ProfileFragment(), "PROFILE");
                updateNavUI("PROFILE");
            }
        }
    }

    public void loadFragment(Fragment fragment, String tag) {
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(tag);
        if (currentFragment != null && currentFragment.isVisible()) {
            return;
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.fragmentContainer, fragment, tag);
        transaction.commit();
        this.currentTab = tag;
    }

    public void updateNavUI(String activeTab) {
        int inactiveColor = Color.parseColor("#A0A8C0");
        int activeColor = ContextCompat.getColor(this, R.color.brand_indigo);

        resetNavItem(ivHome, tvHome, inactiveColor);
        resetNavItem(ivServices, tvServices, inactiveColor);
        resetNavItem(ivCart, tvCart, inactiveColor);
        resetNavItem(ivWallet, tvWallet, inactiveColor);
        resetNavItem(ivProfile, tvProfile, inactiveColor);

        switch (activeTab) {
            case "HOME":
                setActiveNavItem(ivHome, tvHome, activeColor);
                break;
            case "SERVICES":
                setActiveNavItem(ivServices, tvServices, activeColor);
                break;

            case "WALLET":
                setActiveNavItem(ivWallet, tvWallet, activeColor);
                break;
            case "PROFILE":
                setActiveNavItem(ivProfile, tvProfile, activeColor);
                break;
        }
        
        updateCheckoutButtonVisibility();
    }

    private void resetNavItem(ImageView icon, TextView text, int color) {
        icon.setColorFilter(color);
        text.setTextColor(color);
    }

    private void setActiveNavItem(ImageView icon, TextView text, int color) {
        icon.setColorFilter(color);
        text.setTextColor(color);
    }

    @Override
    public void onBackPressed() {
        if (!currentTab.equals("HOME")) {
            loadFragment(new HomeFragment(), "HOME");
            updateNavUI("HOME");
            return;
        }

        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            if (backToast != null) backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            backToast = android.widget.Toast.makeText(getBaseContext(), "Press back again to exit", android.widget.Toast.LENGTH_SHORT);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    private int pendingTopUpAmount = 0;

    public void startRazorpayPayment(int amount) {
        this.pendingTopUpAmount = amount;
        com.razorpay.Checkout checkout = new com.razorpay.Checkout();
        checkout.setKeyID("rzp_test_SPs6AqG8E3r2Cp");
        try {
            org.json.JSONObject options = new org.json.JSONObject();
            options.put("name", "Freshfold Wallet");
            options.put("description", "Wallet Top-Up");
            options.put("currency", "INR");
            options.put("amount", String.valueOf(amount * 100)); // Amount in paisa
            
            com.ankita.freshfold.SessionManager sessionManager = new com.ankita.freshfold.SessionManager(this);
            org.json.JSONObject prefill = new org.json.JSONObject();
            prefill.put("contact", sessionManager.getUserPhone());
            prefill.put("email", "user@freshfold.com");
            options.put("prefill", prefill);
            
            org.json.JSONObject theme = new org.json.JSONObject();
            theme.put("color", "#4F46E5");
            options.put("theme", theme);

            checkout.open(this, options);
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(this, "Error starting payment", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPaymentSuccess(String s) {
        android.widget.Toast.makeText(this, "Payment Successful! ₹" + pendingTopUpAmount + " added to wallet.", android.widget.Toast.LENGTH_SHORT).show();
        // Use the shared ViewModel (same instance as WalletFragment uses)
        WalletViewModel walletViewModel = new ViewModelProvider(this).get(WalletViewModel.class);
        com.ankita.freshfold.SessionManager sessionManager = new com.ankita.freshfold.SessionManager(this);
        String phone = sessionManager.getUserPhone();
        walletViewModel.topUpWallet(phone, (double) pendingTopUpAmount);

        // Navigate to wallet tab so user sees updated balance
        loadFragment(new WalletFragment(), "WALLET");
        updateNavUI("WALLET");
    }

    @Override
    public void onPaymentError(int i, String s) {
        android.widget.Toast.makeText(this, "Payment Failed: " + s, android.widget.Toast.LENGTH_LONG).show();
    }
}
