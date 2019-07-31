package com.example.luyao.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private CameraPreview mPreview;
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
                GlobalParameter.setRegistration_image(bitmap_photo);
                //data.putExtra("photo", byteArray);
                //data.putExtra("user_name", user_name);
                setResult(RESULT_OK, data);
                stopCamera();
                finish();
            }
        });

        //startCamera();
        /*
        mCamera = getCameraInstance();
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        */
    }

    public Camera getCameraInstance(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                //break;
            }
        }
        Camera mCamera = null;
        try {
            mCamera = Camera.open(cameraId);
            Log.e(TAG, "cameraId " + cameraId);
        } catch (Exception e) {
            mCamera = null;
            toast("相机不可用！");
        }
        return mCamera;
    }

    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        protected Activity mActivity;
        private boolean DEBUGGING = true;
        private static final String LOG_TAG = "CameraPreviewSample";
        private static final String CAMERA_PARAM_ORIENTATION = "orientation";
        private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
        private static final String CAMERA_PARAM_PORTRAIT = "portrait";

        public CameraPreview(Context context, Camera camera) {
            super(context);
            mActivity=(Activity)context;
            mCamera = camera;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            //mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){// preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or reformatting changes here

            // start preview with new settings
            try {
                Camera.Parameters cameraParams = mCamera.getParameters();
                boolean portrait = isPortrait();
                configureCameraParameters(cameraParams, portrait);

                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }


        protected void configureCameraParameters(Camera.Parameters cameraParams, boolean portrait) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) { // for 2.1 and before
                if (portrait) {
                    cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
                } else {
                    cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_LANDSCAPE);
                }
            } else { // for 2.2 and later
                int angle;
                Display display = mActivity.getWindowManager().getDefaultDisplay();
                switch (display.getRotation()) {
                    case Surface.ROTATION_0: // This is display orientation
                        angle = 90; // This is camera orientation
                        break;
                    case Surface.ROTATION_90:
                        angle = 0;
                        break;
                    case Surface.ROTATION_180:
                        angle = 270;
                        break;
                    case Surface.ROTATION_270:
                        angle = 180;
                        break;
                    default:
                        angle = 90;
                        break;
                }
                Log.e(TAG, "angle: " + angle);
                mCamera.setDisplayOrientation(angle);
            }

            //cameraParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
            //cameraParams.setPictureSize(mPictureSize.width, mPictureSize.height);
            mCamera.setParameters(cameraParams);
        }

        public boolean isPortrait() {
            return (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        }

        public void onPause() {
            mCamera.release();
            mCamera = null;
        }
    }

    private void startCamera() {
        if (mCamera != null) {
            return;
        }
        //Find the total number of cameras available
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Log.e(TAG, "getNumberOfCameras " + Camera.getNumberOfCameras());
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.e(TAG, "facing " + i);
            }
        }
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                //break;
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
        //boolean portrait = isPortrait();
        //configureCameraParameters(parameters, portrait);
        Log.e(TAG, "parameters.flatten " + parameters.flatten());

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

        for(int i = 0; i < support_fps.size(); i++) {
            Log.e(TAG, "support_fps " + support_fps.size() +
                    " " + support_fps.get(i)[0] + " " + support_fps.get(i)[1]);
        }

        //parameters.setPreviewFpsRange(support_fps.get(0)[0], support_fps.get(0)[0]);
        //parameters.setPreviewFpsRange(5000, 7000);
        ///List<Camera.Size> picture_size = parameters.getSupportedPictureSizes();
        /*
        for(int i = 0; i < picture_size.size(); i++) {
            Log.e(TAG, "picture_size " + picture_size.size() +
                    " " + picture_size.get(i).height + " " + picture_size.get(i).width);
        }
        */
        /*
        Log.e(TAG, "getPreviewSize " + parameters.getPreviewSize().width + " " + parameters.getPreviewSize().height);
        Log.e(TAG, "getSupportedPreviewFormats " + parameters.getSupportedPreviewFormats());
        for(int i = 0; i < parameters.getSupportedPreviewFormats().size(); i++){
            Log.e(TAG, "getSupportedPreviewFormats " + i + " " + parameters.getSupportedPreviewFormats().get(i));
        }*/


        //parameters.setPictureSize(1280, 720 );
        //parameters.setPictureFormat(ImageFormat.JPEG);
        //parameters.setPreviewFormat(ImageFormat.NV21);
        //parameters.setPreviewFormat(ImageFormat.YV12);
        //parameters.setPreviewSize(1440, 1080);
        parameters.setPreviewSize(1920, 1080);

        image_size = parameters.getPreviewSize();
        //parameters.setRotation(90);
        //parameters.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
        //parameters.set("orientation", "portrait");

        //mCamera.setDisplayOrientation(90);
        mCamera.setParameters(parameters);
        mCamera.setErrorCallback(mErrorCallback);
        mCamera.setPreviewCallback(this);

        try {
            surfaceTexture = new SurfaceTexture(10);
            mCamera.setPreviewTexture(surfaceTexture);
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }

        mCamera.startPreview();
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
            //Log.e(TAG, "getPreviewFormat  " + parameters.getPreviewFormat());
            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);
            byte[] bytes = out.toByteArray();
            Bitmap bitmap_data = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap bitmap = Utils.rotateResizeBitmap(270, bitmap_data);

            //Bitmap bitmap = Bitmap.createBitmap(colors, 0, image_size.height, image_size.height, image_size.width, Bitmap.Config.ARGB_8888);
            TakePhotoActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    lock.lock();
                    if(!have_take_photo) {
                        image_registration.setImageBitmap(bitmap);
                        bitmap_photo = bitmap;
                    }
                    lock.unlock();
                }
            });
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
