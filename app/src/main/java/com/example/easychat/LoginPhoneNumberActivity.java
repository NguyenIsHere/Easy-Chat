package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.hbb20.CountryCodePicker;

public class LoginPhoneNumberActivity extends AppCompatActivity {

    CountryCodePicker countryCodePicker;
    EditText phoneInput;
    Button sendOtpBtn;
    ProgressBar progressBar;

    public static Intent newIntent(SplashActivity splashActivity) {
        return new Intent(splashActivity, LoginPhoneNumberActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_phone_number);

        countryCodePicker = findViewById(R.id.login_countrycode);
        phoneInput = findViewById(R.id.login_mobile_number);
        sendOtpBtn = findViewById(R.id.send_otp_btn);
        progressBar = findViewById(R.id.login_progress_bar);

        progressBar.setVisibility(ProgressBar.GONE);

        countryCodePicker.registerCarrierNumberEditText(phoneInput);
        sendOtpBtn.setOnClickListener(v -> {
            if (!countryCodePicker.isValidFullNumber()) {
                phoneInput.setError("Invalid phone number");
                phoneInput.requestFocus();
                return;
            }

            Intent intent = new Intent(LoginPhoneNumberActivity.this, LoginOtpActivity.class);
            intent.putExtra("phone", countryCodePicker.getFullNumberWithPlus().replace(" ", ""));
            startActivity(intent);
        });
    }
}