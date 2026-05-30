package com.ankita.freshfold.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ankita.freshfold.data.repository.AuthRepository;
import com.ankita.freshfold.data.repository.UserRepository;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _otpSent = new MutableLiveData<>();
    public LiveData<Boolean> otpSent = _otpSent;

    private final MutableLiveData<Boolean> _otpVerified = new MutableLiveData<>();
    public LiveData<Boolean> otpVerified = _otpVerified;

    private final MutableLiveData<Boolean> _passwordResetSuccess = new MutableLiveData<>();
    public LiveData<Boolean> passwordResetSuccess = _passwordResetSuccess;

    private String serverOtp;
    
    private final String username = "Experts";
    private final String authkey = "ba9dcdcdfcXX"; 
    private final String senderId = "EXTSKL";
    private final String accusage = "1";

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
        this.userRepository = new UserRepository();
    }

    public void checkUserAndSendOtp(String mobile) {
        _isLoading.setValue(true);
        authRepository.checkUser(mobile).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                sendOtpToUser(mobile);
            } else {
                _isLoading.setValue(false);
                _error.setValue("Number not registered.");
            }
        }).addOnFailureListener(e -> {
            _isLoading.setValue(false);
            _error.setValue("Error: " + e.getMessage());
        });
    }

    private void sendOtpToUser(String mobile) {
        serverOtp = String.valueOf((int) (Math.random() * 900000) + 100000);
        String message = "Your Verification Code for login is " + serverOtp + ". - Expertskill Technology.";
        message = message.replace(" ", "%20");

        String url = "https://mobicomm.dove-sms.com/submitsms.jsp?" +
                "user=" + username +
                "&key=" + authkey +
                "&mobile=+91" + mobile +
                "&message=" + message +
                "&accusage=" + accusage +
                "&senderid=" + senderId;

        authRepository.sendOtp(url, response -> {
            _isLoading.setValue(false);
            if (response.toLowerCase().contains("success") || response.toLowerCase().contains("submitted")) {
                _otpSent.setValue(true);
            } else {
                _error.setValue("SMS API Error: " + response);
            }
        }, error -> {
            _isLoading.setValue(false);
            _error.setValue("Network Error: " + error.getMessage());
        });
    }

    public void verifyOtp(String inputOtp) {
        if (inputOtp.equals(serverOtp)) {
            _otpVerified.setValue(true);
        } else {
            _error.setValue("Invalid OTP");
        }
    }

    public void resetPassword(String mobile, String newPassword) {
        _isLoading.setValue(true);
        String hashedPassword = hashPassword(newPassword);

        Map<String, Object> updates = new HashMap<>();
        updates.put("password", hashedPassword);

        userRepository.updateUser(mobile, updates).addOnSuccessListener(aVoid -> {
            _isLoading.setValue(false);
            _passwordResetSuccess.setValue(true);
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
