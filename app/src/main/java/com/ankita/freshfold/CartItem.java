package com.ankita.freshfold;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String name;
    private String description;
    private int quantity;
    private int pricePerUnit;
    private String unit; // kg or item
    private String emoji;
    private String instructions;
    private int imageRes;

    private boolean isCurrentFlow = false;

    public CartItem(String name, String description, int quantity, int pricePerUnit, String unit, String emoji) {
        this(name, description, quantity, pricePerUnit, unit, emoji, "");
    }

    public CartItem(String name, String description, int quantity, int pricePerUnit, String unit, String emoji, String instructions) {
        this(name, description, quantity, pricePerUnit, unit, emoji, instructions, 0);
    }

    public CartItem(String name, String description, int quantity, int pricePerUnit, String unit, String emoji, String instructions, int imageRes) {
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.unit = unit;
        this.emoji = emoji;
        this.instructions = instructions;
        this.imageRes = imageRes;
        this.isCurrentFlow = true; // Default to true when newly created
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getPricePerUnit() { return pricePerUnit; }
    public String getUnit() { return unit; }
    public String getEmoji() { return emoji; }
    public String getInstructions() { return instructions; }
    public int getImageRes() { return imageRes; }
    public int getTotalPrice() { return quantity * pricePerUnit; }
    
    public boolean isCurrentFlow() { return isCurrentFlow; }
    public void setCurrentFlow(boolean currentFlow) { isCurrentFlow = currentFlow; }
}
