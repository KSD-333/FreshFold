package com.ankita.freshfold.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ankita.freshfold.data.repository.SocietyRepository;
import com.ankita.freshfold.data.repository.UserRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrationViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final SocietyRepository societyRepository;

    private final MutableLiveData<List<Map<String, Object>>> _franchises = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> franchises = _franchises;

    private final MutableLiveData<List<Map<String, Object>>> _areas = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> areas = _areas;

    private final MutableLiveData<List<Map<String, Object>>> _globalSocieties = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> globalSocieties = _globalSocieties;

    private final MutableLiveData<List<Map<String, Object>>> _societies = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> societies = _societies;

    private final MutableLiveData<List<Map<String, Object>>> _buildings = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> buildings = _buildings;

    private final MutableLiveData<List<Map<String, Object>>> _floors = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> floors = _floors;

    private final MutableLiveData<String> _error = new MutableLiveData<>();
    public LiveData<String> error = _error;

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isSuccess = new MutableLiveData<>();
    public LiveData<Boolean> isSuccess = _isSuccess;

    public RegistrationViewModel() {
        this.userRepository = new UserRepository();
        this.societyRepository = new SocietyRepository();
    }
    
    public void loadFranchises() {
        _isLoading.setValue(true);
        societyRepository.getFranchises().addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            _franchises.setValue(list);
            _isLoading.setValue(false);
        }).addOnFailureListener(e -> _isLoading.setValue(false));
    }

    public void loadAreas(String franchiseId) {
        _isLoading.setValue(true);
        societyRepository.getAreas(franchiseId).addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            _areas.setValue(list);
            _isLoading.setValue(false);
        }).addOnFailureListener(e -> _isLoading.setValue(false));
    }

    public void loadGlobalSocieties() {
        societyRepository.getServiceableSocieties().addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            _globalSocieties.setValue(list);
        });
    }

    public void loadSocieties(String franchiseId, String areaId) {
        _isLoading.setValue(true);
        societyRepository.getSocieties(franchiseId, areaId).addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            _societies.setValue(list);
            _isLoading.setValue(false);
        }).addOnFailureListener(e -> _isLoading.setValue(false));
    }

    public void loadBuildings(String franchiseId, String areaId, String societyId) {
        _isLoading.setValue(true);
        societyRepository.getBuildings(franchiseId, areaId, societyId).addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            _buildings.setValue(list);
            _isLoading.setValue(false);
        }).addOnFailureListener(e -> _isLoading.setValue(false));
    }

    public void loadFloors(String franchiseId, String areaId, String societyId, String buildingId) {
        _isLoading.setValue(true);
        societyRepository.getFloors(franchiseId, areaId, societyId, buildingId).addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (data != null) {
                    data.put("id", doc.getId());
                    list.add(data);
                }
            }
            _floors.setValue(list);
            _isLoading.setValue(false);
        }).addOnFailureListener(e -> _isLoading.setValue(false));
    }

    /** Returns a null-safe, comma-separated address string skipping blank parts. */
    private String buildAddress(String flat, String floorName, String buildingName, String societyName, String area, String city, String pincode) {
        List<String> parts = new ArrayList<>();
        if (flat != null && !flat.trim().isEmpty())         parts.add(flat.trim());
        if (floorName != null && !floorName.trim().isEmpty()) parts.add(floorName.trim() + "th Floor");
        if (buildingName != null && !buildingName.trim().isEmpty()) parts.add(buildingName.trim());
        if (societyName != null && !societyName.trim().isEmpty()) parts.add(societyName.trim());
        if (area != null && !area.trim().isEmpty())         parts.add(area.trim());
        if (city != null && !city.trim().isEmpty())         parts.add(city.trim());
        if (pincode != null && !pincode.trim().isEmpty())   parts.add(pincode.trim());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    public void register(String name, String phone, String email, String password, String city, String area, String societyId, String societyName, String buildingId, String buildingName, String floorId, String floorName, String flat, String franchiseId) {
        _isLoading.setValue(true);
        
        // 1. Check uniqueness
        userRepository.getUser(phone).addOnSuccessListener(phoneDoc -> {
            if (phoneDoc.exists()) {
                _isLoading.setValue(false);
                _error.setValue("Phone already registered");
            } else {
                userRepository.getUserByEmail(email).addOnSuccessListener(emailSnap -> {
                    if (!emailSnap.isEmpty()) {
                        _isLoading.setValue(false);
                        _error.setValue("Email already in use");
                    } else {
                        // 2. Check Address Reward
                        String addressKey = generateAddressKey(societyName, buildingName, flat);
                        String fullAddress = buildAddress(flat, floorName, buildingName, societyName, area, city, null);
                        
                        societyRepository.getAddressReward(addressKey).addOnSuccessListener(rewardDoc -> {
                            boolean giveReward = true;
                            if (rewardDoc.exists()) {
                                String status = rewardDoc.getString("status");
                                Timestamp ts = rewardDoc.getTimestamp("timestamp");
                                
                                if (status != null && status.equals("active")) {
                                    giveReward = false;
                                } else if (ts != null) {
                                    long diff = System.currentTimeMillis() - ts.toDate().getTime();
                                    long ninetyDays = 90L * 24 * 60 * 60 * 1000;
                                    if (diff < ninetyDays) {
                                        giveReward = false;
                                    }
                                }
                            }
                            registerFinal(name, phone, email, password, fullAddress, giveReward, true, addressKey, city, area, societyId, societyName, buildingId, buildingName, floorId, floorName, franchiseId);
                        });
                    }
                });
            }
        });
    }

    public void registerManual(String name, String phone, String email, String password, String society, String building, String floor, String flat, String area, String city, String pincode) {
        _isLoading.setValue(true);
        String fullAddress = buildAddress(flat, floor, building, society, area, city, pincode);
        registerFinal(name, phone, email, password, fullAddress, false, false, null, city, area, null, society, null, building, null, floor, null);
    }

    private void registerFinal(String name, String phone, String email, String password, String address, boolean giveReward, boolean isApproved, String addressKey, String city, String area, String societyId, String societyName, String buildingId, String buildingName, String floorId, String floorName, String franchiseId) {
        String hashedPassword = hashPassword(password);
        double initialBalance = giveReward ? 100.0 : 0.0;

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("phone", phone);
        user.put("email", email);
        user.put("password", hashedPassword);
        user.put("address", address);
        user.put("walletBalance", initialBalance);
        user.put("role", "customer");
        user.put("isApproved", isApproved);
        
        if (city != null) user.put("city", city);
        if (area != null) user.put("area", area);
        if (societyId != null) user.put("societyId", societyId);
        if (societyName != null) user.put("societyName", societyName);
        if (buildingId != null) user.put("buildingId", buildingId);
        if (buildingName != null) user.put("buildingName", buildingName);
        if (floorId != null) user.put("floorId", floorId);
        if (floorName != null) user.put("floorName", floorName);
        if (franchiseId != null) user.put("franchiseId", franchiseId);

        userRepository.saveUser(phone, user).addOnSuccessListener(aVoid -> {
            // Save address also in sub-collection "address" with type "registration"
            userRepository.saveAddress(phone, address, "registration", franchiseId);
            if (giveReward && addressKey != null) {
                Map<String, Object> reward = new HashMap<>();
                reward.put("addressKey", addressKey);
                reward.put("phone", phone);
                reward.put("timestamp", Timestamp.now());
                reward.put("status", "active");
                societyRepository.saveAddressReward(addressKey, reward);
            }

            _isLoading.setValue(false);
            if (giveReward) {
                java.util.Map<String, Object> txn = new java.util.HashMap<>();
                txn.put("title", "Registration Bonus");
                txn.put("amount", "100.00");
                txn.put("emoji", "🎁");
                txn.put("isCredit", true);
                txn.put("timestamp", System.currentTimeMillis());
                txn.put("userPhone", phone);
                userRepository.saveTransaction(phone, null, txn);
                _toastMessage.setValue("₹100 bonus added to your wallet!");
            }
            _toastMessage.setValue("Registration successful! Please login.");
            _isSuccess.setValue(true);
        }).addOnFailureListener(e -> {
            _isLoading.setValue(false);
            _error.setValue("Error: " + e.getMessage());
        });
    }

    private String generateAddressKey(String s, String b, String f) {
        String raw = (s + "_" + b + "_" + f).toLowerCase().replaceAll("\\s+", "");
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte hb : hash) sb.append(String.format("%02x", hb));
            return sb.toString();
        } catch (Exception e) { return raw; }
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
