package main;

public class Log {
    public static void i(String tag, String message) {
        if (!TextUtils.isEmpty(tag) && !TextUtils.isEmpty(message)) {
            System.out.println(tag.toUpperCase() + ": " + message);
        } else {
            System.out.println("Log error, empty value");
        }
    }
}