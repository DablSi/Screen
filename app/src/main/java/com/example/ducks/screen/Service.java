package com.example.ducks.screen;


import retrofit2.Call;
import retrofit2.http.*;

public interface Service {

    @GET("/get/{device}")
    public Call<Long> get(@Path("device") String device);

    @Multipart
    @POST("/post")
    public Call<Void> put(@Part("device") String device, @Part("room") Integer room, @Part("date") Long date);
}
