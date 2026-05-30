package com.ankita.freshfold.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ankita.freshfold.R;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final List<Integer> bannerImages;

    public BannerAdapter(List<Integer> bannerImages) {
        this.bannerImages = bannerImages;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        holder.imageView.setImageResource(bannerImages.get(position));

        // Set text based on position (mapped to user's requested text order)
        if (position == 0) {
            holder.tvTitle.setText("Fresh & Fold, Every Time");
            holder.tvSubtitle.setText("Professional laundry at your doorstep");
        } else if (position == 1) {
            holder.tvTitle.setText("Wash. Dry. Deliver.");
            holder.tvSubtitle.setText("Pickup & delivery from your home");
        } else if (position == 2) {
            holder.tvTitle.setText("Crisp Clothes, Zero Effort");
            holder.tvSubtitle.setText("Expert ironing for a spotless look");
        }
    }

    @Override
    public int getItemCount() {
        return bannerImages.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvTitle, tvSubtitle;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewBanner);
            tvTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvSubtitle = itemView.findViewById(R.id.tvBannerSubtitle);
        }
    }
}
