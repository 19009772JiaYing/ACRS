package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SecurityHome extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewACRs;
    private Button buttonCamera;
    private Button buttonLogOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.security_home);

        getSupportActionBar().setTitle("ACRS");

        textViewACRs = (TextView) findViewById(R.id.textViewACRs);
        buttonCamera = (Button) findViewById(R.id.buttonCamera);
        buttonLogOut = (Button) findViewById(R.id.buttonLogOut);

        buttonCamera.setOnClickListener(this);
        buttonLogOut.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == buttonLogOut){
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(),LogIn.class));
            finish();
        }

        if (view == buttonCamera){
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(),ScanActivity.class));
            finish();
        }

    }

}