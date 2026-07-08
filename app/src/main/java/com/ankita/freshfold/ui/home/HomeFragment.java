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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ankita.freshfold.NotificationHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {
    
    private static final java.util.Map<String, Integer> SERVICE_MAP = new java.util.HashMap<>();
    static {
        SERVICE_MAP.put("wash&fold",           R.id.btnWashFold);
        SERVICE_MAP.put("washfold",            R.id.btnWashFold);
        SERVICE_MAP.put("wash_fold",           R.id.btnWashFold);
        SERVICE_MAP.put("laundry",             R.id.btnWashFold);
        SERVICE_MAP.put("steamiron",           R.id.btnSteamIron);
        SERVICE_MAP.put("steam_iron",          R.id.btnSteamIron);
        SERVICE_MAP.put("steam iron",          R.id.btnSteamIron);
        SERVICE_MAP.put("iron",                R.id.btnSteamIron);
        SERVICE_MAP.put("wash&iron",           R.id.btnWashIron);
        SERVICE_MAP.put("washiron",            R.id.btnWashIron);
        SERVICE_MAP.put("wash_iron",           R.id.btnWashIron);
        SERVICE_MAP.put("wash iron",           R.id.btnWashIron);
        SERVICE_MAP.put("drycleaning",         R.id.btnDryClean);
        SERVICE_MAP.put("dry_cleaning",        R.id.btnDryClean);
        SERVICE_MAP.put("dry cleaning",        R.id.btnDryClean);
        SERVICE_MAP.put("dryclean",            R.id.btnDryClean);
        SERVICE_MAP.put("dry_clean",           R.id.btnDryClean);
        SERVICE_MAP.put("shoecare",            R.id.btnShoeCare);
        SERVICE_MAP.put("shoe_care",           R.id.btnShoeCare);
        SERVICE_MAP.put("shoe care",           R.id.btnShoeCare);
        SERVICE_MAP.put("shoes",               R.id.btnShoeCare);
        SERVICE_MAP.put("homeaccessories",     R.id.btnHomeAccessories);
        SERVICE_MAP.put("home_accessories",    R.id.btnHomeAccessories);
        SERVICE_MAP.put("home accessories",    R.id.btnHomeAccessories);
        SERVICE_MAP.put("blanket",             R.id.btnHomeAccessories);
        SERVICE_MAP.put("accessories",         R.id.btnHomeAccessories);
    }

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
        loadFranchiseServiceStatus(view);
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
                    if (!"inactive".equals(v.getTag())) {
                        v.setAlpha(1.0f);
                    }
                    v.setEnabled(true);
                } else {
                    v.setAlpha(0.5f);
                    v.setEnabled(false);
                }
            }
        }
    }

    private void loadFranchiseServiceStatus(View view) {
        if (getContext() == null) return;
        
        com.ankita.freshfold.FranchiseManager cache = com.ankita.freshfold.FranchiseManager.getInstance();
        if (cache.getCachedServices() != null) {
            applyServicesToUI(view, cache.getCachedServices());
            return;
        }

        String phone = sessionManager.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        com.ankita.freshfold.data.repository.UserRepository userRepo = new com.ankita.freshfold.data.repository.UserRepository();
        com.ankita.freshfold.data.repository.ServiceRepository serviceRepo = new com.ankita.freshfold.data.repository.ServiceRepository();

        userRepo.getUser(phone).addOnSuccessListener(userDoc -> {
            if (!isAdded() || userDoc == null || !userDoc.exists()) return;
            String franchiseId = userDoc.getString("franchiseId");
            if (franchiseId == null || franchiseId.isEmpty()) return;
            
            cache.setFranchiseId(franchiseId);
            
            serviceRepo.getFranchiseServices(franchiseId).addOnSuccessListener(querySnapshot -> {
                if (!isAdded() || querySnapshot == null) return;
                cache.setCachedServices(querySnapshot.getDocuments());
                applyServicesToUI(view, querySnapshot.getDocuments());
            });
        });
    }

    private void applyServicesToUI(View view, java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents) {
        for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
            String name = doc.getString("name");
            if (name == null || name.isEmpty()) name = doc.getId();
            Boolean active = doc.getBoolean("active");
            boolean isActive = active == null || active;
            
            String key = name.toLowerCase().trim()
                    .replace("&amp;", "&").replace(" & ", "&").replace("&", "");
            Integer viewId = SERVICE_MAP.get(key);
            if (viewId == null) viewId = SERVICE_MAP.get(name.toLowerCase().trim());
            if (viewId == null) viewId = SERVICE_MAP.get(doc.getId().toLowerCase().trim());
            
            if (viewId != null && !isActive) {
                View v = view.findViewById(viewId);
                if (v != null) {
                    v.setAlpha(0.5f);
                    v.setTag("inactive");
                }
            }
        }
    }

    private void setupInteractions(View view) {
        View btnNotification = view.findViewById(R.id.btnNotification);
        View viewNotificationBadge = view.findViewById(R.id.viewNotificationBadge);

        if (viewNotificationBadge != null) {
            boolean hasUnread = com.ankita.freshfold.NotificationHelper.hasUnreadNotifications(requireContext());
            viewNotificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
        }

        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), com.ankita.freshfold.ui.profile.NotificationActivity.class));
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
            if (!view.isEnabled() || "inactive".equals(view.getTag())) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if ("inactive".equals(view.getTag())) {
                        android.widget.Toast.makeText(requireContext(), "This service is currently not active.", android.widget.Toast.LENGTH_SHORT).show();
                    } else {
                        android.widget.Toast.makeText(requireContext(), "You can book services once the franchise approves your request.", android.widget.Toast.LENGTH_SHORT).show();
                    }
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
        
        // Refresh notification badge when returning to fragment
        View viewNotificationBadge = getView() != null ? getView().findViewById(R.id.viewNotificationBadge) : null;
        if (viewNotificationBadge != null) {
            boolean hasUnread = com.ankita.freshfold.NotificationHelper.hasUnreadNotifications(requireContext());
            viewNotificationBadge.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
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

    private void showNotificationsDialog(View badgeView) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_notifications, null);
        dialog.setContentView(dialogView);

        View layoutEmptyState = dialogView.findViewById(R.id.layoutEmptyState);
        RecyclerView rvNotifications = dialogView.findViewById(R.id.rvNotifications);
        View btnDismiss = dialogView.findViewById(R.id.btnDismiss);

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> dialog.dismiss());
        }

        List<NotificationHelper.AppNotification> list = NotificationHelper.getNotifications(requireContext());

        if (list.isEmpty()) {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
            if (rvNotifications != null) rvNotifications.setVisibility(View.GONE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (rvNotifications != null) {
                rvNotifications.setVisibility(View.VISIBLE);
                rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
                rvNotifications.setAdapter(new NotificationAdapter(list));
            }
        }

        // Mark all as read when opening notifications
        NotificationHelper.markAllAsRead(requireContext());
        if (badgeView != null) {
            badgeView.setVisibility(View.GONE);
        }

        dialog.show();
    }

    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationHelper.AppNotification> list;

        public NotificationAdapter(List<NotificationHelper.AppNotification> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationHelper.AppNotification item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);

            // SimpleDateFormat for timestamp
            String dateStr = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(new Date(item.timestamp));
            holder.tvTime.setText(dateStr);

            // Unread dot indicator
            holder.viewUnreadDot.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;
            View viewUnreadDot;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
                tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
                tvTime = itemView.findViewById(R.id.tvNotificationTime);
                viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
            }
        }
    }
}
