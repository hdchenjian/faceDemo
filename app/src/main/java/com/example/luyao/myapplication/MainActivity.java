package com.example.luyao.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import com.iim.recognition.caffe.LoadLibraryModule;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private LoadLibraryModule loadLibraryModule;
    private final static String TAG = MainActivity.class.getCanonicalName();
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int RC_HANDLE_READ_EXTERNAL_STORAGE = 2;
    private static final int RC_HANDLE_WRITE_EXTERNAL_STORAGE = 3;
    private static final int REQUEST_MODE_SELECT = 1;
    private static final int REQUEST_Recognition = 2;
    private static final int REQUEST_Manager = 3;

    private EditText user_phone_text;
    private EditText user_password_test;
    private Button button_login;
    private AppCompatCheckBox password_checkbox;
    private GlobalParameter globalParameter;

    protected void toast(final String msg) {
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        user_phone_text = findViewById(R.id.user_phone);
        user_password_test = findViewById(R.id.user_password);
        button_login = findViewById(R.id.button_login);

        globalParameter = new GlobalParameter();
        password_checkbox = (AppCompatCheckBox) findViewById(R.id.password_checkbox);
        password_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    if (!isChecked) {
                        user_password_test.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    } else {
                        user_password_test.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    }
                }
            });

        button_login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Login");
                    button_login.setEnabled(false);
                    initDnn();
                    String user_phone_str = user_phone_text.getText().toString();
                    String password_str = user_password_test.getText().toString();
                    if(user_phone_str.equals("") || password_str.equals("")) {
                        toast("请填写手机号和密码");
                        button_login.setEnabled(true);
                        return;
                    }
                    SharedPreferences user_phone_password = getSharedPreferences("user_phone_password", 0);
                    SharedPreferences.Editor user_phone_password_editor = user_phone_password.edit();
                    user_phone_password_editor.putString("phone", user_phone_str);
                    user_phone_password_editor.putString("password", password_str);
                    user_phone_password_editor.commit();
                    Log.d(TAG, "Login: " + user_phone_str + " " + password_str);
                    login(user_phone_str, Utils.md5(password_str));

                }
            });
        SharedPreferences user_phone_password = getSharedPreferences("user_phone_password", 0);
        String user_phone_str = user_phone_password.getString("phone", "");
        String user_password_str = user_phone_password.getString("password", "");
        user_phone_text.setText(user_phone_str);
        user_password_test.setText(user_password_str);
    }

    private void start_mode_select(){
        Intent intent = new Intent(this, ModeSelectActivity.class);
        startActivityForResult(intent, REQUEST_MODE_SELECT);
    }

    private void start_recognition(){
        Intent intent = new Intent(this, RecognitionActivity.class);
        intent.putExtra("organization_id", globalParameter.organization_id);
        startActivityForResult(intent, REQUEST_Recognition);
    }

    private void start_manager(){
        Intent intent = new Intent(this, ManagerActivity.class);
        intent.putExtra("organization_id", globalParameter.organization_id);
        startActivityForResult(intent, REQUEST_Manager);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_MODE_SELECT) {
            if (resultCode == RESULT_OK) {
                int mode = Integer.parseInt(data.getStringExtra("mode"));
                Log.e(TAG, "select mode: " + mode);
                if (mode == 0) {
                    start_manager();
                } else {
                    start_recognition();
                }
            }
        } else if (requestCode == REQUEST_Recognition) {
            Log.e(TAG, "Recognition finish");
        } else {
            Log.e(TAG, "Unknow error");
        }
    }

    private int login(String user_phone, String password) {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(SimpleHttpClient.BASE_URL).build();
        SimpleHttpClient.ServerAPI service = retrofit.create(SimpleHttpClient.ServerAPI.class);
        Call<ResponseBody> call = service.login(user_phone, password);
        call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    JSONObject responseJson = Utils.parseResponse(response, TAG);
                    if (response.code() == 200) {
                        Log.e(TAG, responseJson.optString("organization_id"));
                        globalParameter.organization_id = Integer.parseInt(
                                responseJson.optString("organization_id"));
                    } else {
                        toast("连接网络失败，请稍后再试");
                    }
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                button_login.setEnabled(true);
                                start_mode_select();
                            }
                        });
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    toast("连接网络失败，请检查您的网络");
                    t.printStackTrace();
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                button_login.setEnabled(true);
                            }
                        });
                }
            });
        return 0;
    }

    private void initDnn() {
        if (loadLibraryModule == null) {
            loadLibraryModule = LoadLibraryModule.getInstance();
            loadLibraryModule.recognition_start();
        }
    }

    private void getAllPermission(){
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Camera permission is not granted. Requesting permission");
            final String[] permissions = new String[]{Manifest.permission.CAMERA};
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM_RGB);
            return;
        }

        rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (rc != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_EXTERNAL_STORAGE permission not granted. Requesting permission");
            final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_READ_EXTERNAL_STORAGE);
            return;
        }

        rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (rc != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "WRITE_EXTERNAL_STORAGE permission not granted. Requesting permission");
            final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_WRITE_EXTERNAL_STORAGE);
            return;
        }
        initDnn();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
        getAllPermission();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
                Log.e(TAG, "Get CAMERA_PERM permission");
            } else if(requestCode == RC_HANDLE_READ_EXTERNAL_STORAGE) {
                Log.e(TAG, "Get READ_EXTERNAL_STORAGE permission");
            } else if (requestCode == RC_HANDLE_WRITE_EXTERNAL_STORAGE){
                Log.e(TAG, "Get WRITE_EXTERNAL_STORAGE permission");
            } else {
                Log.e(TAG, "Unknow permission");
            }
            getAllPermission();
        } else {
            String msg = "";
            if (requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
                msg = "获取相机权限失败";
                Log.e(TAG, msg);
            } else if(requestCode == RC_HANDLE_READ_EXTERNAL_STORAGE) {
                msg = "获取读取存储空间权限失败";
                Log.e(TAG, msg);
            } else if (requestCode == RC_HANDLE_WRITE_EXTERNAL_STORAGE){
                msg = "获取写存储空间权限失败";
                Log.e(TAG, msg);
            } else {
                msg = "获取权限失败";
                Log.e(TAG, msg);
            }
            toast(msg);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("");
            builder.setMessage(msg);
            builder.setPositiveButton("确定", mDlgClick);
            builder.show();
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("");
            builder.setMessage("您确定退出吗?");
            builder.setPositiveButton("确定", mDlgClick);
            builder.setNegativeButton("取消", mDlgClick);
            builder.show();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private DialogInterface.OnClickListener mDlgClick = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                //android.os.Process.killProcess(android.os.Process.myPid());
                finish();
            }
        }
    };

    public static class GlobalParameter{
        int organization_id;
    }
}
