package com.example.luyao.myapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.iim.recognition.caffe.LoadLibraryModule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.min;

public class RecognitionActivity extends AppCompatActivity implements Camera.PreviewCallback{

    private LoadLibraryModule loadLibraryModule;
    private final static String TAG = RecognitionActivity.class.getCanonicalName();
    private Camera mCamera = null;
    private Camera.Size image_size;
    private static int cameraId = 0;
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private ImageView image_view;
    private SurfaceTexture surfaceTexture;

    private Handler handler;
    private boolean have_new_image = false;
    private  byte[] current_image_byte;
    private Lock lock = new ReentrantLock();
    private Thread thread_recognition;
    private boolean thread_recognition_stop;

    private int feature_db_num = 1000;
    private int feature_length = 512;
    private int max_face_num = 10;

    private UserFeatureDB userFeatureDB;
    List<Map<String, Object>> all_user_feature;
    public static class PostRegImage{
        public byte[] image_data;
        public String[] user_name;
        public int[][] face_region;
        public int count;
        public float[] score;
    }

    public class RecognitionHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            long startTime = System.currentTimeMillis();
            PostRegImage info = (PostRegImage)msg.obj;
            int[] colors = loadLibraryModule.rgb2bitmap_native(info.image_data);
            Bitmap bitmap = Bitmap.createBitmap(colors, 0, image_size.height,
                    image_size.height, image_size.width, Bitmap.Config.ARGB_8888);
                /*
                try {
                    FileOutputStream out = new FileOutputStream("/sdcard/A/bitmap1.png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    out.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }*/
            if (info.count > 0) {
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(Color.RED);
                paint.setStrokeWidth(5);
                paint.setTextSize(100);
                paint.setTextAlign(Paint.Align.LEFT);
                Bitmap ret = bitmap.copy(bitmap.getConfig(), true);
                Canvas canvas = new Canvas(ret);
                for (int i = 0; i < info.count; i++) {
                    Rect bounds = new Rect();
                    String score_str = String.valueOf(info.score[i]);
                    String str = info.user_name[i] + "  " +
                            score_str.substring(0, min(4, score_str.length() - 1));
                    paint.getTextBounds(str, 0, str.length(), bounds);
                    canvas.drawText(str, info.face_region[i][0],
                            info.face_region[i][1], paint);

                    canvas.drawRect(info.face_region[i][0], info.face_region[i][1],
                            info.face_region[i][0] + info.face_region[i][2],
                            info.face_region[i][1] + info.face_region[i][3], paint);
                }
                //Log.e(TAG, user_name[0] + " " + regcognition_num);
                image_view.setImageBitmap(ret);
            } else {
                image_view.setImageBitmap(bitmap);
            }
            //Log.e(TAG, "handleMessage total spend " + (System.currentTimeMillis() - startTime));
        }
    }

    public class RecognitionThread extends Thread{
        RecognitionThread(RecognitionActivity activity){
            super();
        }

        @Override
        public void run() {
            super.run();
            while(!thread_recognition_stop) {
                long startTime = System.currentTimeMillis();
                byte[] data;
                lock.lock();
                if (!have_new_image) {
                    lock.unlock();
                    try {
                        Thread.sleep(20);
                        Log.e(TAG, "recognition waiting data sleep");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                } else {
                    have_new_image = false;
                    data = current_image_byte.clone();
                    lock.unlock();
                }

                int feature_length = 512;
                int[][] face_region = new int[max_face_num][4];
                float[][] feature = new float[max_face_num][feature_length];
                long[] code_ret = new long[1];
                int face_count = loadLibraryModule.recognition_face(data, face_region, feature, 0, code_ret);

                String[] user_name = new String[max_face_num];
                int[] reg_list = new int[max_face_num];
                float[] score = new float[max_face_num];
                for (int m = 0; m < face_count; m++) {
                    float max_score = 0;
                    int max_score_index = -1;
                    for(int i = 0; i < all_user_feature.size(); i++) {
                        float current_score = 0;
                        float[] feature_db_item = (float[])all_user_feature.get(i).get("feature");
                        for (int j = 0; j < feature_length; j++) {
                            current_score += (feature_db_item[j] * feature[m][j]);
                        }
                        if (current_score > max_score) {
                            max_score = current_score;
                            max_score_index = i;
                        }
                    }
                    if(max_score_index >= 0) {
                        Log.e(TAG, "recognition score: " + max_score + " name: "
                                + all_user_feature.get(max_score_index).get("name")
                                + " person_id: " + all_user_feature.get(max_score_index).get("person_id"));
                    }
                    if (max_score >= 0.42) {
                        user_name[m] = (String)all_user_feature.get(max_score_index).get("name");
                    } else {
                        user_name[m] = "unkonw";
                    }
                    score[m] = max_score;
                }
                Message msg = new Message();
                PostRegImage info = new PostRegImage();
                info.image_data = data;
                info.face_region = face_region;
                info.user_name = user_name;
                info.count = face_count;
                info.score = score;
                msg.obj = info;
                handler.sendMessage(msg);
                String face_size = "";
                for (int m = 0; m < face_count; m++) {
                    face_size += (" " + face_region[m][2] + "x" + face_region[m][3]);
                }
                Log.d(TAG, "face_count: " + face_count + " recognition total spend " +
                        (System.currentTimeMillis() - startTime) + " face_size " + face_size);
            }
        }
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
        for(int j = 0; j < array.length && j < 5; j++){
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

            int[][] face_region = new int[max_face_num][4];
            float[][] feature = new float[max_face_num][feature_length];
            long[] code_ret = new long[1];
            loadLibraryModule.recognition_face(image_data, face_region, feature, 0, code_ret);
            if(code_ret[0] == 1000){
                String feature_str = "";
                for(int kk = 0; kk < feature_length; kk++){
                    feature_str += String.valueOf(feature[0][kk]) +",";
                }
                String[] filePathSplit = array[j].getName().split("\\.");
                String user_name = filePathSplit[0];
                userFeatureDB.addUserFeature(user_name, feature_str);
                File tmp_file = new File(array[j].getParent() + deleted_suffix + "/" + array[j].getName());
                array[j].renameTo(tmp_file);
                Log.e(TAG, "registration_local_image success" + array[j].getName());
            } else {
                Log.e(TAG, "registration_local_image failed " + array[j].getName() +
                        " code:" + code_ret[0]);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("返回");
        }

        loadLibraryModule = LoadLibraryModule.getInstance();
        image_view = findViewById(R.id.image_view);
        handler = new RecognitionHandler();
        userFeatureDB = new UserFeatureDB(this);
        //userFeatureDB.deleteAllUserFeature();
        registration_local_image();
        all_user_feature = userFeatureDB.queryAllUserFeature();
        for(int i = 0; i < all_user_feature.size(); i++) {
            Log.e(TAG, "feature num: " + i + "/" +
                    all_user_feature.size() + " " + all_user_feature.get(i).get("name"));
        }
        thread_recognition_stop = false;
        thread_recognition = new RecognitionThread(this);
        thread_recognition.start();
        startCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startCamera();
        Log.e(TAG, "lifecycle: onStart");
    }

    /* Restarts the camera. */
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "lifecycle: onResume");
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "lifecycle: onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        stopCamera();
        Log.e(TAG, "lifecycle: onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "lifecycle: onDestroy");
        if(thread_recognition != null) thread_recognition_stop = true;
        stopCamera();
        super.onDestroy();
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

    protected void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data == null) {
            Log.e(TAG, "onPreviewFrame data null");
            return;
        } else {
            long startTime = System.currentTimeMillis();
            byte[] tmp = loadLibraryModule.yv122rgb_native(data, image_size.width, image_size.height);
            lock.lock();
            have_new_image = true;
            current_image_byte = tmp;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
