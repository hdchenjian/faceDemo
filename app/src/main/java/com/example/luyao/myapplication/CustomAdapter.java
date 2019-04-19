package com.example.luyao.myapplication;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;

public class CustomAdapter extends BaseAdapter{
    private final static String TAG = CustomAdapter.class.getCanonicalName();
    private static final int REQUEST_PERSON = 10000;

    private Context context;
    private ArrayList<Map<String, Object>> listContent;
    private static LayoutInflater inflater = null;
    private Map<Integer, Map<String, Object>>  person_index_to_info = null;
    private WeakReference<AppCompatActivity> activity;
    private int clickStartIntent;

    public CustomAdapter(Context context, ArrayList<Map<String, Object>> listContent,
                         Map<Integer, Map<String, Object>>person_index_to_info, AppCompatActivity activity,
                         int clickStartIntent) {
        this.context = context;
        this.listContent = listContent;
        this.inflater = ( LayoutInflater )context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.person_index_to_info = person_index_to_info;
        this.activity = new WeakReference<AppCompatActivity>(activity);
        this.clickStartIntent = clickStartIntent;
    }
    @Override
    public int getCount() {
        return listContent.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView;
        rowView = inflater.inflate(R.layout.person, null);
        TextView name = rowView.findViewById(R.id.name);
        ImageView head_image = rowView.findViewById(R.id.head_image);
        name.setText((String)listContent.get(position).get("name"));
        String head_picture_url = (String)listContent.get(position).get("head_picture");
        if(head_picture_url.isEmpty()){
            head_image.setImageResource(R.drawable.default_head_image);
        } else {
            Glide.with(context).load(head_picture_url).into(head_image);
        }
        if(clickStartIntent == 100) {
            rowView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, "You Clicked " + (String) listContent.get(position).get("name"));
                    Intent intent = new Intent(context, PersonActivity.class);
                    int person_id = (int) person_index_to_info.get(position).get("person_id");
                    intent.putExtra("person_id", person_id);
                    ((GroupActivity) (activity.get())).start_person_activity(person_id);

                }
            });
        }
        return rowView;
    }
}
