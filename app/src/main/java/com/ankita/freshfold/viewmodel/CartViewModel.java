package com.ankita.freshfold.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ankita.freshfold.CartItem;

import java.util.ArrayList;
import java.util.List;

public class CartViewModel extends ViewModel {
    private final MutableLiveData<List<CartItem>> _cartList = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<CartItem>> cartList = _cartList;

    private final MutableLiveData<Integer> _subtotal = new MutableLiveData<>(0);
    public LiveData<Integer> subtotal = _subtotal;

    private final MutableLiveData<Boolean> _isMinOrderMet = new MutableLiveData<>(false);
    public LiveData<Boolean> isMinOrderMet = _isMinOrderMet;

    public void initCart(int qtyLaundry, int qtyIron, int qtyDryClean, int qtyShoe, int qtyBlanket) {
        List<CartItem> list = new ArrayList<>();
        if (qtyLaundry > 0) list.add(new CartItem("Laundry", "Wash & Fold", qtyLaundry, 40, "service", "👕"));
        if (qtyIron > 0) list.add(new CartItem("Steam Iron", "Perfect Finish", qtyIron, 20, "item", "🔌"));
        if (qtyDryClean > 0) list.add(new CartItem("Dry Clean", "Premium Care", qtyDryClean, 120, "item", "👔"));
        if (qtyShoe > 0) list.add(new CartItem("Shoe Clean", "Deep Clean", qtyShoe, 50, "pair", "👟"));
        if (qtyBlanket > 0) list.add(new CartItem("Blanket Wash", "Soft & Fresh", qtyBlanket, 150, "item", "🛏️"));
        _cartList.setValue(list);
        calculateSummary();
    }

    public void calculateSummary() {
        int total = 0;
        List<CartItem> currentList = _cartList.getValue();
        if (currentList != null) {
            for (CartItem item : currentList) {
                total += item.getTotalPrice();
            }
        }
        _subtotal.setValue(total);
        _isMinOrderMet.setValue(total >= 80);
    }

    public void removeItem(int position) {
        List<CartItem> currentList = _cartList.getValue();
        if (currentList != null && position < currentList.size()) {
            currentList.remove(position);
            _cartList.setValue(currentList);
            calculateSummary();
        }
    }

    public void updateCart() {
        calculateSummary();
    }
}
