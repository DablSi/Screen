package com.example.ducks.screen;


import retrofit2.Call;
import retrofit2.http.*;

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
    public Void putCoords(@Part int room, @Part int x1, @Part int y1, @Part int x2, @Part int y2, @Part int color);

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
    public Call<Integer> getColor(@Path("device") String device);

    //Получение номера комнаты
    //Camera +
    @GET("/get/room")
    public Call<Integer> getRoom();

    //Добавить видео
    @Multipart
    @POST("/post/video")
    public Call<Void> putVideo(@Part("bytes") String bytes, @Part("room") int room);

    //Получение видео
    @GET("/get/video/{room}")
    public Call<byte[]> getVideo(@Path("room") int room);

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
