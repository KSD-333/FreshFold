package com.ankita.freshfold.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ankita.freshfold.data.repository.AuthRepository;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LoginViewModel extends AndroidViewModel {
    private final AuthRepository authRepository;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _navigationRole = new MutableLiveData<>();
    public LiveData<String> navigationRole = _navigationRole;

    private final MutableLiveData<Boolean> _isApproved = new MutableLiveData<>();
    public LiveData<Boolean> isApproved = _isApproved;

    private final MutableLiveData<Boolean> _otpSent = new MutableLiveData<>();
    public LiveData<Boolean> otpSent = _otpSent;

    private String serverOtp;
    private boolean isResetFlow = false;

    private final String username = "Experts";
    private final String authkey = "ba9dcdcdfcXX"; 
    private final String senderId = "EXTSKL";
    private final String accusage = "1";

    private static final Set<String> BYPASS_PHONES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "8888888888", "9999999999", "7777777777", "6666666666",
                    "5555555555", "4444444444", "3333333333", "2222222222",
                    "1111111111", "0000000000"
            ))
    );

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.authRepository = new AuthRepository(application);
    }

    public void loginWithPassword(String mobile, String password) {
        _isLoading.setValue(true);
        authRepository.checkUser(mobile).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String storedHashedPassword = documentSnapshot.getString("password");
                String inputHashedPassword = hashPassword(password);

                if (inputHashedPassword.equals(storedHashedPassword) || password.equals(storedHashedPassword)) {
                    Boolean approved = documentSnapshot.getBoolean("isApproved");
                    _isApproved.setValue(approved != null ? approved : true);
                    _navigationRole.setValue(documentSnapshot.getString("role"));
                } else {
                    _isLoading.setValue(false);
                    _error.setValue("Incorrect password. You can log in using OTP instead.");
                }
            } else {
                _isLoading.setValue(false);
                _error.setValue("Number not registered.");
            }
        }).addOnFailureListener(e -> {
            _isLoading.setValue(false);
            _error.setValue("Error: " + e.getMessage());
        });
    }

    public void startOtpFlow(String mobile, boolean isReset) {
        this.isResetFlow = isReset;
        if (BYPASS_PHONES.contains(mobile)) {
            _navigationRole.setValue("customer"); // Default for bypass
        } else {
            checkUserAndSendOtp(mobile);
        }
    }

    private void checkUserAndSendOtp(String mobile) {
        _isLoading.setValue(true);
        authRepository.checkUser(mobile).addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                sendOtpToUser(mobile);
            } else {
                _isLoading.setValue(false);
                _error.setValue("Number not registered. Please register first.");
            }
        }).addOnFailureListener(e -> {
            _isLoading.setValue(false);
            _error.setValue("Network Error: " + e.getMessage());
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

    public void verifyOtp(String inputOtp, String mobile) {
        if (inputOtp.equals(serverOtp)) {
            if (isResetFlow) {
                _navigationRole.setValue("reset_password");
            } else {
                _isLoading.setValue(true);
                authRepository.checkUser(mobile).addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean approved = documentSnapshot.getBoolean("isApproved");
                        _isApproved.setValue(approved != null ? approved : true);
                        _navigationRole.setValue(documentSnapshot.getString("role"));
                    }
                });
            }
        } else {
            _error.setValue("Invalid OTP");
        }
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
