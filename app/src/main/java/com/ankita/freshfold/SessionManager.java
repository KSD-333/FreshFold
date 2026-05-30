package com.ankita.freshfold;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context _context;

    private static final String PREF_NAME = "FreshfoldPref";
    private static final String IS_LOGIN = "IsLoggedIn";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_ROLE = "role";
    public static final String KEY_PROFILE_PHOTO = "profilePhoto";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_PREVIOUS_ADDRESS = "previous_address";
    public static final String KEY_IS_APPROVED = "isApproved";

    public SessionManager(Context context) {
        this._context = context;
        pref = _context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String userId, String phone, String role, boolean isApproved) {
        editor.putBoolean(IS_LOGIN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_ROLE, role);
        editor.putBoolean(KEY_IS_APPROVED, isApproved);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }

    public String getUserRole() {
        return pref.getString(KEY_ROLE, "");
    }

    public boolean isApproved() {
        return pref.getBoolean(KEY_IS_APPROVED, true); // Default true for older users
    }

    public void setApproved(boolean isApproved) {
        editor.putBoolean(KEY_IS_APPROVED, isApproved);
        editor.apply();
    }

    public String getUserPhone() {
        return pref.getString(KEY_PHONE, "");
    }

    public void saveProfilePhoto(String base64) {
        editor.putString(KEY_PROFILE_PHOTO, base64);
        editor.apply();
    }

    public String getProfilePhoto() {
        return pref.getString(KEY_PROFILE_PHOTO, null);
    }

    public void saveUserProfile(String name, String email, String address) {
        editor.putString(KEY_NAME, name != null ? name : "");
        editor.putString(KEY_EMAIL, email != null ? email : "");
        editor.putString(KEY_ADDRESS, address != null ? address : "");
        editor.apply();
    }

    public void updateUserAddress(String address) {
        editor.putString(KEY_ADDRESS, address != null ? address : "");
        editor.apply();
    }

    public String getUserName() {
        return pref.getString(KEY_NAME, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_EMAIL, "");
    }

    public String getUserAddress() {
        return pref.getString(KEY_ADDRESS, "");
    }

    public void savePreviousAddress(String address) {
        editor.putString(KEY_PREVIOUS_ADDRESS, address != null ? address : "");
        editor.apply();
    }

    public String getPreviousAddress() {
        return pref.getString(KEY_PREVIOUS_ADDRESS, "");
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
