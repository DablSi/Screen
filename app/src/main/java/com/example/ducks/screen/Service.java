package com.example.ducks.screen;


import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

public interface Service {

    //Добавить девайс
    @Multipart
    @POST("/post")
    public Call<Void> putDevice(@Part("device") String device, @Part("room") Integer room, @Part("date") Long date);

    //Добавить координаты
    //Camera
    @Multipart
    @POST("/post/coords")
    public Call<Void> putCoords(@Part("room") int room, @Part("x1") int x1, @Part("y1") int y1, @Part("x2") int x2, @Part("y2") int y2, @Part("color") int[] color);

    //Получить координаты
    //Video
    @GET("/get/coords/{device}")
    public Call<Coords> getCoords(@Path("device") String device);

    //Получить время запуска
    //Search +
    @GET("/get/{device}")
    public Call<Long> getTime(@Path("device") String device);

    //Получить цвет
    //Search +
    @GET("/get/color/{device}")
    public Call<int[]> getColor(@Path("device") String device);

    //Получение номера комнаты
    //Camera +
    @GET("/get/room")
    public Call<Integer> getRoom();

    @GET(value = "/download/{room}")
    @Streaming
    public Call<ResponseBody> getFile(@Path("room") int room);

    @Multipart
    @POST(value = "/upload")
    public Call<Void> uploadVideo(@Part MultipartBody.Part video, @Part("room") int room);

    //Получение массива цветов
    @GET("/get/colors")
    public Call<int[]> getColors();

    //Добавить время запуска видео
    @Multipart
    @POST("/post/startVideo")
    public Call<Void> putStartVideo(@Part("room") Integer room, @Part("date") Long date);

    //Получить время запуска видео
    @GET("/get/startVideo/{device}")
    public Call<Long> getStartVideo(@Path("device") String device);

    //Получение индексов цветов
    @GET("/get/indexes/{room}")
    public Call<int[]> getIndexes(@Path("room") int room);

    //Данные каждого гаджета
    class DeviceData {
        public Integer color, room;
        public Coords coords;

        public DeviceData(int newRoom) {
            room = newRoom;
        }
    }

    //Данные каждой комнаты
    class RoomData {
        public LinkedList<String> deviceList;
        public Long time;
        public byte[] video;

        public RoomData() {
            deviceList = new LinkedList<>();
        }
    }

    //Класс координат
    class Coords {
        public Integer x1, y1, x2, y2;

        public Coords(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
        }
    }


}
