package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Register extends AppCompatActivity {


    EditText fullName, email, password, phone;
    Button registerBtn, goToLogin;
    boolean valid = true;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        getSupportActionBar().setTitle("ACRS");

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        fullName = findViewById(R.id.registerName);
        email = findViewById(R.id.registerEmail);
        password = findViewById(R.id.registerPassword);
        phone = findViewById(R.id.registerPhone);
        registerBtn = findViewById(R.id.registerBtn);
        goToLogin = findViewById(R.id.gotoLogin);


        //When register button is clicked it will check if they are any empty fields
        registerBtn.setOnClickListener(view -> {
            checkField(fullName);
            checkField(email);
            checkField(password);
            checkField(phone);


            if (valid) {
                String emailInput = email.getText().toString().trim();
                String passwordInput = password.getText().toString().trim();
                String contactInput = phone.getText().toString().trim();


                //If all the conditions is met it will add the user to the database else there will be an error message prompt to the user
                if (!TextUtils.isEmpty(emailInput) && !TextUtils.isEmpty(passwordInput)) {
                    if (isValidPwd(passwordInput) == true) {
                        if (isValidContact(contactInput) == true){
                            if (Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()){
                                fAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnSuccessListener(authResult -> {
                                    FirebaseUser user = fAuth.getCurrentUser();
                                    Toast.makeText(Register.this, "Account created", Toast.LENGTH_SHORT).show();
                                    DocumentReference df = fStore.collection("Users").document(user.getUid());
                                    Map<String, Object> userInfo = new HashMap<>();
                                    userInfo.put("FullName", fullName.getText().toString());
                                    userInfo.put("UserEmail", email.getText().toString());
                                    userInfo.put("PhoneNumber", phone.getText().toString());
                                    userInfo.put("isSecurityGuard", "1");

                                    df.set(userInfo);
                                    startActivity(new Intent(getApplicationContext(), SecurityHome.class));
                                    finish();

                                }).addOnFailureListener(e -> Toast.makeText(Register.this, "Failed To Create Account", Toast.LENGTH_SHORT).show());
                            }
                            else{
                                Toast.makeText(Register.this, "Please Enter A Valid Email Address", Toast.LENGTH_SHORT).show();
                            }

                        }
                        else{
                            Toast.makeText(Register.this, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(Register.this, "Password Too Weak", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(Register.this, "Fields Cannot Be Empty", Toast.LENGTH_SHORT).show();
                }
            }
            });

        goToLogin.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), LogIn.class)));
    }

    //Check if the fields are empty
    public boolean checkField(EditText textField) {
        if (textField.getText().toString().isEmpty()) {
            textField.setError("Empty");
            valid = false;
        } else {
            valid = true;
        }
        return valid;
    }

    public final static boolean isValidPwd(String pwd) {

//        ^                 # start-of-string
//        (?=.*[0-9])       # a digit must occur at least once
//        (?=.*[a-z])       # a lower case letter must occur at least once
//        (?=.*[A-Z])       # an upper case letter must occur at least once
//        (?=.*[@#$%^&+=])  # a special character must occur at least once you can replace with your special characters
//        (?=\\S+$)         # no whitespace allowed in the entire string
//        .{4,}             # anything, at least six places though
//        $                 # end-of-string

        return Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+!_=])(?=\\S+$).{4,}$").matcher(pwd).matches();
    }

    public final static boolean isValidContact(String number) {
        return Pattern.compile("^[89]\\d{7}$").matcher(number).matches();
    }


}
