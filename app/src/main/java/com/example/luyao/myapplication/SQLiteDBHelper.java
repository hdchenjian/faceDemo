package com.example.luyao.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteDBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "face.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "user_feature";

    public SQLiteDBHelper(Context context) {
        //CursorFactory设置为null,使用默认值
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //数据库第一次被创建时onCreate会被调用
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table if not exists user_feature " +
                "(`relation_id` integer primary key not null, " +
                "`relation` varchar(64) not null, " +
                "`feature` text not null, " +
                "`is_child` integer not null, " +
                "`person_id` integer not null, " +
                "`head_picture` varchar(255) not null, " +
                "`name` varchar(64) not null)");
    }

    //如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}