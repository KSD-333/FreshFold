package com.ankita.freshfold.ui.profile;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.ankita.freshfold.R;

public class AboutUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // Transparent status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }

        findViewById(R.id.ivBack).setOnClickListener(v -> finish());
    }
}
