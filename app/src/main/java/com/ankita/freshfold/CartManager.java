package com.ankita.freshfold;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CartManager {

    private static CartManager instance;
    private final List<Order> orders = new ArrayList<>();
    private final List<CartItem> sessionItems = new ArrayList<>();   // My Cart page (current flow only)
    private final List<CartItem> pendingCartItems = new ArrayList<>(); // Bottom nav cart (all pending services)
    private boolean isAddMoreSession = false;

    private static final String PREF_NAME        = "FreshfoldCart";
    private static final String KEY_ORDERS       = "cart_orders";
    private static final String KEY_SESSION      = "session_items";
    private static final String KEY_PENDING_CART = "pending_cart_items";

    private Context appContext;
    private String loadedUserId = "";

    private CartManager() {}

    private void checkUserChanged() {
        if (appContext == null) return;
        SharedPreferences sessionPrefs = appContext.getSharedPreferences("FreshfoldPref", Context.MODE_PRIVATE);
        String currentUserId = sessionPrefs.getString("userId", "");
        if (!currentUserId.equals(loadedUserId)) {
            loadedUserId = currentUserId;
            loadFromPrefs();
        }
    }

    private String getPrefName() {
        if (loadedUserId == null || loadedUserId.isEmpty()) {
            return PREF_NAME;
        }
        return PREF_NAME + "_" + loadedUserId;
    }

    public static CartManager getInstance(Context context) {
        if (instance == null) instance = new CartManager();
        if (context != null) {
            if (instance.appContext == null) {
                instance.appContext = context.getApplicationContext();
            }
            instance.checkUserChanged();
        }
        return instance;
    }

    public static CartManager getInstance() {
        if (instance == null) instance = new CartManager();
        instance.checkUserChanged();
        return instance;
    }

    // ── Global Orders API (Bottom Nav) ────────────────────────────────────────

    public List<Order> getOrders() { return orders; }

    public void removeOrder(int position) {
        if (position >= 0 && position < orders.size()) {
            orders.remove(position);
            saveToPrefs();
        }
    }

    public void clearGlobal() {
        orders.clear();
        saveToPrefs();
    }

    public void removeOrderById(String id) {
        if (id == null) return;
        orders.removeIf(order -> id.equals(order.getId()));
        saveToPrefs();
    }

    // ── Session Cart API (Service Flow) ───────────────────────────────────────

    public List<CartItem> getSessionItems() { return sessionItems; }

    public void addSessionItem(CartItem item) {
        mergeItemIntoList(sessionItems, item);
        saveToPrefs(); 
    }

    public void clearSession() {
        sessionItems.clear();
        isAddMoreSession = false;
        saveToPrefs();
    }

    public void removeItemsByService(String serviceName) {
        if (serviceName == null) return;
        sessionItems.removeIf(item -> serviceName.equalsIgnoreCase(item.getName()));
        saveToPrefs();
    }

    /** Groups current session items into a single Order and moves to global list. Returns new Order ID. */
    public String commitSessionToGlobal() {
        if (!sessionItems.isEmpty()) {
            Order newOrder = new Order(new ArrayList<>(sessionItems));
            orders.add(newOrder);
            String id = newOrder.getId();
            clearSession(); // Also saves to prefs
            clearPendingCart(); // Checkout done — clear bottom nav pending too
            return id;
        }
        return null;
    }

    /**
     * Commit to global Order and clear both lists.
     * navCart=true  → commit pendingCartItems (all services)
     * navCart=false → commit sessionItems (current flow)
     */
    public String commitToGlobal(boolean navCart) {
        List<CartItem> itemsToCommit = navCart
                ? new ArrayList<>(pendingCartItems)
                : new ArrayList<>(sessionItems);

        if (!itemsToCommit.isEmpty()) {
            Order newOrder = new Order(itemsToCommit);
            orders.add(newOrder);
            String id = newOrder.getId();
            // Do not clear items here; clear them only when order successfully completes.
            saveToPrefs();
            return id;
        }
        return null;
    }

    public void clearCartAfterOrder(boolean navCart) {
        if (navCart) {
            pendingCartItems.clear();
        } else {
            sessionItems.clear();
        }
        isAddMoreSession = false;
        saveToPrefs();
    }

    // ── Pending Cart API (Bottom Nav — all services across flows) ─────────────

    /** All pending services visible in bottom nav cart */
    public List<CartItem> getPendingCartItems() { return pendingCartItems; }

    /** Add/replace items for a service in the persistent pending cart */
    public void updatePendingCartForService(String serviceName, List<CartItem> newItems) {
        if (serviceName == null) return;
        // Remove old items for this service
        pendingCartItems.removeIf(item -> serviceName.equalsIgnoreCase(item.getName()));
        // Add new items
        pendingCartItems.addAll(newItems);
        saveToPrefs();
    }

    public int getPendingCartTotalPrice() {
        int total = 0;
        for (CartItem item : pendingCartItems) total += item.getTotalPrice();
        return total;
    }

    public boolean isPendingCartEmpty() { return pendingCartItems.isEmpty(); }

    public void clearPendingCart() {
        pendingCartItems.clear();
        saveToPrefs();
    }

    // ── Helper Logic ─────────────────────────────────────────────────────────

    public boolean isAddMoreSession() { return isAddMoreSession; }
    public void setAddMoreSession(boolean value) { isAddMoreSession = value; }

    private void mergeItemIntoList(List<CartItem> list, CartItem newItem) {
        for (CartItem existing : list) {
            if (existing.getName().equals(newItem.getName()) &&
                existing.getDescription().equals(newItem.getDescription())) {
                existing.setQuantity(existing.getQuantity() + newItem.getQuantity());
                return;
            }
        }
        list.add(newItem);
    }

    public List<CartItem> getItemsByService(String serviceName) {
        if (serviceName == null) return sessionItems;
        List<CartItem> filtered = new ArrayList<>();
        for (CartItem item : sessionItems) {
            if (serviceName.equalsIgnoreCase(item.getName())) filtered.add(item);
        }
        return filtered;
    }

    public int getSessionTotalPriceByService(String serviceName) {
        if (serviceName == null) return getSessionTotalPrice();
        int total = 0;
        for (CartItem item : sessionItems) {
            if (serviceName.equalsIgnoreCase(item.getName())) total += item.getTotalPrice();
        }
        return total;
    }

    public void resetCurrentFlow() {
        for (CartItem item : sessionItems) item.setCurrentFlow(false);
        saveToPrefs();
    }

    public List<CartItem> getCurrentFlowItems() {
        List<CartItem> current = new ArrayList<>();
        for (CartItem item : sessionItems) {
            if (item.isCurrentFlow()) current.add(item);
        }
        return current;
    }

    public int getCurrentFlowTotalPrice() {
        int total = 0;
        for (CartItem item : getCurrentFlowItems()) total += item.getTotalPrice();
        return total;
    }

    public int getSessionTotalPrice() {
        int total = 0;
        for (CartItem item : sessionItems) total += item.getTotalPrice();
        return total;
    }

    public boolean isSessionEmpty() { return sessionItems.isEmpty(); }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void saveToPrefs() {
        if (appContext == null) return;
        SharedPreferences prefs = appContext.getSharedPreferences(getPrefName(), Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_ORDERS,       serializeOrders(orders))
            .putString(KEY_SESSION,      serializeSession(sessionItems))
            .putString(KEY_PENDING_CART, serializeSession(pendingCartItems))
            .apply();
    }

    private void loadFromPrefs() {
        if (appContext == null) return;
        SharedPreferences prefs = appContext.getSharedPreferences(getPrefName(), Context.MODE_PRIVATE);

        orders.clear();
        orders.addAll(deserializeOrders(prefs.getString(KEY_ORDERS, null)));

        sessionItems.clear();
        sessionItems.addAll(deserializeSession(prefs.getString(KEY_SESSION, null)));

        pendingCartItems.clear();
        pendingCartItems.addAll(deserializeSession(prefs.getString(KEY_PENDING_CART, null)));
    }

    private String serializeOrders(List<Order> list) {
        try {
            JSONArray array = new JSONArray();
            for (Order order : list) {
                JSONObject oObj = new JSONObject();
                oObj.put("id", order.getId());
                oObj.put("timestamp", order.getTimestamp());
                oObj.put("status", order.getStatus());
                oObj.put("items", new JSONArray(serializeSession(order.getItems())));
                array.put(oObj);
            }
            return array.toString();
        } catch (JSONException e) { return "[]"; }
    }

    private String serializeSession(List<CartItem> list) {
        try {
            JSONArray array = new JSONArray();
            for (CartItem item : list) {
                JSONObject obj = new JSONObject();
                obj.put("name",         item.getName());
                obj.put("description",  item.getDescription());
                obj.put("quantity",     item.getQuantity());
                obj.put("pricePerUnit", item.getPricePerUnit());
                obj.put("unit",         item.getUnit());
                obj.put("emoji",        item.getEmoji() != null ? item.getEmoji() : "");
                obj.put("instructions", item.getInstructions() != null ? item.getInstructions() : "");
                obj.put("imageRes",     item.getImageRes());
                obj.put("isCurrentFlow", item.isCurrentFlow());
                array.put(obj);
            }
            return array.toString();
        } catch (JSONException e) { return "[]"; }
    }

    private List<Order> deserializeOrders(String json) {
        List<Order> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject oObj = array.getJSONObject(i);
                List<CartItem> items = deserializeSession(oObj.getString("items"));
                Order order = new Order(items);
                if (oObj.has("id")) {
                    order.setId(oObj.getString("id"));
                }
                if (oObj.has("status")) {
                    order.setStatus(oObj.getString("status"));
                }
                list.add(order);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    private List<CartItem> deserializeSession(String json) {
        List<CartItem> list = new ArrayList<>();
        if (json == null) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                CartItem item = new CartItem(
                    obj.getString("name"),
                    obj.getString("description"),
                    obj.getInt("quantity"),
                    obj.getInt("pricePerUnit"),
                    obj.getString("unit"),
                    obj.optString("emoji", ""),
                    obj.optString("instructions", ""),
                    obj.optInt("imageRes", 0)
                );
                item.setCurrentFlow(obj.optBoolean("isCurrentFlow", false));
                list.add(item);
            }
        } catch (JSONException ignored) {}
        return list;
    }

    // LEGACY COMPATIBILITY
    public List<CartItem> getGlobalItems() { 
        List<CartItem> all = new ArrayList<>();
        for (Order o : orders) all.addAll(o.getItems());
        return all;
    }
    public List<CartItem> getItems() { return sessionItems; } 
    public void addItem(CartItem item) { addSessionItem(item); }
    public void removeItem(int pos) { if (pos >= 0 && pos < sessionItems.size()) sessionItems.remove(pos); saveToPrefs(); }
    public void clear() { clearSession(); }
    public int getTotalPrice() { return getSessionTotalPrice(); }
    public boolean isEmpty() { return isSessionEmpty(); }
    public void notifyItemChanged() { saveToPrefs(); }
    public void removeGlobalItem(int pos) { /* No longer applicable for individual items in global */ }

    public void injectDummyOrdersIfEmpty() {
        if (!orders.isEmpty()) return;
        
        // Dummy 1: Completed
        List<CartItem> items1 = new ArrayList<>();
        items1.add(new CartItem("Laundry", "Regular wash", 5, 40, "kg", "👕"));
        Order o1 = new Order(items1);
        o1.setStatus("Delivered");
        orders.add(o1);

        // Dummy 2: Active
        List<CartItem> items2 = new ArrayList<>();
        items2.add(new CartItem("Dry Clean", "Suit", 1, 120, "item", "👔"));
        Order o2 = new Order(items2);
        o2.setStatus("Picked Up");
        orders.add(o2);
        
        // Dummy 3: Pending
        List<CartItem> items3 = new ArrayList<>();
        items3.add(new CartItem("Steam Iron", "Shirt", 3, 20, "item", "💨"));
        Order o3 = new Order(items3);
        o3.setStatus("Picking Pending");
        orders.add(o3);

        saveToPrefs();
    }
}
