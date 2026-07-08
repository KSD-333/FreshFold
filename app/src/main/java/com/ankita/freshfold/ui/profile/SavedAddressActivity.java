package com.ankita.freshfold.ui.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.data.repository.UserRepository;
import com.ankita.freshfold.viewmodel.RegistrationViewModel;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedAddressActivity extends AppCompatActivity {

    private AutoCompleteTextView actvCity, actvArea, actvSociety, actvBuilding, actvFloor;
    private TextInputLayout tilCity, tilArea, tilSociety, tilBuilding, tilFloor, tilFlat;
    private LinearLayout layoutFloorFlat;
    
    private TextInputEditText etFlat, etManualSociety, etManualBuilding, etManualFloor, etManualFlat, etManualCity;
    private LinearLayout layoutManualAddress;
    private TextView btnSaveAddress;
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
    
    private MaterialCardView cardCurrentAddress;
    private TextView tvCurrentAddress;
    private View layoutPreviousAddresses;
    private RecyclerView rvPreviousAddresses;
    private AddressAdapter addressAdapter;
    private final List<String> previousAddresses = new ArrayList<>();

    private SessionManager session;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_address);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        session = new SessionManager(this);
        userRepository = new UserRepository();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving address...");
        progressDialog.setCancelable(false);

        viewModel = new ViewModelProvider(this).get(RegistrationViewModel.class);

        initViews();
        loadCurrentAddress();
        setupObservers();
        setupAddressLogic();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        btnSaveAddress.setOnClickListener(v -> validateAndSave());

        viewModel.loadFranchises();
        viewModel.loadGlobalSocieties();
    }

    private void initViews() {
        actvCity = findViewById(R.id.actvCity);
        actvArea = findViewById(R.id.actvArea);
        actvSociety = findViewById(R.id.actvSociety);
        actvBuilding = findViewById(R.id.actvBuilding);
        actvFloor = findViewById(R.id.actvFloor);
        
        tilCity = findViewById(R.id.tilCity);
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
        etManualCity = findViewById(R.id.etManualCity);
        layoutManualAddress = findViewById(R.id.layoutManualAddress);
        btnSaveAddress = findViewById(R.id.btnSaveAddress);
        btnGetCurrentLocation = findViewById(R.id.btnGetCurrentLocation);
        
        cardCurrentAddress  = findViewById(R.id.cardCurrentAddress);
        tvCurrentAddress    = findViewById(R.id.tvCurrentAddress);
        layoutPreviousAddresses = findViewById(R.id.layoutPreviousAddresses);
        rvPreviousAddresses     = findViewById(R.id.rvPreviousAddresses);

        rvPreviousAddresses.setLayoutManager(new LinearLayoutManager(this));
        addressAdapter = new AddressAdapter(previousAddresses, new AddressAdapter.OnAddressClickListener() {
            @Override
            public void onAddressClick(String address) {
                SavedAddressActivity.this.onAddressClick(address);
            }

            @Override
            public void onAddressDelete(String address, int position) {
                deletePreviousAddress(address, position);
            }

            @Override
            public void onAddressEdit(String address, int position) {
                // Editing old addresses disabled with new format to prevent complexity.
                // Just use the main form to enter new one.
                Toast.makeText(SavedAddressActivity.this, "Editing is disabled, please add it as a new address", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAddressMakeDefault(String address, int position) {
                SavedAddressActivity.this.onAddressClick(address);
            }
        });
        rvPreviousAddresses.setAdapter(addressAdapter);
    }
    
    private void loadCurrentAddress() {
        String cached = session.getUserAddress();
        if (cached != null && !cached.isEmpty()) {
            tvCurrentAddress.setText(cached);
            cardCurrentAddress.setVisibility(View.VISIBLE);
        }

        String phone = session.getUserPhone();
        if (phone == null || phone.isEmpty()) return;
        userRepository.getUser(phone).addOnSuccessListener((DocumentSnapshot doc) -> {
            if (doc.exists()) {
                String address = doc.getString("address");
                if (address != null && !address.isEmpty()) {
                    tvCurrentAddress.setText(address);
                    cardCurrentAddress.setVisibility(View.VISIBLE);
                    session.saveUserProfile(session.getUserName(), session.getUserEmail(), address);
                } else {
                    cardCurrentAddress.setVisibility(View.GONE);
                }

                userRepository.getAddresses(phone).addOnSuccessListener(querySnap -> {
                    previousAddresses.clear();
                    for (DocumentSnapshot addressDoc : querySnap.getDocuments()) {
                        String addrStr = addressDoc.getString("address");
                        if (addrStr != null && !addrStr.isEmpty()) {
                            if (address != null && address.equals(addrStr)) {
                                continue;
                            }
                            if (!previousAddresses.contains(addrStr)) {
                                previousAddresses.add(addrStr);
                            }
                        }
                    }
                    // Hidden for now
                    layoutPreviousAddresses.setVisibility(View.GONE);
                    addressAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void onAddressClick(String address) {
        if (address == null || address.isEmpty()) return;
        String current = tvCurrentAddress.getText().toString().trim();
        if (address.equals(current)) return;
        saveToFirestore(address, null);
    }
    
    private void deletePreviousAddress(String address, int position) {
        String phone = session.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        progressDialog.setMessage("Deleting address...");
        progressDialog.show();

        previousAddresses.remove(position);
        addressAdapter.notifyItemRemoved(position);
        if (previousAddresses.isEmpty()) {
            layoutPreviousAddresses.setVisibility(View.GONE);
        }

        userRepository.deleteAddress(phone, address).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            progressDialog.setMessage("Saving address..."); 
            if (task.isSuccessful()) {
                Toast.makeText(SavedAddressActivity.this, "Address deleted", Toast.LENGTH_SHORT).show();
                // Optionally update address_list for backward compatibility
                Map<String, Object> listUpdate = new HashMap<>();
                listUpdate.put("address_list", previousAddresses);
                userRepository.updateUser(phone, listUpdate);
            } else {
                Toast.makeText(SavedAddressActivity.this, "Failed to delete address", Toast.LENGTH_SHORT).show();
                previousAddresses.add(position, address);
                addressAdapter.notifyItemInserted(position);
                // Hidden for now
                layoutPreviousAddresses.setVisibility(View.GONE);
            }
        });
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
                if (a.get("id") != null) names.add((String) a.get("id"));
            }
            actvArea.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
            tilArea.setVisibility(View.VISIBLE);
        });

        viewModel.societies.observe(this, list -> {
            societyList.clear();
            societyList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> s : societyList) {
                String sName = getStringField(s, "societyName", "name", "society", "id");
                if (sName != null) names.add(sName);
            }
            actvSociety.setAdapter(new SocietyAdapter(this, names));
            tilSociety.setVisibility(View.VISIBLE);
        });

        viewModel.buildings.observe(this, list -> {
            buildingList.clear();
            buildingList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> b : buildingList) {
                String bName = getStringField(b, "buildingName", "name", "wing", "building", "id");
                if (bName != null) names.add(bName);
            }
            actvBuilding.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
            tilBuilding.setVisibility(View.VISIBLE);
        });

        viewModel.floors.observe(this, list -> {
            floorList.clear();
            floorList.addAll(list);
            List<String> names = new ArrayList<>();
            for (Map<String, Object> f : floorList) {
                String fName = getStringField(f, "floorName", "name", "floor", "id");
                if (fName != null) names.add(fName);
            }
            actvFloor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
            layoutFloorFlat.setVisibility(View.VISIBLE);
        });
    }

    private void setupAddressLogic() {
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
                actvSociety.setAdapter(new SocietyAdapter(SavedAddressActivity.this, names));
                tilSociety.setVisibility(View.VISIBLE);
            }
        });

        actvSociety.setOnClickListener(v -> actvSociety.post(() -> actvSociety.showDropDown()));
        actvSociety.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) actvSociety.post(() -> actvSociety.showDropDown()); });
        actvSociety.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            if (selected.equals("Add Society")) {
                selectedSociety = null;
                resetCascadingFrom(3);
                
                tilSociety.setVisibility(View.GONE);
                layoutManualAddress.setVisibility(View.VISIBLE);
                btnGetCurrentLocation.setVisibility(View.VISIBLE);
                etManualSociety.setText("");
            } else {
                layoutManualAddress.setVisibility(View.GONE);
                btnGetCurrentLocation.setVisibility(View.GONE);
                selectedSociety = null;
                
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
                    actvBuilding.setAdapter(new ArrayAdapter<>(SavedAddressActivity.this, android.R.layout.simple_dropdown_item_1line, names));
                }
            }
        });

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
                
                actvFloor.setAdapter(new ArrayAdapter<>(SavedAddressActivity.this, android.R.layout.simple_dropdown_item_1line, names));
            }
        });

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
            areaList.clear();
            actvArea.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, new ArrayList<>()));
        }
        if (level <= 2) {
            selectedSociety = null;
            actvSociety.setText("", false);
            tilSociety.setVisibility(View.VISIBLE); 
            layoutManualAddress.setVisibility(View.GONE);
            btnGetCurrentLocation.setVisibility(View.GONE);
            societyList.clear();
            
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

                    if (locality != null && !locality.isEmpty()) {
                        actvCity.setText(locality, false);
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
            progressDialog.setMessage("Saving address...");
        }
    }

    private void validateAndSave() {
        String fullAddress;
        String franchiseIdToSave = "";

        if (selectedSociety != null && selectedBuilding != null && selectedFloor != null) {
            String flat = etFlat.getText().toString().trim();
            if (flat.isEmpty()) { etFlat.setError("Required"); return; }
            
            String city = selectedFranchise != null ? (String) selectedFranchise.get("address") : actvCity.getText().toString().trim();
            String area = selectedArea != null ? (String) selectedArea.get("id") : actvArea.getText().toString().trim();
            
            if (city.isEmpty()) { Toast.makeText(this, "Please select a City", Toast.LENGTH_SHORT).show(); return; }
            if (area.isEmpty()) { Toast.makeText(this, "Please select an Area", Toast.LENGTH_SHORT).show(); return; }
            
            String fName = getStringField(selectedFloor, "floorName", "name", "floor", "id");
            String bName = getStringField(selectedBuilding, "buildingName", "name", "wing", "building", "id");
            String sName = getStringField(selectedSociety, "societyName", "name", "society", "id");
            
            fullAddress = buildAddress(flat, fName, bName, sName, area, city, "");
            franchiseIdToSave = selectedFranchise != null ? (String) selectedFranchise.get("id") : "";
            if (franchiseIdToSave.isEmpty() && selectedSociety != null && selectedSociety.get("franchiseId") != null) {
                franchiseIdToSave = (String) selectedSociety.get("franchiseId");
            }
        } else if (layoutManualAddress.getVisibility() == View.VISIBLE) {
            String s = etManualSociety.getText().toString().trim();
            String mb = etManualBuilding.getText().toString().trim();
            String mf = etManualFloor.getText().toString().trim();
            String mFlat = etManualFlat.getText().toString().trim();
            String city = actvCity.getText().toString().trim();
            String a = actvArea.getText().toString().trim();
            String p = "";
            
            if (s.isEmpty()) { etManualSociety.setError("Required"); return; }
            if (mb.isEmpty()) { etManualBuilding.setError("Required"); return; }
            if (mf.isEmpty()) { etManualFloor.setError("Required"); return; }
            if (mFlat.isEmpty()) { etManualFlat.setError("Required"); return; }
            if (city.isEmpty()) { Toast.makeText(this, "Please select a City first", Toast.LENGTH_SHORT).show(); return; }
            if (a.isEmpty()) { Toast.makeText(this, "Please select an Area first", Toast.LENGTH_SHORT).show(); return; }
            
            fullAddress = buildAddress(mFlat, mf, mb, s, a, city, p);
            franchiseIdToSave = selectedFranchise != null ? (String) selectedFranchise.get("id") : "";
        } else {
            Toast.makeText(this, "Please select all address fields completely", Toast.LENGTH_SHORT).show();
            return;
        }

        saveToFirestore(fullAddress, franchiseIdToSave);
    }
    
    private String buildAddress(String flat, String floorName, String buildingName, String societyName, String area, String city, String pincode) {
        List<String> parts = new ArrayList<>();
        if (flat != null && !flat.trim().isEmpty())         parts.add(flat.trim());
        if (floorName != null && !floorName.trim().isEmpty()) {
            if (!floorName.toLowerCase().contains("floor")) {
                parts.add(floorName.trim() + "th Floor");
            } else {
                parts.add(floorName.trim());
            }
        }
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

    private void saveToFirestore(String fullAddress, String franchiseId) {
        String phone = session.getUserPhone();
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("address", fullAddress);

        userRepository.updateUser(phone, updates)
                .addOnSuccessListener(aVoid -> {
                    userRepository.saveAddress(phone, fullAddress, "saved", franchiseId).addOnSuccessListener(unused -> {
                        loadCurrentAddress();
                    });
                    progressDialog.dismiss();

                    session.saveUserProfile(session.getUserName(), session.getUserEmail(), fullAddress);
                    tvCurrentAddress.setText(fullAddress);
                    cardCurrentAddress.setVisibility(View.VISIBLE);

                    actvCity.setText("", false);
                    actvArea.setText("", false);
                    actvSociety.setText("", false);
                    actvBuilding.setText("", false);
                    actvFloor.setText("", false);
                    etFlat.setText("");
                    layoutManualAddress.setVisibility(View.GONE);
                    tilBuilding.setVisibility(View.GONE);
                    layoutFloorFlat.setVisibility(View.GONE);
                    tilFlat.setVisibility(View.GONE);
                    btnGetCurrentLocation.setVisibility(View.GONE);
                    
                    selectedFranchise = null;
                    selectedArea = null;
                    selectedSociety = null;
                    selectedBuilding = null;
                    selectedFloor = null;

                    Toast.makeText(this, "Address saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getStringField(Map<String, Object> map, String... keys) {
        if (map == null) return null;
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
                                Toast.makeText(SavedAddressActivity.this, "Society not registered, add your society", Toast.LENGTH_SHORT).show();
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
}
