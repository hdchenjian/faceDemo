package com.example.luyao.myapplication;

import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
    private Thread thread_message;
    private boolean thread_recognition_stop;

    private int feature_length = 512;
    private int max_face_num = 10;

    private UserFeatureDB userFeatureDB;
    private Lock lock_user_feature = new ReentrantLock();
    List<Map<String, Object>> all_user_feature;
    Map<Integer, Date> upload_recognition_image_time = new HashMap<>();
    Map<Integer, Integer> upload_times = new HashMap<>();
    public static class PostRegImage{
        public byte[] image_data;
        public String[] user_name;
        public int[][] face_region;
        public int count;
        public float[] score;
        public String relation_ids;
    }

    public class RecognitionHandler extends Handler {
        private void upload_image(byte[] image_jpg, String relation_ids){
            SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), image_jpg);
            // MultipartBody.Part is used to send also the actual file name
            MultipartBody.Part body = MultipartBody.Part.createFormData(
                    "image", "img.jpg", requestFile);
            // add another part within the multipart request
            //RequestBody relation_id = RequestBody.create(okhttp3.MultipartBody.FORM, relation_id);
            //RequestBody time = RequestBody.create(okhttp3.MultipartBody.FORM, );
            RequestBody relation_ids_body = RequestBody.create(okhttp3.MultipartBody.FORM, relation_ids);
            Date time_now = new Date();
            RequestBody time_body = RequestBody.create(
                    okhttp3.MultipartBody.FORM, String.valueOf(time_now.getTime()));

            Call<ResponseBody> call = service.upload_recognition_image(
                    body, relation_ids_body, time_body, GlobalParameter.getSid());
            call.enqueue(new retrofit2.Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    JSONObject responseJson = Utils.parseResponse(response, TAG);
                    if (response.code() == 200) {
                    } else {
                        toast("连接网络失败，请稍后再试");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    toast("连接网络失败，请检查您的网络");
                    t.printStackTrace();
                }
            });
        }

        @Override
        public void handleMessage(Message msg) {
            long startTime = System.currentTimeMillis();
            PostRegImage info = (PostRegImage)msg.obj;
            if(!info.relation_ids.equals("")) {
                byte[] image_jpg = loadLibraryModule.rgb2jpg_native(info.image_data, image_size.width, image_size.height);
                upload_image(image_jpg, info.relation_ids);
            }

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
                        //Log.e(TAG, "recognition waiting data sleep");
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
                lock_user_feature.lock();
                String relation_ids = "";
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
                                + " relation: " + all_user_feature.get(max_score_index).get("relation"));
                    }
                    if (max_score >= 0.42) {
                        if((int)all_user_feature.get(max_score_index).get("is_child") == 1) {
                            user_name[m] = (String)all_user_feature.get(max_score_index).get("name");
                        } else {
                            user_name[m] = (String)all_user_feature.get(max_score_index).get("name") +
                                    " " + (String) all_user_feature.get(max_score_index).get("relation");
                        }
                        Integer current_relation_id =
                                (Integer)all_user_feature.get(max_score_index).get("relation_id");
                        Date time_now = new Date();
                        boolean need_upload = false;
                        if(upload_recognition_image_time.containsKey(current_relation_id)) {
                            Long time_delta = time_now.getTime() -
                                    upload_recognition_image_time.get(current_relation_id).getTime();
                            int min_time_delta = 30000;
                            if(upload_times.containsKey(current_relation_id) &&
                                    upload_times.get(current_relation_id) >= 3) {
                                min_time_delta *= 1;
                            }
                            if (time_delta > min_time_delta) {
                                need_upload = true;
                            } else {
                                need_upload = false;
                            }
                        } else{
                            need_upload = true;
                        }
                        if(need_upload){
                            relation_ids += String.valueOf(current_relation_id) + ",";
                            upload_recognition_image_time.put(current_relation_id, time_now);
                            if(upload_times.containsKey(current_relation_id)) {
                                upload_times.put(current_relation_id,
                                        upload_times.get(current_relation_id) + 1);
                            } else {
                                upload_times.put(current_relation_id, 1);
                            }
                        }
                    } else {
                        user_name[m] = "unkonw";
                    }
                    score[m] = max_score;
                }
                lock_user_feature.unlock();
                Message msg = new Message();
                PostRegImage info = new PostRegImage();
                info.image_data = data;
                info.face_region = face_region;
                info.user_name = user_name;
                info.count = face_count;
                info.score = score;
                info.relation_ids = relation_ids;
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

    public class UpdateFeatureThread extends Thread{
        private boolean get_new_message_finish = true;
        private int max_message_id;

        UpdateFeatureThread(RecognitionActivity activity){
            super();
            max_message_id = getMax_message_id();
        }

        public int getMax_message_id() {
            SharedPreferences message_index = getSharedPreferences("message_index", 0);
            return message_index.getInt("max_message_id", -1);
        }

        public void setMax_message_id(int max_message_id) {
            SharedPreferences message_index = getSharedPreferences("message_index", 0);
            SharedPreferences.Editor message_index_editor = message_index.edit();
            message_index_editor.putInt("max_message_id", max_message_id);
            message_index_editor.commit();
            this.max_message_id = max_message_id;
        }


        private void get_all_feature(){
            SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
            Call<ResponseBody> call = service.get_all_person_feature(GlobalParameter.getSid());
            try {
                Response<ResponseBody> response = call.execute();
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    JSONArray features = responseJson.optJSONArray("features");
                    for(int i = 0; i < features.length(); i++){
                        JSONObject feature = features.optJSONObject(i);
                        int relation_id = feature.optInt("relation_id");
                        String relation = feature.optString("relation");
                        String feature_str = feature.optString("feature");
                        int is_child = feature.optInt("is_child");
                        String name = feature.optString("name");
                        userFeatureDB.addUserFeature(relation_id, relation, feature_str, is_child, name);
                    }
                    setMax_message_id(0);
                    query_user_feature();
                } else {
                    toast("连接网络失败，请稍后再试");
                }
            } catch (IOException e) {
                toast("连接网络失败，请稍后再试");
                e.printStackTrace();
            }
        }

        private void parseMessage(JSONObject responseJson){
            JSONArray messages = responseJson.optJSONArray("messages");
            ArrayList<Integer> delete_message_ids = new ArrayList<>();
            for(int i = 0; i < messages.length(); i++) {
                JSONObject message = messages.optJSONObject(i);
                String message_type = message.optString("type");
                int message_id = message.optInt("message_id");
                delete_message_ids.add(message_id);
                if (message_type.equals("add")) {
                    int relation_id = message.optInt("relation_id");
                    String relation = message.optString("relation");
                    String feature_str = message.optString("feature");
                    int is_child = message.optInt("is_child");
                    String name = message.optString("name");
                    userFeatureDB.addUserFeature(relation_id, relation, feature_str, is_child, name);
                } else if (message_type.equals("delete")) {
                    int relation_id = message.optInt("relation_id");
                    userFeatureDB.deleteUserFeatureById(relation_id);

                } else if (message_type.equals("update")) {
                    int relation_id = message.optInt("relation_id");
                    String relation = message.optString("relation");
                    String feature_str = message.optString("feature");
                    int is_child = message.optInt("is_child");
                    String name = message.optString("name");
                    userFeatureDB.updateUserFeature(relation_id, relation, feature_str, is_child, name);
                } else {
                    Log.e(TAG, "parseMessage: unknow message type");
                }
            }
            int max_message_id_local = responseJson.optInt("max_message_id");
            if(delete_message_ids.size() > 0){
                query_user_feature();
                delete_message(delete_message_ids, max_message_id_local);
            } else {
                if(max_message_id_local != max_message_id) {
                    setMax_message_id(max_message_id_local);
                }
            }
        }

        private void delete_message(ArrayList<Integer> delete_message_ids, int max_message_id_local){
            SimpleHttpClient.ServerAPI service = Utils.getHttpClient(10);
            Call<ResponseBody> call = service.delete_new_message(delete_message_ids, GlobalParameter.getSid());
            try {
                Response<ResponseBody> response = call.execute();
                if (response.code() == 200) {
                    Log.e(TAG, "delete_message success: " + delete_message_ids);
                    setMax_message_id(max_message_id_local);
                } else {
                    toast("连接网络失败，请稍后再试");
                }
            } catch (IOException e) {
                toast("连接网络失败，请稍后再试");
                e.printStackTrace();
            }
        }

        private void get_new_message(){
            SimpleHttpClient.ServerAPI service = Utils.getHttpClient(180);
            Call<ResponseBody> call = service.get_new_message(max_message_id, GlobalParameter.getSid());
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    JSONObject responseJson = Utils.parseResponse(response, TAG);
                    if (response.code() == 200) {
                        parseMessage(responseJson);
                    } else {
                        toast("连接网络失败，请稍后再试");
                    }
                    get_new_message_finish = true;
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    toast("连接网络失败，请检查您的网络");
                    t.printStackTrace();
                    get_new_message_finish = true;
                }
            });
        }

        @Override
        public void run() {
            super.run();
            while(!thread_recognition_stop) {
                if(max_message_id == -1){
                    get_all_feature();
                } else {
                    if (get_new_message_finish) {
                        get_new_message_finish = false;
                        get_new_message();
                    } else {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
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
                userFeatureDB.addUserFeature(0, "", feature_str, 1, user_name);
                File tmp_file = new File(array[j].getParent() + deleted_suffix + "/" + array[j].getName());
                array[j].renameTo(tmp_file);
                Log.e(TAG, "registration_local_image success" + array[j].getName());
            } else {
                Log.e(TAG, "registration_local_image failed " + array[j].getName() +
                        " code:" + code_ret[0]);
            }
        }
    }

    private void query_user_feature(){
        lock_user_feature.lock();
        all_user_feature = userFeatureDB.queryAllUserFeature();
        lock_user_feature.unlock();
        for(int i = 0; i < all_user_feature.size(); i++) {
            Log.e(TAG, "feature num: " + i + "/" +
                    all_user_feature.size() + " " + all_user_feature.get(i).get("name") +
                    " " + all_user_feature.get(i).get("relation"));
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
        //registration_local_image();
        query_user_feature();
        thread_recognition_stop = false;
        thread_recognition = new RecognitionThread(this);
        thread_recognition.start();
        thread_message = new UpdateFeatureThread(this);
        thread_message.start();
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
            thread_message.join();
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
