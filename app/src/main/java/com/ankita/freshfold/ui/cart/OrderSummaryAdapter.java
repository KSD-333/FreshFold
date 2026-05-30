package com.ankita.freshfold.ui.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.R;
import java.util.List;

public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.ViewHolder> {
    private final List<CartItem> items;

    public OrderSummaryAdapter(List<CartItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_summary_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = items.get(position);
        holder.tvName.setText(item.getName() + " (" + item.getDescription() + ")");
        holder.tvQtyPrice.setText(item.getQuantity() + " x ₹" + item.getPricePerUnit());
        holder.tvTotal.setText("₹" + item.getTotalPrice());
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQtyPrice, tvTotal;
        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvQtyPrice = v.findViewById(R.id.tvQtyPrice);
            tvTotal = v.findViewById(R.id.tvTotal);
        }
    }
}
