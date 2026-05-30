package com.ankita.freshfold;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.ui.auth.LoginActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
public class PressmanDashboardActivity extends AppCompatActivity {

    private static final String PRESSMAN_STATS_PATH = "freshfold/data/pressman/stats";
    private static final int WORKLOAD_THRESHOLD = 100;

    private SessionManager sessionManager;
    private ListenerRegistration statsListener;

    private TextView tvPressedToday;
    private TextView tvPendingTasks;
    private TextView tvTotalPoints;
    private TextView tvPointsProgress;
    private TextView tvWorkloadMessage;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressman_dashboard);

        sessionManager = new SessionManager(this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        tvPressedToday   = findViewById(R.id.tvPressedToday);
        tvPendingTasks   = findViewById(R.id.tvPendingTasks);
        tvTotalPoints    = findViewById(R.id.tvTotalPoints);
        tvPointsProgress = findViewById(R.id.tvPointsProgress);
        tvWorkloadMessage = findViewById(R.id.tvWorkloadMessage);
        progressBar      = findViewById(R.id.progressWorkload);

        // Start with 0 until Firestore responds
        tvPressedToday.setText("0");
        tvPendingTasks.setText("0");
        tvTotalPoints.setText("0");

        listenToStats();

        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());
        findViewById(R.id.btnBottomLogout).setOnClickListener(v -> performLogout());

        // Test button: set totalPoints = 100 in Firestore
        findViewById(R.id.btnSetTestPoints).setOnClickListener(v -> {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("totalPoints",  95L);
            data.put("pendingTasks", 0L);
            data.put("pressedToday", 0L);
            FirebaseFirestore.getInstance()
                .document(PRESSMAN_STATS_PATH)
                .set(data)
                .addOnSuccessListener(unused ->
                    android.widget.Toast.makeText(this, "✅ Points set to 95", android.widget.Toast.LENGTH_SHORT).show()
                );
        });
    }

    private void listenToStats() {
        DocumentReference statsRef = FirebaseFirestore.getInstance().document(PRESSMAN_STATS_PATH);

        // Real-time listener — updates dashboard whenever Firestore changes
        statsListener = statsRef.addSnapshotListener((doc, error) -> {
            if (error != null || doc == null) return;

            long pressedToday = doc.getLong("pressedToday") != null ? doc.getLong("pressedToday") : 0;
            long pendingTasks = doc.getLong("pendingTasks") != null ? doc.getLong("pendingTasks") : 0;
            long totalPoints  = doc.getLong("totalPoints")  != null ? doc.getLong("totalPoints")  : 0;

            tvPressedToday.setText(String.valueOf(pressedToday));
            tvPendingTasks.setText(String.valueOf(pendingTasks));
            tvTotalPoints.setText(String.valueOf(totalPoints));

            // Progress bar — capped at 100 for display
            int progressVal = (int) Math.min(totalPoints, 100);
            if (progressBar != null) {
                progressBar.setProgress(progressVal);
            }
            if (tvPointsProgress != null) {
                tvPointsProgress.setText(totalPoints + " / 100");
            }

            // Workload message
            if (tvWorkloadMessage != null) {
                if (totalPoints >= WORKLOAD_THRESHOLD) {
                    tvWorkloadMessage.setText("Due to high workload, delivery is scheduled for the next available day.");
                } else {
                    tvWorkloadMessage.setText("Your order will be delivered on time.");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener to avoid memory leaks
        if (statsListener != null) {
            statsListener.remove();
        }
    }

    private void performLogout() {
        sessionManager.logoutUser();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
