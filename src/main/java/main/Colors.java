package main;

import javafx.scene.Node;
import javafx.scene.control.Control;

public class Colors {
    public final static String COLOR_PRIMARY = "#3F51B5";
    public final static String COLOR_PRIMARY_DARK = "#303F9F";
    public final static String COLOR_ACCENT = "#FF4081";
    public final static String COLOR_BLACK = "#000000";
    public final static String COLOR_GREEN_STRESS = "#00CC00";
    public final static String COLOR_RED_REACTION = "#CC0000";
    public final static String COLOR_YELLOW_REACTION = "#EEEE00";
    public final static String COLOR_GREY_REACTION = "#444444";
    public final static String COLOR_PURPLE = "#572699";
    public final static String COLOR_WHITE = "#FFFFFF";
    public final static String COLOR_DARKER_GRAY = "#aaa";

    public static void setBackgroundColor(Control control, String color) {
        control.setStyle("-fx-background: " + color + ";");
    }
}
