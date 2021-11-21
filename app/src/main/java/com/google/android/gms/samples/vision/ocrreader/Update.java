package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class Update extends AppCompatActivity {
    TextView getID;
    Button btnUpdt;
    EditText ETContact;
    EditText ETRole;
    EditText ETLicense;
    EditText ETCompany;

    String memContact, memRole, memLicense, memCom;

    DatabaseReference ref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_log);

        ref = FirebaseDatabase.getInstance().getReference("Members");

        btnUpdt = (Button) findViewById(R.id.btnUpdt);
        ETContact = (EditText) findViewById(R.id.ETContact);
        ETRole = (EditText) findViewById(R.id.ETRole);
        ETLicense = (EditText) findViewById(R.id.ETLicense);
        ETCompany = (EditText) findViewById(R.id.ETCompany);
        showMemInfo();


        btnUpdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update();
            }
        });
    }

    // Create a regular expression to match local (Singapore based) contact numbers
    // Singapore registered numbers can only start with 8 or 9
    // The length of the contact number should only be 8
    public final static boolean isValidContact(String number){
        return Pattern.compile("^[89]\\d{7}$").matcher(number).matches();
    }
    // Create a regular expression to check if the pattern of the license plate is valid
    // Can start from any alphabet from A-Z (Can be either upper or lower case)
    // Followed by 3-4 numbers
    // Lastly, ends with a alphabet
    public final static boolean isValidCarplate(String license){
        return Pattern.compile("[A-Za-z]{3}[\\d]{3,4}[A-Za-z]{1}").matcher(license).matches();
    }



    private void update() {
        //Get the user input
        Intent i = getIntent();
        String getid = i.getStringExtra("getid");
        String upContact = ETContact.getText().toString().trim();
        String upRole = ETRole.getText().toString().trim();
        String upLicense = ETLicense.getText().toString().trim();
        String upCompany = ETCompany.getText().toString().trim();

        //Check if any fields are empty
        //If all condition are met it will update the user information according to the user input
        if (!TextUtils.isEmpty(upContact) && !TextUtils.isEmpty(upRole) && !TextUtils.isEmpty(upLicense) && !TextUtils.isEmpty(upCompany)) {
            if (isValidContact(upContact) == true) {
                if (isValidCarplate(upLicense) == true) {
                    ref.child(getid).child("memContact").setValue(upContact);
                    ref.child(getid).child("memRole").setValue(upRole);
                    ref.child(getid).child("memLicense").setValue(upLicense);
                    ref.child(getid).child("memCompany").setValue(upCompany);

                    Toast.makeText(this, "Member Updated", Toast.LENGTH_SHORT).show();
                    Intent x = new Intent(getApplicationContext(), AddMember.class);
                    startActivity(x);

                } else {
                    Toast.makeText(this, "Invalid Car-Plate", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(Update.this, "Please Enter All Fields", Toast.LENGTH_SHORT).show();
        }
    }


    //Show the current information of the user selected by the user before updating.
    private void showMemInfo(){
        Intent i = getIntent();
        String getContact = i.getStringExtra("getcontact");
        String getrole = i.getStringExtra("getrole");
        String getLP = i.getStringExtra("getLP");
        String getcompany = i.getStringExtra("getcompany");

        ETContact.setText(getContact);
        ETRole.setText(getrole);
        ETLicense.setText(getLP);
        ETCompany.setText(getcompany);
    }
}