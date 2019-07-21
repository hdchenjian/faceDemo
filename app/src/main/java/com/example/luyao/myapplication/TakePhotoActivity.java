package com.example.luyao.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TakePhotoActivity extends AppCompatActivity implements Camera.PreviewCallback {

    private final static String TAG = TakePhotoActivity.class.getCanonicalName();
    private Button button_take_photo;
    private Button button_use_photo;
    private Button button_discard_photo;
    private ImageView image_registration;
    private Lock lock = new ReentrantLock();
    Bitmap bitmap_photo;
    boolean have_take_photo = false;

    private Camera mCamera = null;
    private Camera.Size image_size;
    private static int cameraId = 0;
    private final TakePhotoActivity.CameraErrorCallback mErrorCallback = new TakePhotoActivity.CameraErrorCallback();
    private SurfaceTexture surfaceTexture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("返回");
        }

        button_take_photo = findViewById(R.id.button_take_photo);
        image_registration = findViewById(R.id.image_registration);
        button_use_photo = findViewById(R.id.button_use_photo);
        button_discard_photo = findViewById(R.id.button_discard_photo);
        button_use_photo.setVisibility(View.INVISIBLE);
        button_discard_photo.setVisibility(View.INVISIBLE);

        button_take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login");
                lock.lock();
                have_take_photo = true;
                image_registration.setImageBitmap(bitmap_photo);
                lock.unlock();
                button_use_photo.setVisibility(View.VISIBLE);
                button_discard_photo.setVisibility(View.VISIBLE);
                button_take_photo.setVisibility(View.INVISIBLE);
            }
        });

        button_discard_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lock.lock();
                have_take_photo = false;
                lock.unlock();
                button_use_photo.setVisibility(View.INVISIBLE);
                button_discard_photo.setVisibility(View.INVISIBLE);
                button_take_photo.setVisibility(View.VISIBLE);
            }
        });

        button_use_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Login");
                Intent data = new Intent();
                int size = bitmap_photo.getRowBytes() * bitmap_photo.getHeight();
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                bitmap_photo.copyPixelsToBuffer(byteBuffer);
                byte[] byteArray = byteBuffer.array();
                data.putExtra("photo", byteArray);
                //data.putExtra("user_name", user_name);
                setResult(RESULT_OK, data);
                stopCamera();
                finish();
            }
        });
    }

    private void startCamera() {
        if (mCamera != null) {
            return;
        }
        //Find the total number of cameras available
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                if (cameraId == 0) cameraId = i;
            }
        }
        try {
            mCamera = Camera.open(cameraId);
            Log.e(TAG, "cameraId " + cameraId);
        } catch (Exception e) {
            mCamera = null;
            toast("相机不可用！");
            return;
        }

        Camera.Parameters parameters = mCamera.getParameters();
        //Log.e(TAG, "parameters.flatten " + parameters.flatten());
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else {
            Log.e(TAG, "Could not set FOCUS_MODE");
        }
        for(int i = 0; i < focusModes.size(); i++) {
            Log.e(TAG, "focusModes: " + focusModes.size() +
                    " " + focusModes.get(i));
        }
        List<int[]> support_fps = parameters.getSupportedPreviewFpsRange();
        Log.e(TAG, "support_fps: " + support_fps.get(0)[0] + " " + support_fps.get(0)[1]);
        /*
        for(int i = 0; i < support_fps.size(); i++) {
            Log.e(TAG, "support_fps " + support_fps.size() +
                    " " + support_fps.get(i)[0] + " " + support_fps.get(i)[1]);
        }*/
        parameters.setPreviewFpsRange(support_fps.get(0)[0], support_fps.get(0)[0]);
        //parameters.setPreviewFpsRange(7500, 7500);
        ///List<Camera.Size> picture_size = parameters.getSupportedPictureSizes();
        /*
        for(int i = 0; i < picture_size.size(); i++) {
            Log.e(TAG, "picture_size " + picture_size.size() +
                    " " + picture_size.get(i).height + " " + picture_size.get(i).width);
        }
        */
        parameters.setPictureSize(1920, 1080);
        parameters.setPictureFormat(ImageFormat.JPEG);
        //parameters.setPreviewFormat(ImageFormat.NV21);
        parameters.setPreviewFormat(ImageFormat.YV12);
        parameters.setPreviewSize(1440, 1080);

        image_size = parameters.getPreviewSize();

        mCamera.setParameters(parameters);
        mCamera.setErrorCallback(mErrorCallback);
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();
        try {
            surfaceTexture = new SurfaceTexture(10);
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
        //parameters = mCamera.getParameters();
        //Log.e(TAG, "parameters.flatten " + parameters.flatten());
    }

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data == null) {
            Log.e(TAG, "onPreviewFrame data null");
            return;
        } else {
            long startTime = System.currentTimeMillis();
            // byte[] tmp = loadLibraryModule.yv122rgb_native(data, image_size.width, image_size.height);
            // int[] colors = loadLibraryModule.rgb2bitmap_native(info.image_data);

            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;
            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            byte[] bytes = out.toByteArray();
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

            //Bitmap bitmap = Bitmap.createBitmap(colors, 0, image_size.height, image_size.height, image_size.width, Bitmap.Config.ARGB_8888);
            TakePhotoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    image_registration.setImageBitmap(bitmap);
                }
            });
            lock.lock();
            if(!have_take_photo) {
                bitmap_photo = bitmap;
            }
            lock.unlock();
            //Log.e(TAG, "yv122rgb " + (System.currentTimeMillis() - startTime));
        }

    }

    public class CameraErrorCallback implements Camera.ErrorCallback {
        private static final String TAG = "CameraErrorCallback";
        @Override
        public void onError(int error, Camera camera) {
            Log.e(TAG, "Encountered an unexpected camera error: " + error);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
        startCamera();
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
        stopCamera();
        super.onStop();
        Log.e(TAG, "lifecycle: onStop");
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "lifecycle: onDestroy");
        stopCamera();
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
