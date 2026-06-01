package com.ankita.freshfold.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ankita.freshfold.data.repository.UserRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class WalletViewModel extends ViewModel {
    private final UserRepository userRepository;

    private final MutableLiveData<Double> _balance = new MutableLiveData<>(0.0);
    public LiveData<Double> balance = _balance;

    private final MutableLiveData<Double> _availableBalance = new MutableLiveData<>(0.0);
    public LiveData<Double> availableBalance = _availableBalance;

    private final MutableLiveData<java.util.List<com.ankita.freshfold.ui.wallet.Transaction>> _transactions = new MutableLiveData<>(new java.util.ArrayList<>());
    public LiveData<java.util.List<com.ankita.freshfold.ui.wallet.Transaction>> transactions = _transactions;

    private ListenerRegistration balanceListener;
    private String currentPhone;

    public WalletViewModel() {
        this.userRepository = new UserRepository();
    }

    public void fetchBalance(String phone) {
        if (phone == null || phone.isEmpty()) return;
        this.currentPhone = phone;

        // Use real-time listener for better compatibility and instant updates across devices
        if (balanceListener != null) {
            balanceListener.remove();
        }

        // Add real-time listener to user document for balance updates
        balanceListener = FirebaseFirestore.getInstance()
                .collection("freshfold").document("app_data")
                .collection("users").document(phone)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) return;
                    if (doc != null && doc.exists()) {
                        Double actual = doc.getDouble("walletBalance");
                        Double reserved = doc.getDouble("reservedBalance");
                        if (actual == null) actual = 0.0;
                        if (reserved == null) reserved = 0.0;
                        _balance.postValue(actual);                        // total balance
                        _availableBalance.postValue(actual - reserved);    // usable balance
                    }
                });

        fetchTransactions(phone);
    }

    public void fetchTransactions(String phone) {
        userRepository.getTransactions(phone).addOnSuccessListener(results -> {
            java.util.List<com.ankita.freshfold.ui.wallet.Transaction> list = new java.util.ArrayList<>();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
            
            for (Object res : results) {
                com.google.firebase.firestore.QuerySnapshot qs = (com.google.firebase.firestore.QuerySnapshot) res;
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : qs) {
                    String title = doc.getString("title");
                    String amount = doc.getString("amount");
                    String emoji = doc.getString("emoji");
                    Boolean isCredit = doc.getBoolean("isCredit");
                    Long timestamp = doc.getLong("timestamp");
                    String date = timestamp != null ? sdf.format(new java.util.Date(timestamp)) : "";
                    
                    com.ankita.freshfold.ui.wallet.Transaction txn = new com.ankita.freshfold.ui.wallet.Transaction(
                        title != null ? title : "",
                        date,
                        amount != null ? amount : "0.00",
                        emoji != null ? emoji : "💰",
                        isCredit != null ? isCredit : true
                    );
                    // Temporarily store timestamp for sorting
                    txn.timestamp = timestamp != null ? timestamp : 0L;
                    list.add(txn);
                }
            }
            
            // Sort combined list by timestamp descending
            java.util.Collections.sort(list, (t1, t2) -> Long.compare(t2.timestamp, t1.timestamp));
            
            _transactions.setValue(list);
        });
    }

    public void topUpWallet(String phone, double amount) {
        // Use Firestore atomic increment to avoid race conditions
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("walletBalance", com.google.firebase.firestore.FieldValue.increment(amount));

        userRepository.updateUser(phone, updates).addOnSuccessListener(aVoid -> {
            // Save credit transaction
            java.util.Map<String, Object> txn = new java.util.HashMap<>();
            txn.put("title", "Wallet Top-Up");
            txn.put("amount", String.format("%.2f", amount));
            txn.put("emoji", "💳");
            txn.put("isCredit", true);
            txn.put("timestamp", System.currentTimeMillis());
            txn.put("userPhone", phone);

            userRepository.saveTransaction(phone, null, txn).addOnSuccessListener(aVoid1 -> {
                // If real-time listener is not active, manually refresh balance
                if (balanceListener == null) {
                    fetchBalance(phone);
                }
                fetchTransactions(phone);
            });
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (balanceListener != null) {
            balanceListener.remove();
        }
    }
}
