package com.example.luyao.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UserFeature {
    private SQLiteDBHelper helper;
    private SQLiteDatabase db;

    public UserFeature(Context context) {
        helper = new SQLiteDBHelper(context);
        db = helper.getWritableDatabase();
    }

    public Long addUserFeature(String name, String feature) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("feature", feature);
        cv.put("person_id", 0);
        cv.put("have_upload_feature", 0);
        db.beginTransaction();
        try {
            Long id = db.insert(SQLiteDBHelper.TABLE_NAME, null, cv);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void updateUserFeature(ArrayList<Map> user_features) {
        for(int i = 0; i < user_features.size(); i++) {
            Map user_feature = user_features.get(i);
            ContentValues cv = new ContentValues();
            if (user_feature.containsKey("name")) cv.put("name", String.valueOf(user_feature.get("name")));
            if (user_feature.containsKey("feature")) cv.put("feature", String.valueOf(user_feature.get("feature")));
            if (user_feature.containsKey("person_id")) cv.put("person_id", (int)user_feature.get("person_id"));
            if (user_feature.containsKey("have_upload_feature")){
                cv.put("have_upload_feature", (int)user_feature.get("have_upload_feature"));
            }
            db.beginTransaction();
            try {
                db.update(SQLiteDBHelper.TABLE_NAME, cv, "id = ?",
                        new String[]{String.valueOf(user_feature.get("id"))});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deleteUserFeatureById(ArrayList<Integer> ids) {
        for(int i = 0; i < ids.size(); i++) {
            db.beginTransaction();
            try {
                db.delete(SQLiteDBHelper.TABLE_NAME, "id = ?", new String[]{String.valueOf(ids.get(i))});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deleteAllUserFeature() {
            db.beginTransaction();
            try {
                db.delete(SQLiteDBHelper.TABLE_NAME, null, new String[]{});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
    }

    public List<Map<String, Object>> queryUserFeature() {
        ArrayList<Map<String, Object>> user_feature = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "select * from " + SQLiteDBHelper.TABLE_NAME + " order by id asc", new String[] {});
        while (cursor.moveToNext()) {
            HashMap<String, Object> content = new HashMap<>();
            content.put("id", cursor.getInt(cursor.getColumnIndex("id")));
            content.put("person_id", cursor.getInt(cursor.getColumnIndex("person_id")));
            content.put("have_upload_feature", cursor.getInt(cursor.getColumnIndex("have_upload_feature")));
            content.put("feature", cursor.getString(cursor.getColumnIndex("feature")));
            content.put("name", cursor.getString(cursor.getColumnIndex("name")));
            user_feature.add(content);
        }
        cursor.close();
        return user_feature;
    }

    public void close() {
        db.close();
    }
}
