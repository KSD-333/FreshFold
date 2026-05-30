package com.ankita.freshfold.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.Order;
import com.ankita.freshfold.R;

import java.util.List;

public class CartPreviewFragment extends Fragment implements CartPreviewAdapter.OnCartPreviewChangeListener {

    private RecyclerView rvCartPreview;
    private CartPreviewAdapter adapter;

    // Pending cart section
    private LinearLayout sectionPendingCart;
    private LinearLayout layoutPendingItems;
    private TextView tvPendingTotal;
    private View btnGoToCart;

    // Past orders section
    private LinearLayout sectionPastOrders;

    // Empty state
    private View layoutEmptyCart;

    // Header badge
    private TextView tvCartItemCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sectionPendingCart  = view.findViewById(R.id.sectionPendingCart);
        layoutPendingItems  = view.findViewById(R.id.layoutPendingItems);
        tvPendingTotal      = view.findViewById(R.id.tvPendingTotal);
        btnGoToCart         = view.findViewById(R.id.btnGoToCart);
        sectionPastOrders   = view.findViewById(R.id.sectionPastOrders);
        rvCartPreview       = view.findViewById(R.id.rvCartPreview);
        layoutEmptyCart     = view.findViewById(R.id.layoutEmptyCart);
        tvCartItemCount     = view.findViewById(R.id.tvCartItemCount);

        // Past orders RecyclerView
        List<Order> orders = CartManager.getInstance().getOrders();
        adapter = new CartPreviewAdapter(orders, this);
        rvCartPreview.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCartPreview.setAdapter(adapter);

        // "Continue to Checkout" → open CartActivity with from_nav=true
        btnGoToCart.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CartActivity.class);
            intent.putExtra("from_nav", true);
            startActivity(intent);
        });

        refreshUI();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUI();
    }

    /** Called by MainActivity when cart tab is tapped while fragment is already visible */
    public void refreshFromOutside() {
        refreshUI();
    }

    private void refreshUI() {
        List<CartItem> pendingItems = CartManager.getInstance().getPendingCartItems();
        List<Order> orders          = CartManager.getInstance().getOrders();

        boolean hasPending = !pendingItems.isEmpty();
        boolean hasOrders  = !orders.isEmpty();
        boolean isEmpty    = !hasPending && !hasOrders;

        // ── Empty state ──
        layoutEmptyCart.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        // ── Pending cart section ──
        if (hasPending) {
            sectionPendingCart.setVisibility(View.VISIBLE);
            buildPendingItemsView(pendingItems);
            tvPendingTotal.setText("₹" + CartManager.getInstance().getPendingCartTotalPrice());
        } else {
            sectionPendingCart.setVisibility(View.GONE);
        }

        // ── Past orders section ──
        if (hasOrders) {
            sectionPastOrders.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
        } else {
            sectionPastOrders.setVisibility(View.GONE);
        }

        // ── Header badge ──
        int totalCount = pendingItems.size() + orders.size();
        if (tvCartItemCount != null) {
            if (totalCount > 0) {
                tvCartItemCount.setVisibility(View.VISIBLE);
                tvCartItemCount.setText(String.valueOf(totalCount));
            } else {
                tvCartItemCount.setVisibility(View.GONE);
            }
        }

        // Update bottom nav badge in parent activity
        if (getActivity() instanceof CartBadgeUpdater) {
            ((CartBadgeUpdater) getActivity()).updateCartBadge();
        }
    }

    /**
     * Builds the pending items list inside the pending cart card.
     * Groups by service name — shows service name + item count + subtotal per service.
     */
    private void buildPendingItemsView(List<CartItem> items) {
        if (layoutPendingItems == null) return;
        layoutPendingItems.removeAllViews();

        // Group by service name
        java.util.LinkedHashMap<String, java.util.List<CartItem>> grouped = new java.util.LinkedHashMap<>();
        for (CartItem item : items) {
            String key = item.getName();
            if (!grouped.containsKey(key)) grouped.put(key, new java.util.ArrayList<>());
            grouped.get(key).add(item);
        }

        boolean isFirst = true;
        for (java.util.Map.Entry<String, java.util.List<CartItem>> entry : grouped.entrySet()) {
            String serviceName = entry.getKey();
            List<CartItem> serviceItems = entry.getValue();

            // Divider between services
            if (!isFirst) {
                View divider = new View(getContext());
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1));
                dp.setMargins(0, dpToPx(8), 0, dpToPx(8));
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(getResources().getColor(R.color.divider, null));
                layoutPendingItems.addView(divider);
            }
            isFirst = false;

            // Service row
            View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_pending_service_row, layoutPendingItems, false);

            TextView tvService  = row.findViewById(R.id.tvPendingServiceName);
            TextView tvDetails  = row.findViewById(R.id.tvPendingServiceDetails);
            TextView tvSubtotal = row.findViewById(R.id.tvPendingServiceSubtotal);

            tvService.setText(serviceName);

            // Build details: "Shirt ×2, Trousers ×1"
            StringBuilder details = new StringBuilder();
            int serviceTotal = 0;
            int itemCount = 0;
            for (CartItem ci : serviceItems) {
                if (details.length() > 0) details.append("  •  ");
                details.append(ci.getDescription()).append(" ×").append(ci.getQuantity());
                serviceTotal += ci.getTotalPrice();
                itemCount += ci.getQuantity();
            }
            tvDetails.setText(details.toString());
            tvSubtotal.setText("₹" + serviceTotal);

            layoutPendingItems.addView(row);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onItemRemoved(int position) {
        CartManager.getInstance().removeOrder(position);
        adapter.notifyItemRemoved(position);
        adapter.notifyItemRangeChanged(position, CartManager.getInstance().getOrders().size());
        refreshUI();
    }

    @Override
    public void onQuantityChanged() {
        refreshUI();
    }

    /** Interface so MainActivity can update the nav badge */
    public interface CartBadgeUpdater {
        void updateCartBadge();
    }
}
