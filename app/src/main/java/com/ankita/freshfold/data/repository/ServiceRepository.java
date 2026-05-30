package com.ankita.freshfold.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ServiceRepository {
    private final FirebaseFirestore db;

    public ServiceRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /** Global services (legacy) */
    public Task<QuerySnapshot> getAllServices() {
        return db.collection("freshfold").document("app_data").collection("services").get();
    }

    /** Franchise-specific services with active/inactive status */
    public Task<QuerySnapshot> getFranchiseServices(String franchiseId) {
        return db.collection("freshfold").document("app_data")
                .collection("franchises").document(franchiseId)
                .collection("services").get();
    }
}
