package com.example.luyao.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class ManagerActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener{

    private final static String TAG = ManagerActivity.class.getCanonicalName();
    private ListView organization_list;
    private Map<Integer, Map<String, Object>> group_index_to_info = null;
    private int group_operate_position = -1;

    private static final int REQUEST_Group = 1000;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.group_add_button:
                addGroupClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void addGroup(String name, int sort) {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.add_group(GlobalParameter.getOrganization_id(), name, sort, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    getGroupInfo();
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

    public void addGroupClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.group_add, null);
        dialog.setView(dialogView);
        dialog.show();
        EditText text_name = dialogView.findViewById(R.id.text_name);
        EditText text_sort = dialogView.findViewById(R.id.text_sort);
        Button confirm_button = dialogView.findViewById(R.id.confirm_button);
        Button cancel_button = dialogView.findViewById(R.id.cancel_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = text_name.getText().toString();
                final String sort_str = text_sort.getText().toString();
                if (name.isEmpty() || sort_str.isEmpty()) {
                    toast("名字和排序不能为空!");
                    return;
                } else {
                    final int sort = Integer.parseInt(text_sort.getText().toString());
                    addGroup(name, sort);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("返回");
        }
        organization_list = findViewById(R.id.organization_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_add_menu, menu);
        return super.onCreateOptionsMenu(menu);
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
                    group_info.put("name", group_info_json.optString("name") + " (" +
                            group_info_json.optInt("person_count") + ")");
                    group_info.put("sort", group_info_json.optInt("sort"));
                    group_info.put("person_count", group_info_json.optInt("person_count"));
                    group_index_to_info.put(i, group_info);
                    groupList.add(group_info);
                }
                SimpleAdapter simpleAdapter = new SimpleAdapter(ManagerActivity.this, groupList, R.layout.group,
                        new String[] {"name"}, new int[] {R.id.name});
                organization_list.setAdapter(simpleAdapter);
                organization_list.setOnItemClickListener(ManagerActivity.this);
                organization_list.setOnItemLongClickListener(ManagerActivity.this);
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

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.e(TAG, "onItemLongClick: " + position + " " + group_index_to_info.get(position));
        group_operate_position = position;
        AlertDialog.Builder builder = new AlertDialog.Builder(ManagerActivity.this);
        builder.setTitle("");
        builder.setMessage("班级管理");
        builder.setNeutralButton("删除", groupDlgClick);
        builder.setNegativeButton("更新", groupDlgClick);
        builder.setPositiveButton("取消", groupDlgClick);
        builder.show();
        return true;
    }

    private DialogInterface.OnClickListener groupDlgClick = new DialogInterface.OnClickListener() {
        private DialogInterface.OnClickListener mDlgClick = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    deleteGroup((int)group_index_to_info.get(group_operate_position).get("group_id"));
                }
            }
        };

        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_NEGATIVE) {
                updateGroupClient();
            } else if (which == AlertDialog.BUTTON_NEUTRAL) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ManagerActivity.this);
                builder.setTitle("");
                builder.setMessage("确定删除该班级吗?");
                builder.setPositiveButton("确定", mDlgClick);
                builder.setNegativeButton("取消", mDlgClick);
                builder.show();
            } else {
            }
        }
    };

    private void updateGroup(String name, int sort) {
        int group_id = (int)group_index_to_info.get(group_operate_position).get("group_id");
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.update_group(group_id, name, sort, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    getGroupInfo();
                    toast("更新成功");
                } else {
                    toast("连接网络失败，请稍后再试");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                toast("cannot connect to server");
                t.printStackTrace();
            }
        });
    }

    public void updateGroupClient() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.group_update, null);
        dialog.setView(dialogView);
        dialog.show();
        EditText text_name = dialogView.findViewById(R.id.text_name);
        EditText text_sort = dialogView.findViewById(R.id.text_sort);
        text_name.setText((String)group_index_to_info.get(group_operate_position).get("name"));
        text_sort.setText(String.valueOf((int)group_index_to_info.get(group_operate_position).get("sort")));
        Button confirm_button = dialogView.findViewById(R.id.confirm_button);
        Button cancel_button = dialogView.findViewById(R.id.cancel_button);
        confirm_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String name = text_name.getText().toString();
                final String sort = text_sort.getText().toString();
                Log.e(TAG, name + " " + sort);
                if (name.isEmpty() || sort.isEmpty()) {
                    toast("名字和排序不能为空!");
                    return;
                } else {
                    updateGroup(name, Integer.parseInt(sort));
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

    private void deleteGroup(int group_id) {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.delete_group(group_id, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    toast("删除成功");
                    getGroupInfo();
                } else {
                    toast("连接网络失败，请稍后再试");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                toast("cannot connect to server");
                t.printStackTrace();
            }
        });
    }

    private void getGroupInfo() {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.get_all_group(GlobalParameter.getOrganization_id(), GlobalParameter.getSid());
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
