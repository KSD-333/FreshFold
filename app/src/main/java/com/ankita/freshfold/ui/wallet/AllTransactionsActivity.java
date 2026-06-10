package com.ankita.freshfold.ui.wallet;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.viewmodel.WalletViewModel;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;

public class AllTransactionsActivity extends AppCompatActivity {

    private WalletViewModel viewModel;
    private SessionManager sessionManager;

    private RecyclerView rvTransactions;
    private TransactionAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvNoTransactions;
    private MaterialButtonToggleGroup toggleGroupFilters;

    private List<Transaction> allTransactions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_transactions);

        // Transparent status bar over gradient header
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(WalletViewModel.class);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        rvTransactions = findViewById(R.id.rvTransactions);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));

        progressBar = findViewById(R.id.progressBar);
        tvNoTransactions = findViewById(R.id.tvNoTransactions);
        toggleGroupFilters = findViewById(R.id.toggleGroupFilters);

        adapter = new TransactionAdapter(new ArrayList<>());
        rvTransactions.setAdapter(adapter);

        setupFilters();
        observeViewModel();

        progressBar.setVisibility(View.VISIBLE);
        viewModel.fetchTransactions(sessionManager.getUserPhone());
    }

    private void observeViewModel() {
        viewModel.transactions.observe(this, transactions -> {
            progressBar.setVisibility(View.GONE);
            if (transactions != null) {
                allTransactions = transactions;
                applyFilter(toggleGroupFilters.getCheckedButtonId());
            }
        });
    }

    private void setupFilters() {
        toggleGroupFilters.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                applyFilter(checkedId);
            }
        });
    }

    private void applyFilter(int checkedId) {
        List<Transaction> filteredList = new ArrayList<>();

        for (Transaction t : allTransactions) {
            if (checkedId == R.id.btnFilterAll) {
                filteredList.add(t);
            } else if (checkedId == R.id.btnFilterDeposits) {
                if (t.isCredit()) {
                    filteredList.add(t);
                }
            } else if (checkedId == R.id.btnFilterOrders) {
                if (!t.isCredit()) {
                    filteredList.add(t);
                }
            }
        }

        if (filteredList.isEmpty()) {
            tvNoTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvNoTransactions.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            // Re-bind adapter to refresh list properly
            adapter = new TransactionAdapter(filteredList);
            rvTransactions.setAdapter(adapter);
        }
    }
}
