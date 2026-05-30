package com.ankita.freshfold.data.repository;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AuthRepository {
    private final FirebaseFirestore db;
    private final RequestQueue requestQueue;

    public AuthRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public Task<DocumentSnapshot> checkUser(String mobile) {
        return db.collection("freshfold").document("app_data")
                .collection("users").document(mobile).get();
    }

    public void sendOtp(String url, com.android.volley.Response.Listener<String> listener, com.android.volley.Response.ErrorListener errorListener) {
        StringRequest request = new StringRequest(Request.Method.GET, url, listener, errorListener);
        requestQueue.add(request);
    }
}
