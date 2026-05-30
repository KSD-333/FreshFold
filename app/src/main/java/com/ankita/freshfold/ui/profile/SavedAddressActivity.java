package com.ankita.freshfold.ui.profile;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.data.repository.SocietyRepository;
import com.ankita.freshfold.data.repository.UserRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SavedAddressActivity extends AppCompatActivity {

    private AutoCompleteTextView actvSociety, actvBuilding, actvFloor;
    private TextInputEditText etFlat, etManualSociety, etManualArea, etManualPincode;
    private LinearLayout layoutSocietyFields, layoutManualAddress;
    private TextView btnSaveAddress;
    private MaterialCardView cardCurrentAddress;
    private TextView tvCurrentAddress;
    private View layoutPreviousAddresses;
    private RecyclerView rvPreviousAddresses;
    private AddressAdapter addressAdapter;
    private final List<String> previousAddresses = new ArrayList<>();
    private ProgressDialog progressDialog;

    private final List<Map<String, Object>> societyList = new ArrayList<>();
    private Map<String, Object> selectedSociety = null;

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

        initViews();
        loadCurrentAddress();
        loadSocieties();
        setupDropdownLogic();

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
        btnSaveAddress.setOnClickListener(v -> validateAndSave());
    }

    private void initViews() {
        actvSociety         = findViewById(R.id.actvSociety);
        actvBuilding        = findViewById(R.id.actvBuilding);
        actvFloor           = findViewById(R.id.actvFloor);
        etFlat              = findViewById(R.id.etFlat);
        etManualSociety     = findViewById(R.id.etManualSociety);
        etManualArea        = findViewById(R.id.etManualArea);
        etManualPincode     = findViewById(R.id.etManualPincode);
        layoutSocietyFields = findViewById(R.id.layoutSocietyFields);
        layoutManualAddress = findViewById(R.id.layoutManualAddress);
        btnSaveAddress      = findViewById(R.id.btnSaveAddress);
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
                editPreviousAddress(address, position);
            }

            @Override
            public void onAddressMakeDefault(String address, int position) {
                SavedAddressActivity.this.onAddressClick(address);
            }
        });
        rvPreviousAddresses.setAdapter(addressAdapter);

        // Seed dropdown with "Other" until Firestore loads
        actvSociety.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new String[]{"Other"}));
        actvSociety.setThreshold(1);
    }

    /** Show the user's current saved address at the top */
    private void loadCurrentAddress() {
        // Show current address from cache instantly
        String cached = session.getUserAddress();
        if (cached != null && !cached.isEmpty()) {
            tvCurrentAddress.setText(cached);
            cardCurrentAddress.setVisibility(View.VISIBLE);
        }

        // Always refresh from Firestore — source of truth
        String phone = session.getUserPhone();
        if (phone == null || phone.isEmpty()) return;
        userRepository.getUser(phone).addOnSuccessListener((DocumentSnapshot doc) -> {
            if (doc.exists()) {
                String address = doc.getString("address");
                if (address != null && !address.isEmpty()) {
                    tvCurrentAddress.setText(address);
                    cardCurrentAddress.setVisibility(View.VISIBLE);
                    // Keep session cache in sync
                    session.saveUserProfile(
                            session.getUserName(),
                            session.getUserEmail(),
                            address);
                } else {
                    cardCurrentAddress.setVisibility(View.GONE);
                }

                // Load previous addresses list
                List<String> list = (List<String>) doc.get("address_list");
                previousAddresses.clear();
                if (list != null) {
                    previousAddresses.addAll(list);
                }
                if (!previousAddresses.isEmpty()) {
                    layoutPreviousAddresses.setVisibility(View.VISIBLE);
                } else {
                    layoutPreviousAddresses.setVisibility(View.GONE);
                }
                addressAdapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> {
            // Firestore failed — keep showing cached value if available
        });
    }

    private void onAddressClick(String address) {
        if (address == null || address.isEmpty()) return;
        String current = tvCurrentAddress.getText().toString().trim();
        if (address.equals(current)) return;

        // Tapped address becomes current, current moves to previous list
        saveToFirestore(address);
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

        Map<String, Object> listUpdate = new HashMap<>();
        listUpdate.put("address_list", previousAddresses);
        userRepository.updateUser(phone, listUpdate).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            progressDialog.setMessage("Saving address..."); // Reset for save operations
            if (task.isSuccessful()) {
                Toast.makeText(SavedAddressActivity.this, "Address deleted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SavedAddressActivity.this, "Failed to delete address", Toast.LENGTH_SHORT).show();
                // Rollback on failure
                previousAddresses.add(position, address);
                addressAdapter.notifyItemInserted(position);
                layoutPreviousAddresses.setVisibility(View.VISIBLE);
            }
        });
    }

    private void editPreviousAddress(String oldAddress, int position) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_address, null);

        AutoCompleteTextView dialogActvSociety = view.findViewById(R.id.dialogActvSociety);
        AutoCompleteTextView dialogActvBuilding = view.findViewById(R.id.dialogActvBuilding);
        AutoCompleteTextView dialogActvFloor = view.findViewById(R.id.dialogActvFloor);
        TextInputEditText dialogEtFlat = view.findViewById(R.id.dialogEtFlat);
        
        LinearLayout dialogLayoutSocietyFields = view.findViewById(R.id.dialogLayoutSocietyFields);
        LinearLayout dialogLayoutManualAddress = view.findViewById(R.id.dialogLayoutManualAddress);
        
        TextInputEditText dialogEtManualSociety = view.findViewById(R.id.dialogEtManualSociety);
        TextInputEditText dialogEtManualArea = view.findViewById(R.id.dialogEtManualArea);
        TextInputEditText dialogEtManualPincode = view.findViewById(R.id.dialogEtManualPincode);
        
        TextView dialogBtnCancel = view.findViewById(R.id.dialogBtnCancel);
        TextView dialogBtnSave = view.findViewById(R.id.dialogBtnSave);

        // Setup dropdown data
        List<String> names = new ArrayList<>();
        for (Map<String, Object> s : societyList) {
            names.add((String) s.get("name"));
        }
        names.add("Other");
        dialogActvSociety.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names));
        dialogActvSociety.setThreshold(1);

        // To hold selected society in dialog context
        final Map<String, Object>[] dialogSelectedSociety = new Map[]{null};

        // Populate society details helper inside dialog
        Runnable populateSocietyDetailsDialog = new Runnable() {
            @Override
            public void run() {
                if (dialogSelectedSociety[0] != null) {
                    @SuppressWarnings("unchecked")
                    List<String> buildings = (List<String>) dialogSelectedSociety[0].get("buildings");
                    if (buildings != null) {
                        dialogActvBuilding.setAdapter(new ArrayAdapter<>(SavedAddressActivity.this,
                                android.R.layout.simple_dropdown_item_1line, buildings));
                    }
                    Long maxFloors = (Long) dialogSelectedSociety[0].get("maxFloors");
                    if (maxFloors != null) {
                        List<String> floors = new ArrayList<>();
                        for (int i = 0; i <= maxFloors; i++) {
                            floors.add(String.valueOf(i));
                        }
                        dialogActvFloor.setAdapter(new ArrayAdapter<>(SavedAddressActivity.this,
                                android.R.layout.simple_dropdown_item_1line, floors));
                    }
                }
            }
        };

        // Setup dropdown logic for dialog
        dialogActvSociety.setOnClickListener(v -> dialogActvSociety.showDropDown());
        dialogActvSociety.setOnFocusChangeListener((v, focus) -> { if (focus) dialogActvSociety.showDropDown(); });
        
        dialogActvBuilding.setOnClickListener(v -> dialogActvBuilding.showDropDown());
        dialogActvBuilding.setOnFocusChangeListener((v, focus) -> { if (focus) dialogActvBuilding.showDropDown(); });
        
        dialogActvFloor.setOnClickListener(v -> dialogActvFloor.showDropDown());
        dialogActvFloor.setOnFocusChangeListener((v, focus) -> { if (focus) dialogActvFloor.showDropDown(); });

        dialogActvSociety.setOnItemClickListener((parent, view1, pos, id) -> {
            String selected = (String) parent.getItemAtPosition(pos);
            if ("Other".equals(selected)) {
                dialogSelectedSociety[0] = null;
                dialogLayoutSocietyFields.setVisibility(View.GONE);
                dialogLayoutManualAddress.setVisibility(View.VISIBLE);
            } else {
                dialogLayoutManualAddress.setVisibility(View.GONE);
                dialogLayoutSocietyFields.setVisibility(View.VISIBLE);
                for (Map<String, Object> s : societyList) {
                    if (selected.equals(s.get("name"))) {
                        dialogSelectedSociety[0] = s;
                        populateSocietyDetailsDialog.run();
                        dialogActvBuilding.setText("", false);
                        dialogActvFloor.setText("", false);
                        break;
                    }
                }
            }
        });

        // Let's parse oldAddress to prefill the dialog fields
        boolean parsed = false;
        if (oldAddress != null) {
            String addressTrim = oldAddress.trim();
            // Let's try parsing society address first
            // Format: flat + ", " + f + "th Floor, " + b + ", " + societyName + ", Pune"
            // For example: "101, 5th Floor, Tower 1, Blue Ridge, Pune"
            if (addressTrim.contains("Floor") && addressTrim.endsWith(", Pune")) {
                String[] parts = addressTrim.split(", ");
                if (parts.length >= 4) {
                    String parsedFlat = parts[0].trim();
                    String parsedFloor = parts[1].replaceAll("(?i)(st|nd|rd|th)?\\s+Floor", "").trim();
                    String parsedBuilding = parts[2].trim();
                    String parsedSocietyName = parts[3].trim();

                    // Look if parsedSocietyName matches any known society
                    Map<String, Object> matchedSociety = null;
                    for (Map<String, Object> s : societyList) {
                        if (parsedSocietyName.equalsIgnoreCase((String) s.get("name"))) {
                            matchedSociety = s;
                            break;
                        }
                    }

                    if (matchedSociety != null) {
                        dialogSelectedSociety[0] = matchedSociety;
                        dialogActvSociety.setText(parsedSocietyName, false);
                        dialogLayoutManualAddress.setVisibility(View.GONE);
                        dialogLayoutSocietyFields.setVisibility(View.VISIBLE);

                        // Populate building and floor adapters
                        @SuppressWarnings("unchecked")
                        List<String> buildings = (List<String>) matchedSociety.get("buildings");
                        if (buildings != null) {
                            dialogActvBuilding.setAdapter(new ArrayAdapter<>(this,
                                    android.R.layout.simple_dropdown_item_1line, buildings));
                        }
                        Long maxFloors = (Long) matchedSociety.get("maxFloors");
                        if (maxFloors != null) {
                            List<String> floors = new ArrayList<>();
                            for (int i = 0; i <= maxFloors; i++) {
                                floors.add(String.valueOf(i));
                            }
                            dialogActvFloor.setAdapter(new ArrayAdapter<>(this,
                                    android.R.layout.simple_dropdown_item_1line, floors));
                        }

                        dialogActvBuilding.setText(parsedBuilding, false);
                        dialogActvFloor.setText(parsedFloor, false);
                        dialogEtFlat.setText(parsedFlat);
                        parsed = true;
                    }
                }
            }

            // If not parsed, let's treat it as a manual address
            // Format: s + ", " + a + ", Pune - " + p
            // For example: "MySociety, Hinjewadi, Pune - 411057"
            if (!parsed) {
                String[] parts = addressTrim.split(", ");
                if (parts.length >= 2) {
                    String parsedSoc = parts[0].trim();
                    String parsedArea = "";
                    String parsedPincode = "";

                    if (parts.length >= 3) {
                        parsedArea = parts[1].trim();
                        String lastPart = parts[2].trim(); // "Pune - 411057" or similar
                        if (lastPart.startsWith("Pune - ")) {
                            parsedPincode = lastPart.substring("Pune - ".length()).trim();
                        } else {
                            parsedPincode = lastPart;
                        }
                    } else {
                        parsedArea = parts[1].trim();
                    }

                    dialogActvSociety.setText("Other", false);
                    dialogLayoutSocietyFields.setVisibility(View.GONE);
                    dialogLayoutManualAddress.setVisibility(View.VISIBLE);
                    dialogEtManualSociety.setText(parsedSoc);
                    dialogEtManualArea.setText(parsedArea);
                    dialogEtManualPincode.setText(parsedPincode);
                    parsed = true;
                }
            }

            // If still not parsed or failed, just fill manual society with the whole old address
            if (!parsed) {
                dialogActvSociety.setText("Other", false);
                dialogLayoutSocietyFields.setVisibility(View.GONE);
                dialogLayoutManualAddress.setVisibility(View.VISIBLE);
                dialogEtManualSociety.setText(addressTrim);
            }
        }

        dialogBtnCancel.setOnClickListener(v -> dialog.dismiss());

        dialogBtnSave.setOnClickListener(v -> {
            String newAddress;
            if (dialogSelectedSociety[0] != null) {
                String b = dialogActvBuilding.getText().toString().trim();
                String f = dialogActvFloor.getText().toString().trim();
                String flat = dialogEtFlat.getText().toString().trim();
                if (b.isEmpty()) { dialogActvBuilding.setError("Required"); return; }
                if (f.isEmpty()) { dialogActvFloor.setError("Required"); return; }
                if (flat.isEmpty()) { dialogEtFlat.setError("Required"); return; }
                newAddress = flat + ", " + f + "th Floor, " + b + ", "
                        + dialogSelectedSociety[0].get("name") + ", Pune";
            } else if (dialogLayoutManualAddress.getVisibility() == View.VISIBLE) {
                String s = dialogEtManualSociety.getText().toString().trim();
                String a = dialogEtManualArea.getText().toString().trim();
                String p = dialogEtManualPincode.getText().toString().trim();
                if (s.isEmpty()) { dialogEtManualSociety.setError("Required"); return; }
                if (a.isEmpty()) { dialogEtManualArea.setError("Required"); return; }
                if (p.length() != 6) { dialogEtManualPincode.setError("6 digits required"); return; }
                newAddress = s + ", " + a + ", Pune - " + p;
            } else {
                Toast.makeText(this, "Please select an address type", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newAddress.equals(oldAddress)) {
                dialog.dismiss();
                return;
            }
            String current = tvCurrentAddress.getText().toString().trim();
            if (newAddress.equals(current)) {
                Toast.makeText(this, "This is already your current address", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            updatePreviousAddressInFirestore(oldAddress, newAddress, position);
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void updatePreviousAddressInFirestore(String oldAddress, String newAddress, int position) {
        String phone = session.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        progressDialog.setMessage("Updating address...");
        progressDialog.show();

        previousAddresses.set(position, newAddress);
        addressAdapter.notifyItemChanged(position);

        Map<String, Object> listUpdate = new HashMap<>();
        listUpdate.put("address_list", previousAddresses);
        userRepository.updateUser(phone, listUpdate).addOnCompleteListener(task -> {
            progressDialog.dismiss();
            progressDialog.setMessage("Saving address..."); // Reset
            if (task.isSuccessful()) {
                Toast.makeText(SavedAddressActivity.this, "Address updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SavedAddressActivity.this, "Failed to update address", Toast.LENGTH_SHORT).show();
                // Rollback on failure
                previousAddresses.set(position, oldAddress);
                addressAdapter.notifyItemChanged(position);
            }
        });
    }

    /** Load hardcoded + Firestore societies into dropdown */
    private void loadSocieties() {
        addHardcodedSocieties();
        new SocietyRepository().getServiceableSocieties()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            societyList.add(data);
                        }
                    }
                    refreshSocietyAdapter();
                })
                .addOnFailureListener(e -> refreshSocietyAdapter());
    }

    private void refreshSocietyAdapter() {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> s : societyList) names.add((String) s.get("name"));
        names.add("Other");
        actvSociety.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names));
    }

    private void setupDropdownLogic() {
        actvSociety.setOnClickListener(v -> actvSociety.showDropDown());
        actvSociety.setOnFocusChangeListener((v, focus) -> { if (focus) actvSociety.showDropDown(); });

        actvSociety.setOnItemClickListener((parent, view, pos, id) -> {
            String selected = (String) parent.getItemAtPosition(pos);
            if ("Other".equals(selected)) {
                selectedSociety = null;
                layoutSocietyFields.setVisibility(View.GONE);
                layoutManualAddress.setVisibility(View.VISIBLE);
            } else {
                layoutManualAddress.setVisibility(View.GONE);
                layoutSocietyFields.setVisibility(View.VISIBLE);
                for (Map<String, Object> s : societyList) {
                    if (selected.equals(s.get("name"))) {
                        selectedSociety = s;
                        populateSocietyDetails(s);
                        break;
                    }
                }
            }
        });
    }

    private void populateSocietyDetails(Map<String, Object> society) {
        @SuppressWarnings("unchecked")
        List<String> buildings = (List<String>) society.get("buildings");
        if (buildings != null) {
            actvBuilding.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, buildings));
            actvBuilding.setText("", false);
        }
        Long maxFloors = (Long) society.get("maxFloors");
        if (maxFloors != null) {
            List<String> floors = new ArrayList<>();
            for (int i = 0; i <= maxFloors; i++) floors.add(String.valueOf(i));
            actvFloor.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, floors));
            actvFloor.setText("", false);
        }
    }

    private void addHardcodedSocieties() {
        societyList.add(makeSociety("Blue Ridge",
                new String[]{"Tower 1","Tower 2","Tower 3","Tower 4","Tower 5"}, 25L));
        societyList.add(makeSociety("Megapolis Sunway",
                new String[]{"A1","A2","B1","B2","C1","C2"}, 20L));
        societyList.add(makeSociety("Amanora Park Town",
                new String[]{"Adreno Towers","Aspire Towers","Future Towers"}, 30L));
        societyList.add(makeSociety("Life Republic",
                new String[]{"Sector R1","Sector R2","Sector R3"}, 22L));
    }

    private HashMap<String, Object> makeSociety(String name, String[] buildings, Long maxFloors) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", name);
        List<String> bList = new ArrayList<>();
        for (String b : buildings) bList.add(b);
        map.put("buildings", bList);
        map.put("maxFloors", maxFloors);
        return map;
    }

    private void validateAndSave() {
        String fullAddress;

        if (selectedSociety != null) {
            String b    = actvBuilding.getText().toString().trim();
            String f    = actvFloor.getText().toString().trim();
            String flat = etFlat.getText().toString().trim();
            if (b.isEmpty())    { actvBuilding.setError("Required"); return; }
            if (f.isEmpty())    { actvFloor.setError("Required");    return; }
            if (flat.isEmpty()) { etFlat.setError("Required");       return; }
            fullAddress = flat + ", " + f + "th Floor, " + b + ", "
                    + selectedSociety.get("name") + ", Pune";

        } else if (layoutManualAddress.getVisibility() == View.VISIBLE) {
            String s = etManualSociety.getText().toString().trim();
            String a = etManualArea.getText().toString().trim();
            String p = etManualPincode.getText().toString().trim();
            if (s.isEmpty())     { etManualSociety.setError("Required");         return; }
            if (a.isEmpty())     { etManualArea.setError("Required");            return; }
            if (p.length() != 6) { etManualPincode.setError("6 digits required"); return; }
            fullAddress = s + ", " + a + ", Pune - " + p;

        } else {
            Toast.makeText(this, "Please select a society or choose Other",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        saveToFirestore(fullAddress);
    }

    private void saveToFirestore(String fullAddress) {
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
                    // Also save to address sub-collection with type "saved"
                    userRepository.saveAddress(phone, fullAddress, "saved");

                    progressDialog.dismiss();

                    // Manage previous address list
                    String oldAddr = tvCurrentAddress.getText().toString().trim();
                    if (!oldAddr.isEmpty() && !oldAddr.equals(fullAddress)) {
                        // 1. Add to local list if not present
                        if (!previousAddresses.contains(oldAddr)) {
                            previousAddresses.add(0, oldAddr);
                        }
                        // 2. Remove ALL occurrences of the new current address from previous list
                        while (previousAddresses.contains(fullAddress)) {
                            previousAddresses.remove(fullAddress);
                        }
                        
                        // 2.5 Clean up any existing duplicates in the previous list
                        java.util.List<String> distinct = new java.util.ArrayList<>();
                        for (String s : previousAddresses) {
                            if (!distinct.contains(s)) distinct.add(s);
                        }
                        previousAddresses.clear();
                        previousAddresses.addAll(distinct);

                        // 3. Update Firestore with new list
                        Map<String, Object> listUpdate = new HashMap<>();
                        listUpdate.put("address_list", previousAddresses);
                        userRepository.updateUser(phone, listUpdate);

                        // 4. Update UI
                        layoutPreviousAddresses.setVisibility(View.VISIBLE);
                        addressAdapter.notifyDataSetChanged();
                    }

                    // Update local session cache with new address
                    session.saveUserProfile(session.getUserName(), session.getUserEmail(), fullAddress);

                    // Update current address card
                    tvCurrentAddress.setText(fullAddress);
                    cardCurrentAddress.setVisibility(View.VISIBLE);

                    // Reset form
                    actvSociety.setText("", false);
                    layoutSocietyFields.setVisibility(View.GONE);
                    layoutManualAddress.setVisibility(View.GONE);
                    selectedSociety = null;

                    Toast.makeText(this, "Address saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
