package com.example.luyao.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.alfeye.a1io.A1IoDevBaseUtil;
import com.alfeye.a1io.A1IoDevManager;
import com.iim.recognition.caffe.LoadLibraryModule;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private UserFeatureDB userFeatureDB;
    private LoadLibraryModule loadLibraryModule;
    private InitNetworkThread initNetworkThread;
    private final static String TAG = MainActivity.class.getCanonicalName();
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int RC_HANDLE_READ_EXTERNAL_STORAGE = 2;
    private static final int RC_HANDLE_WRITE_EXTERNAL_STORAGE = 3;

    private Button button_mode_manager;
    private Button button_mode_recognition;
    private Button button_exit;

    private static final int REQUEST_Recognition = 2;
    private static final int REQUEST_Manager = 3;
    private boolean getAllPermissionSuccess = false;

    protected void toast(final String msg) {
        runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void start_recognition(){
        Intent intent = new Intent(this, RecognitionActivity.class);
        startActivityForResult(intent, REQUEST_Recognition);
    }

    private void start_manager(){
        Intent intent = new Intent(this, RegistrationActivity.class);
        startActivityForResult(intent, REQUEST_Manager);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_select);

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

        button_mode_manager.setEnabled(false);
        button_mode_recognition.setEnabled(false);
        userFeatureDB = new UserFeatureDB(this);
        initNetworkThread = new InitNetworkThread();
        initNetworkThread.start();
        Log.e(TAG, "oncreate finish");
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

    private void initDnn() {
        if (loadLibraryModule == null) {
            loadLibraryModule = LoadLibraryModule.getInstance();
            String model_path = "/sdcard/A/";
            int use_spoofing = 1;
            loadLibraryModule.recognition_start(model_path, use_spoofing);
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
        //initDnn();
        getAllPermissionSuccess = true;
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
        if (initNetworkThread != null) {
            try {
                initNetworkThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    public class InitNetworkThread extends Thread {
        public InitNetworkThread() {
            super();
        }


        private void registration_local_image(){
            String registration_image_path = "/sdcard/A/注册图片";
            String deleted_suffix = "/已注册图片";
            String deleted_image_path_path = registration_image_path + deleted_suffix;
            File image_path = new File(registration_image_path);
            if(!image_path.exists()){
                image_path.mkdirs();
            }
            File deleted_image_path = new File(deleted_image_path_path);
            if(!deleted_image_path.exists()){
                deleted_image_path.mkdirs();
            }
            File[] array = image_path.listFiles();
            if(array.length == 0) {
                Log.e(TAG, "registration_local_image None image found in current directory!");
            }
            for(int j = 0; j < array.length; j++){
                if(!array[j].isFile()) continue;
                if(!array[j].getName().endsWith(".jpg") && !array[j].getName().endsWith(".png")) continue;
                Log.e(TAG, array[j].getName());
                byte[] image_data = new byte[(int)array[j].length()];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(array[j]));
                    buf.read(image_data, 0, image_data.length);
                    buf.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                int max_face_num = 10;
                int feature_length = 512;
                int[] face_region = new int[max_face_num * 4];
                float[] feature = new float[max_face_num * feature_length];
                long[] code_ret = new long[1];
                int width = 0;
                int height = 0;
                loadLibraryModule.recognition_face(image_data, face_region, feature, code_ret, width, height);
                if(code_ret[0] == 1000){
                    String feature_str = "";
                    for(int kk = 0; kk < feature_length; kk++){
                        feature_str += String.valueOf(feature[kk]) +",";
                    }
                    String[] filePathSplit = array[j].getName().split("\\.");
                    String user_name = filePathSplit[0];
                    int max_id = userFeatureDB.queryMaxId();
                    userFeatureDB.addUserFeature(max_id + 1, feature_str, "", user_name);
                    File tmp_file = new File(array[j].getParent() + deleted_suffix + "/" + array[j].getName());
                    array[j].renameTo(tmp_file);
                    Log.e(TAG, "registration_local_image success " + array[j].getName());
                } else {
                    Log.e(TAG, "registration_local_image failed " + array[j].getName() +
                            " code:" + code_ret[0]);
                }
            }
        }


        @Override
        public void run() {
            while(!getAllPermissionSuccess) {
                try {
                    Thread.sleep(100);
                    Log.e(TAG, "waiting for getAllPermissionSuccess");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            initDnn();
            Log.e(TAG, "initDnn over ");
            registration_local_image();

            login("15919460519", Utils.md5("123456"));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    button_mode_manager.setEnabled(true);
                    button_mode_recognition.setEnabled(true);
                }
            });
        }
    }

    private int login(String user_phone, String password) {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        //String mac = Utils.getMac(this).replace(":", "");
        String mac = "FCA47AC01A75";
        Call<ResponseBody> call = service.login(user_phone, password, mac);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    GlobalParameter.setOrganization_id(responseJson.optInt("organization_id"));
                    GlobalParameter.setSid(responseJson.optString("sid"));
                } else {
                    //toast("连接网络失败，请稍后再试");
                    toast(responseJson.optString("detail"));
                }
                if(GlobalParameter.getOrganization_id() > 0 &&
                        GlobalParameter.getSid() != null && GlobalParameter.getSid().length() == 32) {
                    toast("登录成功");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                toast("连接网络失败，请检查您的网络");
                t.printStackTrace();
            }
        });
        return 0;
    }
}
