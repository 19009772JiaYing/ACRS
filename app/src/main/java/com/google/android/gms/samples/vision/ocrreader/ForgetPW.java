package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgetPW extends AppCompatActivity implements View.OnClickListener {

    private EditText editTextEmail;
    private Button buttonResetPW;
    private TextView textViewForgetPW;
    private ProgressBar progressBar;
    private TextView textViewLogIn;

    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forget_pw);

        getSupportActionBar().setTitle("ACRS");

        editTextEmail = (EditText) findViewById(R.id.editTextEmail);
        buttonResetPW = (Button) findViewById(R.id.buttonResetPW);
        textViewForgetPW = (TextView) findViewById(R.id.textViewForgetPW);
        progressBar = findViewById(R.id.progressBar);
        textViewLogIn = (TextView) findViewById(R.id.textViewLogIn);

        firebaseAuth = FirebaseAuth.getInstance();

        textViewLogIn.setOnClickListener(this);
        buttonResetPW.setOnClickListener(new View.OnClickListener() {
            @Override

            //When the user clicked on forget password it will send email to the user, so that they can reset their password
            public void onClick(View view) {
                progressBar.setVisibility(view.VISIBLE);
                firebaseAuth.sendPasswordResetEmail(editTextEmail.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(view.GONE);
                        if (task.isSuccessful()){
                            Toast.makeText(ForgetPW.this, "Password Send To Your Email", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ForgetPW.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    //When login button iis click it will run the LogIn class
    @Override
    public void onClick(View view) {
        if (view == textViewLogIn) {
            finish();
            startActivity(new Intent(this, LogIn.class));
        }
    }
}