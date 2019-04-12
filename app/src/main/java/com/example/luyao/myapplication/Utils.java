package com.example.luyao.myapplication;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class Utils {
    public static final String md5(final String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static final JSONObject parseResponse(Response<ResponseBody> response, String TAG) {
        int code = response.code();
        Log.d(TAG, code + response.toString());
        JSONObject responseJson = null;
        try {
            if (code == 200) {
                String response_str = response.body().string();
                responseJson = new JSONObject(response_str);
                Log.d(TAG, responseJson.toString());

            } else {
                String error_str = response.errorBody().string();
                //error_detail = error_detail.replaceAll("\"", "\\\\\"");
                responseJson = new JSONObject(error_str);
                Log.d(TAG, responseJson.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return responseJson;
    }

    public static final Long getDatetimeString() {
        //SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //Date now = new Date();
        //return format.format(now)
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        return ts.getTime() / 1000;
    }
}