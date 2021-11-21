package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LogIn extends AppCompatActivity {

    EditText email,password;
    Button loginBtn,gotoRegister,forgetPW;
    boolean valid = true;
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        getSupportActionBar().setTitle("ACRS");

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        email = findViewById(R.id.loginEmail);
        password = findViewById(R.id.loginPassword);
        loginBtn = findViewById(R.id.loginBtn);
        gotoRegister = findViewById(R.id.gotoRegister);
        forgetPW = findViewById(R.id.forgetPW);

        //When login button is clicked it will check the user input against the verified information via database
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkField(email);
                checkField(password);
                Log.d("TAG","OnClick: " + email.getText().toString());

                if(valid){
                    fAuth.signInWithEmailAndPassword(email.getText().toString(),password.getText().toString()).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            Toast.makeText(LogIn.this, "Successful", Toast.LENGTH_SHORT).show();
                            checkUserAccessLevel(authResult.getUser().getUid());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LogIn.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        checkField(email);
        checkField(password);

        //When register button is clicked it will run the register class.
        gotoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Register.class));
            }
        });


        //When the suer clicked on the forget password it will run the ForgetPW class
        forgetPW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),ForgetPW.class));
            }
        });

    }

    //This method is to check if the user is Admin OR security guard
    private void checkUserAccessLevel(String uid) {
        DocumentReference df = fStore.collection("Users").document(uid);
        df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Log.d("TAG","onSuccess: " + documentSnapshot.getData());

                if (documentSnapshot.getString("isStaff") != null){
                    startActivity(new Intent(getApplicationContext(),Admin.class));
                    finish();
                }

                if (documentSnapshot.getString("isSecurityGuard") != null){
                    startActivity(new Intent(getApplicationContext(), SecurityHome.class));
                    finish();
                }
            }
        });
    }

    public void login_home(View v)
    {
        Intent i = new Intent(LogIn.this,RegisterOrLogin.class);
        startActivity(i);
    }

    //Check if they are any empty field
    public boolean checkField(EditText textField){
        if(textField.getText().toString().isEmpty()){
            textField.setError("Empty");
            valid = false;
        }else {
            valid = true;
        }

        return valid;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            DocumentReference df = FirebaseFirestore.getInstance().collection("Users").document(FirebaseAuth.getInstance().getUid());
            df.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    //When the user login successfully and the the access level is admin it will go to the Admin class
                    if (documentSnapshot.getString("isStaff") != null){
                        startActivity(new Intent(getApplicationContext(), Admin.class));
                        finish();
                    }
                    //When the user login successfully and the the access level is security guard it will go to the SecurityHome class
                    if (documentSnapshot.getString("isSecurityGuard") != null){
                        startActivity(new Intent(getApplicationContext(), SecurityHome.class));
                        finish();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                //When login fail it will go back to the Login Class
                public void onFailure(@NonNull Exception e) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(getApplicationContext(),LogIn.class));
                    finish();
                }
            });
        }
    }
}