package com.example.luyao.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class PersonActivity extends AppCompatActivity{

    private final static String TAG = PersonActivity.class.getCanonicalName();
    private int person_id;
    private String person_name;
    private String person_parent_phone;

    private ListView relation_list;
    private ActionBar actionBar;
    private Map<Integer, Map<String, Object>> relation_index_to_info = null;
    private int relation_delete_position = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);
        actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("返回");
        }
        person_id = getIntent().getIntExtra("person_id", 0);
        relation_list = findViewById(R.id.relation_list);

        relation_list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, "position: " + position + " " + relation_index_to_info.get(position));
                relation_delete_position = position;
                AlertDialog.Builder builder = new AlertDialog.Builder(PersonActivity.this);
                builder.setTitle("");
                builder.setMessage("您确定删除该家长吗?");
                builder.setPositiveButton("确定", relationDlgClick);
                builder.setNegativeButton("取消", relationDlgClick);
                builder.show();
                return true;
            }
        });
        getPersonInfo();
    }

    private DialogInterface.OnClickListener relationDlgClick = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                deleteRelation((int)relation_index_to_info.get(relation_delete_position).get("relation_id"));
            } else {
            }
        }
    };

    private void deleteRelation(int relation_id) {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.delete_relation(relation_id, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    toast("删除成功");
                    getPersonInfo();
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

    private void fillPersonInfo(final JSONObject person_info_json){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Map<String, Object>> relationList = new ArrayList<>();
                relation_index_to_info = new HashMap<>();
                JSONArray person_relation_json = person_info_json.optJSONArray("relation");
                if(actionBar != null) {
                    String title = person_info_json.optString("name") + "   (" + person_relation_json.length() + ")";
                    actionBar.setTitle(title);
                }
                person_name = person_info_json.optString("name");
                person_parent_phone = person_info_json.optString("phone");

                ArrayList<Map<String, Object>> person_relations = new ArrayList<>();
                for(int k = 0; k < person_relation_json.length(); k++) {
                    HashMap<String, Object> person_relation = new HashMap<>();
                    JSONObject relation_json = person_relation_json.optJSONObject(k);
                    person_relation.put("relation_id", relation_json.optInt("relation_id"));
                    person_relation.put("relation", relation_json.optString("relation"));
                    String head_picture = SimpleHttpClient.BASE_URL + relation_json.optString("head_picture");
                    person_relation.put("head_picture", head_picture);
                    person_relations.add(person_relation);
                    relation_index_to_info.put(k, person_relation);
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", relation_json.optString("relation"));
                    map.put("head_picture", head_picture);
                    relationList.add(map);
                }
                CustomAdapter customAdapter = new CustomAdapter(
                        PersonActivity.this, relationList, relation_index_to_info, PersonActivity.this, 0);
                relation_list.setAdapter(customAdapter);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.person_update_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void getPersonInfo() {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.get_person(person_id, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    fillPersonInfo(responseJson);
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

    private void updatePerson(String name, String phone) {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.update_person(person_id, name, phone, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    getPersonInfo();
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

    public void updatePersonClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final AlertDialog dialog = builder.create();
        View dialogView = View.inflate(this, R.layout.person_update, null);
        dialog.setView(dialogView);
        dialog.show();
        EditText text_name = dialogView.findViewById(R.id.text_name);
        EditText text_parent_phone = dialogView.findViewById(R.id.text_parent_phone);
        text_name.setText(person_name);
        text_parent_phone.setText(person_parent_phone);
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
                    updatePerson(name, phone);
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

    private void deletePerson() {
        SimpleHttpClient.ServerAPI service = Utils.getHttpClient(6);
        Call<ResponseBody> call = service.delete_person(person_id, GlobalParameter.getSid());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                JSONObject responseJson = Utils.parseResponse(response, TAG);
                if (response.code() == 200) {
                    getPersonInfo();
                    toast("删除成功");
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

    private DialogInterface.OnClickListener mDlgClick = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            if (which == AlertDialog.BUTTON_POSITIVE) {
                deletePerson();
                finish();
            } else {
            }
        }
    };

    public void deletePersonClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PersonActivity.this);
        builder.setTitle("");
        builder.setMessage("您确定删除该学生吗?");
        builder.setPositiveButton("确定", mDlgClick);
        builder.setNegativeButton("取消", mDlgClick);
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.person_update_button:
                updatePersonClick();
                return true;
            case R.id.person_delete_button:
                deletePersonClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "lifecycle: onStart");
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
