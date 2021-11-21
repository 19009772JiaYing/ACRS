package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class AddVehicle extends AppCompatActivity {

    EditText ETVehLicense;
    EditText ETVehBrand;
    EditText ETVehModel;

    Button btnAddVeh;
    Button btnBack;

    ListView listViewVeh;

    DatabaseReference databaseVehicle;

    List<Vehicles> vehicles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_vehicle);
        // Refer to add_vehicle layout class for the ID for each item
        ETVehLicense = (EditText) findViewById(R.id.vehLicense);
        ETVehBrand = (EditText) findViewById(R.id.vehBrand);
        ETVehModel = (EditText) findViewById(R.id.vehModel);

        btnAddVeh = (Button) findViewById(R.id.btnAddVeh);
        btnBack = (Button) findViewById(R.id.btnBack);

        // ListView to store every vehicle that is being added
        listViewVeh = (ListView) findViewById(R.id.listViewVeh);
        Intent i = getIntent();
        String getid = i.getStringExtra("getid");

        vehicles = new ArrayList<>();



        // Making reference against Firebase to get the path
        databaseVehicle = FirebaseDatabase.getInstance().getReference("vehicles").child(getid);

        //Go to the addmember class when back button is clicked
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homepage = new Intent(getApplicationContext(), AddMember.class);
                startActivity(homepage);
            }
        });

        //When add vehicle button is click execute the saveVehicle();
        btnAddVeh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveVehicle();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Add the existing vehicle data, retrieve from Firebase to the list
        databaseVehicle.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                vehicles.clear();

                for (DataSnapshot vehicleSnapshot : dataSnapshot.getChildren()){
                    Vehicles vehicle = vehicleSnapshot.getValue(Vehicles.class);
                    vehicles.add(vehicle);
                }

                VehicleList vehicleListAdapter = new VehicleList(AddVehicle.this, vehicles);
                listViewVeh.setAdapter(vehicleListAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void saveVehicle() {
        // This method is to verify if details entered by the user are valid or invalid
        // Every of the vehicle added, Firebase will generate and ID
        String vehLicense = ETVehLicense.getText().toString();
        String vehBrand = ETVehBrand.getText().toString();
        String vehModel = ETVehModel.getText().toString();
        if (!TextUtils.isEmpty(vehLicense) && !TextUtils.isEmpty(vehBrand) && !TextUtils.isEmpty(vehModel)){
            if (isValidCarplate(vehLicense) == true) {
                String id = databaseVehicle.push().getKey();
                Vehicles vehicles = new Vehicles(id, vehLicense, vehBrand, vehModel);
                databaseVehicle.child(id).setValue(vehicles);
                Toast.makeText(this, "Vehicle Information saved successfully", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Invalid Car-Plate", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(this, "All Fields Should Be Filled Up", Toast.LENGTH_SHORT).show();
        }
    }
    public final static boolean isValidCarplate(String license) {
        // Create a regular expression to check if the pattern of the license plate is valid
        // Can start from any alphabet from A-Z (Can be either upper or lower case)
        // Followed by 3-4 numbers
        // Lastly, ends with a alphabet
        return Pattern.compile("[A-Za-z]{3}[\\d]{3,4}[A-Za-z]{1}").matcher(license).matches();
    }
}