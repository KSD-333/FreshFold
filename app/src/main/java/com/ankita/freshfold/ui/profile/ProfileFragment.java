package com.ankita.freshfold.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.data.repository.UserRepository;
import com.ankita.freshfold.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ProfileFragment extends Fragment {

    private ImageView ivProfileAvatar;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sessionManager = new SessionManager(requireContext());
        String phone = sessionManager.getUserPhone();

        // ── Avatar ──
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);
        // Load saved photo if exists
        String savedPhoto = sessionManager.getProfilePhoto();
        if (savedPhoto != null) {
            setAvatarFromBase64(savedPhoto);
        }

        // Avatar is view-only here
        ivProfileAvatar = view.findViewById(R.id.ivProfileAvatar);

        // ── Bind basic info from session cache (instant — no loading) ──
        TextView tvPhone = view.findViewById(R.id.tvProfilePhone);
        TextView tvName  = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvAddr  = view.findViewById(R.id.tvAddress);

        if (tvPhone != null && phone != null) tvPhone.setText("+91 " + phone);
        if (tvName  != null) tvName.setText(sessionManager.getUserName());
        if (tvEmail != null) tvEmail.setText(sessionManager.getUserEmail());
        if (tvAddr  != null) tvAddr.setText(sessionManager.getUserAddress());

        setupMenuRow(view, R.id.rowPersonalInfo,
                R.drawable.ic_profile_nav, "Personal Information", null, null);
        setupMenuRow(view, R.id.rowSavedAddress,
                R.drawable.ic_location_pin, "Saved Address", null, null);
        setupMenuRow(view, R.id.rowPaymentMethods,
                R.drawable.ic_wallet_outline, "Payment Methods", null, null);
        setupMenuRow(view, R.id.rowMyOrders,
                R.drawable.ic_orders, "My Orders", null, null);

        setupMenuRow(view, R.id.rowHelp,
                R.drawable.ic_phone_call, "Help & Support", null, null);
        setupMenuRow(view, R.id.rowAboutUs,
                R.drawable.ic_info, "About Us", null, null);

        // ── My Orders click → load MyOrdersFragment via parent activity ──
        View rowMyOrders = view.findViewById(R.id.rowMyOrders);
        if (rowMyOrders != null) {
            rowMyOrders.setOnClickListener(v -> {
                if (getActivity() instanceof com.ankita.freshfold.ui.home.MainActivity) {
                    com.ankita.freshfold.ui.home.MainActivity mainActivity =
                            (com.ankita.freshfold.ui.home.MainActivity) getActivity();
                    mainActivity.loadFragment(
                            new com.ankita.freshfold.ui.orders.MyOrdersFragment(), "ORDERS");
                }
            });
        }

        // ── Personal Information click → open PersonalInfoActivity ──
        View rowPersonalInfo = view.findViewById(R.id.rowPersonalInfo);
        if (rowPersonalInfo != null) {
            rowPersonalInfo.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PersonalInfoActivity.class)));
        }

        // ── Saved Address click → open SavedAddressActivity (read-only address from registration) ──
        View rowSavedAddress = view.findViewById(R.id.rowSavedAddress);
        View dividerSavedAddress = view.findViewById(R.id.dividerSavedAddress);
        if (rowSavedAddress != null) {
            rowSavedAddress.setVisibility(View.GONE);
        }
        if (dividerSavedAddress != null) {
            dividerSavedAddress.setVisibility(View.GONE);
        }

        // ── Edit button on profile card → open PersonalInfoActivity in edit mode ──
        View btnEditProfile = view.findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Intent editIntent = new Intent(requireContext(), PersonalInfoActivity.class);
                editIntent.putExtra("start_edit_mode", true);
                startActivity(editIntent);
            });
        }

        // ── Other rows click → show Toast ──
        int[] otherRows = {
            R.id.rowPaymentMethods
        };
        for (int id : otherRows) {
            View row = view.findViewById(id);
            if (row != null) {
                row.setOnClickListener(v -> 
                    android.widget.Toast.makeText(requireContext(), "Coming Soon!", android.widget.Toast.LENGTH_SHORT).show());
            }
        }

        // ── Help & Support click ──
        View rowHelp = view.findViewById(R.id.rowHelp);
        if (rowHelp != null) {
            rowHelp.setOnClickListener(v -> 
                startActivity(new Intent(requireContext(), HelpSupportActivity.class)));
        }

        // ── About Us click ──
        View rowAboutUs = view.findViewById(R.id.rowAboutUs);
        if (rowAboutUs != null) {
            rowAboutUs.setOnClickListener(v -> 
                startActivity(new Intent(requireContext(), AboutUsActivity.class)));
        }

        // ── Logout ──
        MaterialButton btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                sessionManager.logoutUser();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        // ── Back Button click ──
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof com.ankita.freshfold.ui.home.MainActivity) {
                    ((com.ankita.freshfold.ui.home.MainActivity) getActivity()).loadFragment(
                            new com.ankita.freshfold.ui.home.HomeFragment(), "HOME");
                    // Also update bottom nav selection in MainActivity if possible
                } else {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshUserData();
    }

    private void refreshUserData() {
        // Refresh avatar
        String savedPhoto = sessionManager.getProfilePhoto();
        if (ivProfileAvatar != null && savedPhoto != null) {
            setAvatarFromBase64(savedPhoto);
        }

        // Refresh from Firestore and update cache + UI
        String phone = sessionManager.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        View view = getView();
        if (view == null) return;

        TextView tvName  = view.findViewById(R.id.tvProfileName);
        TextView tvEmail = view.findViewById(R.id.tvProfileEmail);
        TextView tvAddr  = view.findViewById(R.id.tvAddress);

        // 1. Update from cache instantly for snappy feel
        if (tvName  != null) tvName.setText(sessionManager.getUserName());
        if (tvEmail != null) tvEmail.setText(sessionManager.getUserEmail());
        if (tvAddr  != null) tvAddr.setText(sessionManager.getUserAddress());
        setupMenuRow(view, R.id.rowSavedAddress, R.drawable.ic_location_pin, "Saved Address", sessionManager.getUserAddress(), null);

        // 2. Refresh from Firestore for consistency
        UserRepository repo = new UserRepository();
        repo.getUser(phone).addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                String name    = doc.getString("name");
                String email   = doc.getString("email");
                String address = doc.getString("address");

                // Update UI
                if (tvName  != null) tvName.setText(name    != null ? name    : "");
                if (tvEmail != null) tvEmail.setText(email  != null ? email   : "");
                if (tvAddr  != null) tvAddr.setText(address != null ? address : "");
                setupMenuRow(view, R.id.rowSavedAddress, R.drawable.ic_location_pin, "Saved Address", address, null);

                // Update session cache
                sessionManager.saveUserProfile(name, email, address);
            }
        });
    }

    private void setAvatarFromBase64(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (ivProfileAvatar != null && bitmap != null) {
                ivProfileAvatar.setPadding(0, 0, 0, 0);
                ivProfileAvatar.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap scaleBitmap(Bitmap original, int maxSize) {
        int width  = original.getWidth();
        int height = original.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale >= 1f) return original;
        int newWidth  = Math.round(width  * scale);
        int newHeight = Math.round(height * scale);
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private void setupMenuRow(View parent, int rowId, int iconRes, String title, @Nullable String subtitle, @Nullable String badge) {
        View row = parent.findViewById(rowId);
        if (row == null) return;

        ImageView icon   = row.findViewById(R.id.ivMenuIcon);
        TextView tvTitle = row.findViewById(R.id.tvMenuTitle);
        TextView tvSub   = row.findViewById(R.id.tvMenuSubtitle);
        TextView tvBadge = row.findViewById(R.id.tvMenuBadge);

        if (icon    != null) icon.setImageResource(iconRes);
        if (tvTitle != null) tvTitle.setText(title);
        
        if (tvSub != null) {
            if (subtitle != null && !subtitle.isEmpty()) {
                tvSub.setText(subtitle);
                tvSub.setVisibility(View.VISIBLE);
            } else {
                tvSub.setVisibility(View.GONE);
            }
        }

        if (tvBadge != null) {
            if (badge != null) {
                tvBadge.setText(badge);
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }
        }
    }
}
