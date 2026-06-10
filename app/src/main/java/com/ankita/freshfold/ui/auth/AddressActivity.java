package com.ankita.freshfold.ui.auth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.ankita.freshfold.R;
import com.ankita.freshfold.viewmodel.RegistrationViewModel;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressActivity extends AppCompatActivity {

    private AutoCompleteTextView actvCity, actvArea, actvSociety, actvBuilding, actvFloor;
    private TextInputLayout tilArea, tilSociety, tilBuilding, tilFloor, tilFlat;
    private LinearLayout layoutFloorFlat;
    
    private TextInputEditText etFlat, etManualSociety, etManualBuilding, etManualFloor, etManualFlat, etManualArea, etManualCity, etManualPincode;
    private LinearLayout layoutManualAddress;
    private TextView btnRegister;
    private View btnGetCurrentLocation;
    private ProgressDialog progressDialog;
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private List<Map<String, Object>> franchiseList = new ArrayList<>();
    private List<Map<String, Object>> areaList = new ArrayList<>();
    private List<Map<String, Object>> societyList = new ArrayList<>();
    private List<Map<String, Object>> globalSocietyList = new ArrayList<>();
    private List<Map<String, Object>> buildingList = new ArrayList<>();
    private List<Map<String, Object>> floorList = new ArrayList<>();

    private Map<String, Object> selectedFranchise = null;
    private Map<String, Object> selectedArea = null;
    private Map<String, Object> selectedSociety = null;
    private Map<String, Object> selectedBuilding = null;
    private Map<String, Object> selectedFloor = null;

    private RegistrationViewModel viewModel;

    private String name, phone, email, password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        name = getIntent().getStringExtra("name");
        phone = getIntent().getStringExtra("phone");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating account...");
        progressDialog.setCancelable(false);

        viewModel = new ViewModelProvider(this).get(RegistrationViewModel.class);

        initViews();
        setupObservers();
        setupAddressLogic();

        btnRegister.setOnClickListener(v -> validateAndRegister());

        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Step 1: Fetch franchises/cities and global societies
        viewModel.loadFranchises();
        viewModel.loadGlobalSocieties();
    }

    private void initViews() {
        actvCity = findViewById(R.id.actvCity);
        actvArea = findViewById(R.id.actvArea);
        actvSociety = findViewById(R.id.actvSociety);
        actvBuilding = findViewById(R.id.actvBuilding);
        actvFloor = findViewById(R.id.actvFloor);
        
        tilArea = findViewById(R.id.tilArea);
        tilSociety = findViewById(R.id.tilSociety);
        tilBuilding = findViewById(R.id.tilBuilding);
        tilFloor = findViewById(R.id.tilFloor);
        tilFlat = findViewById(R.id.tilFlat);
        layoutFloorFlat = findViewById(R.id.layoutFloorFlat);
        
        etFlat = findViewById(R.id.etFlat);
        
        etManualSociety = findViewById(R.id.etManualSociety);
        etManualBuilding = findViewById(R.id.etManualBuilding);
        etManualFloor = findViewById(R.id.etManualFloor);
        etManualFlat = findViewById(R.id.etManualFlat);
        layoutManualAddress = findViewById(R.id.layoutManualAddress);
        btnRegister = findViewById(R.id.btnRegister);
        btnGetCurrentLocation = findViewById(R.id.btnGetCurrentLocation);
    }

    private void setupObservers() {
        viewModel.franchises.observe(this, list -> {
            franchiseList.clear();
            franchiseList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> f : franchiseList) {
                if (f.get("address") != null) names.add((String) f.get("address"));
            }
            actvCity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
        });

        viewModel.globalSocieties.observe(this, list -> {
            globalSocietyList.clear();
            globalSocietyList.addAll(list);
            // If area is not selected yet, allow global search
            if (selectedArea == null) {
                List<String> names = new ArrayList<>();
                for (Map<String, Object> s : globalSocietyList) {
                    String sName = getStringField(s, "societyName", "name", "society", "id");
                    if (sName != null) names.add(sName);
                }
                actvSociety.setAdapter(new SocietyAdapter(this, names));
            }
        });

        viewModel.areas.observe(this, list -> {
            areaList.clear();
            areaList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> a : areaList) {
                // Document ID is the area name
                if (a.get("id") != null) names.add((String) a.get("id"));
            }
            actvArea.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
            // Always show area field once franchise is selected (data may be empty)
            tilArea.setVisibility(View.VISIBLE);
        });

        viewModel.societies.observe(this, list -> {
            societyList.clear();
            societyList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> s : societyList) {
                // Try multiple possible field names
                String sName = getStringField(s, "societyName", "name", "society", "id");
                if (sName != null) names.add(sName);
            }
            actvSociety.setAdapter(new SocietyAdapter(this, names));
            // Always show society field once area is selected
            tilSociety.setVisibility(View.VISIBLE);
        });

        viewModel.buildings.observe(this, list -> {
            buildingList.clear();
            buildingList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> b : buildingList) {
                // Try multiple possible field names
                String bName = getStringField(b, "buildingName", "name", "wing", "building", "id");
                if (bName != null) names.add(bName);
            }
            actvBuilding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
            // Always show building field once society is selected
            tilBuilding.setVisibility(View.VISIBLE);
        });

        viewModel.floors.observe(this, list -> {
            floorList.clear();
            floorList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> f : floorList) {
                // Try multiple possible field names
                String fName = getStringField(f, "floorName", "name", "floor", "id");
                if (fName != null) names.add(fName);
            }
            actvFloor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
            // Always show floor+flat row once building is selected
            layoutFloorFlat.setVisibility(View.VISIBLE);
        });

        viewModel.isLoading.observe(this, loading -> {
            if (loading) progressDialog.show();
            else progressDialog.dismiss();
        });

        viewModel.error.observe(this, err -> Toast.makeText(this, err, Toast.LENGTH_SHORT).show());
        viewModel.toastMessage.observe(this, msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
        viewModel.isSuccess.observe(this, success -> {
            if (success) {
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupAddressLogic() {
        // City
        actvCity.setOnClickListener(v -> actvCity.showDropDown());
        actvCity.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvCity.showDropDown(); });
        actvCity.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedFranchise = null;
            for (Map<String, Object> f : franchiseList) {
                if (selected.equals(f.get("address"))) {
                    selectedFranchise = f;
                    break;
                }
            }
            resetCascadingFrom(1);
            if (selectedFranchise != null) {
                viewModel.loadAreas((String) selectedFranchise.get("id"));
            }
        });

        // Area
        actvArea.setOnClickListener(v -> actvArea.post(() -> actvArea.showDropDown()));
        actvArea.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvArea.post(() -> actvArea.showDropDown()); });
        actvArea.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedArea = null;
            for (Map<String, Object> a : areaList) {
                if (selected.equals(a.get("id"))) {
                    selectedArea = a;
                    break;
                }
            }
            resetCascadingFrom(2);
            if (selectedArea != null) {
                // Populate societyList from selectedArea map
                societyList.clear();
                Object socObj = selectedArea.get("societies");
                if (socObj instanceof Map) {
                    Map<String, Object> socMap = (Map<String, Object>) socObj;
                    for (Map.Entry<String, Object> entry : socMap.entrySet()) {
                        if (entry.getValue() instanceof Map) {
                            Map<String, Object> sData = (Map<String, Object>) entry.getValue();
                            sData.put("id", entry.getKey());
                            societyList.add(sData);
                        }
                    }
                }
                
                List<String> names = new ArrayList<>();
                for (Map<String, Object> s : societyList) {
                    String sName = getStringField(s, "societyName", "name", "society", "id");
                    if (sName != null) names.add(sName);
                }
                actvSociety.setAdapter(new SocietyAdapter(AddressActivity.this, names));
                tilSociety.setVisibility(View.VISIBLE);
            }
        });

        // Society
        actvSociety.setOnClickListener(v -> actvSociety.post(() -> actvSociety.showDropDown()));
        actvSociety.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvSociety.post(() -> actvSociety.showDropDown()); });
        actvSociety.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (selected.equals("Add Society")) {
                selectedSociety = null;
                resetCascadingFrom(3);
                
                // Hide tilSociety, show layoutManualAddress and Location Button
                tilSociety.setVisibility(View.GONE);
                layoutManualAddress.setVisibility(View.VISIBLE);
                btnGetCurrentLocation.setVisibility(View.VISIBLE);
                etManualSociety.setText("");
            } else {
                layoutManualAddress.setVisibility(View.GONE);
                btnGetCurrentLocation.setVisibility(View.GONE);
                selectedSociety = null;
                
                // Check local societyList first, if empty/null, check globalSocietyList
                List<Map<String, Object>> searchList = societyList.isEmpty() ? globalSocietyList : societyList;
                for (Map<String, Object> s : searchList) {
                    String sName = getStringField(s, "societyName", "name", "society", "id");
                    if (selected.equals(sName)) {
                        selectedSociety = s;
                        break;
                    }
                }
                resetCascadingFrom(3);
                tilBuilding.setVisibility(View.VISIBLE);
                layoutFloorFlat.setVisibility(View.VISIBLE);
                tilFlat.setVisibility(View.VISIBLE);

                if (selectedSociety != null) {
                    // Try to auto-fill City and Area if picking from global search
                    if (societyList.isEmpty()) {
                        String fId = (String) selectedSociety.get("franchiseId");
                        String aId = (String) selectedSociety.get("areaId");
                        if (fId != null) {
                            for (Map<String, Object> f : franchiseList) {
                                if (fId.equals(f.get("id"))) {
                                    selectedFranchise = f;
                                    actvCity.setText((String) f.get("address"), false);
                                    viewModel.loadAreas(fId);
                                    break;
                                }
                            }
                        }
                        if (aId != null) {
                            actvArea.setText(aId, false);
                        }
                    }

                    buildingList.clear();
                    Object wingsObj = selectedSociety.get("wings");
                    if (wingsObj instanceof List) {
                        List<Object> wingsList = (List<Object>) wingsObj;
                        for (int i = 0; i < wingsList.size(); i++) {
                            Object w = wingsList.get(i);
                            if (w instanceof Map) {
                                Map<String, Object> wData = (Map<String, Object>) w;
                                wData.put("id", String.valueOf(i));
                                buildingList.add(wData);
                            }
                        }
                    }
                    
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> b : buildingList) {
                        String bName = getStringField(b, "buildingName", "name", "wing", "building", "id");
                        if (bName != null) names.add(bName);
                    }
                    actvBuilding.setAdapter(new ArrayAdapter<>(AddressActivity.this, android.R.layout.simple_dropdown_item_1line, names));
                }
            }
        });

        // Building
        actvBuilding.setOnClickListener(v -> actvBuilding.post(() -> actvBuilding.showDropDown()));
        actvBuilding.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvBuilding.post(() -> actvBuilding.showDropDown()); });
        actvBuilding.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedBuilding = null;
            for (Map<String, Object> b : buildingList) {
                String bName = getStringField(b, "buildingName", "name", "wing", "building", "id");
                if (selected.equals(bName)) {
                    selectedBuilding = b;
                    break;
                }
            }
            resetCascadingFrom(4);
            layoutFloorFlat.setVisibility(View.VISIBLE);
            tilFlat.setVisibility(View.VISIBLE);
            
            if (selectedBuilding != null) {
                floorList.clear();
                Object floorsObj = selectedBuilding.get("floors");
                int numFloors = 0;
                if (floorsObj instanceof Number) {
                    numFloors = ((Number) floorsObj).intValue();
                } else if (floorsObj instanceof String) {
                    try { numFloors = Integer.parseInt((String) floorsObj); } catch (Exception e){}
                }
                
                List<String> names = new ArrayList<>();
                for (int i = 0; i <= numFloors; i++) {
                    Map<String, Object> fData = new HashMap<>();
                    String fName = i == 0 ? "Ground" : String.valueOf(i);
                    fData.put("id", String.valueOf(i));
                    fData.put("name", fName);
                    floorList.add(fData);
                    names.add(fName);
                }
                
                actvFloor.setAdapter(new ArrayAdapter<>(AddressActivity.this, android.R.layout.simple_dropdown_item_1line, names));
            }
        });

        // Floor
        actvFloor.setOnClickListener(v -> actvFloor.post(() -> actvFloor.showDropDown()));
        actvFloor.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvFloor.post(() -> actvFloor.showDropDown()); });
        actvFloor.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            selectedFloor = null;
            for (Map<String, Object> f : floorList) {
                String fName = getStringField(f, "floorName", "name", "floor", "id");
                if (selected.equals(fName)) {
                    selectedFloor = f;
                    break;
                }
            }
            if (selectedFloor != null) {
                tilFlat.setVisibility(View.VISIBLE);
            }
        });

        btnGetCurrentLocation.setOnClickListener(v -> checkLocationPermission());
    }

    private void resetCascadingFrom(int level) {
        if (level <= 1) {
            selectedArea = null;
            actvArea.setText("", false);
            // Area stays visible — only clear data
            areaList.clear();
            actvArea.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));
        }
        if (level <= 2) {
            selectedSociety = null;
            actvSociety.setText("", false);
            tilSociety.setVisibility(View.VISIBLE); // Keep visible for search
            layoutManualAddress.setVisibility(View.GONE);
            btnGetCurrentLocation.setVisibility(View.GONE);
            societyList.clear();
            
            // Revert to global search if area is cleared
            if (selectedArea == null && !globalSocietyList.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Map<String, Object> s : globalSocietyList) {
                    String sName = getStringField(s, "societyName", "name", "society", "id");
                    if (sName != null) names.add(sName);
                }
                actvSociety.setAdapter(new SocietyAdapter(this, names));
            }
        }
        if (level <= 3) {
            selectedBuilding = null;
            actvBuilding.setText("", false);
            tilBuilding.setVisibility(View.GONE);
            buildingList.clear();
        }
        if (level <= 4) {
            selectedFloor = null;
            actvFloor.setText("", false);
            layoutFloorFlat.setVisibility(View.GONE);
            tilFlat.setVisibility(View.GONE);
            etFlat.setText("");
            floorList.clear();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchCurrentLocation();
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchCurrentLocation() {
        progressDialog.setMessage("Fetching location...");
        progressDialog.show();
        try {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            if (locationManager != null) {
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

                if (isNetworkEnabled) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (location == null && isGPSEnabled) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }

            if (location != null) {
                Geocoder geocoder = new Geocoder(this, java.util.Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String subLocality = address.getSubLocality();
                    String locality = address.getLocality();
                    String pincode = address.getPostalCode();

                    // Auto-fill the main City and Area dropdowns
                    if (locality != null && !locality.isEmpty()) {
                        actvCity.setText(locality, false);
                        // Try to match against loaded franchise list
                        for (Map<String, Object> f : franchiseList) {
                            String fCity = getStringField(f, "address", "city", "name", "id");
                            if (locality.equalsIgnoreCase(fCity)) {
                                selectedFranchise = f;
                                viewModel.loadAreas((String) f.get("id"));
                                break;
                            }
                        }
                    }
                    if (subLocality != null && !subLocality.isEmpty()) {
                        actvArea.setText(subLocality, false);
                    }
                    Toast.makeText(this, "Location fetched successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Could not determine address", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Turn on GPS and try again", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();
        } finally {
            progressDialog.dismiss();
            progressDialog.setMessage("Creating account...");
        }
    }

    private void validateAndRegister() {
        if (selectedSociety != null && selectedBuilding != null && selectedFloor != null) {
            String flat = etFlat.getText().toString().trim();
            if (flat.isEmpty()) { etFlat.setError("Required"); return; }
            
            String city = selectedFranchise != null ? (String) selectedFranchise.get("address") : actvCity.getText().toString().trim();
            String areaId = selectedArea != null ? (String) selectedArea.get("id") : actvArea.getText().toString().trim();
            String franchiseId = selectedFranchise != null ? (String) selectedFranchise.get("id") : "";
            if (franchiseId.isEmpty() && selectedSociety != null && selectedSociety.get("franchiseId") != null) {
                franchiseId = (String) selectedSociety.get("franchiseId");
            }

            if (city.isEmpty()) { Toast.makeText(this, "Please select a City", Toast.LENGTH_SHORT).show(); return; }
            if (areaId.isEmpty()) { Toast.makeText(this, "Please select an Area", Toast.LENGTH_SHORT).show(); return; }

            viewModel.register(
                name, phone, email, password,
                city,
                areaId,
                (String) selectedSociety.get("id"),
                getStringField(selectedSociety, "societyName", "name", "society", "id"),
                (String) selectedBuilding.get("id"),
                getStringField(selectedBuilding, "buildingName", "name", "wing", "building", "id"),
                (String) selectedFloor.get("id"),
                getStringField(selectedFloor, "floorName", "name", "floor", "id"),
                flat,
                franchiseId
            );
        } else if (layoutManualAddress.getVisibility() == View.VISIBLE) {
            // Society name = whatever user typed in custom input
            String s = etManualSociety.getText().toString().trim();
            String mb = etManualBuilding.getText().toString().trim();
            String mf = etManualFloor.getText().toString().trim();
            String mFlat = etManualFlat.getText().toString().trim();
            // City and Area come from the main dropdowns
            String city = actvCity.getText().toString().trim();
            String a = actvArea.getText().toString().trim();
            if (s.isEmpty()) { etManualSociety.setError("Required"); return; }
            if (mb.isEmpty()) { etManualBuilding.setError("Required"); return; }
            if (mf.isEmpty()) { etManualFloor.setError("Required"); return; }
            if (mFlat.isEmpty()) { etManualFlat.setError("Required"); return; }
            if (city.isEmpty()) { Toast.makeText(this, "Please select a City first", Toast.LENGTH_SHORT).show(); return; }
            if (a.isEmpty()) { Toast.makeText(this, "Please select an Area first", Toast.LENGTH_SHORT).show(); return; }
            viewModel.registerManual(name, phone, email, password, s, mb, mf, mFlat, a, city, "");
        } else {
            Toast.makeText(this, "Please select all address fields completely", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View v = getCurrentFocus();
            if (v instanceof android.widget.EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * Helper to safely read a field from a Map trying multiple possible key names.
     * Returns the first non-null value found, or null if none found.
     */
    private String getStringField(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof String && !((String) val).isEmpty()) {
                return (String) val;
            }
        }
        return null;
    }

    private class SocietyAdapter extends ArrayAdapter<String> {
        private List<String> originalData;
        private List<String> filteredData;
        private boolean toastShownForNotFound = false;

        public SocietyAdapter(Context context, List<String> data) {
            super(context, android.R.layout.simple_dropdown_item_1line, data);
            this.originalData = new ArrayList<>(data);
            this.filteredData = new ArrayList<>(data);
        }

        @Override
        public int getCount() {
            return filteredData.size();
        }

        @Override
        public String getItem(int position) {
            return filteredData.get(position);
        }

        @Override
        public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<String> suggestions = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        suggestions.addAll(originalData);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        for (String item : originalData) {
                            if (item.toLowerCase().contains(filterPattern)) {
                                suggestions.add(item);
                            }
                        }
                    }

                    // Always show "Add Society" as the last option
                    suggestions.add("Add Society");

                    results.values = suggestions;
                    results.count = suggestions.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredData.clear();
                    if (results != null && results.count > 0) {
                        filteredData.addAll((List<String>) results.values);
                        
                        if (filteredData.size() == 1 && filteredData.get(0).equals("Add Society")) {
                            if (!toastShownForNotFound && constraint != null && constraint.length() > 0) {
                                Toast.makeText(AddressActivity.this, "Society not registered, add your society", Toast.LENGTH_SHORT).show();
                                toastShownForNotFound = true;
                            }
                        } else {
                            toastShownForNotFound = false;
                        }
                    } else {
                        toastShownForNotFound = false;
                    }
                    notifyDataSetChanged();
                }
            };
        }
    }
}
