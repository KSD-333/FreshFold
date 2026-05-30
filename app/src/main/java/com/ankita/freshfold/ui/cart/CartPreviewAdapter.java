package com.ankita.freshfold.ui.cart;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.Order;
import com.ankita.freshfold.OrderDetailActivity;
import com.ankita.freshfold.R;

import java.util.List;

public class CartPreviewAdapter extends RecyclerView.Adapter<CartPreviewAdapter.OrderViewHolder> {

    public interface OnCartPreviewChangeListener {
        void onItemRemoved(int position);
        void onQuantityChanged();
    }

    private final List<Order> orders;
    private final OnCartPreviewChangeListener listener;

    public CartPreviewAdapter(List<Order> orders, OnCartPreviewChangeListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_group, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvServices.setText(order.getCombinedNames());
        
        int serviceCount = order.getItems().size();
        String stats = serviceCount + (serviceCount == 1 ? " Service" : " Services") 
                     + " • ₹" + order.getTotalPrice();
        holder.tvStats.setText(stats);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), OrderDetailActivity.class);
            intent.putExtra("order", order);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvServices, tvStats;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServices = itemView.findViewById(R.id.tvOrderServices);
            tvStats    = itemView.findViewById(R.id.tvOrderStats);
        }
    }
}
