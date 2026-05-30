package com.ankita.freshfold.ui.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ankita.freshfold.R;
import com.ankita.freshfold.viewmodel.ForgotPasswordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private LinearLayout layoutStepMobile, layoutStepOtp, layoutStepPassword;
    private TextInputEditText etMobile, etNewPassword, etConfirmPassword, etOtp;
    private TextInputEditText otp1, otp2, otp3, otp4, otp5, otp6;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnResetPassword;
    private TextView tvTimer, tvResendOtp;
    private ProgressDialog progressDialog;
    private CountDownTimer countDownTimer;
    
    private ForgotPasswordViewModel viewModel;
    private String mobileNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Status bar — transparent to show gradient
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);
        
        initViews();
        setupObservers();
        setupListeners();

        // Auto-fill mobile if passed from Login
        String passedMobile = getIntent().getStringExtra("mobile");
        if (passedMobile != null && !passedMobile.isEmpty()) {
            etMobile.setText(passedMobile);
            this.mobileNumber = passedMobile;
        }
    }

    private void initViews() {
        layoutStepMobile = findViewById(R.id.layoutStepMobile);
        layoutStepOtp = findViewById(R.id.layoutStepOtp);
        layoutStepPassword = findViewById(R.id.layoutStepPassword);
        
        etMobile = findViewById(R.id.etMobile);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etOtp = findViewById(R.id.etOtp);
        
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        
        tvTimer = findViewById(R.id.tvTimer);
        tvResendOtp = findViewById(R.id.tvResendOtp);

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
            if (loading) {
                progressDialog.setMessage("Processing...");
                progressDialog.show();
            } else {
                progressDialog.dismiss();
            }
        });

        viewModel.error.observe(this, err -> {
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });

        viewModel.otpSent.observe(this, sent -> {
            if (sent) {
                layoutStepMobile.setVisibility(View.GONE);
                layoutStepOtp.setVisibility(View.VISIBLE);
                startCountdownTimer();
            }
        });

        viewModel.otpVerified.observe(this, verified -> {
            if (verified) {
                layoutStepOtp.setVisibility(View.GONE);
                layoutStepPassword.setVisibility(View.VISIBLE);
            }
        });

        viewModel.passwordResetSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_LONG).show();
                finish(); // Goes back to Login
            }
        });
    }

    private void setupListeners() {
        btnSendOtp.setOnClickListener(v -> {
            mobileNumber = etMobile.getText().toString().trim();
            if (mobileNumber.length() != 10) {
                etMobile.setError("Enter 10-digit number");
                return;
            }
            viewModel.checkUserAndSendOtp(mobileNumber);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() != 6) {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show();
                return;
            }
            viewModel.verifyOtp(otp);
        });

        btnResetPassword.setOnClickListener(v -> {
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (newPass.length() < 6) {
                etNewPassword.setError("Minimum 6 characters");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            viewModel.resetPassword(mobileNumber, newPass);
        });

        tvResendOtp.setOnClickListener(v -> {
            viewModel.checkUserAndSendOtp(mobileNumber);
        });

        TextView tvBackToLogin = findViewById(R.id.tvBackToLogin);
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> finish());
        }
    }

    private void startCountdownTimer() {
        tvTimer.setVisibility(View.VISIBLE);
        tvResendOtp.setVisibility(View.GONE);
        if (countDownTimer != null) countDownTimer.cancel();
        
        countDownTimer = new CountDownTimer(90000, 1000) { // 1:30 = 90 seconds
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvTimer.setText(String.format("%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setVisibility(View.GONE);
                tvResendOtp.setVisibility(View.VISIBLE);
            }
        }.start();
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
                    etOtp.setText(sb.toString());
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
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
