package com.outsystems.smsretriever;

import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created on : Oct 10, 2019 Author : Paulo Camilo
 */
public class SmsRetrieverPlugin extends CordovaPlugin {

    private static final String GENERATE_HASH_KEY = "generateHashKey";
    private static final String START_SMS_LISTENER = "startSmsListener";
    private static final String INVALID_ACTION = "Invalid or not found action.";
    private CallbackContext callbackContext;
    private SmsRetrieverHandler smsRetrieverHandler;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        smsRetrieverHandler = new SmsRetrieverHandler(cordova.getActivity());
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action != null) {
            if (GENERATE_HASH_KEY.equals(action)) {
                generateHashKey();
            }

            if (START_SMS_LISTENER.equals(action)) {

                startSMSListener();
                smsRetrieverHandler.startBroadcastReceiver();

                smsRetrieverHandler.setOtpReceivedCallback(new OtpReceivedInterface<String>() {
                    @Override
                    public void onOtpReceived(String otp) {
                        Log.v("onOtpReceived", otp);
                        String codeReceived = getOTPCode(otp);
                        JSONObject objectCode = new JSONObject();

                        try {
                            objectCode.put("code", codeReceived);
                            callbackContext.success(objectCode);
                        } catch (JSONException e) {
                            callbackContext.error(e.getMessage());
                            Log.v("JSONException", e.getMessage());
                        }
                    }

                    @Override
                    public void onOtpTimeout() {
                        startSMSListener();
                        smsRetrieverHandler.startBroadcastReceiver();
                    }
                });
            }
        } else {
            this.callbackContext.error(INVALID_ACTION);
        }

        return true;
    }

    /**
     * Start OTP listener to receive SMS with code
     */
    private void startSMSListener() {
        SmsRetrieverClient mClient = SmsRetriever.getClient(this.cordova.getActivity());
        Task<Void> mTask = mClient.startSmsRetriever();

        mTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.v("onSuccess", "success to start listener");
            }
        });

        mTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.v("onFailure", "Error to start listener: " + e.getMessage());
            }
        });

    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        startSMSListener();
        smsRetrieverHandler.startBroadcastReceiver();
    }

    /**
     * Hash key string to determine which verification messages to send to your app
     */
    private void generateHashKey() {
        AppSignatureHelper appSignatureHelper = new AppSignatureHelper(this.cordova.getActivity());
        String hashKey = appSignatureHelper.getAppSignatures().get(0);

        JSONObject objectHashKey = new JSONObject();
        try {
            objectHashKey.put("hashKey", hashKey);
            Log.v("hashKey", hashKey);
            this.callbackContext.success(objectHashKey);
        } catch (JSONException e) {
            this.callbackContext.error(e.getMessage());
        }
    }

    /**
     * Extract the code of the message received
     *
     * @param message the message OTP
     * @return the code extracted
     */
    private String getOTPCode(String message) {
        Pattern p = Pattern.compile("\\b\\d{4}\\b");
        Matcher m = p.matcher(message);
        String code = "";
        while (m.find()) {
            code = m.group(0);
        }
        return code;
    }
}
