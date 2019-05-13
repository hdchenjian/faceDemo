package com.example.luyao.myapplication;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;


public class SimpleHttpClient {
    public static String BASE_URL = "http://www.duolekong.com/api/";

    public interface ServerAPI {
        @FormUrlEncoded
        @POST("login")
        Call<ResponseBody> login(@Field("phone") String phone, @Field("password") String password,
                @Field("mac") String mac);

        @GET("get_all_group")
        Call<ResponseBody> get_all_group(@Query("organization_id") int organization_id, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("add_group")
        Call<ResponseBody> add_group(@Field("organization_id") int organization_id,
                                     @Field("name") String name, @Field("sort") int sort, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("delete_group")
        Call<ResponseBody> delete_group(@Field("group_id") int group_id, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("update_group")
        Call<ResponseBody> update_group(@Field("group_id") int group_id, @Field("name") String name,
                                        @Field("sort") int sort, @Header("sid") String sid);

        @GET("get_group_person")
        Call<ResponseBody> get_group_person(@Query("group_id") int group_id, @Header("sid") String sid);

        @GET("get_person")
        Call<ResponseBody> get_person(@Query("person_id") int person_id, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("update_person")
        Call<ResponseBody> update_person(@Field("person_id") int person_id, @Field("name") String name,
                                         @Field("phone") String phone, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("delete_person")
        Call<ResponseBody> delete_person(@Field("person_id") int person_id, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("add_person")
        Call<ResponseBody> add_person(@Field("group_id") int group_id, @Field("name") String name,
                                      @Field("phone") String phone, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("delete_relation")
        Call<ResponseBody> delete_relation(@Field("relation_id") int relation_id, @Header("sid") String sid);

        @FormUrlEncoded
        @POST("delete_new_message")
        Call<ResponseBody> delete_new_message(@Field("message_ids") List<Integer> message_ids,
                                              @Header("sid") String sid);

        @GET("get_new_message")
        Call<ResponseBody> get_new_message(@Query("message_id") int message_id, @Header("sid") String sid);

        @GET("get_all_person_feature")
        Call<ResponseBody> get_all_person_feature(@Header("sid") String sid);

        @Multipart
        @POST("upload_recognition_image")
        Call<ResponseBody> upload_recognition_image(@Part MultipartBody.Part image,
                                                    @Part("relation_ids") RequestBody relation_ids,
                                                    @Part("time") RequestBody time,
                                                    @Header("sid") String sid);

    }
}