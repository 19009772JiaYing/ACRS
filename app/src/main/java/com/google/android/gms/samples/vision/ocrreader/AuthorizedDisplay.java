package com.google.android.gms.samples.vision.ocrreader;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;


public class AuthorizedDisplay extends AppCompatActivity {

    TextView carOwner, licensePlate, designation;
    DatabaseReference databaseAuthLog;
    Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorize_display);

        databaseAuthLog = FirebaseDatabase.getInstance().getReference("AuthorizedLog");

        carOwner = findViewById(R.id.nameInput);
        licensePlate = findViewById(R.id.lpInput);
        designation = findViewById(R.id.desigInput);
        showAllUserData();

        //When done button is clicked go the ScanActivity class
        this.findViewById(R.id.doneBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addAuthlog();
                Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
                startActivity(intent);
            }
        });

    }


    //After authenticating the Vehicle license plate it will display the information about the owner retrieved from Firebase
    private void showAllUserData(){
        Intent intent = getIntent();
        String carOwnerName = intent.getStringExtra("memName");
        String carOwnerLP = intent.getStringExtra("memLicense").toUpperCase();
        String carOwnerDesignation = intent.getStringExtra("memRole");

        carOwner.setText(carOwnerName);
        licensePlate.setText(carOwnerLP);
        designation.setText(carOwnerDesignation);
    }


    //After authenticating the license plate it will create a log of the vehicle entry to the Firebase
    private void addAuthlog(){
        Intent intent = getIntent();
        String carOwnerName = intent.getStringExtra("memName");
        String carOwnerLP = intent.getStringExtra("memLicense").toUpperCase();
        String carOwnerDesignation = intent.getStringExtra("memRole");

        calendar = Calendar.getInstance();

        SimpleDateFormat cdformat = new SimpleDateFormat("dd MMMM yyyy");
        String cd = cdformat.format(calendar.getTime());

        SimpleDateFormat ctformat = new SimpleDateFormat("HH:mm:ss");
        String ct = ctformat.format(calendar.getTime());

        String id =  databaseAuthLog.push().getKey();
        AuthorizedLog authorizedLog = new AuthorizedLog(id, carOwnerName,carOwnerLP, carOwnerDesignation,cd, ct);
        databaseAuthLog.child(id).setValue(authorizedLog);

        if (id.equalsIgnoreCase("")){
            Toast.makeText(getApplicationContext(), "Unable To Save Vehicle Entry", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "Entry Recorded", Toast.LENGTH_SHORT).show();
        }

    }
}

