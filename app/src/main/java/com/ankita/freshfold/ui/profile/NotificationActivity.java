package com.ankita.freshfold.ui.profile;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ankita.freshfold.NotificationHelper;
import com.ankita.freshfold.OrderDetailActivity;
import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.Order;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private LinearLayout layoutEmptyState;
    private TextView tvClearAll;
    private NotificationAdapter adapter;
    private List<NotificationHelper.AppNotification> notificationsList = new ArrayList<>();
    private SessionManager sessionManager;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        sessionManager = new SessionManager(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading order details...");
        progressDialog.setCancelable(false);

        initViews();
        loadNotifications();
    }

    private void initViews() {
        rvNotifications = findViewById(R.id.rvNotifications);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        tvClearAll = findViewById(R.id.tvClearAll);

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        tvClearAll.setOnClickListener(v -> {
            NotificationHelper.markAllAsRead(this);
            loadNotifications();
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show();
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationsList, this::onNotificationClicked);
        rvNotifications.setAdapter(adapter);
    }

    private void loadNotifications() {
        notificationsList.clear();
        notificationsList.addAll(NotificationHelper.getNotifications(this));
        
        if (notificationsList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            tvClearAll.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            tvClearAll.setVisibility(View.VISIBLE);
        }
        adapter.notifyDataSetChanged();
    }

    private void onNotificationClicked(NotificationHelper.AppNotification item) {
        // Mark as read immediately
        NotificationHelper.markAsRead(this, item.id);
        
        if (item.orderId != null && !item.orderId.isEmpty()) {
            Intent intent = new Intent(this, com.ankita.freshfold.ui.home.MainActivity.class);
            intent.putExtra("navigate_to_fragment", "ORDERS");
            intent.putExtra("highlight_order_id", item.orderId);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            loadNotifications();
        }
    }

    private static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private final List<NotificationHelper.AppNotification> list;
        private final OnNotificationClickListener listener;

        interface OnNotificationClickListener {
            void onClick(NotificationHelper.AppNotification item);
        }

        public NotificationAdapter(List<NotificationHelper.AppNotification> list, OnNotificationClickListener listener) {
            this.list = list;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationHelper.AppNotification item = list.get(position);
            holder.tvTitle.setText(item.title);
            holder.tvMessage.setText(item.message);

            String dateStr = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date(item.timestamp));
            holder.tvTime.setText(dateStr);

            holder.viewUnreadDot.setVisibility(item.isRead ? View.GONE : View.VISIBLE);
            
            // Set light blue background for unread, white/surface for read
            if (item.isRead) {
                holder.cardNotification.setCardBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.bg_surface));
            } else {
                holder.cardNotification.setCardBackgroundColor(android.graphics.Color.parseColor("#F0F9FF"));
            }
            
            holder.itemView.setOnClickListener(v -> listener.onClick(item));
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvMessage, tvTime;
            View viewUnreadDot;
            com.google.android.material.card.MaterialCardView cardNotification;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
                tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
                tvTime = itemView.findViewById(R.id.tvNotificationTime);
                viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
                cardNotification = itemView.findViewById(R.id.cardNotification);
            }
        }
    }
}
