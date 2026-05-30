package com.ankita.freshfold;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import android.util.Log;
import com.google.android.gms.common.api.Status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsBroadcastReceiver extends BroadcastReceiver {

    public static final String OTP_RETRIEVED = "com.ankita.freshfold.OTP_RETRIEVED";
    private static final String TAG = "SmsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
                if (status != null) {
                    switch (status.getStatusCode()) {
                        case CommonStatusCodes.SUCCESS:
                            // Get SMS message contents
                            String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                            Log.d(TAG, "onReceive: " + message);
                            if (message != null) {
                                // Extract OTP using regex (assuming 6 digits)
                                Pattern pattern = Pattern.compile("(|^)\\d{6}");
                                Matcher matcher = pattern.matcher(message);
                                if (matcher.find()) {
                                    String otp = matcher.group(0);
                                    Intent otpIntent = new Intent(OTP_RETRIEVED);
                                    otpIntent.putExtra("otp", otp);
                                    context.sendBroadcast(otpIntent);
                                }
                            }
                            break;
                        case CommonStatusCodes.TIMEOUT:
                            Log.d(TAG, "onReceive: Timeout");
                            break;
                    }
                }
            }
        }
    }
}
