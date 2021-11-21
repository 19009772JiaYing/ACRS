package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Pattern;

public class AddMember extends AppCompatActivity {

    public static final String MEM_NAME = "memberName";
    public static final String MEM_ID = "memberId";

    EditText editTextName;
    EditText editTextContact;
    EditText editTextRole;
    EditText editTextLicense;
    EditText editTextCom;
    Button btnAdd,btnHome;
    private static final String TAG = AddMember.class.getSimpleName();
    DatabaseReference dbMem;
    String verifyLP = "";
    // String verifyCon = "";

    DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child("Members");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addmem_page);
        dbMem = FirebaseDatabase.getInstance().getReference("Members");
        editTextName = (EditText) findViewById(R.id.mem_name);
        editTextContact = (EditText) findViewById(R.id.mem_contact);
        editTextRole = (EditText) findViewById(R.id.mem_role);
        editTextLicense = (EditText) findViewById(R.id.mem_license);
        editTextCom = (EditText) findViewById(R.id.mem_company);
        btnAdd = (Button) findViewById(R.id.addbtn);
        btnHome = (Button) findViewById(R.id.admin_home);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // read user inputs
                String name = editTextName.getText().toString();
                String contact = editTextContact.getText().toString().trim();
                String role = editTextRole.getText().toString();
                String license = editTextLicense.getText().toString().toUpperCase().trim();
                String company = editTextCom.getText().toString();

                // methods to read user data to verify from firebase
                // In this method, we can check if the data entered by the user are valid or invalid
                // Messages will be displayed accordingly to how data is entered
                checklp(license);
                if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(contact) && !TextUtils.isEmpty(role) && !TextUtils.isEmpty(license) && !TextUtils.isEmpty(company)) {
                    if (isValidCarplate(license) == true){
                        if (!verifyLP.equalsIgnoreCase(license)){
                            //  if (!verifyCon.equalsIgnoreCase(contact)){
                            if (isValidContact(contact) == true){
                                String id = dbMem.push().getKey();
                                Members members = new Members(id, name, contact, role, license, company);
                                dbMem.child(id).setValue(members);
                                Toast.makeText(AddMember.this, "Member Data Added", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(AddMember.this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else {
                            Toast.makeText(AddMember.this, "License Plate Existed", Toast.LENGTH_SHORT).show();
                        }
                    }
//                    else {
//                        Toast.makeText(AddMember.this, "Contact existed", Toast.LENGTH_SHORT).show();
//                    }
//                }
                    else {
                        Toast.makeText(AddMember.this, "Invalid Car-Plate", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(AddMember.this, "Please Enter All Fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // When user clicked on the retrieve button, it will direct them to the memberList class
        this.findViewById(R.id.retrievebtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), memberList.class);
                startActivity(i);
            }
        });
        // When user clicked on admin_home icon, it will direct them to the Admin class
        this.findViewById(R.id.admin_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), Admin.class);
                startActivity(i);
            }
        });
    }

//    private void checkContact(String contact) {
//        Query checkCon = reference.orderByChild("memContact").equalTo(contact);
//        checkCon.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    for (DataSnapshot userCon : snapshot.getChildren()) {
//                          verifyCon = userCon.child("memContact").getValue(String.class);
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//            }
//        });
//    }

    private void checklp(String license) {
        // This method is to check the license plate entered by the user is equal to the one stored in Firebase
        Query checkLP = reference.orderByChild("memLicense").equalTo(license);
        checkLP.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        Members mem = userSnapshot.getValue(Members.class);
                        verifyLP = mem.getMemLicense();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    // Create a regular expression to match local (Singapore based) contact numbers
    // Singapore registered numbers can only start with 8 or 9
    // The length of the contact number should only be 8
    public final static boolean isValidContact(String number) {
        return Pattern.compile("^[89]\\d{7}$").matcher(number).matches();
    }
    // Create a regular expression to check if the pattern of the license plate is valid
    // Can start from any alphabet from A-Z (Can be either upper or lower case)
    // Followed by 3-4 numbers
    // Lastly, ends with a alphabet
    public final static boolean isValidCarplate(String license) {
        return Pattern.compile("[A-Za-z]{3}[\\d]{3,4}[A-Za-z]{1}").matcher(license).matches();
    }
}



