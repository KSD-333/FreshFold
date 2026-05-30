package com.ankita.freshfold;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class Order implements Serializable {
    private String id;
    private List<CartItem> items;
    private long timestamp;
    private String status; // "Picking Pending", "Picked Up", "Out for Delivery", "Delivered"
    private String address;

    public Order(List<CartItem> items) {
        this.id = UUID.randomUUID().toString();
        this.items = items;
        this.timestamp = System.currentTimeMillis();
        this.status = "Picking Pending";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public List<CartItem> getItems() { return items; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getTotalPrice() {
        int total = 0;
        for (CartItem item : items) total += item.getTotalPrice();
        return total;
    }

    public String getCombinedNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (CartItem item : items) names.add(item.getName());
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String name : names) {
            if (i > 0) sb.append(" + ");
            sb.append(name);
            i++;
        }
        return sb.toString();
    }
}
