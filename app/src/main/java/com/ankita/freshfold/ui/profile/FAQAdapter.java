package com.ankita.freshfold.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.R;

import java.util.List;

public class FAQAdapter extends RecyclerView.Adapter<FAQAdapter.FAQViewHolder> {

    private List<FAQ> faqList;

    public FAQAdapter(List<FAQ> faqList) {
        this.faqList = faqList;
    }

    @NonNull
    @Override
    public FAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_faq, parent, false);
        return new FAQViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FAQViewHolder holder, int position) {
        FAQ faq = faqList.get(position);
        holder.tvQuestion.setText(faq.getQuestion());
        holder.tvAnswer.setText(faq.getAnswer());

        boolean isExpanded = faq.isExpanded();
        holder.tvAnswer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpand.setRotation(isExpanded ? 180f : 0f);

        // Icon background toggle
        holder.ivIcon.setBackgroundResource(isExpanded ? R.drawable.bg_faq_icon_open : R.drawable.bg_faq_icon);
        
        // Relevant icon logic
        String question = faq.getQuestion().toLowerCase();
        if (question.contains("order") || question.contains("place")) {
            holder.ivIcon.setImageResource(R.drawable.ic_cart);
        } else if (question.contains("delivery") || question.contains("track")) {
            holder.ivIcon.setImageResource(R.drawable.ic_location_pin);
        } else if (question.contains("cancel")) {
            holder.ivIcon.setImageResource(R.drawable.ic_delete);
        } else if (question.contains("damaged") || question.contains("contact")) {
            holder.ivIcon.setImageResource(R.drawable.ic_phone_call);
        } else if (question.contains("coupon") || question.contains("offer")) {
            holder.ivIcon.setImageResource(R.drawable.ic_rewards);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_info);
        }

        holder.itemView.setOnClickListener(v -> {
            faq.setExpanded(!faq.isExpanded());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

    static class FAQViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvAnswer;
        ImageView ivExpand, ivIcon;

        public FAQViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
