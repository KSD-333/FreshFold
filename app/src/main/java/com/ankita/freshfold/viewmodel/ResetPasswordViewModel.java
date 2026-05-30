package com.ankita.freshfold.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ankita.freshfold.data.repository.UserRepository;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ResetPasswordViewModel extends ViewModel {
    private final UserRepository userRepository;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isSuccess = new MutableLiveData<>();
    public LiveData<Boolean> isSuccess = _isSuccess;

    public ResetPasswordViewModel() {
        this.userRepository = new UserRepository();
    }

    public void resetPassword(String mobile, String newPassword) {
        _isLoading.setValue(true);
        String hashedPassword = hashPassword(newPassword);

        Map<String, Object> updates = new HashMap<>();
        updates.put("password", hashedPassword);

        userRepository.saveUser(mobile, updates).addOnSuccessListener(aVoid -> {
            _isLoading.setValue(false);
            _isSuccess.setValue(true);
        }).addOnFailureListener(e -> {
            _isLoading.setValue(false);
            _error.setValue("Error: " + e.getMessage());
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) { return password; }
    }
}
