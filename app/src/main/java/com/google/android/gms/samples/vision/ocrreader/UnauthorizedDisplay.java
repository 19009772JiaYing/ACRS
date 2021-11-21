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


public class UnauthorizedDisplay extends AppCompatActivity {

    TextView LicensePLate;
    DatabaseReference databaseUnauthLog;

    Calendar calendar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unauthorize_display);

        databaseUnauthLog = FirebaseDatabase.getInstance().getReference("UnauthorizedLog");

        LicensePLate = findViewById(R.id.unlpInput);

        showunUnauthorizedP();

        this.findViewById(R.id.doneBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addUnauthlog();
                Intent intent = new Intent(getApplicationContext(), ScanActivity.class);
                startActivity(intent);
            }
        });


    }
    //After authenticating the license plate and it is un-registered it will display the un-authorized screen
    private void showunUnauthorizedP(){
        Intent intent = getIntent();
        String unAuthorizedLP = intent.getStringExtra("unAuthorizedLP").toUpperCase();
        LicensePLate.setText(unAuthorizedLP);
    }

    //After displaying un-authorized screen it will add the scanned license plate to the database
    private void addUnauthlog(){
        Intent intent = getIntent();
        String unAuthorizedLP = intent.getStringExtra("unAuthorizedLP").toUpperCase();

        //Get the current date
        calendar = Calendar.getInstance();

        //Change the format of the date
        SimpleDateFormat cdformat = new SimpleDateFormat("dd MMMM yyyy");
        String cd = cdformat.format(calendar.getTime());

        //Change the format of the time
        SimpleDateFormat ctformat = new SimpleDateFormat("HH:mm:ss");
        String ct = ctformat.format(calendar.getTime());

        String id =  databaseUnauthLog.push().getKey();
        UnauthorizedLog unauthorizedLog = new UnauthorizedLog(id,unAuthorizedLP,cd, ct);
        databaseUnauthLog.child(id).setValue(unauthorizedLog);
        if (id.equalsIgnoreCase("")){
            Toast.makeText(getApplicationContext(), "Unable To Save Vehicle Entry", Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "Entry Recorded", Toast.LENGTH_SHORT).show();
        }
    }
}

