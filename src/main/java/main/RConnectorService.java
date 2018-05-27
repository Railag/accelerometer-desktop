package main;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

import java.util.ArrayList;

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

    @FormUrlEncoded
    @POST("/user/startup_login")
    Call<UserResult> startupLogin(@Field("login") String login, @Field("token") String token);

    @FormUrlEncoded
    @POST("/user")
    Call<UserResult> createAccount(@Field("login") String login, @Field("password") String password, @Field("email") String email, @Field("age") int age, @Field("time") int time);

    @FormUrlEncoded
    @POST("/user/results_focusing")
    Call<Result> sendFocusingResults(@Field("user_id") long userId, @Field("times[]") ArrayList<Double> times, @Field("error_values[]") ArrayList<Long> errors);

    @FormUrlEncoded
    @POST("/user/results_volume")
    Call<Result> sendAttentionDistributionResults(@Field("user_id") long userId, @Field("wins") long wins, @Field("fails") long fails, @Field("misses") long misses);

    @FormUrlEncoded
    @POST("/user/results_stability")
    Call<Result> sendAttentionStabilityResults(@Field("user_id") long userId, @Field("times[]") ArrayList<Double> times, @Field("misses") long misses, @Field("errors_value") long errors);

    @FormUrlEncoded
    @POST("/user/results_attention_volume")
    Call<Result> sendAttentionVolumeResults(@Field("user_id") long userId, @Field("time") Double time, @Field("wins") long wins);

   /* @FormUrlEncoded
    @POST("/user/fcm_token")
    Call<Result> sendFCMToken(@Field("user_id") long userId, @Field("fcm_token") String fcmToken);

    @FormUrlEncoded
    @POST("/user/update")
    Observable<UserResult> updateInfo(@Field("user_id") long userId, @Field("email") String email, @Field("age") int age, @Field("time") int time);

    @FormUrlEncoded
    @POST("/user/results_stress")
    Observable<Result> sendStressResults(@Field("user_id") long userId, @Field("times[]") ArrayList<Double> times, @Field("misses") long misses);

    @FormUrlEncoded
    @POST("/user/results_focusing")
    Observable<Result> sendFocusingResults(@Field("user_id") long userId, @Field("times[]") ArrayList<Double> times, @Field("error_values[]") ArrayList<Long> errors);

    @FormUrlEncoded
    @POST("/user/results_stability")
    Observable<Result> sendAttentionStabilityResults(@Field("user_id") long userId, @Field("times[]") ArrayList<Double> times, @Field("misses") long misses, @Field("errors_value") long errors);

    @FormUrlEncoded
    @POST("/user/results_english")
    Observable<Result> sendEnglishResults(@Field("user_id") long userId, @Field("times[]") ArrayList<Double> times, @Field("words[]") ArrayList<String> words, @Field("errors_value") long errors);

    @FormUrlEncoded
    @POST("/user/results_accelerometer")
    Observable<Result> sendAccelerometerResults(@Field("user_id") long userId, @Field("x[]") ArrayList<Double> x,
                                                @Field("y[]") ArrayList<Double> y);

    @FormUrlEncoded
    @POST("/user/results_ram_volume")
    Observable<Result> sendRAMVolumeResults(@Field("user_id") long userId, @Field("time") Double time, @Field("wins") long wins);

    @FormUrlEncoded
    @POST("/user/statistics")
    Observable<StatisticsResult> fetchStatistics(@Field("user_id") long userId);*/
}