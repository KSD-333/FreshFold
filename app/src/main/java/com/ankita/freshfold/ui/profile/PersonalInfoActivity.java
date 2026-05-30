package com.ankita.freshfold.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.data.repository.UserRepository;

public class PersonalInfoActivity extends AppCompatActivity {

    private boolean isEditMode = false;

    // Toolbar
    private TextView tvEditToggle;

    // Header
    private TextView tvHeaderName;

    // Info rows — view mode
    private TextView tvName, tvPhone, tvEmail, tvAddress, tvPhoneNote;

    // Edit mode inputs
    private com.google.android.material.textfield.TextInputLayout tilName, tilEmail;
    private com.google.android.material.textfield.TextInputEditText etName, etEmail;

    // Edit action buttons
    private LinearLayout layoutEditActions;
    private com.google.android.material.button.MaterialButton btnSave;
    private TextView btnCancelEdit;

    // Misc
    private ProgressBar progressBar;
    private NestedScrollView scroll;
    private SessionManager session;
    private UserRepository repo;

    // Avatar
    private ImageView ivProfileAvatar;
    private View btnChangePhoto;

    // Photo Picker (Android Photo Picker — no storage permission required)
    private final androidx.activity.result.ActivityResultLauncher<
            androidx.activity.result.PickVisualMediaRequest>
            photoPickerLauncher = registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
                    uri -> {
                        if (uri != null) {
                            // Persist read permission across process restarts
                            getContentResolver().takePersistableUriPermission(
                                    uri,
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            try {
                                android.graphics.Bitmap bitmap =
                                        android.provider.MediaStore.Images.Media
                                                .getBitmap(getContentResolver(), uri);
                                android.graphics.Bitmap scaled = scaleBitmap(bitmap, 300);
                                java.io.ByteArrayOutputStream baos =
                                        new java.io.ByteArrayOutputStream();
                                scaled.compress(
                                        android.graphics.Bitmap.CompressFormat.JPEG, 80, baos);
                                String base64 = android.util.Base64.encodeToString(
                                        baos.toByteArray(), android.util.Base64.DEFAULT);
                                session.saveProfilePhoto(base64);
                                setAvatarFromBase64(base64);
                            } catch (java.io.IOException e) {
                                e.printStackTrace();
                                Toast.makeText(this, "Could not load image",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        // Transparent / light status bar over gradient header
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0); // dark icons off — white icons on gradient
        }

        // ── Bind views ──────────────────────────────────────────────
        tvEditToggle    = findViewById(R.id.tvEditToggle);
        tvHeaderName    = findViewById(R.id.tvHeaderName);

        tvName          = findViewById(R.id.tvName);
        tvPhone         = findViewById(R.id.tvPhone);
        tvEmail         = findViewById(R.id.tvEmail);
        tvAddress       = findViewById(R.id.tvAddress);
        tvPhoneNote     = findViewById(R.id.tvPhoneNote);

        tilName         = findViewById(R.id.tilName);
        tilEmail        = findViewById(R.id.tilEmail);
        etName          = findViewById(R.id.etName);
        etEmail         = findViewById(R.id.etEmail);

        layoutEditActions = findViewById(R.id.layoutEditActions);
        btnSave           = findViewById(R.id.btnSave);
        btnCancelEdit     = findViewById(R.id.btnCancelEdit);

        progressBar     = findViewById(R.id.progressBar);
        scroll          = findViewById(R.id.scrollContent);

        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        btnChangePhoto  = findViewById(R.id.btnChangePhoto);

        // ── Init ────────────────────────────────────────────────────
        session = new SessionManager(this);
        repo    = new UserRepository();

        String savedPhoto = session.getProfilePhoto();
        if (savedPhoto != null) setAvatarFromBase64(savedPhoto);

        // ── Listeners ───────────────────────────────────────────────
        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        tvEditToggle.setOnClickListener(v -> toggleEditMode());
        btnSave.setOnClickListener(v -> saveChanges());
        btnCancelEdit.setOnClickListener(v -> {
            if (isEditMode) toggleEditMode();
        });
        btnChangePhoto.setOnClickListener(v -> openGallery());
        findViewById(R.id.cardAvatar).setOnClickListener(v -> {
            if (isEditMode) openGallery();
        });

        // ── Auto-start edit mode if launched from profile card edit button ──
        boolean startEditMode = getIntent().getBooleanExtra("start_edit_mode", false);

        loadUserData(startEditMode);
    }

    // ── Photo Picker ─────────────────────────────────────────────────
    private void openGallery() {
        photoPickerLauncher.launch(
                new androidx.activity.result.PickVisualMediaRequest.Builder()
                        .setMediaType(androidx.activity.result.contract.ActivityResultContracts
                                .PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
    }

    private void setAvatarFromBase64(String base64) {
        try {
            byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            android.graphics.Bitmap bitmap =
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (ivProfileAvatar != null && bitmap != null) {
                ivProfileAvatar.setPadding(0, 0, 0, 0);
                ivProfileAvatar.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private android.graphics.Bitmap scaleBitmap(android.graphics.Bitmap original, int maxSize) {
        int w = original.getWidth(), h = original.getHeight();
        float scale = Math.min((float) maxSize / w, (float) maxSize / h);
        if (scale >= 1f) return original;
        return android.graphics.Bitmap.createScaledBitmap(
                original, Math.round(w * scale), Math.round(h * scale), true);
    }

    // ── Load data ───────────────────────────────────────────────────
    private void loadUserData(boolean autoEdit) {
        String phone = session.getUserPhone();
        progressBar.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.GONE);

        repo.getUser(phone).addOnSuccessListener(doc -> {
            progressBar.setVisibility(View.GONE);
            scroll.setVisibility(View.VISIBLE);

            if (doc.exists()) {
                String name    = doc.getString("name");
                String email   = doc.getString("email");
                String address = doc.getString("address");

                String displayName = (name != null && !name.isEmpty()) ? name : "—";
                tvName.setText(displayName);
                tvHeaderName.setText(displayName);
                tvPhone.setText("+91 " + phone);
                tvEmail.setText((email   != null && !email.isEmpty())   ? email   : "—");
                tvAddress.setText((address != null && !address.isEmpty()) ? address : "—");

                etName.setText(name);
                etEmail.setText(email);
            } else {
                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
            }

            // Auto-enter edit mode after data is loaded
            if (autoEdit && !isEditMode) {
                toggleEditMode();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show();
        });
    }

    // ── Toggle edit / view mode ──────────────────────────────────────
    private void toggleEditMode() {
        isEditMode = !isEditMode;

        if (isEditMode) {
            tvEditToggle.setText("Done");
            tvEditToggle.setTextColor(android.graphics.Color.WHITE);

            // Show edit inputs
            tvName.setVisibility(View.GONE);
            tilName.setVisibility(View.VISIBLE);
            tvEmail.setVisibility(View.GONE);
            tilEmail.setVisibility(View.VISIBLE);

            tvPhoneNote.setVisibility(View.VISIBLE);
            layoutEditActions.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.VISIBLE);
            btnChangePhoto.setVisibility(View.VISIBLE);
        } else {
            tvEditToggle.setText("Edit");
            tvEditToggle.setTextColor(android.graphics.Color.WHITE);

            // Show view labels
            tvName.setVisibility(View.VISIBLE);
            tilName.setVisibility(View.GONE);
            tvEmail.setVisibility(View.VISIBLE);
            tilEmail.setVisibility(View.GONE);

            tvPhoneNote.setVisibility(View.GONE);
            layoutEditActions.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);
            btnChangePhoto.setVisibility(View.GONE);

            // Reset fields
            etName.setText(tvName.getText().toString().equals("—") ? "" : tvName.getText());
            etEmail.setText(tvEmail.getText().toString().equals("—") ? "" : tvEmail.getText());
        }
    }

    // ── Save ────────────────────────────────────────────────────────
    private void saveChanges() {
        String newName  = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (newName.isEmpty()) {
            tilName.setError("Name is required");
            return;
        }
        tilName.setError(null);

        if (!newEmail.isEmpty()
                && !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            tilEmail.setError("Invalid email address");
            return;
        }
        tilEmail.setError(null);

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("name", newName);
        updates.put("email", newEmail);

        repo.updateUser(session.getUserPhone(), updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    tvName.setText(newName);
                    tvHeaderName.setText(newName);
                    tvEmail.setText(newEmail.isEmpty() ? "—" : newEmail);

                    toggleEditMode();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Update failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
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
