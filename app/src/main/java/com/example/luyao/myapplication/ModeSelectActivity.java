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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
                Intent resultData = new Intent();
                resultData.putExtra("mode", "0");
                setResult(Activity.RESULT_OK, resultData);
                finish();
            }
        });

        button_mode_recognition = findViewById(R.id.button_mode_recognition);
        button_mode_recognition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button_mode_recognition.setEnabled(false);
                Intent resultData = new Intent();
                resultData.putExtra("mode", "1");
                setResult(Activity.RESULT_OK, resultData);
                finish();
            }
        });
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
