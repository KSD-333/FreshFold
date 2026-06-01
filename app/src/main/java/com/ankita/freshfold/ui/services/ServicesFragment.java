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

    // Maps a normalised service key → card view ID
    private static final Map<String, Integer> SERVICE_MAP = new HashMap<>();

    static {
        // Key format: lowercase, no spaces/special chars
        // Value: cardViewId
        SERVICE_MAP.put("wash&fold",           R.id.cardLaundry);
        SERVICE_MAP.put("washfold",            R.id.cardLaundry);
        SERVICE_MAP.put("wash_fold",           R.id.cardLaundry);
        SERVICE_MAP.put("laundry",             R.id.cardLaundry);

        SERVICE_MAP.put("steamiron",           R.id.cardIron);
        SERVICE_MAP.put("steam_iron",          R.id.cardIron);
        SERVICE_MAP.put("steam iron",          R.id.cardIron);
        SERVICE_MAP.put("iron",                R.id.cardIron);

        SERVICE_MAP.put("wash&iron",           R.id.cardWashIron);
        SERVICE_MAP.put("washiron",            R.id.cardWashIron);
        SERVICE_MAP.put("wash_iron",           R.id.cardWashIron);
        SERVICE_MAP.put("wash iron",           R.id.cardWashIron);

        SERVICE_MAP.put("drycleaning",         R.id.cardDryClean);
        SERVICE_MAP.put("dry_cleaning",        R.id.cardDryClean);
        SERVICE_MAP.put("dry cleaning",        R.id.cardDryClean);
        SERVICE_MAP.put("dryclean",            R.id.cardDryClean);
        SERVICE_MAP.put("dry_clean",           R.id.cardDryClean);

        SERVICE_MAP.put("shoecare",            R.id.cardShoe);
        SERVICE_MAP.put("shoe_care",           R.id.cardShoe);
        SERVICE_MAP.put("shoe care",           R.id.cardShoe);
        SERVICE_MAP.put("shoes",               R.id.cardShoe);

        SERVICE_MAP.put("homeaccessories",     R.id.cardBlanket);
        SERVICE_MAP.put("home_accessories",    R.id.cardBlanket);
        SERVICE_MAP.put("home accessories",    R.id.cardBlanket);
        SERVICE_MAP.put("blanket",             R.id.cardBlanket);
        SERVICE_MAP.put("accessories",         R.id.cardBlanket);
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
        
        com.ankita.freshfold.FranchiseManager cache = com.ankita.freshfold.FranchiseManager.getInstance();
        if (cache.getCachedServices() != null) {
            applyServicesToUI(view, cache.getCachedServices());
            return;
        }

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
            
            cache.setFranchiseId(franchiseId);

            // Step 2: fetch services from that franchise
            serviceRepo.getFranchiseServices(franchiseId).addOnSuccessListener(querySnapshot -> {
                if (!isAdded() || querySnapshot == null) return;
                
                cache.setCachedServices(querySnapshot.getDocuments());
                applyServicesToUI(view, querySnapshot.getDocuments());
            }).addOnFailureListener(e -> {
                // Silently fail — cards remain clickable
            });
        }).addOnFailureListener(e -> {
            // Silently fail
        });
    }

    private void applyServicesToUI(View view, java.util.List<DocumentSnapshot> documents) {
        for (DocumentSnapshot doc : documents) {
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
            Integer id = SERVICE_MAP.get(key);
            if (id == null) id = SERVICE_MAP.get(name.toLowerCase().trim());
            if (id == null) {
                // Try matching the doc ID
                id = SERVICE_MAP.get(doc.getId().toLowerCase().trim());
            }

            if (id != null) {
                applyServiceStatus(view, id, isActive);
            }
        }
    }

    private void applyServiceStatus(View root, int cardId, boolean isActive) {
        if (!isAdded()) return;
        View card    = root.findViewById(cardId);
        if (card == null) return;

        if (isActive) {
            card.setAlpha(1f);
            // Keep the original click listener set in setupCardClicks
        } else {
            card.setAlpha(0.5f);
            // Override click to show toast instead of opening the service
            card.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                    "This service is currently not active.",
                    Toast.LENGTH_SHORT).show()
            );
        }
    }
}
