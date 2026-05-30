package com.ankita.freshfold.ui.home;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.ui.services.ServiceDetailActivity;
import com.ankita.freshfold.viewmodel.HomeViewModel;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {
    private SessionManager sessionManager;
    private HomeViewModel viewModel;

    // Auto-scroll banner
    private static final long BANNER_SCROLL_DELAY_MS = 3000L;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private Runnable bannerRunnable;
    private ViewPager2 bannerViewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionManager = new SessionManager(requireContext());
        
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvUserName = view.findViewById(R.id.tvUserName);

        if (tvGreeting != null) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour >= 4 && hour < 12) greeting = "Good Morning 👋";
            else if (hour >= 12 && hour < 16) greeting = "Good Afternoon 👋";
            else if (hour >= 16 && hour < 20) greeting = "Good Evening 👋";
            else greeting = "Good Night 🌙";
            tvGreeting.setText(greeting);
        }

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Show cached name instantly — no "Loading..." flicker
        String cachedName = sessionManager.getUserName();
        if (tvUserName != null) {
            tvUserName.setText(cachedName != null && !cachedName.isEmpty() ? cachedName : "");
        }

        setupObservers(tvUserName, view);
        viewModel.fetchUserInfo(sessionManager.getUserPhone());
        
        setupInteractions(view);
        updateServiceLockState(view, sessionManager.isApproved());
        setupBannerCarousel(view);

        // Scroll to Our Services section if coming from Add More Services
        if (getArguments() != null && getArguments().getBoolean("scroll_to_services", false)) {
            View servicesLabel = view.findViewById(R.id.tvOurServicesLabel);
            view.post(() -> {
                // Find the NestedScrollView (root of fragment_home)
                if (view instanceof NestedScrollView) {
                    NestedScrollView nsv = (NestedScrollView) view;
                    if (servicesLabel != null) {
                        nsv.smoothScrollTo(0, servicesLabel.getTop());
                    }
                }
            });
        }

        return view;
    }

    private void setupObservers(TextView tvUserName, View view) {
        viewModel.userName.observe(getViewLifecycleOwner(), name -> {
            if (tvUserName != null) tvUserName.setText(name);
        });

        viewModel.isApproved.observe(getViewLifecycleOwner(), isApproved -> {
            if (isApproved != null) {
                sessionManager.setApproved(isApproved);
                updateServiceLockState(view, isApproved);
            }
        });
    }

    private void updateServiceLockState(View view, boolean isApproved) {
        View layoutPendingApproval = view.findViewById(R.id.layoutPendingApproval);
        if (layoutPendingApproval != null) {
            layoutPendingApproval.setVisibility(isApproved ? View.GONE : View.VISIBLE);
        }

        int[] serviceIds = {R.id.btnSteamIron, R.id.btnWashFold, R.id.btnWashIron, R.id.btnDryClean, R.id.btnShoeCare, R.id.btnHomeAccessories};
        for (int id : serviceIds) {
            View v = view.findViewById(id);
            if (v != null) {
                if (isApproved) {
                    v.setAlpha(1.0f);
                    v.setEnabled(true);
                } else {
                    v.setAlpha(0.5f);
                    v.setEnabled(false);
                }
            }
        }
    }

    private void setupInteractions(View view) {
        View btnNotification = view.findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                android.widget.Toast.makeText(requireContext(), "Notifications coming soon!", android.widget.Toast.LENGTH_SHORT).show();
            });
            applyScaleInteraction(btnNotification);
        }

        int[] serviceIds = {R.id.btnSteamIron, R.id.btnWashFold, R.id.btnWashIron, R.id.btnDryClean, R.id.btnShoeCare, R.id.btnHomeAccessories};

        String[] serviceNames    = {"Steam Iron",                             "Wash & Fold",                    "Wash & Iron",                "Dry Cleaning",               "Shoe Care",                  "Home Accessories"};
        String[] serviceSubtitles= {"Perfectly pressed clothes.",             "Premium wash & fold service.",   "Wash and iron together.",    "Professional deep cleaning.", "Shine & protect your shoes.", "Clean your home accessories."};
        int[]    serviceImages   = {R.drawable.img_ironing_new,               R.drawable.img_laundry_new,       R.drawable.wash_iron,         R.drawable.img_dry_clean_new,  R.drawable.img_shoe_new,       R.drawable.img_blanket_new};
        int[]    servicePrices   = {20,                                        40,                               50,                           120,                           50,                            150};

        // Check if this fragment was opened via Add More Services
        boolean addMoreFlow = getArguments() != null && getArguments().getBoolean("add_more_flow", false);

        for (int i = 0; i < serviceIds.length; i++) {
            View v = view.findViewById(serviceIds[i]);
            if (v != null) {
                final String name     = serviceNames[i];
                final String subtitle = serviceSubtitles[i];
                final int    price    = servicePrices[i];
                final int    imageRes = serviceImages[i];
                
                // Set the click action first
                v.setOnClickListener(btn -> {
                    Intent intent = new Intent(requireContext(), ServiceDetailActivity.class);
                    intent.putExtra("service_name",     name);
                    intent.putExtra("service_subtitle", subtitle);
                    intent.putExtra("service_image_res", imageRes);
                    intent.putExtra("price_per_item",   price);
                    intent.putExtra("add_more_flow",    addMoreFlow);
                    startActivity(intent);
                });
                
                // Apply the premium touch scaling effect (same as Quick Actions)
                applyScaleInteraction(v);
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void applyScaleInteraction(View v) {
        v.setOnTouchListener((view, event) -> {
            if (!view.isEnabled()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    android.widget.Toast.makeText(requireContext(), "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                view.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.press_scale_down));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    view.performClick();
                }
                view.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.press_scale_up));
            }
            return true;
        });
    }

    private void setupBannerCarousel(View view) {
        bannerViewPager = view.findViewById(R.id.bannerViewPager);
        if (bannerViewPager == null) return;

        LinearLayout dotsLayout = view.findViewById(R.id.bannerDotsLayout);

        List<Integer> bannerImages = Arrays.asList(
                R.drawable.img_laundry_new,
                R.drawable.img_carousel_1,
                R.drawable.img_ironing_new
        );

        BannerAdapter adapter = new BannerAdapter(bannerImages);
        bannerViewPager.setAdapter(adapter);

        // ── Build dot indicators ──
        if (dotsLayout != null) {
            dotsLayout.removeAllViews();
            for (int i = 0; i < bannerImages.size(); i++) {
                View dot = new View(requireContext());
                int size = (int) (8 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                int margin = (int) (4 * getResources().getDisplayMetrics().density);
                params.setMargins(margin, 0, margin, 0);
                dot.setLayoutParams(params);
                dot.setBackgroundResource(R.drawable.bg_dot_purple);
                dot.setAlpha(i == 0 ? 1.0f : 0.35f);
                dotsLayout.addView(dot);
            }
        }

        // ── Page change callback to update dots ──
        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (dotsLayout == null) return;
                for (int i = 0; i < dotsLayout.getChildCount(); i++) {
                    dotsLayout.getChildAt(i).setAlpha(i == position ? 1.0f : 0.35f);
                }
            }
        });

        // Auto-scroll
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerViewPager == null || adapter.getItemCount() == 0) return;
                int next = (bannerViewPager.getCurrentItem() + 1) % adapter.getItemCount();
                bannerViewPager.setCurrentItem(next, true);
                bannerHandler.postDelayed(this, BANNER_SCROLL_DELAY_MS);
            }
        };
        bannerHandler.postDelayed(bannerRunnable, BANNER_SCROLL_DELAY_MS);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
            bannerHandler.postDelayed(bannerRunnable, BANNER_SCROLL_DELAY_MS);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
        bannerViewPager = null;
    }
}
