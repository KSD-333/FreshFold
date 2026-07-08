package com.ankita.freshfold.ui.address;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ankita.freshfold.CartItem;
import com.ankita.freshfold.CartManager;
import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PickupSlotActivity extends AppCompatActivity {

    private MaterialCardView cardTomorrow, cardDayAfter, cardAddress;
    private ImageView ivCheckTomorrow, ivCheckDayAfter;
    private TextView tvDateTomorrow, tvDateDayAfter, tvDayTomorrow, tvDayDayAfter;
    private TextInputEditText etPickupInstructions;
    private MaterialCardView btnSaveInstruction;
    private boolean isTomorrowSelected = true;

    private SessionManager sessionManager;
    private FirebaseFirestore db;
    private boolean isOrderProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(0);

        setContentView(R.layout.activity_pickup_slot);

        sessionManager = new SessionManager(this);
        db = FirebaseFirestore.getInstance();

        cardTomorrow    = findViewById(R.id.cardTomorrow);
        cardDayAfter    = findViewById(R.id.cardDayAfter);
        cardAddress     = findViewById(R.id.cardAddress);
        cardAddress.setOnClickListener(v -> showAddressSelectionDialog());
        ivCheckTomorrow = findViewById(R.id.ivCheckTomorrow);
        ivCheckDayAfter = findViewById(R.id.ivCheckDayAfter);
        tvDateTomorrow  = findViewById(R.id.tvDateTomorrow);
        tvDateDayAfter  = findViewById(R.id.tvDateDayAfter);
        tvDayTomorrow   = findViewById(R.id.tvDayTomorrow);
        tvDayDayAfter   = findViewById(R.id.tvDayDayAfter);
        etPickupInstructions = findViewById(R.id.etPickupInstructions);
        btnSaveInstruction   = findViewById(R.id.btnSaveInstruction);

        TextView tvDisclaimer = findViewById(R.id.tvDisclaimer);
        tvDisclaimer.setOnClickListener(v -> showDisclaimerDialog());

        // Display current address from session
        displayCurrentAddress();

        setupDates();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            rollbackUnconfirmedOrder();
            finish();
        });

        // Date card clicks
        cardTomorrow.setOnClickListener(v -> selectDate(true));
        cardDayAfter.setOnClickListener(v -> selectDate(false));

        // ── Auto-fetch saved instruction ──
        loadSavedInstruction();

        // ── Save Instruction button ──
        btnSaveInstruction.setOnClickListener(v -> saveInstruction());

        // Confirm Order
        MaterialCardView btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setOnClickListener(v -> {
            // Prevent any duplicate processing
            if (isOrderProcessing) return;
            isOrderProcessing = true;

            // Disable immediately to prevent double click
            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.6f);
            String date = isTomorrowSelected
                    ? tvDateTomorrow.getText().toString()
                    : tvDateDayAfter.getText().toString();

            String orderIdToClear = getIntent().getStringExtra("order_id_to_clear");
            
            boolean hasIroning = false;
            int totalItems = 0;
            int totalPrice = 0;
            List<CartItem> selectedItems = new java.util.ArrayList<>();
            for (com.ankita.freshfold.Order o : CartManager.getInstance().getOrders()) {
                if (o.getId().equals(orderIdToClear)) {
                    selectedItems = o.getItems();
                    break;
                }
            }
            final List<CartItem> items = selectedItems;

            for (CartItem item : items) {
                totalItems += item.getQuantity();
                totalPrice += item.getTotalPrice();
                if ("Ironing".equalsIgnoreCase(item.getName())) {
                    hasIroning = true;
                }
            }

            // Safety check — if items not found, don't proceed
            if (items.isEmpty() || totalPrice <= 0) {
                isOrderProcessing = false;
                btnContinue.setEnabled(true);
                btnContinue.setAlpha(1.0f);
                return;
            }
            String phone = sessionManager.getUserPhone();
            
            if (phone == null || phone.isEmpty()) {
                android.widget.Toast.makeText(this, "User not logged in.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            final boolean finalHasIroning = hasIroning;
            final int finalTotalItems = totalItems;
            final int finalTotalPrice = totalPrice;

            String currentAddress = sessionManager.getUserAddress();

            // First look up or create address document
            db.collection("freshfold").document("app_data")
              .collection("users").document(phone)
              .collection("address")
              .whereEqualTo("address", currentAddress != null ? currentAddress : "")
              .limit(1)
              .get()
              .addOnSuccessListener(querySnap -> {
                  final String addressDocId;
                  final String[] addressFranchiseId = {""};
                  if (!querySnap.isEmpty()) {
                      addressDocId = querySnap.getDocuments().get(0).getId();
                      addressFranchiseId[0] = querySnap.getDocuments().get(0).getString("franchiseId");
                  } else {
                      // Fallback create if not found
                      com.google.firebase.firestore.DocumentReference newRef = db.collection("freshfold").document("app_data")
                                       .collection("users").document(phone)
                                       .collection("address").document();
                      Map<String,Object> map = new HashMap<>();
                      map.put("address", currentAddress);
                      map.put("type", "saved");
                      map.put("timestamp", com.google.firebase.Timestamp.now());
                      newRef.set(map);
                      addressDocId = newRef.getId();
                  }

                  // Check Wallet Balance before placing order
                  db.collection("freshfold").document("app_data")
                      .collection("users").document(phone)
                      .get().addOnSuccessListener(doc -> {
                    double actual = 0.0;
                    double reserved = 0.0;
                    String rootFranchiseId = "";
                    if (doc.exists()) {
                        actual = doc.contains("walletBalance") && doc.getDouble("walletBalance") != null ? doc.getDouble("walletBalance") : 0.0;
                        reserved = doc.contains("reservedBalance") && doc.getDouble("reservedBalance") != null ? doc.getDouble("reservedBalance") : 0.0;
                        if (doc.getString("franchiseId") != null) rootFranchiseId = doc.getString("franchiseId");
                    }
                    final String finalFranchiseId = (addressFranchiseId[0] != null && !addressFranchiseId[0].isEmpty()) ? addressFranchiseId[0] : rootFranchiseId;
                    
                    double available = actual - reserved;
                    
                    if (available >= finalTotalPrice) {
                        // Hold balance
                        java.util.Map<String, Object> update = new HashMap<>();
                        update.put("reservedBalance", reserved + finalTotalPrice);
                        
                        db.collection("freshfold").document("app_data")
                            .collection("users").document(phone)
                            .set(update, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                
                                // Save debit transaction immediately since the UI shows balance - reserved
                                Map<String, Object> txn = new java.util.HashMap<>();
                                txn.put("title", "Order Payment");
                                txn.put("amount", String.valueOf(finalTotalPrice));
                                txn.put("emoji", "🛒");
                                txn.put("isCredit", false);
                                txn.put("timestamp", System.currentTimeMillis());
                                txn.put("userPhone", phone);
                                txn.put("addressDocId", addressDocId);
                                db.collection("freshfold").document("app_data")
                                  .collection("users").document(phone)
                                  .collection("address").document(addressDocId)
                                  .collection("transactions").document()
                                  .set(txn);

                                // Save order to Firestore
                                saveOrderToFirestore(orderIdToClear, phone, addressDocId, items, date, finalTotalPrice, finalFranchiseId);

                                // Clear the cart since order is successfully confirmed!
                                boolean fromNav = getIntent().getBooleanExtra("from_nav", false);
                                CartManager.getInstance().clearCartAfterOrder(fromNav);
                                CartManager.getInstance().clearPendingCart();
                                // Remove the committed order from in-memory list so it
                                // no longer shows up in "My Cart" after confirmation.
                                CartManager.getInstance().removeOrderById(orderIdToClear);

                                // Build service names string
                                java.util.LinkedHashSet<String> serviceNames = new java.util.LinkedHashSet<>();
                                for (com.ankita.freshfold.CartItem ci : items) serviceNames.add(ci.getName());
                                String servicesText = android.text.TextUtils.join(" + ", serviceNames);

                                if (finalHasIroning) {
                                    android.content.Intent intent = new android.content.Intent(PickupSlotActivity.this, com.ankita.freshfold.IroningConfirmationActivity.class);
                                    intent.putExtra("pickup_date", date);
                                    intent.putExtra("item_count", finalTotalItems);
                                    intent.putExtra("order_id_to_clear", orderIdToClear);
                                    intent.putExtra("address_doc_id", addressDocId);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    android.widget.Toast.makeText(PickupSlotActivity.this, "Order Placed Successfully!", android.widget.Toast.LENGTH_LONG).show();
                                    android.content.Intent intent = new android.content.Intent(PickupSlotActivity.this, com.ankita.freshfold.ui.cart.OrderConfirmationActivity.class);
                                    intent.putExtra("pickup_date", date);
                                    intent.putExtra("services_text", servicesText);
                                    intent.putExtra("total_price", finalTotalPrice);
                                    intent.putExtra("order_id", orderIdToClear);
                                    intent.putExtra("address_doc_id", addressDocId);
                                    startActivity(intent);
                                    finish();
                                }
                            });
                    } else {
                        android.widget.Toast.makeText(PickupSlotActivity.this, "Insufficient Balance (Available: ₹" + available + ")", android.widget.Toast.LENGTH_LONG).show();
                        // Rollback unconfirmed order since it failed due to balance
                        CartManager.getInstance().removeOrderById(orderIdToClear);
                        // Re-enable button so user can top up wallet and try again
                        isOrderProcessing = false;
                        btnContinue.setEnabled(true);
                        btnContinue.setAlpha(1.0f);
                    }
                }).addOnFailureListener(e -> {
                    android.widget.Toast.makeText(PickupSlotActivity.this, "Failed to check wallet balance.", android.widget.Toast.LENGTH_SHORT).show();
                    // Re-enable button on failure too
                    isOrderProcessing = false;
                    btnContinue.setEnabled(true);
                    btnContinue.setAlpha(1.0f);
                });
              }).addOnFailureListener(e -> {
                  android.widget.Toast.makeText(PickupSlotActivity.this, "Failed to resolve address.", android.widget.Toast.LENGTH_SHORT).show();
                  isOrderProcessing = false;
                  btnContinue.setEnabled(true);
                  btnContinue.setAlpha(1.0f);
              });
        });
    }

    private void showDisclaimerDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Important Disclaimer")
            .setMessage("All clothes are processed carefully according to standard care label instructions. However, we cannot be held responsible for color bleeding, shrinkage, or damage to weak/delicate fabrics unless explicitly stated in special instructions and agreed upon. Please review your pockets before handover.")
            .setPositiveButton("I Understand", null)
            .show();
    }

    private String calculateDeliveryDate(String pickupDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            java.util.Date date = sdf.parse(pickupDate);
            if (date != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                cal.add(Calendar.DAY_OF_YEAR, 1);
                return sdf.format(cal.getTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void saveOrderToFirestore(String orderId, String phone, String addressDocId, List<CartItem> items, String pickupDate, int totalPrice, String franchiseId) {
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("userPhone", phone);
        orderData.put("addressDocId", addressDocId);
        orderData.put("timestamp", System.currentTimeMillis());
        orderData.put("status", "Picking Pending");
        orderData.put("pickupDate", pickupDate);
        orderData.put("deliveryDate", calculateDeliveryDate(pickupDate));
        orderData.put("totalPrice", totalPrice);
        
        // Save pickup instruction if available
        String instruction = "";
        if (etPickupInstructions != null && etPickupInstructions.getText() != null) {
            instruction = etPickupInstructions.getText().toString().trim();
        }
        orderData.put("pickupInstruction", instruction);

        // Save franchiseId with order so it is directly queryable from the order document
        orderData.put("franchiseId", franchiseId != null ? franchiseId : "");

        // Save current address with order
        String userAddress = sessionManager.getUserAddress();
        orderData.put("address", userAddress != null ? userAddress : "");

        List<Map<String, Object>> itemsList = new java.util.ArrayList<>();
        for (CartItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("name", item.getName());
            itemMap.put("description", item.getDescription());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("pricePerUnit", item.getPricePerUnit());
            itemMap.put("unit", item.getUnit());
            itemMap.put("emoji", item.getEmoji());
            itemMap.put("instructions", item.getInstructions());
            itemsList.add(itemMap);
        }
        orderData.put("items", itemsList);

        // Also save to user's orders sub-collection
        db.collection("freshfold").document("app_data")
          .collection("users").document(phone)
          .collection("address").document(addressDocId)
          .collection("orders").document(orderId)
          .set(orderData);
    }

    // ── Load saved instruction from Firestore ──
    private void loadSavedInstruction() {
        String phone = sessionManager.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        db.collection("freshfold").document("app_data")
            .collection("users").document(phone)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String saved = doc.getString("pickup_instruction");
                    if (saved != null && !saved.isEmpty()) {
                        if (etPickupInstructions != null) {
                            etPickupInstructions.setText(saved);
                        }
                    }
                }
            })
            .addOnFailureListener(e -> {
                // Silently fail — field stays empty
            });
    }

    // ── Save instruction to Firestore ──
    private void saveInstruction() {
        String phone = sessionManager.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        String instruction = "";
        if (etPickupInstructions != null && etPickupInstructions.getText() != null) {
            instruction = etPickupInstructions.getText().toString().trim();
        }

        Map<String, Object> update = new HashMap<>();
        update.put("pickup_instruction", instruction);

        db.collection("freshfold").document("app_data")
            .collection("users").document(phone)
            .update(update)
            .addOnSuccessListener(aVoid -> {
                com.google.android.material.snackbar.Snackbar
                    .make(findViewById(android.R.id.content),
                          "✓  Saved Successfully",
                          com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getResources().getColor(R.color.brand_primary, getTheme()))
                    .setTextColor(Color.WHITE)
                    .show();
            })
            .addOnFailureListener(e -> {
                // Try set (in case document doesn't exist)
                db.collection("freshfold").document("app_data")
                    .collection("users").document(phone)
                    .set(update, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid2 -> {
                        com.google.android.material.snackbar.Snackbar
                            .make(findViewById(android.R.id.content),
                                  "✓  Saved Successfully",
                                  com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(getResources().getColor(R.color.brand_primary, getTheme()))
                            .setTextColor(Color.WHITE)
                            .show();
                    });
            });
    }

    private void setupDates() {
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat dayFmt  = new SimpleDateFormat("EEEE", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_YEAR, 1);
        tvDateTomorrow.setText(dateFmt.format(cal.getTime()));
        tvDayTomorrow.setText(dayFmt.format(cal.getTime()));

        cal.add(Calendar.DAY_OF_YEAR, 1);
        tvDateDayAfter.setText(dateFmt.format(cal.getTime()));
        tvDayDayAfter.setText(dayFmt.format(cal.getTime()));
    }

    private void selectDate(boolean tomorrow) {
        isTomorrowSelected = tomorrow;
        int selectedColor  = getResources().getColor(R.color.tint_primary, getTheme());
        int normalColor    = getResources().getColor(android.R.color.white, getTheme());
        int selectedStroke = getResources().getColor(R.color.brand_primary, getTheme());
        int normalStroke   = getResources().getColor(R.color.stroke_light, getTheme());
        float density      = getResources().getDisplayMetrics().density;

        if (tomorrow) {
            cardTomorrow.setCardBackgroundColor(selectedColor);
            cardTomorrow.setStrokeColor(selectedStroke);
            cardTomorrow.setStrokeWidth(Math.round(2 * density));
            cardDayAfter.setCardBackgroundColor(normalColor);
            cardDayAfter.setStrokeColor(normalStroke);
            cardDayAfter.setStrokeWidth(Math.round(1 * density));
            ivCheckTomorrow.setVisibility(View.VISIBLE);
            ivCheckDayAfter.setVisibility(View.GONE);
        } else {
            cardDayAfter.setCardBackgroundColor(selectedColor);
            cardDayAfter.setStrokeColor(selectedStroke);
            cardDayAfter.setStrokeWidth(Math.round(2 * density));
            cardTomorrow.setCardBackgroundColor(normalColor);
            cardTomorrow.setStrokeColor(normalStroke);
            cardTomorrow.setStrokeWidth(Math.round(1 * density));
            ivCheckDayAfter.setVisibility(View.VISIBLE);
            ivCheckTomorrow.setVisibility(View.GONE);
        }
    }

    private void rollbackUnconfirmedOrder() {
        String orderIdToClear = getIntent().getStringExtra("order_id_to_clear");
        if (orderIdToClear != null) {
            CartManager.getInstance().removeOrderById(orderIdToClear);
        }
    }

    @Override
    public void onBackPressed() {
        rollbackUnconfirmedOrder();
        super.onBackPressed();
    }

    private void showAddressSelectionDialog() {
        String phone = sessionManager.getUserPhone();
        if (phone == null || phone.isEmpty()) return;

        db.collection("freshfold").document("app_data")
          .collection("users").document(phone)
          .collection("address")
          .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
          .get()
          .addOnSuccessListener(querySnap -> {
              if (querySnap.isEmpty()) {
                  android.widget.Toast.makeText(this, "No saved addresses found.", android.widget.Toast.LENGTH_SHORT).show();
                  return;
              }
              
              java.util.List<String> addressList = new java.util.ArrayList<>();
              for (com.google.firebase.firestore.DocumentSnapshot doc : querySnap.getDocuments()) {
                  String addr = doc.getString("address");
                  if (addr != null && !addr.isEmpty() && !addressList.contains(addr)) {
                      addressList.add(addr);
                  }
              }
              
              if (addressList.isEmpty()) {
                  android.widget.Toast.makeText(this, "No saved addresses found.", android.widget.Toast.LENGTH_SHORT).show();
                  return;
              }

              // Premium BottomSheet
              com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                      new com.google.android.material.bottomsheet.BottomSheetDialog(this);
              
              android.view.View bottomSheetView = android.view.LayoutInflater.from(this).inflate(R.layout.bottom_sheet_addresses, null);
              android.widget.LinearLayout layoutAddressList = bottomSheetView.findViewById(R.id.layoutAddressList);
              
              String currentAddr = sessionManager.getUserAddress();

              for (String addr : addressList) {
                  android.view.View itemView = android.view.LayoutInflater.from(this).inflate(R.layout.item_address_selection, layoutAddressList, false);
                  android.widget.TextView tvAddressText = itemView.findViewById(R.id.tvAddressText);
                  android.widget.ImageView ivSelectedMark = itemView.findViewById(R.id.ivSelectedMark);
                  com.google.android.material.card.MaterialCardView cardContainer = (com.google.android.material.card.MaterialCardView) itemView;

                  tvAddressText.setText(addr);
                  
                  if (addr.equals(currentAddr)) {
                      ivSelectedMark.setVisibility(android.view.View.VISIBLE);
                      cardContainer.setStrokeColor(getResources().getColor(R.color.brand_primary, getTheme()));
                      cardContainer.setCardBackgroundColor(getResources().getColor(R.color.tint_primary, getTheme()));
                  }

                  itemView.setOnClickListener(v -> {
                      sessionManager.updateUserAddress(addr);
                      displayCurrentAddress();
                      bottomSheetDialog.dismiss();
                  });

                  layoutAddressList.addView(itemView);
              }
              
              bottomSheetDialog.setContentView(bottomSheetView);
              // Make background transparent so rounded corners show
              bottomSheetDialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
              bottomSheetDialog.show();
          })
          .addOnFailureListener(e -> {
              android.widget.Toast.makeText(this, "Failed to load addresses", android.widget.Toast.LENGTH_SHORT).show();
          });
    }

    private void displayCurrentAddress() {
        TextView tvAddressLine1 = findViewById(R.id.tvAddressLine1);
        TextView tvAddressLine2 = findViewById(R.id.tvAddressLine2);

        String address = sessionManager.getUserAddress();
        if (address != null && !address.isEmpty()) {
            tvAddressLine1.setText(address);
            tvAddressLine2.setVisibility(View.GONE);
        } else {
            tvAddressLine1.setText("No address set");
            tvAddressLine2.setText("Please update your address in profile");
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
