package main;

public class Utils {
    private final static double NANO = 1000000000;

    public static double calcTime(long startTime) {
        long currTime = System.nanoTime();
        long diff = currTime - startTime;

        return diff / NANO;
    }

}
