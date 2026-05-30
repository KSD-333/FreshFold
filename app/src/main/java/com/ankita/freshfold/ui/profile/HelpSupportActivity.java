package com.ankita.freshfold.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ankita.freshfold.R;
import java.util.ArrayList;
import java.util.List;

public class HelpSupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_support);

        // Transparent status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());

        RecyclerView rvFaqs = findViewById(R.id.rvFaqs);
        rvFaqs.setLayoutManager(new LinearLayoutManager(this));

        List<FAQ> faqList = new ArrayList<>();
        faqList.add(new FAQ("How do I place an order?", 
                "You can place an order by selecting the services from the home screen, adding them to your cart, and proceeding to checkout."));
        faqList.add(new FAQ("What are the delivery charges?", 
                "Delivery is free for orders above ₹500. For orders below that, a nominal charge of ₹50 applies."));
        faqList.add(new FAQ("How can I track my order?", 
                "You can track your order in the 'My Orders' section of the app."));
        faqList.add(new FAQ("Can I cancel my order?", 
                "Yes, you can cancel your order before it is picked up by our executive."));
        faqList.add(new FAQ("What should I do if I received a damaged item?", 
                "Please contact our support team immediately through the app or call our helpline."));
        faqList.add(new FAQ("How do I apply a coupon code?", 
                "You can enter the coupon code at the checkout page before making the payment."));

        FAQAdapter adapter = new FAQAdapter(faqList);
        rvFaqs.setAdapter(adapter);

        findViewById(R.id.btnContact).setOnClickListener(v -> showContactOptions());
    }

    private void showContactOptions() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_contact_options, null);
        
        view.findViewById(R.id.btnCallService).setOnClickListener(v -> {
            dialNumber("1111111111");
            dialog.dismiss();
        });

        view.findViewById(R.id.btnCallTechnical).setOnClickListener(v -> {
            dialNumber("9999999999");
            dialog.dismiss();
        });

        dialog.setContentView(view);
        dialog.show();
    }

    private void dialNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }
}
