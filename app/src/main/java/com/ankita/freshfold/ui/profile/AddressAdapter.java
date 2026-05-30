package com.ankita.freshfold.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.R;

import java.util.List;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {

    private final List<String> addressList;
    private final OnAddressClickListener listener;

    public interface OnAddressClickListener {
        void onAddressClick(String address);
        void onAddressDelete(String address, int position);
        void onAddressEdit(String address, int position);
        void onAddressMakeDefault(String address, int position);
    }

    public AddressAdapter(List<String> addressList, OnAddressClickListener listener) {
        this.addressList = addressList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_previous_address, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String address = addressList.get(position);
        holder.tvAddress.setText(address);
        
        // Handle whole item click for selection
        holder.itemView.setOnClickListener(v -> listener.onAddressClick(address));
        
        // Handle options click
        holder.ivOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), holder.ivOptions);
            popup.getMenu().add("Make Default");
            popup.getMenu().add("Edit");
            popup.getMenu().add("Delete");
            popup.setOnMenuItemClickListener(item -> {
                int adapterPos = holder.getAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION) return false;
                
                String title = item.getTitle().toString();
                if (title.equals("Make Default")) {
                    listener.onAddressMakeDefault(address, adapterPos);
                    return true;
                } else if (title.equals("Edit")) {
                    listener.onAddressEdit(address, adapterPos);
                    return true;
                } else if (title.equals("Delete")) {
                    listener.onAddressDelete(address, adapterPos);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAddress;
        ImageView ivOptions;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAddress = itemView.findViewById(R.id.tvPreviousAddress);
            ivOptions = itemView.findViewById(R.id.ivOptions);
        }
    }
}
