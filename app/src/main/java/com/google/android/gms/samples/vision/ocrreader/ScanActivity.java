package com.google.android.gms.samples.vision.ocrreader;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class ScanActivity extends AppCompatActivity {

    private ImageView iv2, iv3;
    private Button scanButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_homepage);

        iv2 = (ImageView) findViewById(R.id.imageView2);
        iv3 = (ImageView) findViewById(R.id.imageView3);
        scanButton = (Button) findViewById(R.id.scanBtn);
    }

    public void btn_scan(View v) {
        Intent i = new Intent(ScanActivity.this, OcrCaptureActivity.class);
        startActivity(i);
    }
    public void sg_btnhome(View v) {
        Intent i = new Intent(ScanActivity.this, SecurityHome.class);
        startActivity(i);
    }
}
