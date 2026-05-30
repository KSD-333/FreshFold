package com.ankita.freshfold.ui.services;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.data.repository.ServiceRepository;
import com.ankita.freshfold.data.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ServicesFragment extends Fragment {

    // Maps a normalised service key → card view ID and overlay view ID
    private static final Map<String, int[]> SERVICE_MAP = new HashMap<>();

    static {
        // Key format: lowercase, no spaces/special chars
        // Value: [cardViewId, overlayViewId]
        SERVICE_MAP.put("wash&fold",           new int[]{R.id.cardLaundry,   R.id.overlayLaundry});
        SERVICE_MAP.put("washfold",            new int[]{R.id.cardLaundry,   R.id.overlayLaundry});
        SERVICE_MAP.put("wash_fold",           new int[]{R.id.cardLaundry,   R.id.overlayLaundry});
        SERVICE_MAP.put("laundry",             new int[]{R.id.cardLaundry,   R.id.overlayLaundry});

        SERVICE_MAP.put("steamiron",           new int[]{R.id.cardIron,      R.id.overlayIron});
        SERVICE_MAP.put("steam_iron",          new int[]{R.id.cardIron,      R.id.overlayIron});
        SERVICE_MAP.put("steam iron",          new int[]{R.id.cardIron,      R.id.overlayIron});
        SERVICE_MAP.put("iron",                new int[]{R.id.cardIron,      R.id.overlayIron});

        SERVICE_MAP.put("wash&iron",           new int[]{R.id.cardWashIron,  R.id.overlayWashIron});
        SERVICE_MAP.put("washiron",            new int[]{R.id.cardWashIron,  R.id.overlayWashIron});
        SERVICE_MAP.put("wash_iron",           new int[]{R.id.cardWashIron,  R.id.overlayWashIron});
        SERVICE_MAP.put("wash iron",           new int[]{R.id.cardWashIron,  R.id.overlayWashIron});

        SERVICE_MAP.put("drycleaning",         new int[]{R.id.cardDryClean,  R.id.overlayDryClean});
        SERVICE_MAP.put("dry_cleaning",        new int[]{R.id.cardDryClean,  R.id.overlayDryClean});
        SERVICE_MAP.put("dry cleaning",        new int[]{R.id.cardDryClean,  R.id.overlayDryClean});
        SERVICE_MAP.put("dryclean",            new int[]{R.id.cardDryClean,  R.id.overlayDryClean});
        SERVICE_MAP.put("dry_clean",           new int[]{R.id.cardDryClean,  R.id.overlayDryClean});

        SERVICE_MAP.put("shoecare",            new int[]{R.id.cardShoe,      R.id.overlayShoe});
        SERVICE_MAP.put("shoe_care",           new int[]{R.id.cardShoe,      R.id.overlayShoe});
        SERVICE_MAP.put("shoe care",           new int[]{R.id.cardShoe,      R.id.overlayShoe});
        SERVICE_MAP.put("shoes",               new int[]{R.id.cardShoe,      R.id.overlayShoe});

        SERVICE_MAP.put("homeaccessories",     new int[]{R.id.cardBlanket,   R.id.overlayBlanket});
        SERVICE_MAP.put("home_accessories",    new int[]{R.id.cardBlanket,   R.id.overlayBlanket});
        SERVICE_MAP.put("home accessories",    new int[]{R.id.cardBlanket,   R.id.overlayBlanket});
        SERVICE_MAP.put("blanket",             new int[]{R.id.cardBlanket,   R.id.overlayBlanket});
        SERVICE_MAP.put("accessories",         new int[]{R.id.cardBlanket,   R.id.overlayBlanket});
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        // Back button → go to Home
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof com.ankita.freshfold.ui.home.MainActivity) {
                    com.ankita.freshfold.ui.home.MainActivity main =
                        (com.ankita.freshfold.ui.home.MainActivity) getActivity();
                    main.loadFragment(new com.ankita.freshfold.ui.home.HomeFragment(), "HOME");
                    main.updateNavUI("HOME");
                }
            });
        }

        // Wire click listeners for all 6 cards
        setupCardClicks(view);

        // Fetch franchise services and apply active/inactive status
        loadFranchiseServiceStatus(view);

        return view;
    }

    // ──────────────────────────────────────────────────────────
    //  Card click listeners (unchanged behaviour)
    // ──────────────────────────────────────────────────────────
    private void setupCardClicks(View view) {
        bindCard(view, R.id.cardLaundry,  () -> startActivity(new Intent(requireContext(), WashFoldInfoActivity.class)));
        bindCard(view, R.id.cardIron,     () -> startActivity(new Intent(requireContext(), SteamIronInfoActivity.class)));
        bindCard(view, R.id.cardWashIron, () -> startActivity(new Intent(requireContext(), WashIronInfoActivity.class)));
        bindCard(view, R.id.cardDryClean, () -> startActivity(new Intent(requireContext(), DryCleanInfoActivity.class)));
        bindCard(view, R.id.cardShoe,     () -> startActivity(new Intent(requireContext(), ShoeCareInfoActivity.class)));
        bindCard(view, R.id.cardBlanket,  () -> startActivity(new Intent(requireContext(), HomeAccessoriesInfoActivity.class)));
    }

    private void bindCard(View root, int cardId, Runnable action) {
        View card = root.findViewById(cardId);
        if (card != null) card.setOnClickListener(v -> action.run());
    }

    // ──────────────────────────────────────────────────────────
    //  Franchise service status fetch
    // ──────────────────────────────────────────────────────────
    private void loadFranchiseServiceStatus(View view) {
        if (getContext() == null) return;

        SessionManager session = new SessionManager(requireContext());
        String phone = session.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        UserRepository userRepo = new UserRepository();
        ServiceRepository serviceRepo = new ServiceRepository();

        // Step 1: get user's franchiseId
        userRepo.getUser(phone).addOnSuccessListener(userDoc -> {
            if (!isAdded() || userDoc == null || !userDoc.exists()) return;

            String franchiseId = userDoc.getString("franchiseId");
            if (franchiseId == null || franchiseId.isEmpty()) return; // no franchise assigned

            // Step 2: fetch services from that franchise
            serviceRepo.getFranchiseServices(franchiseId).addOnSuccessListener(querySnapshot -> {
                if (!isAdded() || querySnapshot == null) return;

                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    // Service name: try "name" field first, then document ID
                    String name = doc.getString("name");
                    if (name == null || name.isEmpty()) name = doc.getId();

                    Boolean active = doc.getBoolean("active");
                    boolean isActive = active == null || active; // default = active

                    // Normalise the name to match SERVICE_MAP keys
                    String key = name.toLowerCase().trim()
                            .replace("&amp;", "&")
                            .replace(" & ", "&")
                            .replace("&", ""); // "wash & fold" → "washfold"

                    // Try exact key first, then with spaces removed
                    int[] ids = SERVICE_MAP.get(key);
                    if (ids == null) ids = SERVICE_MAP.get(name.toLowerCase().trim());
                    if (ids == null) {
                        // Try matching the doc ID
                        ids = SERVICE_MAP.get(doc.getId().toLowerCase().trim());
                    }

                    if (ids != null) {
                        applyServiceStatus(view, ids[0], ids[1], isActive);
                    }
                }
            }).addOnFailureListener(e -> {
                // Silently fail — cards remain clickable
            });
        }).addOnFailureListener(e -> {
            // Silently fail
        });
    }

    /**
     * If isActive → card is clickable, overlay hidden.
     * If !isActive → overlay shown, card click blocked with a toast.
     */
    private void applyServiceStatus(View root, int cardId, int overlayId, boolean isActive) {
        if (!isAdded()) return;
        View card    = root.findViewById(cardId);
        View overlay = root.findViewById(overlayId);
        if (card == null || overlay == null) return;

        if (isActive) {
            overlay.setVisibility(View.GONE);
            card.setAlpha(1f);
            // Keep the original click listener set in setupCardClicks
        } else {
            overlay.setVisibility(View.VISIBLE);
            card.setAlpha(0.85f);
            // Override click to show toast instead of opening the service
            card.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                    "This service is currently unavailable in your area.",
                    Toast.LENGTH_SHORT).show()
            );
        }
    }
}
