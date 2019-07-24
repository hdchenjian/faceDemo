package com.example.luyao.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iim.recognition.caffe.LoadLibraryModule;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.min;

public class RecognitionActivity extends AppCompatActivity implements Camera.PreviewCallback{

    private LoadLibraryModule loadLibraryModule;
    private final static String TAG = RecognitionActivity.class.getCanonicalName();
    private Camera mCamera = null;
    private Camera.Size image_size;
    private static int cameraId = 0;
    int camera_image_width = 0;
    int camera_image_height = 0;
    int camera_image_format;
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private long recognition_time = 0;
    private long detect_face_time = 0;
    private List<Bitmap> recognition_images = new ArrayList<>();
    private List<Integer> recognition_relation_ids = new ArrayList<>();
    private List<String> recognition_name = new ArrayList<>();
    private List<ImageView> recognition_image_view = new ArrayList<>();
    private List<TextView> recognition_image_user_name = new ArrayList<>();

    private ImageView image_view;
    private ImageView image_1;
    private TextView image_1_relation;
    private ImageView image_2;
    private TextView image_2_relation;
    private ImageView image_3;
    private TextView image_3_relation;

    private ImageView recognition_success;
    private TextView text_recognition;

    private SurfaceTexture surfaceTexture;

    private Handler handler;
    private boolean have_new_image = false;
    private Bitmap current_image_bitmap;
    private Bitmap current_image_bitmap_bak;
    private Lock lock = new ReentrantLock();
    private Thread thread_recognition;
    private boolean thread_recognition_stop;

    private int max_face_num = 10;

    private UserFeatureDB userFeatureDB;
    private Lock lock_user_feature = new ReentrantLock();
    List<Map<String, Object>> all_user_feature;

    public static class PostRegImage{
        public Bitmap image_data;
        public String[] user_name;
        public int[] face_region;
        public int count;
        public float[] score;
        public String relation_ids;
    }

    public class RecognitionHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            long startTime = System.currentTimeMillis();
            PostRegImage info = (PostRegImage)msg.obj;
            Bitmap bitmap = info.image_data;
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
                //Bitmap ret = bitmap.copy(bitmap.getConfig(), true);
                Canvas canvas = new Canvas(bitmap);
                for (int i = 0; i < info.count; i++) {
                    Rect bounds = new Rect();
                    String score_str = String.valueOf(info.score[i]);
                    String str = info.user_name[i] + "  " +
                            score_str.substring(0, min(4, score_str.length() - 1));
                    paint.getTextBounds(str, 0, str.length(), bounds);
                    canvas.drawText(str, info.face_region[0],
                            info.face_region[1], paint);

                    canvas.drawRect(info.face_region[0], info.face_region[1],
                            info.face_region[2],
                            info.face_region[3], paint);
                }
                //Log.e(TAG, user_name[0] + " " + regcognition_num);
                image_view.setImageBitmap(bitmap);
            } else {
                image_view.setImageBitmap(bitmap);
            }
            Log.e(TAG, "handleMessage total spend " + (System.currentTimeMillis() - startTime));
        }
    }

    public class RecognitionThread extends Thread{
        RecognitionThread(RecognitionActivity activity){
            super();
        }

        private Bitmap update_recognition_image(Map<String, Object> user_feature,
                                                int m, byte[] data, int[] face_region, Bitmap bitmap){
            long startTime = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recognition_success.setVisibility(View.VISIBLE);
                    text_recognition.setVisibility(View.VISIBLE);
                }
            });

            int relation_id = (int)user_feature.get("relation_id");
            boolean need_update_image = false;
            if(recognition_relation_ids.size() == 0 ||
                    recognition_relation_ids.get(recognition_relation_ids.size() - 1) != relation_id){
                need_update_image = true;
            }
            if(need_update_image) {
                if (recognition_images.size() == 3) {
                    recognition_images.remove(0);
                    recognition_name.remove(0);
                    recognition_relation_ids.remove(0);
                } else {
                }

                int face_x = (int) (face_region[0] - face_region[2] * 0.2);
                if (face_x < 0) face_x = 0;
                int face_y = (int) (face_region[1] - face_region[3] * 0.2);
                if (face_y < 0) face_y = 0;
                int face_width = (int) (face_region[2] * 1.4);
                int face_height = (int) (face_region[3] * 1.4);
                if (face_width > bitmap.getWidth() - face_x)
                    face_width = bitmap.getWidth() - face_x;
                if (face_height > bitmap.getHeight() - face_y)
                    face_height = bitmap.getHeight() - face_y;
                Bitmap current_photo = Bitmap.createBitmap(bitmap, face_x, face_y, face_width, face_height);
                recognition_images.add(current_photo);
                recognition_relation_ids.add(relation_id);
                recognition_name.add((String)user_feature.get("name"));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < recognition_images.size(); i++) {
                            recognition_image_user_name.get(i).setText(recognition_name.get(recognition_name.size() - 1 - i));
                            recognition_image_user_name.get(i).setVisibility(View.VISIBLE);
                            recognition_image_view.get(i).setImageBitmap(recognition_images.get(recognition_images.size() - 1 - i));
                            recognition_image_view.get(i).setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
            Log.e(TAG, "update_recognition_image spend " + (System.currentTimeMillis() - startTime));
            return bitmap;
        }

        private void clear_recognition_image(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recognition_success.setVisibility(View.INVISIBLE);
                    text_recognition.setVisibility(View.INVISIBLE);
                }
            });
        }

        @Override
        public void run() {
            super.run();
            while(!thread_recognition_stop) {
                long startTime = System.currentTimeMillis();
                Date time_now_tmp = new Date();
                if(time_now_tmp.getTime() - recognition_time > 1000 * 3){
                    clear_recognition_image();
                }
                if(time_now_tmp.getTime() - detect_face_time > 1000 * 60){
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                byte[] data;
                while(true) {
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
                        current_image_bitmap_bak = current_image_bitmap;
                        lock.unlock();
                        data = Utils.bitmapToByte(current_image_bitmap_bak);
                        break;
                    }
                }

                int feature_length = 512;
                int[] face_region = new int[max_face_num * 4];
                float[] feature = new float[max_face_num * feature_length];
                long[] code_ret = new long[1];
                int face_count = loadLibraryModule.recognition_face(data, face_region, feature, code_ret);

                String[] user_name = new String[max_face_num];
                float[] score = new float[max_face_num];
                lock_user_feature.lock();
                String relation_ids = "";
                Date time_now = new Date();
                for (int m = 0; m < face_count; m++) {
                    detect_face_time = time_now .getTime();
                    float max_score = 0;
                    int max_score_index = -1;
                    for(int i = 0; i < all_user_feature.size(); i++) {
                        float current_score = 0;
                        float[] feature_db_item = (float[])all_user_feature.get(i).get("feature");
                        for (int j = 0; j < feature_length; j++) {
                            current_score += (feature_db_item[j] * feature[j]);
                        }
                        if (current_score > max_score) {
                            max_score = current_score;
                            max_score_index = i;
                        }
                    }
                    Map<String, Object> user_feature = all_user_feature.get(max_score_index);
                    if (max_score >= 0.55) {
                        update_recognition_image(user_feature, m, data, face_region, current_image_bitmap_bak);
                        recognition_time = time_now.getTime();
                        user_name[m] = (String)user_feature.get("name");
                    } else {
                        user_name[m] = "unkonw";
                    }
                    score[m] = max_score;
                }
                lock_user_feature.unlock();
                Message msg = new Message();
                PostRegImage info = new PostRegImage();
                info.image_data = current_image_bitmap_bak;
                info.face_region = face_region;
                info.user_name = user_name;
                info.count = face_count;
                info.score = score;
                info.relation_ids = relation_ids;
                msg.obj = info;
                handler.sendMessage(msg);
                String face_size = "";
                for (int m = 0; m < face_count; m++) {
                    face_size += (" " + face_region[2] + "x" + face_region[3]);
                }
                Log.e(TAG, "face_count: " + face_count + " recognition total spend " +
                        (System.currentTimeMillis() - startTime) + " face_size " + face_size);
            }
        }
    }

    private void query_user_feature(){
        lock_user_feature.lock();
        all_user_feature = userFeatureDB.queryAllUserFeature();
        for(int i = 0; i < all_user_feature.size(); i++) {
            Map<String, Object> user_feature = all_user_feature.get(i);
            Log.e(TAG, "feature num: " + i + "/" +
                    all_user_feature.size() + " " + user_feature.get("name"));
        }
        lock_user_feature.unlock();
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

        Date time_now = new Date();
        detect_face_time = time_now.getTime();
        loadLibraryModule = LoadLibraryModule.getInstance();
        image_view = findViewById(R.id.image_view);
        image_1 = findViewById(R.id.image_1);
        image_1_relation = findViewById(R.id.image_1_relation);
        image_2 = findViewById(R.id.image_2);
        image_2_relation = findViewById(R.id.image_2_relation);
        image_3 = findViewById(R.id.image_3);
        image_3_relation = findViewById(R.id.image_3_relation);
        recognition_image_view.add(image_1);
        recognition_image_view.add(image_2);
        recognition_image_view.add(image_3);
        recognition_image_user_name.add(image_1_relation);
        recognition_image_user_name.add(image_2_relation);
        recognition_image_user_name.add(image_3_relation);

        recognition_success = findViewById(R.id.recognition_success);
        text_recognition = findViewById(R.id.text_recognition);
        recognition_success.setVisibility(View.INVISIBLE);
        text_recognition.setVisibility(View.INVISIBLE);

        handler = new RecognitionHandler();
        userFeatureDB = new UserFeatureDB(this);
        //userFeatureDB.deleteAllUserFeature();
        //registration_local_image();
        query_user_feature();
        thread_recognition_stop = false;
        thread_recognition = new RecognitionThread(this);
        thread_recognition.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
        startCamera();
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
        try {
            thread_recognition.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
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
        //parameters.setPreviewFpsRange(support_fps.get(0)[0], support_fps.get(0)[0]);
        parameters.setPreviewFpsRange(3000,30000);
        ///List<Camera.Size> picture_size = parameters.getSupportedPictureSizes();
        /*
        for(int i = 0; i < picture_size.size(); i++) {
            Log.e(TAG, "picture_size " + picture_size.size() +
                    " " + picture_size.get(i).height + " " + picture_size.get(i).width);
        }
        */
        //parameters.setPictureSize(1920, 1080);
        //parameters.setPictureFormat(ImageFormat.JPEG);
        //parameters.setPreviewFormat(ImageFormat.NV21);
        //parameters.setPreviewFormat(ImageFormat.YV12);
        //parameters.setPreviewSize(1280, 720);
        parameters.setPreviewSize(1920, 1080);

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

            if(camera_image_width == 0) {
                Camera.Parameters parameters = camera.getParameters();
                camera_image_width = parameters.getPreviewSize().width;
                camera_image_height = parameters.getPreviewSize().height;
                camera_image_format = parameters.getPreviewFormat();
            }
            YuvImage yuv = new YuvImage(data, camera_image_format, camera_image_width, camera_image_height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, camera_image_width, camera_image_height), 50, out);
            byte[] bytes = out.toByteArray();
            Bitmap bitmap_data = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap bitmap = Utils.rotateResizeBitmap(270, bitmap_data);

            lock.lock();
            have_new_image = true;
            current_image_bitmap = bitmap;
            lock.unlock();
            /*
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    image_view.setImageBitmap(bitmap);
                }
            });
            Log.e(TAG, "yv122rgb " + (System.currentTimeMillis() - startTime));
            */
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
