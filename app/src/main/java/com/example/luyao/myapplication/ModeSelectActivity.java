package com.example.luyao.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class ModeSelectActivity extends AppCompatActivity {

    private final static String TAG = ModeSelectActivity.class.getCanonicalName();
    private Button button_mode_manager;
    private Button button_mode_recognition;
    private Button button_exit;

    private static final int REQUEST_Recognition = 2;
    private static final int REQUEST_Manager = 3;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void start_recognition(){
        Intent intent = new Intent(this, RecognitionActivity.class);
        startActivityForResult(intent, REQUEST_Recognition);
    }

    private void start_manager(){
        Intent intent = new Intent(this, ManagerActivity.class);
        startActivityForResult(intent, REQUEST_Manager);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("返回");
        }

        button_mode_manager = findViewById(R.id.button_mode_manager);
        button_mode_manager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_mode_manager.setEnabled(false);
                start_manager();
            }
        });

        button_mode_recognition = findViewById(R.id.button_mode_recognition);
        button_mode_recognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_mode_recognition.setEnabled(false);
                start_recognition();
            }
        });

        button_exit = findViewById(R.id.button_exit);
        button_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_exit.setEnabled(false);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_Recognition) {
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "Recognition finish");
            }
            button_mode_recognition.setEnabled(true);
        } else if (requestCode == REQUEST_Manager) {
            if (resultCode == RESULT_OK) {
                Log.e(TAG, "Manager finish");
            }
            button_mode_manager.setEnabled(true);
        } else {
            Log.e(TAG, "Unknow error");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "lifecycle: onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "lifecycle: onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "lifecycle: onStop");
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "lifecycle: onDestroy");
        super.onDestroy();
    }
}
