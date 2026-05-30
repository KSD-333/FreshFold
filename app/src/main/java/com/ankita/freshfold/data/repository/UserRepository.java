package com.ankita.freshfold.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class UserRepository {
    private final FirebaseFirestore db;

    public UserRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<DocumentSnapshot> getUser(String phone) {
        return db.collection("freshfold").document("app_data")
                .collection("users").document(phone).get();
    }

    public Task<QuerySnapshot> getUserByEmail(String email) {
        return db.collection("freshfold").document("app_data")
                .collection("users")
                .whereEqualTo("email", email).get();
    }

    public Task<Void> saveUser(String phone, Map<String, Object> user) {
        // Also sync ONLY basic details to root users collection
        Map<String, Object> rootUser = new java.util.HashMap<>();
        String[] allowed = {"name", "phone", "email", "address", "userId"};
        for (String key : allowed) {
            if (user.containsKey(key)) {
                rootUser.put(key, user.get(key));
            }
        }
        db.collection("users").document(phone).set(rootUser);

        return db.collection("freshfold").document("app_data")
                .collection("users").document(phone).set(user);
        }
    
    // Save address as a new document in the "address" sub-collection
    // type = "registration" or "saved"
    public Task<Void> saveAddress(String phone, String address, String type) {
        com.google.android.gms.tasks.TaskCompletionSource<Void> tcs = new com.google.android.gms.tasks.TaskCompletionSource<>();
        
        db.collection("freshfold").document("app_data")
            .collection("users").document(phone)
            .collection("address")
            .whereEqualTo("address", address)
            .limit(1)
            .get()
            .addOnSuccessListener(querySnap -> {
                if (!querySnap.isEmpty()) {
                    // Address already exists, just update timestamp
                    String docId = querySnap.getDocuments().get(0).getId();
                    db.collection("freshfold").document("app_data")
                        .collection("users").document(phone)
                        .collection("address").document(docId)
                        .update("timestamp", com.google.firebase.Timestamp.now())
                        .addOnSuccessListener(aVoid -> tcs.setResult(null))
                        .addOnFailureListener(e -> tcs.setException(e));
                } else {
                    // Create new address entry
                    Map<String, Object> data = new java.util.HashMap<>();
                    data.put("address", address);
                    data.put("type", type);
                    data.put("timestamp", com.google.firebase.Timestamp.now());
                    db.collection("freshfold").document("app_data")
                        .collection("users").document(phone)
                        .collection("address").document()
                        .set(data)
                        .addOnSuccessListener(aVoid -> tcs.setResult(null))
                        .addOnFailureListener(e -> tcs.setException(e));
                }
            })
            .addOnFailureListener(e -> tcs.setException(e));
            
        return tcs.getTask();
    }

    // Get all addresses from the sub-collection
    public Task<QuerySnapshot> getAddresses(String phone) {
        return db.collection("freshfold").document("app_data")
                .collection("users").document(phone)
                .collection("address")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get();
    }

    public Task<Void> updateUser(String phone, Map<String, Object> updates) {
        // Also sync ONLY allowed fields to root users collection
        Map<String, Object> rootUpdates = new java.util.HashMap<>();
        String[] allowed = {"name", "phone", "email", "address", "userId"};
        for (String key : allowed) {
            if (updates.containsKey(key)) {
                rootUpdates.put(key, updates.get(key));
            }
        }
        if (!rootUpdates.isEmpty()) {
            db.collection("users").document(phone).set(rootUpdates, com.google.firebase.firestore.SetOptions.merge());
        }

        return db.collection("freshfold").document("app_data")
                .collection("users").document(phone).update(updates);
    }

    public void handleOrderStatusWalletUpdate(String phone, double amount, String newStatus) {
        if (phone == null || phone.isEmpty()) return;

        db.collection("freshfold").document("app_data")
            .collection("users").document(phone)
            .get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    double actual = doc.contains("walletBalance") && doc.getDouble("walletBalance") != null ? doc.getDouble("walletBalance") : 0.0;
                    double reserved = doc.contains("reservedBalance") && doc.getDouble("reservedBalance") != null ? doc.getDouble("reservedBalance") : 0.0;

                    Map<String, Object> updates = new java.util.HashMap<>();
                    
                    if ("Delivered".equalsIgnoreCase(newStatus) || "Completed".equalsIgnoreCase(newStatus)) {
                        // Deduct actual, remove hold (Transaction already recorded when order was placed)
                        updates.put("walletBalance", Math.max(0.0, actual - amount));
                        updates.put("reservedBalance", Math.max(0.0, reserved - amount));

                    } else if ("Cancelled".equalsIgnoreCase(newStatus)) {
                        // Keep actual, remove hold
                        updates.put("reservedBalance", Math.max(0.0, reserved - amount));
                        
                        // Save refund transaction
                        Map<String, Object> txn = new java.util.HashMap<>();
                        txn.put("title", "Refund (Cancelled)");
                        txn.put("amount", String.valueOf(amount));
                        txn.put("emoji", "🔙");
                        txn.put("isCredit", true);
                        txn.put("timestamp", System.currentTimeMillis());
                        txn.put("userPhone", phone);
                        // For refunds, we'll try to get addressDocId if available, 
                        // but handleOrderStatusWalletUpdate doesn't have it currently.
                        // We will save to root for now if it's missing.
                        saveTransaction(phone, null, txn);
                    } else {
                        // Other statuses: no wallet change
                        return;
                    }

                    updateUser(phone, updates);
                }
            });
    }

    public Task<Void> saveTransaction(String phone, String addressDocId, Map<String, Object> transaction) {
        if (addressDocId == null || addressDocId.isEmpty()) {
            return db.collection("freshfold").document("app_data")
                    .collection("users").document(phone)
                    .collection("transactions").document()
                    .set(transaction);
        }
        return db.collection("freshfold").document("app_data")
                .collection("users").document(phone)
                .collection("address").document(addressDocId)
                .collection("transactions").document()
                .set(transaction);
    }

    public Task<java.util.List<com.google.firebase.firestore.QuerySnapshot>> getTransactions(String phone) {
        return db.collection("freshfold").document("app_data")
            .collection("users").document(phone)
            .collection("address")
            .get()
            .continueWithTask(task -> {
                java.util.List<Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new java.util.ArrayList<>();
                // Fetch root transactions (e.g. wallet top-ups or old ones)
                tasks.add(db.collection("freshfold").document("app_data")
                    .collection("users").document(phone)
                    .collection("transactions").get());
                
                if (task.isSuccessful() && task.getResult() != null) {
                    for (DocumentSnapshot doc : task.getResult()) {
                        tasks.add(doc.getReference().collection("transactions").get());
                    }
                }
                return com.google.android.gms.tasks.Tasks.whenAllSuccess(tasks);
            });
    }
}
