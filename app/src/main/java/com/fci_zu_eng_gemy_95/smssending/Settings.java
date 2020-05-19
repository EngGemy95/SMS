


package com.fci_zu_eng_gemy_95.smssending;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.fci_zu_eng_gemy_95.smssending.databinding.ActivitySettingsBinding;

import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class Settings extends AppCompatActivity {

    ActivitySettingsBinding binding;
    String countryCode;
    private Handler mHandler;
    static int NUMBEROFSMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 100;
    String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    PendingIntent sentPI, deliveredPI;
    BroadcastReceiver smsSentReceiver, smsDeliveredReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        binding.ccp.registerCarrierNumberEditText(binding.phoneTextSettings);

        mHandler = new Handler();
        countryCode = binding.ccp.getSelectedCountryCode();

        sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SENT), 0);
        deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(DELIVERED), 0);

        binding.toggleBtnSettings.setTextColor(getResources().getColor(R.color.green));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        }

        binding.toggleBtnSettings.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && checkData()) {
                    submit();
                } else {
                    binding.toggleBtnSettings.setChecked(false);
                    binding.toggleBtnSettings.setTextOn("Start");
                    binding.toggleBtnSettings.setTextColor(getResources().getColor(R.color.green));
                    mHandler.removeCallbacks(runable);
                    NUMBEROFSMS = 1;
                }
            }
        });
    }

    private void submit() {
        binding.toggleBtnSettings.setTextOn("Stop");
        binding.toggleBtnSettings.setTextColor(getResources().getColor(R.color.red));
        runable.run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        smsSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS Sent", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "Generic Failure", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(context, "No Service", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(context, "Radio Off", Toast.LENGTH_SHORT).show();
                        break;

                }
            }
        };

        smsDeliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "SMS Delivered Successfully", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(context, "SMS not delivered!", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        registerReceiver(smsSentReceiver, new IntentFilter(SENT));
        registerReceiver(smsDeliveredReceiver, new IntentFilter(DELIVERED));

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(smsDeliveredReceiver);
        unregisterReceiver(smsSentReceiver);
    }


    private boolean checkData() {
        if (binding.smsBtnSettings.getText().length() != 0) {
            if (verifyPhoneNumber()) {
                if (binding.frequencyEdtSettings.getText().length() != 0) {
                    return true;
                } else {
                    Toast.makeText(this, "Please Enter Frequency", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                Toast.makeText(this, "Wrong Phone Number", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(this, "Please Enter SMS", Toast.LENGTH_SHORT).show();
            return false;
        }
    }


    private Runnable runable = new Runnable() {
        @Override
        public void run() {
            makeMessage();
        }
    };


    private void sendSMS(String mobileNumber, String SMSText) {
        SmsManager smgr = SmsManager.getDefault();
        smgr.sendTextMessage(
                mobileNumber,
                null,
                SMSText,
                sentPI,
                deliveredPI);
    }

    private void makeMessage() {
        int freq = Integer.parseInt(binding.frequencyEdtSettings.getText().toString()) * 1000;
        sendSMS(binding.phoneTextSettings.getText().toString(), binding.smsBtnSettings.getText() + "\n\n SMS : " + NUMBEROFSMS);
        mHandler.postDelayed(runable, freq);
        NUMBEROFSMS++;
    }

    private boolean verifyPhoneNumber() {
        if (!binding.ccp.getFullNumberWithPlus().equals("")) {
            if (validateUsing_libphonenumber(countryCode, binding.ccp.getFullNumberWithPlus())) {
                return true;
            } else {
                Toast.makeText(Settings.this, "Invalid phone number ", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return false;
    }

    private boolean validateUsing_libphonenumber(String countryCode, String phNumber) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.createInstance(this);
        String isoCode = phoneNumberUtil.getRegionCodeForCountryCode(Integer.parseInt(countryCode));
        Phonenumber.PhoneNumber phoneNumber = null;
        try {
            phoneNumber = phoneNumberUtil.parse(phNumber, isoCode);
        } catch (NumberParseException e) {
            System.err.println(e);
        }
        if (phoneNumber != null) {
            boolean isValid = phoneNumberUtil.isValidNumber(phoneNumber);
            if (isValid) {
                String internationalFormat = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                //Toast.makeText(this, "Phone Number is Valid " + internationalFormat, Toast.LENGTH_LONG).show();
                return true;
            } else {
                //Toast.makeText(this, "Phone Number is Invalid " + phoneNumber, Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            //Toast.makeText(this, "Phone Number is Invalid " + phoneNumber, Toast.LENGTH_LONG).show();
            return false;
        }
    }

}