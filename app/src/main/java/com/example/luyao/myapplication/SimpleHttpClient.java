package com.example.luyao.myapplication;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;


public class SimpleHttpClient {
    public static String BASE_URL = "http://120.79.161.218:5055";

    public static class UserInfo {
        public final String name;
        public final String nikename;
        public final String logo;
        public UserInfo(String name, String nikename, String logo) {
            this.name = name;
            this.nikename = nikename;
            this.logo = logo;
        }
    }

    public interface ServerAPI {
        @FormUrlEncoded
        @POST("login")
        Call<ResponseBody> login(@Field("phone") String phone, @Field("password") String password);
    }
}