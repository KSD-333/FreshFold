package com.ankita.freshfold.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class SocietyRepository {
    private final FirebaseFirestore db;

    public SocietyRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Task<QuerySnapshot> getServiceableSocieties() {
        return db.collection("freshfold").document("app_data").collection("societies")
                .whereEqualTo("isServiceable", true)
                .get();
    }

    public Task<QuerySnapshot> getFranchises() {
        return db.collection("freshfold").document("app_data").collection("franchises").get();
    }

    public Task<QuerySnapshot> getAreas(String franchiseId) {
        return db.collection("freshfold").document("app_data")
                .collection("franchises").document(franchiseId)
                .collection("areas").get();
    }

    public Task<QuerySnapshot> getSocieties(String franchiseId, String areaName) {
        return db.collection("freshfold").document("app_data")
                .collection("franchises").document(franchiseId)
                .collection("areas").document(areaName)
                .collection("societies").get();
    }

    public Task<QuerySnapshot> getBuildings(String franchiseId, String areaName, String societyId) {
        return db.collection("freshfold").document("app_data")
                .collection("franchises").document(franchiseId)
                .collection("areas").document(areaName)
                .collection("societies").document(societyId)
                .collection("buildings").get();
    }

    public Task<QuerySnapshot> getFloors(String franchiseId, String areaName, String societyId, String buildingId) {
        return db.collection("freshfold").document("app_data")
                .collection("franchises").document(franchiseId)
                .collection("areas").document(areaName)
                .collection("societies").document(societyId)
                .collection("buildings").document(buildingId)
                .collection("floors").get();
    }

    public Task<DocumentSnapshot> getAddressReward(String addressKey) {
        return db.collection("freshfold").document("app_data").collection("address_rewards")
                .document(addressKey).get();
    }

    public Task<Void> saveAddressReward(String addressKey, Map<String, Object> reward) {
        return db.collection("freshfold").document("app_data").collection("address_rewards")
                .document(addressKey).set(reward);
    }
}
