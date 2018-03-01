package sample;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface RConnectorService {
    //String API_ENDPOINT = "http://127.0.0.1:3000";
    //String API_ENDPOINT = "http://10.0.2.2:3000";
    //String API_ENDPOINT = "http://10.0.3.2:3000";
    String API_ENDPOINT = "https://firrael.herokuapp.com";

    @FormUrlEncoded
    @POST("/user/login")
    Call<UserResult> login(@Field("login") String login, @Field("password") String password);

    @FormUrlEncoded
    @POST("/user/accelerometer")
    Call<AccelerometerResult> fetchAccelerometerData(@Field("user_id") long userId);
}