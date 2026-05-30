package com.ankita.freshfold.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PickupSlotViewModel extends ViewModel {
    private final MutableLiveData<String> _tomorrowDate = new MutableLiveData<>();
    public LiveData<String> tomorrowDate = _tomorrowDate;

    private final MutableLiveData<String> _dayAfterDate = new MutableLiveData<>();
    public LiveData<String> dayAfterDate = _dayAfterDate;

    private final MutableLiveData<String> _selectedDate = new MutableLiveData<>("");
    public LiveData<String> selectedDate = _selectedDate;

    public PickupSlotViewModel() {
        calculateDates();
    }

    private void calculateDates() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_YEAR, 1);
        _tomorrowDate.setValue(sdf.format(cal.getTime()));

        cal.add(Calendar.DAY_OF_YEAR, 1);
        _dayAfterDate.setValue(sdf.format(cal.getTime()));
    }

    public void selectDate(String date) {
        _selectedDate.setValue(date);
    }
}
