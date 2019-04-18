package com.example.luyao.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
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

public class ManagerActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private final static String TAG = ManagerActivity.class.getCanonicalName();
    private int organization_id;
    private ListView organization_list;
    private Map<Integer, Map<String, Object>> group_index_to_info = null;

    private static final int REQUEST_Group = 1000;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("返回");
        }
        organization_id = getIntent().getIntExtra("organization_id", 0);

        organization_list = findViewById(R.id.organization_list);

    }

    private void fillGroupInfo(final JSONArray group_infos){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Map<String, Object>> groupList = new ArrayList<>();
                group_index_to_info = new HashMap<>();
                for(int i = 0; i < group_infos.length(); i++) {
                    Map<String, Object> group_info = new HashMap<String, Object>();
                    JSONObject group_info_json = group_infos.optJSONObject(i);
                    group_info.put("group_id", group_info_json.optInt("group_id"));
                    group_info.put("name", group_info_json.optString("name"));
                    group_info.put("person_count", group_info_json.optInt("person_count"));
                    group_index_to_info.put(i, group_info);
                    groupList.add(group_info);
                }
                SimpleAdapter simpleAdapter = new SimpleAdapter(ManagerActivity.this, groupList, R.layout.group,
                        new String[] {"name", "person_count"}, new int[] {R.id.name, R.id.person_count});
                organization_list.setAdapter(simpleAdapter);
                organization_list.setOnItemClickListener(ManagerActivity.this);
            }
        });
    }

    private void start_group_activity(int group_id){
        Intent intent = new Intent(this, GroupActivity.class);
        intent.putExtra("group_id", group_id);
        startActivityForResult(intent, REQUEST_Group);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.e(TAG, "position: " + position + " " + group_index_to_info.get(position));
        start_group_activity((int)group_index_to_info.get(position).get("group_id"));
    }

    private void getGroupInfo() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(SimpleHttpClient.BASE_URL).build();
        SimpleHttpClient.ServerAPI service = retrofit.create(SimpleHttpClient.ServerAPI.class);
        Call<ResponseBody> call = service.get_all_group(organization_id);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call,
                                   Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    fillGroupInfo(responseJson.optJSONArray("group_info"));
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

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
        getGroupInfo();
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
