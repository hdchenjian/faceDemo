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

public class UserFeatureDB {
    private SQLiteDBHelper helper;
    private SQLiteDatabase db;

    public UserFeatureDB(Context context) {
        helper = new SQLiteDBHelper(context);
        db = helper.getWritableDatabase();
    }

    public Long addUserFeature(int person_id, String feature, String head_picture, String name) {
        ContentValues cv = new ContentValues();
        cv.put("person_id", person_id);
        cv.put("feature", feature);
        cv.put("head_picture", head_picture);
        cv.put("name", name);
        db.beginTransaction();
        try {
            Long id = db.insert(SQLiteDBHelper.TABLE_NAME, null, cv);
            db.setTransactionSuccessful();
            return id;
        } finally {
            db.endTransaction();
        }
    }

    public void updateUserFeature(int person_id, String feature, String head_picture, String name) {
            ContentValues cv = new ContentValues();
        cv.put("person_id", person_id);
        cv.put("feature", feature);
        cv.put("head_picture", head_picture);
        cv.put("name", name);
        db.beginTransaction();
            try {
                db.update(SQLiteDBHelper.TABLE_NAME, cv, "person_id = ?",
                        new String[]{String.valueOf(person_id)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
    }

    public void deleteUserFeatureById(int person_id) {
            db.beginTransaction();
            try {
                db.delete(SQLiteDBHelper.TABLE_NAME, "person_id = ?",
                        new String[]{String.valueOf(person_id)});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
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

    public int queryMaxId() {
        Cursor cursor = db.rawQuery(
                "select * from " + SQLiteDBHelper.TABLE_NAME + " order by person_id desc limit 1", new String[] {});
        int max_id = 1000;
        while (cursor.moveToNext()) {
            max_id = cursor.getInt(cursor.getColumnIndex("person_id"));
        }
        cursor.close();
        return max_id;
    }

    public List<Map<String, Object>> queryAllUserFeature() {
        ArrayList<Map<String, Object>> user_feature = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "select * from " + SQLiteDBHelper.TABLE_NAME + " order by person_id asc", new String[] {});
        while (cursor.moveToNext()) {
            HashMap<String, Object> content = new HashMap<>();
            String feature_str = cursor.getString(cursor.getColumnIndex("feature"));
            String[] feature_str_list = feature_str.split(",");
            int feature_length = 512;
            float[] feature_float = new float[feature_length];
            for(int i = 0; i < feature_length; i++) {
                feature_float[i] = Float.parseFloat(feature_str_list[i]);
            }
            content.put("feature", feature_float);
            content.put("person_id", cursor.getInt(cursor.getColumnIndex("person_id")));
            content.put("head_picture", cursor.getString(cursor.getColumnIndex("head_picture")));
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
