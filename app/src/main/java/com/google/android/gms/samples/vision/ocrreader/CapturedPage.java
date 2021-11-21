package com.google.android.gms.samples.vision.ocrreader;

import android.app.ProgressDialog;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.firebase.client.Firebase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

public class CapturedPage extends AppCompatActivity {

    EditText capturedLicensePlate;
    ProgressDialog progressDialog;
    String LicensePlate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.captured_lp);
        capturedLicensePlate =(EditText) findViewById(R.id.capturedLP);
        progressDialog = new ProgressDialog(this);



        Intent intent2 = getIntent();
        String receive = intent2.getStringExtra("getLP");
        capturedLicensePlate.setText(receive);
        Firebase.setAndroidContext(this);

    }
    public final static boolean isValidCarplate(String license){
        return Pattern.compile("[A-Za-z]{3}[\\d]{3,4}[A-Za-z]{1}").matcher(license).matches();
    }

    public void btn_fetch(View v) {
        LicensePlate = capturedLicensePlate.getText().toString().toUpperCase().trim();
        if(!TextUtils.isEmpty(LicensePlate)){
            if(isValidCarplate(LicensePlate) == true){
                isAuthorized();
            }
            else{
                capturedLicensePlate.setError("Invalid License Plate Please Scan Again");
            }
        }
        else{
            capturedLicensePlate.setError("License Plate Missing");
        }
    }

    public void btn_home(View v)
    {
        Intent i = new Intent(CapturedPage.this, SecurityHome.class);
        startActivity(i);
    }

    public void btn_scanagain(View v)
    {
        Intent i = new Intent(CapturedPage.this, ScanActivity.class);
        startActivity(i);
    }


    private void isAuthorized() {
        LicensePlate = capturedLicensePlate.getText().toString().toUpperCase().trim();
        if(LicensePlate.equalsIgnoreCase("")){
            capturedLicensePlate.setError("License Plate Missing");
            capturedLicensePlate.requestFocus();
        }
        else{
            progressDialog.setMessage("Authenticating");
            progressDialog.show();

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Members");
            Query checkLP = reference.orderByChild("memLicense").equalTo(LicensePlate);
            checkLP.addListenerForSingleValueEvent (new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){

                        String carownerfromDB = "";
                        String LPfromDB = "";
                        String desigfromDB = "";
                        for (DataSnapshot userSnapshot: dataSnapshot.getChildren()) {
                            carownerfromDB = userSnapshot.child("memName").getValue(String.class);
                            LPfromDB = userSnapshot.child("memLicense").getValue(String.class);
                            desigfromDB = userSnapshot.child("memRole").getValue(String.class);

                        }

                        Intent intent = new Intent(getApplicationContext(), AuthorizedDisplay.class);
                        intent.putExtra("memName",carownerfromDB);
                        intent.putExtra("memLicense",LPfromDB);
                        intent.putExtra("memRole",desigfromDB);

                        startActivity(intent);
                    }
                    else{
                        Intent i = new Intent(getApplicationContext(), UnauthorizedDisplay.class);
                        i.putExtra("unAuthorizedLP",LicensePlate);
                        startActivity(i);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }
}
