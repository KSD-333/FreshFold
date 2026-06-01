package com.ankita.freshfold;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class FranchiseManager {
    private static FranchiseManager instance;
    private String franchiseId;
    private List<DocumentSnapshot> cachedServices;
    private boolean isFetching = false;

    private FranchiseManager() {}

    public static synchronized FranchiseManager getInstance() {
        if (instance == null) {
            instance = new FranchiseManager();
        }
        return instance;
    }

    public String getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(String franchiseId) {
        this.franchiseId = franchiseId;
    }

    public List<DocumentSnapshot> getCachedServices() {
        return cachedServices;
    }

    public void setCachedServices(List<DocumentSnapshot> cachedServices) {
        this.cachedServices = cachedServices;
    }

    public boolean isFetching() {
        return isFetching;
    }

    public void setFetching(boolean fetching) {
        isFetching = fetching;
    }

    public void clearCache() {
        franchiseId = null;
        cachedServices = null;
        isFetching = false;
    }
}
