package me.chen_wei.retrofitdemo.api;

import me.chen_wei.retrofitdemo.model.Top250;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;

/**
 * Created by Hander on 15/6/18.
 */
public interface DoubanAPI {

    @GET("/v2/movie/top250")
    public void getResult(@Query("start") int start, @Query("count") int count,
                          Callback<Top250> response);
}
