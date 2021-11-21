package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterOrLogin extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewACRs;
    private Button buttonRegister;
    private Button buttonLogIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_or_login);

        getSupportActionBar().setTitle("ACRS");

        textViewACRs = (TextView) findViewById(R.id.textViewACRs);
        buttonRegister = (Button) findViewById(R.id.buttonRegister);
        buttonLogIn = (Button) findViewById(R.id.buttonLogIn);

        buttonRegister.setOnClickListener(this);
        buttonLogIn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonRegister) {
            finish();
            startActivity(new Intent(this, Register.class));
        }

        if (view == buttonLogIn) {
            finish();
            startActivity(new Intent(this, LogIn.class));
        }
    }
}