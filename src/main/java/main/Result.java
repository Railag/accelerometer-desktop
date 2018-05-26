package main;

public class Result {
    public String error;
    public String result;

    public boolean invalid() {
        return error != null;
    }
}
