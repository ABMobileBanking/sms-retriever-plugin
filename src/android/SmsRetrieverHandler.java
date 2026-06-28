package com.outsystems.smsretriever;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import android.os.Build; //Added for android 14 crash issue fix

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

import com.huawei.hms.support.sms.common.ReadSmsConstant;

/**
 * Created by pcamilo on 10/10/2019
 */
public class SmsRetrieverHandler {

    private Activity activity;
    private static String TAG = SmsRetrieverHandler.class.getSimpleName();

    public SmsRetrieverHandler(Activity activity) {
        TAG = this.getClass().getSimpleName();
        this.activity = activity;
    }

    void startBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
        //this.activity.registerReceiver(mSmsBroadcastReceiver, intentFilter,ContextCompat.RECEIVER_EXPORTED);

        //Below code is used for android 14 crash issue fix
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.activity.registerReceiver(mSmsBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        }else{
            this.activity.registerReceiver(mSmsBroadcastReceiver, intentFilter);
        }
    }

    protected OtpReceivedInterface<String> onOtpReceived;

    public void setOtpReceivedCallback(OtpReceivedInterface<String> callback) {
        onOtpReceived = callback;
    }

    private BroadcastReceiver mSmsBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
            if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                Bundle extras = intent.getExtras();
                Status mStatus = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                switch (mStatus.getStatusCode()) {
                case CommonStatusCodes.SUCCESS:
                    // Get SMS message contents'
                    String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                    Log.d(TAG, "onReceive: failure " + message);
                    if (onOtpReceived != null) {
                        String otpMessage = message.replace("<#> Your otp code is: ", "");
                        String otp = otpMessage.split("\n")[0];
                        onOtpReceived.onOtpReceived(otp);
                    }
                    break;
                case CommonStatusCodes.TIMEOUT:
                    // Waiting for SMS timed out (5 minutes)
                    Log.d(TAG, "onReceive: failure");
                    if (onOtpReceived != null) {
                        onOtpReceived.onOtpTimeout();
                    }
                    break;
                }
            }
        }
    };
}
