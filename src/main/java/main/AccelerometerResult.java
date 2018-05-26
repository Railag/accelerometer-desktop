package main;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AccelerometerResult extends Result {

    @SerializedName("accelerometer_results")
    public List<AccelerometerResults> accelerometerResults;

    public static class AccelerometerResults implements Comparable<AccelerometerResults> {
        @SerializedName("id")
        public int id;
        @SerializedName("x")
        public List<Double> x;
        @SerializedName("y")
        public List<Double> y;
        @SerializedName("z")
        public List<Double> z;

        @Override
        public int compareTo(AccelerometerResults accelerometerResults) {
            return Integer.valueOf(id).compareTo(accelerometerResults.id);
        }
    }
}
