package com.ankita.freshfold.ui.wallet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ankita.freshfold.R;
import com.ankita.freshfold.SessionManager;
import com.ankita.freshfold.ui.home.MainActivity;
import com.ankita.freshfold.viewmodel.WalletViewModel;

import java.util.ArrayList;
import java.util.List;

public class WalletFragment extends Fragment {
    private WalletViewModel viewModel;
    private SessionManager sessionManager;
    private TextView tvBalance;
    private TextView tvAvailableBalance;
    private RecyclerView rvTransactions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallet, container, false);

        sessionManager = new SessionManager(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(WalletViewModel.class);

        tvBalance = view.findViewById(R.id.tvBalance);
        tvAvailableBalance = view.findViewById(R.id.tvAvailableBalance);
        rvTransactions = view.findViewById(R.id.rvTransactions);

        setupClickListeners(view);
        observeViewModel();
        setupTransactions();

        viewModel.fetchBalance(sessionManager.getUserPhone());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh balance every time fragment becomes visible (e.g., after payment)
        if (viewModel != null && sessionManager != null) {
            viewModel.fetchBalance(sessionManager.getUserPhone());
        }
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new com.ankita.freshfold.ui.home.HomeFragment(), "HOME");
                ((MainActivity) getActivity()).updateNavUI("HOME");
            }
        });

        view.findViewById(R.id.btnAddMoneyCard).setOnClickListener(v -> {
            com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
            builder.setTitle("Top Up Wallet");
            builder.setMessage("Enter the amount you want to add to your wallet.");
            
            final android.widget.EditText input = new android.widget.EditText(requireContext());
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setHint("Amount (₹)");
            input.setBackgroundResource(android.R.drawable.edit_text);
            
            android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
            int margin = (int) (20 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, margin / 2, margin, margin / 2);
            input.setLayoutParams(params);
            container.addView(input);
            builder.setView(container);

            builder.setPositiveButton("Add Money", (dialog, which) -> {
                String amountStr = input.getText().toString();
                if (!amountStr.isEmpty()) {
                    int amount = Integer.parseInt(amountStr);
                    if (amount > 0 && getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).startRazorpayPayment(amount);
                    } else {
                        Toast.makeText(getContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
    }

    private void observeViewModel() {
        viewModel.balance.observe(getViewLifecycleOwner(), balance -> {
            tvBalance.setText("₹ " + String.format("%.2f", balance));
        });

        viewModel.availableBalance.observe(getViewLifecycleOwner(), available -> {
            if (tvAvailableBalance != null) {
                tvAvailableBalance.setText("₹ " + String.format("%.2f", available));
            }
        });

        viewModel.transactions.observe(getViewLifecycleOwner(), transactions -> {
            TransactionAdapter adapter = new TransactionAdapter(transactions);
            rvTransactions.setAdapter(adapter);
        });
    }

    private void setupTransactions() {
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
    }
}

