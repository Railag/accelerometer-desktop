package main.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public enum Sign {
    TYPE1,
    TYPE2,
    TYPE3,
    TYPE4,
    TYPE5,
    TYPE6,
    TYPE7,
    TYPE8,
    TYPE9,
    TYPE10,
    TYPE11,
    TYPE12,
    TYPE13,
    TYPE14,
    TYPE15,
    TYPE16,
    TYPE17,
    TYPE18,
    TYPE19,
    TYPE20;

    public String getDrawableString() {
        switch (this) {
            case TYPE1:
                return "oval.png";
            case TYPE2:
                return "oval.png";
            case TYPE3:
                return "oval.png";
            case TYPE4:
                return "oval.png";
            case TYPE5:
                return "rectangle.png";
            case TYPE6:
                return "rectangle.png";
            case TYPE7:
                return "rectangle.png";
            case TYPE8:
                return "rectangle.png";
            case TYPE9:
                return "rectangle2.png";
            case TYPE10:
                return "rectangle2.png";
            case TYPE11:
                return "rectangle2.png";
            case TYPE12:
                return "trapezium.png";
            case TYPE13:
                return "trapezium.png";
            case TYPE14:
                return "trapezium.png";
            case TYPE15:
                return "trapezium.png";
            case TYPE16:
                return "trapezium.png";
            case TYPE17:
                return "triangle.png";
            case TYPE18:
                return "triangle.png";
            case TYPE19:
                return "triangle.png";
            case TYPE20:
                return "triangle.png";
            default:
                return "triangle.png";
        }
    }

    private int counter;

    public int getCounter() {
        return counter;
    }

    public void increase() {
        counter++;
    }

    public static List<Sign> randomSigns(int min, int max) {
        int size = random.nextInt((max - min) + 1) + min;

        List<Sign> signs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Sign sign = randomEnum(Sign.class);
            if (!signs.contains(sign)) {
                signs.add(sign);
            } else {
                i--;
            }
        }
        return signs;
    }

    private boolean wasShown;
    private boolean selected;
    private boolean chosen;

    public boolean wasShown() {
        return wasShown;
    }

    public void setShown(boolean shown) {
        wasShown = shown;
    }

    public void reset() {
        wasShown = false;
        counter = 0;
    }

    public static List<Sign> randomSigns(int size) {
        List<Sign> signs = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Sign sign = randomEnum(Sign.class);
            if (!signs.contains(sign)) {
                signs.add(sign);
            } else {
                i--;
            }
        }
        return signs;
    }

    private static final Random random = new Random();

    private static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isChosen() {
        return chosen;
    }

    public void setChosen(boolean chosen) {
        this.chosen = chosen;
    }
}

