package com.ankita.freshfold.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ankita.freshfold.data.repository.ServiceRepository;
import com.ankita.freshfold.data.repository.UserRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;

    private final MutableLiveData<String> _userName = new MutableLiveData<>();
    public LiveData<String> userName = _userName;

    private final MutableLiveData<Double> _walletBalance = new MutableLiveData<>();
    public LiveData<Double> walletBalance = _walletBalance;

    private final MutableLiveData<Boolean> _isApproved = new MutableLiveData<>();
    public LiveData<Boolean> isApproved = _isApproved;

    private final MutableLiveData<List<Map<String, Object>>> _services = new MutableLiveData<>();
    public LiveData<List<Map<String, Object>>> services = _services;

    private ListenerRegistration userListener;

    public HomeViewModel() {
        this.userRepository = new UserRepository();
        this.serviceRepository = new ServiceRepository();
    }

    public void fetchUserInfo(String phone) {
        if (phone == null || phone.isEmpty()) return;

        if (userListener != null) userListener.remove();

        // Real-time listener for user data ensures balance and approval status updates instantly across all devices
        userListener = FirebaseFirestore.getInstance()
                .collection("freshfold").document("app_data")
                .collection("users").document(phone)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;
                    if (doc != null && doc.exists()) {
                        _userName.setValue(doc.getString("name"));
                        
                        Double actual = doc.getDouble("walletBalance");
                        Double reserved = doc.getDouble("reservedBalance");
                        if (actual == null) actual = 0.0;
                        if (reserved == null) reserved = 0.0;
                        _walletBalance.setValue(actual - reserved);

                        Boolean approved = doc.getBoolean("isApproved");
                        _isApproved.setValue(approved != null ? approved : true);
                    }
                });
    }

    public void loadServices() {
        serviceRepository.getAllServices().addOnSuccessListener(querySnapshot -> {
            List<Map<String, Object>> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                list.add(doc.getData());
            }
            _services.setValue(list);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
