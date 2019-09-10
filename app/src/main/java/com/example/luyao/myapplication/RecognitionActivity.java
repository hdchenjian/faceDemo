package com.example.luyao.myapplication;

import android.content.SharedPreferences;
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
import android.hardware.camera2.CameraManager;
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

import com.alfeye.a1io.A1IoDevBaseUtil;
import com.alfeye.a1io.A1IoDevManager;
import com.iim.recognition.caffe.LoadLibraryModule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.lang.Math.min;

public class RecognitionActivity extends AppCompatActivity implements Camera.PreviewCallback{
    private A1IoDevBaseUtil a1IoDevManager;
    private boolean enable_spoofing = true;
    private boolean save_spoofing_image = true;
    private boolean use_spoofing = true;

    private LoadLibraryModule loadLibraryModule;
    private final static String TAG = RecognitionActivity.class.getCanonicalName();
    private Camera mCamera = null;
    private Camera infrared_camera = null;
    private Camera.Size image_size;
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();
    private InfraredCameraPreviewCallback infraredCameraPreviewCallback = new InfraredCameraPreviewCallback();

    private long recognition_time = 0;
    private long detect_face_time = 0;
    private List<Bitmap> recognition_images = new ArrayList<>();
    private List<Integer> recognition_person_ids = new ArrayList<>();
    private List<String> recognition_name = new ArrayList<>();
    private List<ImageView> recognition_image_view = new ArrayList<>();
    private List<TextView> recognition_image_user_name = new ArrayList<>();

    private ImageView image_view;
    private ImageView image_view_infrared;
    private ImageView image_1;
    private TextView image_1_relation;
    private ImageView image_2;
    private TextView image_2_relation;
    private ImageView image_3;
    private TextView image_3_relation;

    private ImageView recognition_success;
    private TextView text_recognition;

    private SurfaceTexture surfaceTexture = new SurfaceTexture(10);
    private SurfaceTexture surfaceTexture_infrared = new SurfaceTexture(10);

    private Handler handler;
    private boolean have_new_image = false;
    private Bitmap current_image_bitmap;
    private int[] current_image_data;
    private int[] current_image_data_infrared;
    private Lock lock = new ReentrantLock();
    private Thread thread_recognition;
    private boolean thread_recognition_stop;
    private Thread thread_message;

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
        public String person_ids;
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
                Bitmap bitmap_copy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(bitmap_copy);
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
                image_view.setImageBitmap(bitmap_copy);
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

        private Bitmap update_recognition_image(Map<String, Object> user_feature,
                                                int m, int[] face_region, Bitmap bitmap){
            long startTime = System.currentTimeMillis();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recognition_success.setVisibility(View.VISIBLE);
                    text_recognition.setVisibility(View.VISIBLE);
                }
            });

            int person_id = (int)user_feature.get("person_id");
            boolean need_update_image = false;
            if(recognition_person_ids.size() == 0 ||
                    recognition_person_ids.get(recognition_person_ids.size() - 1) != person_id){
                need_update_image = true;
            }
            if(need_update_image) {
                if (recognition_images.size() == 3) {
                    recognition_images.remove(0);
                    recognition_name.remove(0);
                    recognition_person_ids.remove(0);
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
                recognition_person_ids.add(person_id);
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
            //Log.e(TAG, "update_recognition_image spend " + (System.currentTimeMillis() - startTime));
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
                int[] current_image_data_bak;
                int[] current_image_data_infrared_bak;
                Bitmap current_image_bitmap_bak;
                while(true) {
                    if(thread_recognition_stop) return;
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
                        current_image_bitmap_bak = current_image_bitmap;
                        current_image_data_bak = current_image_data;
                        current_image_data_infrared_bak = current_image_data_infrared;
                        lock.unlock();
                        //data = Utils.bitmapToByte(current_image_bitmap_bak);
                        //Log.e(TAG, "recognition waiting data sleep " + current_image_data_bak.length);
                        data = loadLibraryModule.bitmap2rgb_native(current_image_data_bak);
                        break;
                    }
                }

                int feature_length = 512;
                int[] face_region = new int[max_face_num * 4];
                float[] feature = new float[max_face_num * feature_length];
                long[] code_ret = new long[1];
                int face_count = 0;

                /*
                int[] pix = new int[current_image_bitmap_bak.getHeight() * current_image_bitmap_bak.getWidth()];
                current_image_bitmap_bak.getPixels(pix, 0, current_image_bitmap_bak.getWidth(), 0, 0,
                        current_image_bitmap_bak.getWidth(), current_image_bitmap_bak.getHeight());
                data = loadLibraryModule.bitmap2rgb_native(pix);
                */

                face_count = loadLibraryModule.recognition_face(data, face_region, feature, code_ret,
                        current_image_bitmap_bak.getWidth(), current_image_bitmap_bak.getHeight());

                String[] user_name = new String[max_face_num];
                float[] score = new float[max_face_num];
                lock_user_feature.lock();
                String person_ids = "";
                Date time_now = new Date();
                boolean recognition_success = false;
                int recognition_index = -1;
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
                    if (max_score >= 0.50) {
                        Map<String, Object> user_feature = all_user_feature.get(max_score_index);
                        update_recognition_image(user_feature, m, face_region, current_image_bitmap_bak);
                        recognition_time = time_now.getTime();
                        user_name[m] = (String)user_feature.get("name");
                        recognition_success = true;
                        recognition_index = m;
                    } else {
                        user_name[m] = "unkonw";
                    }
                    score[m] = max_score;
                }
                lock_user_feature.unlock();

                int is_real = 1;
                if(recognition_success && save_spoofing_image){
                    loadLibraryModule.save_spoofingimage(data, face_region, current_image_bitmap_bak.getWidth(),
                            current_image_bitmap_bak.getHeight(), 1, is_real);
                }

                if(recognition_success && recognition_index >= 0 && enable_spoofing) {
                    byte[] data_infrared = loadLibraryModule.bitmap2rgb_native(current_image_data_infrared_bak);
                    int[] face_region_infrared = new int[max_face_num * 4];
                    float[] feature_infrared = new float[max_face_num * feature_length];
                    long[] code_ret_infrared = new long[1];
                    int face_count_infrared = loadLibraryModule.recognition_face(
                            data_infrared, face_region_infrared, feature_infrared, code_ret_infrared,
                            current_image_bitmap_bak.getWidth(), current_image_bitmap_bak.getHeight());

                    /*
                    for (int j = 0; j < 4; j++) {
                        Log.e(TAG, "face_region rgb " + face_region[j] + " face_region_infrared " + face_region_infrared[j]);
                    }
                    float fB = 0.310F;
                    Log.e(TAG, "D " + Math.abs((float)(face_region[0] - face_region_infrared[0]) / 100.0F));
                    float distance = fB / Math.abs((float)(face_region[0] - face_region_infrared[0]) / 100.0F);
                    */

                    if(face_count_infrared != 1 || code_ret_infrared[0] != 1000){
                        user_name[recognition_index] = "假脸 " + user_name[recognition_index];
                    } else {
                        if(save_spoofing_image) {
                            loadLibraryModule.save_spoofingimage(data_infrared, face_region_infrared,
                                    current_image_bitmap_bak.getWidth(),
                                    current_image_bitmap_bak.getHeight(), 0, is_real);
                        }
                        float score_local = 0;
                        for (int j = 0; j < feature_length; j++) {
                            score_local += (feature_infrared[j] * feature[j]);
                        }
                        Log.e(TAG, "feature_infrared score " + score_local);
                        if (score_local < 0.5) {
                            user_name[recognition_index] = "假脸 " + user_name[recognition_index];
                        }
                        if(use_spoofing){
                            int spoofing_result_infrared = loadLibraryModule.run_spoofing(data_infrared,
                                    face_region_infrared, current_image_bitmap_bak.getWidth(),
                                    current_image_bitmap_bak.getHeight());
                            int spoofing_result = loadLibraryModule.run_spoofing(data, face_region,
                                    current_image_bitmap_bak.getWidth(), current_image_bitmap_bak.getHeight());
                            Log.e(TAG, "spoofing_result " + spoofing_result +
                                    " spoofing_result_infrared " + spoofing_result_infrared);
                            if (spoofing_result_infrared != 1 || spoofing_result != 1) {
                                user_name[recognition_index] = "假脸 " + user_name[recognition_index];
                            }
                        }
                    }
                }

                Message msg = new Message();
                PostRegImage info = new PostRegImage();
                info.image_data = current_image_bitmap_bak;
                /*
                face_region[0] = 0;
                face_region[1] = 0;
                face_region[2] = 0;
                face_region[3] = 0;
                //Log.e(TAG, "opencv detect_face " + current_image_bitmap_bak.getWidth() + " " + current_image_bitmap_bak.getHeight());
                startTime = System.currentTimeMillis();
                face_count = loadLibraryModule.detect_face(data, face_region,
                        current_image_bitmap_bak.getWidth(), current_image_bitmap_bak.getHeight());
                Log.e(TAG, "opencv detect_face total spend " + (System.currentTimeMillis() - startTime) + " face_count " + face_count);
                */
                info.face_region = face_region;
                info.user_name = user_name;
                info.count = face_count;
                info.score = score;
                info.person_ids = person_ids;
                msg.obj = info;
                handler.sendMessage(msg);
                String face_size = "";
                for (int m = 0; m < face_count; m++) {
                    face_size += (" " + face_region[2] + "x" + face_region[3]);
                }
                Log.e(TAG, "face_count: " + face_count + " recognition total spend " +
                        (System.currentTimeMillis() - startTime) + "ms face_size " + face_size);

            }
        }
    }

    public class UpdateFeatureThread extends Thread{
        private boolean get_new_message_finish = true;
        private boolean delete_new_message_finish = true;
        private int max_message_id;

        UpdateFeatureThread(RecognitionActivity activity){
            super();
            max_message_id = getMax_message_id();
            Log.e(TAG, "max_message_id: " + max_message_id);
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
            Log.e(TAG, "setMax_message_id max_message_id: " + max_message_id);
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
                        String feature_str = feature.optString("feature");
                        int person_id = feature.optInt("person_id");
                        String head_picture = feature.optString("head_picture");
                        String name = feature.optString("name");
                        userFeatureDB.addUserFeature(person_id, feature_str, head_picture, name);
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
                    String feature_str = message.optString("feature");
                    int person_id = message.optInt("person_id");
                    String head_picture = message.optString("head_picture");
                    String name = message.optString("name");
                    userFeatureDB.addUserFeature(person_id, feature_str, head_picture, name);
                } else if (message_type.equals("delete")) {
                    int person_id = message.optInt("person_id");
                    userFeatureDB.deleteUserFeatureById(person_id);
                } else if (message_type.equals("sync")) {
                    setMax_message_id(-1);
                    return;
                } else if (message_type.equals("update")) {
                    String feature_str = message.optString("feature");
                    int person_id = message.optInt("person_id");
                    String head_picture = message.optString("head_picture");
                    String name = message.optString("name");
                    userFeatureDB.updateUserFeature(person_id, feature_str, head_picture, name);
                } else {
                    Log.e(TAG, "parseMessage: unknow message type");
                }
            }
            int max_message_id_local = responseJson.optInt("max_message_id");
            if(delete_message_ids.size() > 0){
                query_user_feature();
                delete_new_message_finish = false;
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
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.code() == 200) {
                        Log.e(TAG, "delete_message success: " + delete_message_ids);
                        setMax_message_id(max_message_id_local);
                    } else {
                        toast("连接网络失败，请稍后再试");
                    }
                    delete_new_message_finish = true;
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    toast("连接网络失败，请检查您的网络");
                    t.printStackTrace();
                    delete_new_message_finish = true;
                }
            });
        }

        private void get_new_message(){
            SimpleHttpClient.ServerAPI service = Utils.getHttpClient(180);
            Call<ResponseBody> call = service.get_new_message(max_message_id, GlobalParameter.getSid());
            Log.e(TAG, "get_new_message max_message_id: " + max_message_id);
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
                    if (get_new_message_finish && delete_new_message_finish) {
                        get_new_message_finish = false;
                        get_new_message();

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
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

    private void query_user_feature(){
        lock_user_feature.lock();
        all_user_feature = userFeatureDB.queryAllUserFeature();
        for(int i = 0; i < all_user_feature.size(); i++) {
            Map<String, Object> user_feature = all_user_feature.get(i);
            Log.e(TAG, "feature num: " + i + "/" +
                    all_user_feature.size() + " " + user_feature.get("name"));
        }
        Log.e(TAG, "total feature num " + all_user_feature.size());
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

        a1IoDevManager = A1IoDevManager.initIOManager();
        a1IoDevManager.openIRDA();
        a1IoDevManager.openLED(10);

        Date time_now = new Date();
        detect_face_time = time_now.getTime();
        loadLibraryModule = LoadLibraryModule.getInstance();
        image_view = findViewById(R.id.image_view);
        image_view_infrared = findViewById(R.id.image_view_infrared);
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

        thread_message = new UpdateFeatureThread(this);
        thread_message.start();
        Log.e(TAG, "onCreate " +  Thread.currentThread().getId());
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
        a1IoDevManager.closeIRDA();
        a1IoDevManager.closeLED();
        if(thread_recognition != null) thread_recognition_stop = true;
        try {
            thread_recognition.join();
            //Log.e(TAG, "lifecycle: onDestroy thread_recognition joined");
            thread_message.join();
            //Log.e(TAG, "lifecycle: onDestroy thread_message joined");
        } catch (InterruptedException e){
            e.printStackTrace();
        }
        stopCamera();
        super.onDestroy();
        Log.e(TAG, "lifecycle: onDestroy over");
    }

    private void startCamera() {
        if (mCamera != null || infrared_camera != null) {
            return;
        }
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int camera_index_infrared = 0;
        for (int camera_index = 0; camera_index < Camera.getNumberOfCameras(); camera_index++) {
            Camera.getCameraInfo(camera_index, cameraInfo);
            if (cameraInfo.facing != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                continue;
            }
            if(!enable_spoofing && camera_index == camera_index_infrared) continue;

            Camera camera_local = null;
            try {
                camera_local = Camera.open(camera_index);
            } catch (Exception e) {
                toast("相机不可用！");
                return;
            }
            if(camera_index == camera_index_infrared) infrared_camera = camera_local;
            else mCamera = camera_local;

            Camera.Parameters parameters = camera_local.getParameters();
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
            for (int i = 0; i < focusModes.size(); i++) {
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
            //parameters.setPreviewFpsRange(3000,30000);
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
            //parameters.setPreviewSize(1440, 1080);
            parameters.setPreviewSize(1920, 1080);

            image_size = parameters.getPreviewSize();
            Log.e(TAG, "image_size " + image_size.width + " " + image_size.height + " " + parameters.getFlashMode());

            if(camera_index == camera_index_infrared) {
                //parameters.setMeteringAreas(meteringAreas);
                //parameters.setFocusAreas(meteringAreas);
            }else{
                camera_local.setPreviewCallback(this);
            }
            //parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            camera_local.setParameters(parameters);
            camera_local.setErrorCallback(mErrorCallback);
            camera_local.setDisplayOrientation(90);
            if(camera_index == camera_index_infrared) {
                camera_local.setPreviewCallback(infraredCameraPreviewCallback);
            }else{
                camera_local.setPreviewCallback(this);
            }
            camera_local.startPreview();
            try {
                if(camera_index == camera_index_infrared) {
                    camera_local.setPreviewTexture(surfaceTexture);
                }else{
                    camera_local.setPreviewTexture(surfaceTexture_infrared);
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not preview the image.", e);
            }
            //parameters = mCamera.getParameters();
            //Log.e(TAG, "parameters.flatten " + parameters.flatten());
            //camera_local.stopPreview();
            //camera_local.release();
        }
        //finish();
    }

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallbackWithBuffer(null);
            mCamera.setErrorCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if (infrared_camera != null) {
            infrared_camera.stopPreview();
            infrared_camera.setPreviewCallbackWithBuffer(null);
            infrared_camera.setErrorCallback(null);
            infrared_camera.release();
            infrared_camera = null;
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
        //Log.e(TAG, "onPreviewFrame " +  Thread.currentThread().getId());
        if (data == null) {
            Log.e(TAG, "onPreviewFrame data null");
            return;
        } else {
            long startTime = System.currentTimeMillis();
            /*
            YuvImage yuv = new YuvImage(data, camera_image_format, camera_image_width, camera_image_height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, camera_image_width, camera_image_height), 50, out);
            byte[] bytes = out.toByteArray();
            Bitmap bitmap_data = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            Bitmap bitmap = Utils.rotateResizeBitmap(270, bitmap_data);
            */
            int height_out = image_size.width - 640;
            int[] colors = loadLibraryModule.yuv2bitmap_native(
                    data, image_size.width, image_size.height, height_out);
            Bitmap bitmap = Bitmap.createBitmap(colors, 0, image_size.height,
                    image_size.height, height_out, Bitmap.Config.ARGB_8888);

            lock.lock();
            have_new_image = true;
            current_image_bitmap = bitmap;
            current_image_data = colors;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class InfraredCameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            //Log.e(TAG, "InfraredCameraPreviewCallback " + Thread.currentThread().getId());
            if (data == null) {
                Log.e(TAG, "InfraredCameraPreviewCallback data null");
                return;
            } else {
                int height_out = image_size.width - 640;
                int[] colors = loadLibraryModule.yuv2bitmap_native(
                        data, image_size.width, image_size.height, height_out);
                lock.lock();
                current_image_data_infrared = colors;
                lock.unlock();

                Bitmap bitmap = Bitmap.createBitmap(colors, 0, image_size.height,
                        image_size.height, height_out, Bitmap.Config.ARGB_8888);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        image_view_infrared.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }
}
