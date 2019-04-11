package com.example.luyao.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;

import com.iim.recognition.caffe.LoadLibraryModule;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        Camera.PreviewCallback{

    LoadLibraryModule loadLibraryModule;
    private final static String TAG = MainActivity.class.getCanonicalName();
    private Camera mCamera;
    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;
    private static final int RC_HANDLE_READ_EXTERNAL_STORAGE = 2;
    private static final int RC_HANDLE_WRITE_EXTERNAL_STORAGE = 3;
    Camera.Size image_size;
    private boolean isCameraUsable;
    private static int cameraId = 0;
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
    private final CameraRawCallback raw_callback = new CameraRawCallback();

    private long mainThreadId;
    private SurfaceView surface_view;
    private ImageView image_view;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType;
    private Allocation in, out;

    Handler handler;
    private boolean have_new_image = false;
    private  byte[] current_image_byte;
    private Lock lock = new ReentrantLock();
    Thread thread_recognition;
    private boolean thread_recognition_stop;

    int feature_db_num = 1000;
    int feature_length = 512;
    float[][] feature_db = new float[feature_db_num][feature_length];
    String[] feature_db_name = new String[feature_db_num];
    int feature_db_index = 0;

    public static class PostRegImage{
        public byte[] image_data;
        public String[] user_name;
        public int[][] face_region;
        public int count;
        public int[] reg_list;
        public float[] score;
    }

    public class RegThread extends Thread{
        //private WeakReference<MainActivity> activityWeakReference;
        RegThread(MainActivity activity){
            super();
            //activityWeakReference = new WeakReference<>(activity);;
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
                        System.out.println("recognition waiting data sleep");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                } else {
                    have_new_image = false;
                    data = current_image_byte.clone();
                    lock.unlock();
                }
                /*
                Bitmap bitmap = nv21ToBitmap(data, image_size.width, image_size.height);
                System.out.println("recognition spend " + (System.currentTimeMillis() - startTime));
                bitmap = rotate(bitmap, 270);
                System.out.println("recognition spend " + (System.currentTimeMillis() - startTime));

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                System.out.println("recognition spend " + (System.currentTimeMillis() - startTime));
                */
                //System.out.println("recognition spend, copy data " + (System.currentTimeMillis() - startTime));
                int feature_length = 512;
                int[][] face_region = new int[10][4];
                float[][] feature = new float[10][feature_length];
                long[] code_ret = new long[1];
                int face_count = loadLibraryModule.recognition_face(data, face_region, feature, 0, code_ret);
                //System.out.println("recognition spend " + (System.currentTimeMillis() - startTime));
                //System.out.println("face_count: " + face_count + " code:" + code_ret[0]);

                String[] user_name = new String[10];
                int regcognition_num = 0;
                int[] reg_list = new int[10];
                float[] score = new float[10];
                for (int m = 0; m < face_count; m++) {
                    for (int n = 0; n < 10; n++) {
                        //System.out.println(feature[m][n]);
                    }
                    float max_score = 0;
                    int max_score_index = -1;
                    for (int i = 0; i < feature_db_index; i++) {
                        float current_score = 0;
                        for (int k = 0; k < feature_length; k++) {
                            current_score += (feature_db[i][k] * feature[m][k]);
                        }
                        if (current_score > max_score) {
                            max_score = current_score;
                            max_score_index = i;
                        }
                    }
                    if(max_score_index >= 0) {
                        System.out.println("recognition score: " + max_score + " name: "
                                + feature_db_name[max_score_index]);
                    }
                    if (max_score >= 0.42) {
                        user_name[m] = feature_db_name[max_score_index];
                        reg_list[m] = 1;
                        regcognition_num += 1;
                    } else {
                        user_name[m] = "unkonw";
                        reg_list[m] = 1;
                        regcognition_num += 1;
                    }
                    score[m] = max_score;
                }
                Message msg = new Message();
                PostRegImage info = new PostRegImage();
                info.image_data = data;
                info.face_region = face_region;
                info.user_name = user_name;
                info.count = regcognition_num;
                info.reg_list = reg_list;
                info.score = score;
                msg.obj = info;
                handler.sendMessage(msg);
                String face_size = "";
                for (int m = 0; m < face_count; m++) {
                    face_size += ("" + face_region[m][2] + "x" + face_region[m][3]);
                }
                System.out.println("face_count: " + face_count + "recognition total spend " +
                        (System.currentTimeMillis() - startTime) + " face_size " + face_size);
                /*try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }
        }
    }

    private void init_registration(){
        File image_path = new File("/sdcard/A/registor_image");
        File[] array = image_path.listFiles();
        if(array.length == 0) {
            System.out.println("init_registration None image found in current directory!");
        }
        for(int j = 0; j < array.length && j < 5; j++){
            if(!array[j].isFile()) continue;
            if(array[j].getName().lastIndexOf(".jpg") < 0 && array[j].getName().lastIndexOf(".png") < 0) continue;
            System.out.println(array[j].getName());
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

            int[][] face_region = new int[10][4];
            float[][] feature = new float[10][feature_length];
            long[] code_ret = new long[1];
            loadLibraryModule.recognition_face(image_data, face_region, feature, 0, code_ret);
            if(code_ret[0] == 1000){
                for(int kk = 0; kk < feature_length; kk++){
                    feature_db[feature_db_index][kk] = feature[0][kk];
                }
                String[] filePathSplit = array[j].getName().split("\\.");
                feature_db_name[feature_db_index] = filePathSplit[0];
                System.out.println("init_registration success" + " name: " + filePathSplit[0]);
                feature_db_index += 1;
            } else {
                System.out.println("init_registration failed " + array[j].getName() +
                        " code:" + code_ret[0]);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadLibraryModule = new LoadLibraryModule();

        mainThreadId = android.os.Process.myTid();
        image_view = findViewById(R.id.image_view);
        surface_view = findViewById(R.id.surface_view);
        SurfaceHolder holder = surface_view.getHolder();
        holder.addCallback(this);
        holder.setFormat(ImageFormat.NV21);
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        handler = new Handler(){
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
                    for (int i = 0; i < info.reg_list.length; i++) {
                        //System.out.println(info.user_name[i] + " " + i + " " + info.reg_list[i]);
                        if(info.reg_list[i] != 0) {
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
                    }
                    //System.out.println(user_name[0] + " " + regcognition_num);
                    image_view.setImageBitmap(ret);
                } else {
                    image_view.setImageBitmap(bitmap);
                }
                //System.out.println("handleMessage total spend " + (System.currentTimeMillis() - startTime));
            }
        };
        init_registration();
        thread_recognition_stop = false;
        thread_recognition = new RegThread(this);
        thread_recognition.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
    }

    /* Restarts the camera. */
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "lifecycle: onResume");
    }

    /** Stops the camera. */
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
        if(thread_recognition != null) thread_recognition_stop = true;
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        while(true) {
            int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Camera permission is not granted. Requesting permission");
                final String[] permissions = new String[]{Manifest.permission.CAMERA};
                ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM_RGB);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        while(true) {
            int rc = ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "READ_EXTERNAL_STORAGE permission not granted. Requesting permission");
                final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(
                        this, permissions, RC_HANDLE_READ_EXTERNAL_STORAGE);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        while(true) {
            int rc = ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "READ_EXTERNAL_STORAGE permission not granted. Requesting permission");
                final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(
                        this, permissions, RC_HANDLE_WRITE_EXTERNAL_STORAGE);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
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
            isCameraUsable = true;
            Log.e(TAG, "cameraId " + cameraId);
        } catch (Exception e) {
            mCamera = null;
            showToast("相机不可用！");
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
        mCamera.startPreview();
        mCamera.setPreviewCallback(this);
        try {
            mCamera.setPreviewDisplay(surface_view.getHolder());
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
        parameters = mCamera.getParameters();
        //Log.e(TAG, "parameters.flatten " + parameters.flatten());
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.e(TAG, "lifecycle: surfaceChanged");
        if (!isCameraUsable || mCamera == null) return;
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            //mCamera.stopPreview();
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    public synchronized Bitmap nv21ToBitmap(byte[] nv21, int width, int height) {
        if (yuvType == null) {
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(nv21.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);

        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);
        // argb8888 -> rgb565
        return bmpout.copy(Bitmap.Config.RGB_565, false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
            } else if(requestCode == RC_HANDLE_READ_EXTERNAL_STORAGE) {
            }
        }
    }

    private void showToast(final String text) {
        long currentThreadId = android.os.Process.myTid();
        if (currentThreadId != mainThreadId) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    }

    public Bitmap rotate(Bitmap b, float degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
                    b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
            }
        }
        return b;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.w(TAG, "onPreviewFrame");
        if (data == null) {
            return;
        } else {
            /*Log.w(TAG, "onPreviewFrame have data");
            Camera.Parameters ps = mCamera.getParameters();
            if (ps.getPictureFormat() != ImageFormat.JPEG) {
                Log.w(TAG, "PixelFormat != JPEG " + ps.getPictureFormat());
                return;
            }*/
            lock.lock();
            have_new_image = true;
            long startTime = System.currentTimeMillis();
            //current_image_byte = yuv2rgb_native(data, image_size.width, image_size.height);
            current_image_byte = loadLibraryModule.yv122rgb_native(data, image_size.width, image_size.height);
            System.out.println("yuv2rgb " + (System.currentTimeMillis() - startTime));

            //current_image_byte = data.clone();
            lock.unlock();
        }

    }

    public class CameraErrorCallback implements Camera.ErrorCallback {
        private static final String TAG = "CameraErrorCallback";
        @Override
        public void onError(int error, Camera camera) {
            Log.e(TAG, "Encountered an unexpected camera error: " + error);
        }
    }


    public class CameraRawCallback implements Camera.PictureCallback {
        private static final String TAG = "CameraRawCallback";
        @Override
        public void onPictureTaken(byte[] data, Camera _camera) {
            if (data == null){
                //button_start.setEnabled(true);
                return;
            } else {
                Log.w(TAG, "onPictureTaken");
                /*
                Camera.Parameters ps = mCamera.getParameters();
                if (ps.getPictureFormat() != ImageFormat.JPEG) {
                    Log.w(TAG, "PixelFormat != JPEG " + ps.getPictureFormat());
                    return;
                }
                */
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                image_view.setImageBitmap(bitmap);
                mCamera.startPreview(); // preview again
                //button_start.setEnabled(true);
            }
        }
    }
}
