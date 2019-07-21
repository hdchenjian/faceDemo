package com.example.luyao.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.iim.recognition.caffe.LoadLibraryModule;

public class RegistrationActivity extends AppCompatActivity {

    private final static String TAG = RegistrationActivity.class.getCanonicalName();
    private static final int REQUEST_Taken_Photo = 4;

    private EditText user_name;
    private Button button_registration;
    private Button button_take_photo;
    private ImageView image_registration;
    private LoadLibraryModule loadLibraryModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("返回");
        }

        user_name = findViewById(R.id.user_name);
        button_registration = findViewById(R.id.button_registration);
        button_take_photo = findViewById(R.id.button_take_photo);
        image_registration = findViewById(R.id.image_registration);

        button_registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login");
                button_registration.setEnabled(false);
                String user_name_str = user_name.getText().toString();
                if(user_name_str.equals("")) {
                    toast("请填写用户姓名");
                    button_registration.setEnabled(true);
                    return;
                }
                Log.d(TAG, "Login: " + user_name_str + " ");
            }
        });

        button_take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login");
                button_take_photo.setEnabled(false);
                start_take_photo();
                Log.d(TAG, "Login: " + " ");
            }
        });

        if (loadLibraryModule == null) {
            loadLibraryModule = LoadLibraryModule.getInstance();
            loadLibraryModule.recognition_start();
        }
    }
    private void start_take_photo() {
        Intent intent = new Intent(this, TakePhotoActivity.class);
        startActivityForResult(intent, REQUEST_Taken_Photo);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_Taken_Photo) {
            if (resultCode == RESULT_OK) {
                String user_name = data.getStringExtra("user_name");
                byte[] bitmap_photo = data.getByteArrayExtra("photo");

                int feature_length = 512;
                int[] face_region = new int[4];
                float[] feature = new float[feature_length];
                long[] code_ret = new long[1];
                int face_count = loadLibraryModule.recognition_face(bitmap_photo, face_region, feature, code_ret);
                if(face_count != 1 || code_ret[0] != 1000){
                    toast("注册失败");
                } else {
                    toast("注册成功");
                }
            }
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

    protected void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
