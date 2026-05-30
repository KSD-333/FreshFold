package com.ankita.freshfold.ui.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ankita.freshfold.R;
import com.ankita.freshfold.viewmodel.ResetPasswordViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmNewPassword;
    private MaterialButton btnResetPassword;
    private ProgressDialog progressDialog;
    private String mobile;
    private ResetPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mobile = getIntent().getStringExtra("mobile");
        if (mobile == null || mobile.isEmpty()) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating password...");
        progressDialog.setCancelable(false);

        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmNewPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        setupObservers();

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void setupObservers() {
        viewModel.isLoading.observe(this, loading -> {
            if (loading) progressDialog.show();
            else progressDialog.dismiss();
        });

        viewModel.error.observe(this, err -> {
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });

        viewModel.isSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "Password reset successful. Please log in.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void resetPassword() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmNewPassword.getText().toString().trim();

        if (newPass.length() < 6) {
            etNewPassword.setError("Minimum 6 characters");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            etConfirmNewPassword.setError("Passwords do not match");
            return;
        }

        viewModel.resetPassword(mobile, newPass);
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
}
