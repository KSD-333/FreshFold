package com.ankita.freshfold.ui.orders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.Order;
import com.ankita.freshfold.OrderHistoryAdapter;
import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyOrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private OrderHistoryAdapter adapter;
    private TextView tabAll, tabActive, tabCompleted;
    private List<Order> allOrders = new ArrayList<>();
    private String currentFilter = "ALL";

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvOrders    = view.findViewById(R.id.rvOrders);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tabAll      = view.findViewById(R.id.tabAll);
        tabActive   = view.findViewById(R.id.tabActive);
        tabCompleted = view.findViewById(R.id.tabCompleted);

        db             = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(requireContext());

        android.widget.ImageView ivBack = view.findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                if (getActivity() instanceof com.ankita.freshfold.ui.home.MainActivity) {
                    com.ankita.freshfold.ui.home.MainActivity mainActivity =
                            (com.ankita.freshfold.ui.home.MainActivity) getActivity();
                    mainActivity.loadFragment(
                            new com.ankita.freshfold.ui.profile.ProfileFragment(), "PROFILE");
                    mainActivity.updateNavUI("PROFILE");
                } else {
                    requireActivity().onBackPressed();
                }
            });
        }

        setupRecyclerView();
        setupFilters();
        loadOrdersFromFirestore();
    }

    private void loadOrdersFromFirestore() {
        String phone = sessionManager.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        // Show loading state
        layoutEmpty.setVisibility(View.GONE);
        rvOrders.setVisibility(View.GONE);

        db.collection("freshfold").document("app_data")
            .collection("users").document(phone)
            .collection("address")
            .get()
            .continueWithTask(task -> {
                java.util.List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new java.util.ArrayList<>();
                // Fetch root orders (backward compatibility)
                tasks.add(db.collection("freshfold").document("app_data")
                    .collection("users").document(phone)
                    .collection("orders").get());
                
                if (task.isSuccessful() && task.getResult() != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult()) {
                        tasks.add(doc.getReference().collection("orders").get());
                    }
                }
                return com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks);
            })
            .addOnSuccessListener(results -> {
                allOrders.clear();
                for (Object res : results) {
                    com.google.firebase.firestore.QuerySnapshot qs = (com.google.firebase.firestore.QuerySnapshot) res;
                    for (QueryDocumentSnapshot doc : qs) {
                        Order order = documentToOrder(doc);
                        if (order != null) allOrders.add(order);
                    }
                }
                // Sort combined orders by timestamp descending
                java.util.Collections.sort(allOrders, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                updateFilter(currentFilter);

                // Auto-open highlighted order summary if requested (e.g. from notification click navigation)
                if (getArguments() != null && getArguments().containsKey("highlight_order_id")) {
                    String highlightId = getArguments().getString("highlight_order_id");
                    if (highlightId != null && !highlightId.isEmpty()) {
                        for (Order order : allOrders) {
                            if (highlightId.equals(order.getId())) {
                                if (adapter != null) {
                                    adapter.showOrderSummarySheet(requireContext(), order);
                                }
                                getArguments().remove("highlight_order_id");
                                break;
                            }
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                updateFilter(currentFilter);
            });
    }

    private Order documentToOrder(QueryDocumentSnapshot doc) {
        try {
            List<Map<String, Object>> itemsData = (List<Map<String, Object>>) doc.get("items");
            List<CartItem> cartItems = new ArrayList<>();

            if (itemsData != null) {
                for (Map<String, Object> itemMap : itemsData) {
                    String name        = (String) itemMap.get("name");
                    String description = (String) itemMap.get("description");
                    int quantity       = itemMap.get("quantity") instanceof Long ? ((Long) itemMap.get("quantity")).intValue() : 0;
                    int pricePerUnit   = itemMap.get("pricePerUnit") instanceof Long ? ((Long) itemMap.get("pricePerUnit")).intValue() : 0;
                    String unit        = (String) itemMap.get("unit");
                    String emoji       = (String) itemMap.get("emoji");
                    String instructions = (String) itemMap.get("instructions");

                    CartItem item = new CartItem(
                        name != null ? name : "",
                        description != null ? description : "",
                        quantity, pricePerUnit,
                        unit != null ? unit : "",
                        emoji != null ? emoji : "",
                        instructions != null ? instructions : "",
                        0
                    );
                    cartItems.add(item);
                }
            }

            Order order = new Order(cartItems);
            order.setId(doc.getId());
            order.setDocumentPath(doc.getReference().getPath());
            if (doc.getString("status") != null) order.setStatus(doc.getString("status"));
            if (doc.getString("address") != null) order.setAddress(doc.getString("address"));

            Long ts = doc.getLong("timestamp");
            if (ts != null) order.setTimestamp(ts);

            return order;
        } catch (Exception e) {
            return null;
        }
    }

    private void setupRecyclerView() {
        adapter = new OrderHistoryAdapter(new ArrayList<>());
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(adapter);
    }

    private void setupFilters() {
        tabAll.setOnClickListener(v -> updateFilter("ALL"));
        tabActive.setOnClickListener(v -> updateFilter("ACTIVE"));
        tabCompleted.setOnClickListener(v -> updateFilter("COMPLETED"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;

        updateTabStyle(tabAll,       filter.equals("ALL"));
        updateTabStyle(tabActive,    filter.equals("ACTIVE"));
        updateTabStyle(tabCompleted, filter.equals("COMPLETED"));

        long threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);

        List<Order> filteredList = new ArrayList<>();
        for (Order order : allOrders) {
            if (order.getTimestamp() < threeMonthsAgo && order.getTimestamp() > 0) continue;

            boolean isCompleted = "Delivered".equalsIgnoreCase(order.getStatus()) || "Completed".equalsIgnoreCase(order.getStatus());
            boolean isCancelled = "Cancelled".equalsIgnoreCase(order.getStatus());
            boolean isActive    = !isCompleted && !isCancelled;

            if (filter.equals("ALL")) {
                filteredList.add(order);
            } else if (filter.equals("ACTIVE") && isActive) {
                filteredList.add(order);
            } else if (filter.equals("COMPLETED") && (isCompleted || isCancelled)) {
                filteredList.add(order);
            }
        }

        java.util.Collections.sort(filteredList, (o1, o2) -> {
            boolean a1 = !("Delivered".equalsIgnoreCase(o1.getStatus()) || "Completed".equalsIgnoreCase(o1.getStatus()) || "Cancelled".equalsIgnoreCase(o1.getStatus()));
            boolean a2 = !("Delivered".equalsIgnoreCase(o2.getStatus()) || "Completed".equalsIgnoreCase(o2.getStatus()) || "Cancelled".equalsIgnoreCase(o2.getStatus()));
            if (a1 && !a2) return -1;
            if (!a1 && a2) return 1;
            return Long.compare(o2.getTimestamp(), o1.getTimestamp());
        });

        adapter.setOrders(filteredList);
        refreshUI();
    }

    private void updateTabStyle(TextView tab, boolean isSelected) {
        tab.setBackgroundResource(isSelected ? R.drawable.bg_nav_pill_premium : R.drawable.bg_pill_badge);
        tab.setTextColor(isSelected ? android.graphics.Color.WHITE : ContextCompat.getColor(requireContext(), R.color.text_muted));
        tab.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void refreshUI() {
        boolean isEmpty = adapter.getItemCount() == 0;
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrdersFromFirestore();
    }
}
