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

        @GET("get_all_group")
        Call<ResponseBody> get_all_group(@Query("organization_id") int organization_id);

        @FormUrlEncoded
        @POST("add_group")
        Call<ResponseBody> add_group(@Field("organization_id") int organization_id, @Field("name") String name,
                                     @Field("sort") int sort);

        @FormUrlEncoded
        @POST("delete_group")
        Call<ResponseBody> delete_group(@Field("group_id") int group_id);

        @FormUrlEncoded
        @POST("update_group")
        Call<ResponseBody> update_group(@Field("group_id") int group_id, @Field("name") String name,
                                        @Field("sort") int sort);

        @GET("get_group_person")
        Call<ResponseBody> get_group_person(@Query("group_id") int group_id);

        @GET("get_person")
        Call<ResponseBody> get_person(@Query("person_id") int person_id);

        @FormUrlEncoded
        @POST("update_person")
        Call<ResponseBody> update_person(@Field("person_id") int person_id, @Field("name") String name,
                                         @Field("phone") String phone);

        @FormUrlEncoded
        @POST("delete_person")
        Call<ResponseBody> delete_person(@Field("person_id") int person_id);

        @FormUrlEncoded
        @POST("add_person")
        Call<ResponseBody> add_person(@Field("group_id") int group_id, @Field("name") String name,
                                      @Field("phone") String phone);

        @FormUrlEncoded
        @POST("delete_relation")
        Call<ResponseBody> delete_relation(@Field("relation_id") int relation_id);

    }
}