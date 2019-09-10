package com.example.luyao.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.iim.recognition.caffe.LoadLibraryModule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RegistrationActivity extends AppCompatActivity {

    private final static String TAG = RegistrationActivity.class.getCanonicalName();
    private static final int REQUEST_Taken_Photo = 4;

    private EditText user_name;
    private Button button_registration;
    private Button button_take_photo;
    private ImageView image_registration;
    private LoadLibraryModule loadLibraryModule;
    Bitmap bitmap_photo = null;
    int[] bitmap_photo_data;
    private UserFeatureDB userFeatureDB;

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

        userFeatureDB = new UserFeatureDB(this);
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
                if(bitmap_photo == null){
                    toast("请拍摄一张照片");
                    button_registration.setEnabled(true);
                    return;
                }
                Log.d(TAG, "Login: " + user_name_str + " ");
                registration_image(user_name_str);
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
            //loadLibraryModule.recognition_start();
        }
    }

    private void registration_image(String user_name){
        int feature_length = 512;
        int[] face_region = new int[4];
        float[] feature = new float[feature_length];
        long[] code_ret = new long[1];

        byte[] byteArray = loadLibraryModule.bitmap2rgb_native(bitmap_photo_data);
        /*
        try {
            FileOutputStream out = new FileOutputStream("/sdcard/A/bitmap1.png");
            bitmap_photo.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        }catch (IOException e) {
            e.printStackTrace();
        }*/
        int face_count = loadLibraryModule.recognition_face(byteArray, face_region, feature, code_ret,
                bitmap_photo.getWidth(), bitmap_photo.getHeight());
        if(face_count != 1 || code_ret[0] != 1000){
            toast("注册失败");
        } else {
            toast("注册成功");
            String feature_str = "";
            for(int kk = 0; kk < feature_length; kk++){
                feature_str += String.valueOf(feature[kk]) +",";
            }
            int max_id = userFeatureDB.queryMaxId();
            userFeatureDB.addUserFeature(max_id + 1, feature_str, "", user_name);
        }
        button_registration.setEnabled(true);
        bitmap_photo = null;
        image_registration.setImageResource(android.R.color.transparent);
    }

    private void start_take_photo() {
        Intent intent = new Intent(this, TakePhotoActivity.class);
        startActivityForResult(intent, REQUEST_Taken_Photo);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_Taken_Photo) {
            if (resultCode == RESULT_OK) {
                bitmap_photo = GlobalParameter.getRegistration_image_bitmap();
                bitmap_photo_data = GlobalParameter.getRegistration_image();
                image_registration.setImageBitmap(bitmap_photo);
            }
            button_take_photo.setEnabled(true);
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
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
