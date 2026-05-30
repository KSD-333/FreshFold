package com.ankita.freshfold.ui.auth;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ankita.freshfold.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout tilEmail;
    private TextView btnRegister, tvEmailError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        initViews();
        setupEmailValidation();

        btnRegister.setOnClickListener(v -> validateAndProceed());

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        etName            = findViewById(R.id.etName);
        etPhone           = findViewById(R.id.etPhone);
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilEmail          = findViewById(R.id.tilEmail);
        tvEmailError      = findViewById(R.id.tvEmailError);
        btnRegister       = findViewById(R.id.btnRegister);
    }

    private void setupEmailValidation() {
        etEmail.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(android.text.Editable s) {
                String email = s.toString().trim();
                if (email.isEmpty()) {
                    // Empty — reset to default blue
                    tvEmailError.setVisibility(View.GONE);
                    tilEmail.setBoxStrokeColor(ContextCompat.getColor(RegistrationActivity.this, R.color.brand_primary));
                    tilEmail.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(RegistrationActivity.this, R.color.brand_primary)));
                } else if (email.contains(" ") || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    // Invalid — red border (error text shown only on submit)
                    tvEmailError.setVisibility(View.GONE);
                    tilEmail.setBoxStrokeColor(0xFFFF3B30);
                    tilEmail.setStartIconTintList(android.content.res.ColorStateList.valueOf(0xFFFF3B30));
                } else {
                    // Valid — green border
                    tvEmailError.setVisibility(View.GONE);
                    tilEmail.setBoxStrokeColor(0xFF34C759);
                    tilEmail.setStartIconTintList(android.content.res.ColorStateList.valueOf(0xFF34C759));
                }
            }
        });
    }

    private void showEmailError(String message) {
        tvEmailError.setText(message);
        tvEmailError.setVisibility(View.VISIBLE);
        tilEmail.setBoxStrokeColor(0xFFFF3B30);
        tilEmail.setStartIconTintList(android.content.res.ColorStateList.valueOf(0xFFFF3B30));
    }

    private void clearEmailError() {
        tvEmailError.setVisibility(View.GONE);
    }

    private void validateAndProceed() {
        String name     = etName.getText().toString().trim();
        String phone    = etPhone.getText().toString().trim();
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty())       { etName.setError("Required"); return; }
        if (phone.length() != 10) { etPhone.setError("10 digits required"); return; }

        // Email validation
        if (email.isEmpty()) {
            showEmailError("Email address is required");
            return;
        }
        if (email.contains(" ") || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showEmailError("Please enter a valid email address");
            return;
        }
        clearEmailError();

        if (password.isEmpty() || password.length() < 6) { etPassword.setError("Min 6 characters"); return; }
        if (!password.equals(confirm))                   { etConfirmPassword.setError("Passwords do not match"); return; }

        Intent intent = new Intent(this, AddressActivity.class);
        intent.putExtra("name",     name);
        intent.putExtra("phone",    phone);
        intent.putExtra("email",    email);
        intent.putExtra("password", password);
        startActivity(intent);
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
