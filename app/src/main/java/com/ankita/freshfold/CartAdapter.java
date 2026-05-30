package com.ankita.freshfold;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private OnCartChangeListener listener;

    public interface OnCartChangeListener {
        void onCartUpdated();
        void onItemDeleted(int position);
    }

    public CartAdapter(List<CartItem> cartItems, OnCartChangeListener listener) {
        this.cartItems = cartItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item.getImageRes() != 0) {
            holder.ivItemImage.setImageResource(item.getImageRes());
        }
        holder.tvName.setText(item.getName());
        holder.tvDesc.setText(item.getDescription());
        holder.tvQtyDisplay.setText("₹" + item.getPricePerUnit() + " x " + item.getQuantity());
        holder.tvQty.setText(String.valueOf(item.getQuantity()));
        holder.tvPrice.setText("₹" + item.getTotalPrice());
        
        if (item.getInstructions() != null && !item.getInstructions().isEmpty()) {
            holder.layoutInstructions.setVisibility(View.VISIBLE);
            holder.tvInstructionsText.setText("Notes: " + item.getInstructions());
        } else {
            holder.layoutInstructions.setVisibility(View.GONE);
        }

        holder.btnPlus.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                item.setQuantity(item.getQuantity() + 1);
                CartManager.getInstance().notifyItemChanged();
                notifyItemChanged(pos);
                listener.onCartUpdated();
                animateBounce(v);
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    CartManager.getInstance().notifyItemChanged();
                    notifyItemChanged(pos);
                    listener.onCartUpdated();
                    animateBounce(v);
                } else if (item.getQuantity() == 1) {
                    listener.onItemDeleted(pos);
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onItemDeleted(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    private void animateBounce(View v) {
        v.animate().scaleX(0.80f).scaleY(0.80f).setDuration(75)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(75).start())
                .start();
    }

    public void updateItems(List<CartItem> newItems) {
        this.cartItems = newItems;
        notifyDataSetChanged();
    }

    public static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc, tvQtyDisplay, tvQty, tvPrice, tvInstructionsText;
        ImageView ivItemImage, btnDelete;
        View btnPlus, btnMinus;
        android.view.ViewGroup layoutInstructions;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage         = itemView.findViewById(R.id.ivItemImage);
            tvName              = itemView.findViewById(R.id.tvItemName);
            tvDesc              = itemView.findViewById(R.id.tvItemDesc);
            tvQtyDisplay        = itemView.findViewById(R.id.tvItemQtyDisplay);
            tvQty               = itemView.findViewById(R.id.tvItemQty);
            tvPrice             = itemView.findViewById(R.id.tvItemTotalPrice);
            btnPlus             = itemView.findViewById(R.id.btnPlus);
            btnMinus            = itemView.findViewById(R.id.btnMinus);
            btnDelete           = itemView.findViewById(R.id.btnDelete);
            layoutInstructions  = itemView.findViewById(R.id.tvItemInstructions);
            tvInstructionsText  = itemView.findViewById(R.id.tvInstructionsText);
        }
    }
}
