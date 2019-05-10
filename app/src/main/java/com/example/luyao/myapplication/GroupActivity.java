package com.example.luyao.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class GroupActivity extends AppCompatActivity{

    private final static String TAG = GroupActivity.class.getCanonicalName();
    private int group_id;
    private ListView person_list;
    private ActionBar actionBar;
    private Map<Integer, Map<String, Object>> person_index_to_info = null;

    private static final int REQUEST_PERSON = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);
        actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("返回");
        }
        group_id = getIntent().getIntExtra("group_id", 0);
        person_list = findViewById(R.id.person_list);
    }

    private void fillGroupInfo(final JSONObject group_info_json){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Map<String, Object>> personList = new ArrayList<>();
                person_index_to_info = new HashMap<>();
                JSONArray group_person_json = group_info_json.optJSONArray("person");
                if(actionBar != null) {
                    String title = group_info_json.optString("name") + "   (" + group_person_json.length() + ")";
                    actionBar.setTitle(title);
                }
                for(int j = 0; j < group_person_json.length(); j++) {
                    HashMap<String, Object> group_person = new HashMap<>();
                    JSONObject person_json =  group_person_json.optJSONObject(j);
                    group_person.put("name", person_json.optString("name"));
                    group_person.put("phone", person_json.optString("phone"));
                    group_person.put("person_id", person_json.optInt("person_id"));
                    String head_picture = SimpleHttpClient.BASE_URL + person_json.optString("head_picture");
                    group_person.put("head_picture", head_picture);
                    group_person.put("relation_count", person_json.optInt("relation_count"));
                    person_index_to_info.put(j, group_person);
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", person_json.optString("name") +
                            " (" + person_json.optInt("relation_count") + ")");
                    map.put("head_picture", head_picture);
                    personList.add(map);
                }
                CustomAdapter customAdapter = new CustomAdapter(
                        GroupActivity.this, personList, person_index_to_info,
                        GroupActivity.this, 100);
                person_list.setAdapter(customAdapter);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.person_add_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void start_person_activity(int person_id){
        Intent intent = new Intent(this, PersonActivity.class);
        intent.putExtra("person_id", person_id);
        startActivityForResult(intent, REQUEST_PERSON);
    }

    private void getGroupPersonInfo() {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.get_group_person(group_id, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    fillGroupInfo(responseJson);
                } else {
                    toast("连接网络失败，请稍后再试");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Log error here since request failed
                toast("cannot connect to server");
                t.printStackTrace();
            }
        });
    }

    private void addPerson(String name, String phone) {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.add_person(group_id, name, phone, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    getGroupPersonInfo();
                    toast("添加成功");
                } else {
                    toast("连接网络失败，请稍后再试");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Log error here since request failed
                toast("cannot connect to server");
                t.printStackTrace();
            }
        });
    }

    public void addPersonClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.person_add, null);
        dialog.setView(dialogView);
        dialog.show();
        EditText text_name = dialogView.findViewById(R.id.text_name);
        EditText text_parent_phone = dialogView.findViewById(R.id.text_parent_phone);
        Button confirm_button = dialogView.findViewById(R.id.confirm_button);
        Button cancel_button = dialogView.findViewById(R.id.cancel_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = text_name.getText().toString();
                final String phone = text_parent_phone.getText().toString();
                Log.e(TAG, name + " " + phone);
                if (name.isEmpty() || phone.isEmpty()) {
                    toast("名字和手机号不能为空!");
                    return;
                } else {
                    addPerson(name, phone);
                }
                dialog.dismiss();
            }
        });
        cancel_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.person_add_button:
                addPersonClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
        getGroupPersonInfo();
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
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
