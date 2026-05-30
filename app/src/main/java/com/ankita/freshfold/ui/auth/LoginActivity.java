package com.ankita.freshfold.ui.auth;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.ankita.freshfold.DeliveryDashboardActivity;
import com.ankita.freshfold.PressmanDashboardActivity;
import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.SmsBroadcastReceiver;
import com.ankita.freshfold.ui.home.MainActivity;
import com.ankita.freshfold.viewmodel.LoginViewModel;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private LinearLayout layoutOtp, layoutPasswordSection;
    private TextInputEditText etMobile, etPassword, etOtp;
    private TextInputEditText otp1, otp2, otp3, otp4, otp5, otp6;
    private TextView btnLogin, btnSendOtp, btnVerifyOtp;
    private TextView tvTimer, tvResendOtp, tvForgotPassword;
    private ProgressDialog progressDialog;

    private BroadcastReceiver otpBroadcastReceiver;
    private CountDownTimer countDownTimer;
    private SessionManager sessionManager;

    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            navigateByRole(sessionManager.getUserRole());
            return;
        }

        setContentView(R.layout.activity_login);

        // Dark header — transparent status bar with light icons
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(LoginViewModel.class);

        initViews();
        setupObservers();
        setupListeners();
        registerOtpBroadcastReceiver();
    }

    private void initViews() {
        layoutOtp = findViewById(R.id.layoutOtp);
        layoutPasswordSection = findViewById(R.id.layoutPasswordSection);
        etMobile = findViewById(R.id.etMobile);
        etPassword = findViewById(R.id.etPassword);
        etOtp = findViewById(R.id.etOtp);
        btnLogin = findViewById(R.id.btnLogin);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvTimer = findViewById(R.id.tvTimer);
        tvResendOtp = findViewById(R.id.tvResendOtp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);

        setupOtpAutoFocus();
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
    }

    private void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            if (loading) showProgressDialog("Processing...");
            else hideProgressDialog();
        });

        viewModel.error.observe(this, err -> {
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });

        viewModel.otpSent.observe(this, sent -> {
            if (sent) {
                showOtpFields();
                startCountdownTimer();
                startSmsRetriever();
            }
        });

        viewModel.navigationRole.observe(this, role -> {
            if (role.equals("reset_password")) {
                String mobile = etMobile.getText().toString().trim();
                Intent intent = new Intent(this, ResetPasswordActivity.class);
                intent.putExtra("mobile", mobile);
                startActivity(intent);
            } else {
                Boolean isApproved = viewModel.isApproved.getValue();

                String userId = etMobile.getText().toString().trim();
                sessionManager.createLoginSession(userId, userId, role, isApproved != null ? isApproved : true);

                // Cache user profile from Firestore so UI shows instantly (no loading)
                if ("customer".equalsIgnoreCase(role) || role.isEmpty() || role.equals("user")) {
                    new com.ankita.freshfold.data.repository.UserRepository()
                        .getUser(userId)
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                sessionManager.saveUserProfile(
                                    doc.getString("name"),
                                    doc.getString("email"),
                                    doc.getString("address")
                                );
                            }
                        });
                }

                if (isApproved != null && !isApproved && "customer".equalsIgnoreCase(role)) {
                    Toast.makeText(this, "You can book services once the franchise approves your request.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                }
                navigateByRole(role);
            }
        });
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            String mobile = etMobile.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            // Bypass for Pressman (Check this first)
            if (mobile.equals("1212121212")) {
                sessionManager.createLoginSession(mobile, "Pressman", "pressman", true);
                Toast.makeText(this, "Welcome Pressman!", Toast.LENGTH_SHORT).show();
                navigateByRole("pressman");
                return;
            }

            if (mobile.length() != 10) { etMobile.setError("Required"); return; }
            if (password.isEmpty()) { etPassword.setError("Required"); return; }
            
            viewModel.loginWithPassword(mobile, password);
        });

        btnSendOtp.setOnClickListener(v -> {
            String mobile = etMobile.getText().toString().trim();
            if (mobile.length() != 10) { etMobile.setError("Required"); return; }
            viewModel.startOtpFlow(mobile, false);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String inputOtp = etOtp.getText().toString().trim();
            String mobile = etMobile.getText().toString().trim();
            if (inputOtp.isEmpty()) { Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show(); return; }
            viewModel.verifyOtp(inputOtp, mobile);
        });

        tvResendOtp.setOnClickListener(v -> {
            tvResendOtp.setVisibility(View.GONE);
            String mobile = etMobile.getText().toString().trim();
            viewModel.startOtpFlow(mobile, false);
        });

        tvForgotPassword.setOnClickListener(v -> {
            String mobile = etMobile.getText().toString().trim();
            if (mobile.length() != 10) {
                etMobile.setError("Please enter your 10-digit mobile number first");
                etMobile.requestFocus();
                return;
            }
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            intent.putExtra("mobile", mobile);
            startActivity(intent);
        });

        findViewById(R.id.tvRegister).setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
        });
    }

    private void navigateByRole(String role) {
        Intent intent;
if ("delivery".equalsIgnoreCase(role)) {
            intent = new Intent(this, DeliveryDashboardActivity.class);
        } else if ("pressman".equalsIgnoreCase(role)) {
            intent = new Intent(this, PressmanDashboardActivity.class);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void showOtpFields() {
        btnSendOtp.setText("OTP Sent ✓");
        btnSendOtp.setEnabled(false);
        btnSendOtp.setAlpha(0.5f);
        etMobile.setEnabled(false);
        // Hide password section + Sign In button, show OTP section in their place
        layoutPasswordSection.setVisibility(View.GONE);
        btnLogin.setVisibility(View.GONE);
        layoutOtp.setVisibility(View.VISIBLE);
    }

    private void startCountdownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        tvTimer.setVisibility(View.VISIBLE);
        tvResendOtp.setVisibility(View.GONE);
        countDownTimer = new CountDownTimer(120000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("Resend OTP in %02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.GONE);
                tvResendOtp.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void registerOtpBroadcastReceiver() {
        otpBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SmsBroadcastReceiver.OTP_RETRIEVED.equals(intent.getAction())) {
                    String otp = intent.getStringExtra("otp");
                    if (otp != null && !otp.isEmpty()) {
                        etOtp.setText(otp);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(SmsBroadcastReceiver.OTP_RETRIEVED);
        registerReceiver(otpBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void startSmsRetriever() {
        SmsRetrieverClient client = SmsRetriever.getClient(this);
        Task<Void> task = client.startSmsRetriever();
        task.addOnSuccessListener(aVoid -> Log.d(TAG, "SMS Retriever started"));
    }

    private void showProgressDialog(String message) {
        progressDialog.setMessage(message);
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog.isShowing()) progressDialog.dismiss();
    }

    private void setupOtpAutoFocus() {
        TextInputEditText[] boxes = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < boxes.length; i++) {
            final int idx = i;
            boxes[i].addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                public void afterTextChanged(android.text.Editable s) {
                    if (s.length() == 1 && idx < boxes.length - 1) boxes[idx + 1].requestFocus();
                    if (s.length() == 0 && idx > 0) boxes[idx - 1].requestFocus();
                    StringBuilder sb = new StringBuilder();
                    for (TextInputEditText b : boxes) if (b.getText() != null) sb.append(b.getText().toString());
                    if (etOtp != null) etOtp.setText(sb.toString());
                }
            });
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (otpBroadcastReceiver != null) unregisterReceiver(otpBroadcastReceiver);
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
