package com.ankita.freshfold;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private List<Order> orders;

    public OrderHistoryAdapter(List<Order> orders) {
        this.orders = orders;
    }

    public void setOrders(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvTotalPrice.setText("₹" + order.getTotalPrice());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());
        holder.tvOrderDate.setText(sdf.format(new Date(order.getTimestamp())));

        // Status logic
        String status = order.getStatus();
        holder.tvStatus.setText(status);
        
        if ("Delivered".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(0xFF2E7D32); // Green
            holder.tvStatus.getBackground().setTint(0xFFE8F5E9);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnTrack.setVisibility(View.GONE);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(0xFFD32F2F); // Red
            holder.tvStatus.getBackground().setTint(0xFFFFEBEE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnTrack.setVisibility(View.GONE);
        } else {
            // Active / Pending
            holder.tvStatus.setTextColor(0xFFB45309); // Dark Orange/Brown
            holder.tvStatus.getBackground().setTint(0xFFFEF3C7); // Light Yellow
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnTrack.setVisibility(View.VISIBLE);
        }

        holder.btnCancel.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(v.getContext())
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel this order?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    String docPath = order.getDocumentPath();
                    if (docPath != null && !docPath.isEmpty()) {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .document(docPath)
                            .update("status", "Cancelled")
                            .addOnSuccessListener(aVoid -> {
                                SessionManager sm = new SessionManager(v.getContext());
                                String phone = sm.getUserPhone();
                                if (phone != null && !phone.isEmpty()) {
                                    new com.ankita.freshfold.data.repository.UserRepository()
                                        .handleOrderStatusWalletUpdate(phone, order.getTotalPrice(), "Cancelled");
                                }
                                order.setStatus("Cancelled");
                                notifyItemChanged(position);
                            })
                            .addOnFailureListener(e -> {
                                android.widget.Toast.makeText(v.getContext(), "Failed to cancel order.", android.widget.Toast.LENGTH_SHORT).show();
                            });
                    } else {
                        // Fallback to local remove if no doc path
                        CartManager.getInstance(v.getContext()).removeOrderById(order.getId());
                        orders.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, orders.size());
                    }
                })
                .setNegativeButton("No, Keep", null)
                .show();
        });

        // Summary without emojis
        List<CartItem> items = order.getItems();
        StringBuilder summary = new StringBuilder();
        int displayCount = Math.min(items.size(), 2);
        
        for (int i = 0; i < displayCount; i++) {
            CartItem item = items.get(i);
            if (i > 0) summary.append(", ");
            summary.append(item.getName()).append(" x").append(item.getQuantity());
        }
        
        if (items.size() > 2) {
            summary.append(" (+").append(items.size() - 2).append(" more)");
        }
        holder.tvItemSummary.setText(summary.toString());
        
        if (order.getAddress() != null && !order.getAddress().isEmpty()) {
            holder.tvOrderAddress.setText(order.getAddress());
            holder.tvOrderAddress.setVisibility(View.VISIBLE);
        } else {
            holder.tvOrderAddress.setVisibility(View.GONE);
            // Hide the address icon if there is no address, but wait, the icon is in a LinearLayout without an ID, so we might need to handle the parent or just let it be. Wait, the parent LinearLayout doesn't have an ID.
            // Let's just set text to "No Address" or leave it. We'll set it to "Address not available" if null.
            holder.tvOrderAddress.setText("Address not available");
        }
        
        holder.btnTrack.setOnClickListener(v -> showTrackingSheet(v.getContext(), order));
        holder.itemView.setOnClickListener(v -> showOrderSummarySheet(v.getContext(), order));

        // Set dynamic service icon
        if (!items.isEmpty()) {
            String firstName = items.get(0).getName().toLowerCase();
            if (firstName.contains("dry")) {
                holder.ivServiceIcon.setImageResource(R.drawable.img_dry_clean_new);
            } else if (firstName.contains("iron")) {
                holder.ivServiceIcon.setImageResource(R.drawable.img_ironing_new);
            } else if (firstName.contains("shoe")) {
                holder.ivServiceIcon.setImageResource(R.drawable.img_shoe_new);
            } else if (firstName.contains("blanket")) {
                holder.ivServiceIcon.setImageResource(R.drawable.img_blanket_new);
            } else {
                holder.ivServiceIcon.setImageResource(R.drawable.img_laundry_new);
            }
        }
    }

    private void showOrderSummarySheet(Context context, Order order) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.layout_order_summary_sheet, null);
        
        TextView dialogTotal = sheetView.findViewById(R.id.dialogTotal);
        LinearLayout itemsListContainer = sheetView.findViewById(R.id.itemsListContainer);
        TextView btnClose = sheetView.findViewById(R.id.btnClose);

        dialogTotal.setText("₹" + order.getTotalPrice());

        for (CartItem item : order.getItems()) {
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_dialog_order, itemsListContainer, false);
            
            TextView itemName = itemView.findViewById(R.id.itemName);
            TextView itemQty = itemView.findViewById(R.id.itemQty);
            TextView itemSubtotal = itemView.findViewById(R.id.itemSubtotal);

            itemName.setText(item.getName());
            itemQty.setText("Quantity: " + item.getQuantity() + " " + (item.getUnit() != null ? item.getUnit() : "pcs"));
            itemSubtotal.setText("₹" + (item.getPricePerUnit() * item.getQuantity()));

            itemsListContainer.addView(itemView);
        }

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    private void showTrackingSheet(Context context, Order order) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.layout_order_tracking_sheet, null);
        
        TextView tvOrderId = sheetView.findViewById(R.id.tvOrderId);
        TextView btnClose = sheetView.findViewById(R.id.btnClose);

        tvOrderId.setText("Order #" + order.getId().substring(0, Math.min(order.getId().length(), 8)).toUpperCase());
        
        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.setContentView(sheetView);
        bottomSheetDialog.show();
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTotalPrice, tvOrderDate, tvStatus, tvItemSummary, btnCancel, tvOrderAddress;
        View btnTrack;
        android.widget.ImageView ivServiceIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvItemSummary = itemView.findViewById(R.id.tvItemSummary);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnTrack = itemView.findViewById(R.id.btnTrack);
            ivServiceIcon = itemView.findViewById(R.id.ivServiceIcon);
            tvOrderAddress = itemView.findViewById(R.id.tvOrderAddress);
        }
    }
}
