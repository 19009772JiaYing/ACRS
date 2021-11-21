package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class Admin extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewACRs;
    private Button buttonMemberList;
    private Button buttonLogOut;
    private Button buttonExcelUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin);

        getSupportActionBar().setTitle("ACRS");

        textViewACRs = (TextView) findViewById(R.id.textViewACRs);
        buttonMemberList = (Button) findViewById(R.id.buttonMemberList);
        buttonLogOut = (Button) findViewById(R.id.buttonLogOut);
        buttonExcelUpload = (Button) findViewById(R.id.buttonExcelUpload);

        buttonMemberList.setOnClickListener(this);
        buttonMemberList.setOnClickListener(this);

    }

    //When sign out button is click go to the Login page
    public void logoutAdmin(View view){
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(getApplicationContext(),LogIn.class));
            finish();
    }

    //When buttonMemberList is clicked it will run the AddMember class
    @Override
    public void onClick(View v) {
        if (v == buttonMemberList){
            startActivity(new Intent(getApplicationContext(),AddMember.class));
            finish();
        }
        if (v == buttonExcelUpload){
            startActivity(new Intent(getApplicationContext(),AddMember.class));
            finish();
        }
    }
}