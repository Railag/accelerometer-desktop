package main.model;

import java.util.List;
import java.util.Random;


public enum Circle {
    TOP_LEFT,
    TOP_RIGHT,
    DOWN_RIGHT,
    DOWN_LEFT;

    public static int rotation(Circle circle) {
        switch (circle) {

            case TOP_RIGHT:
                return 180;
            case TOP_LEFT:
                return 90;
            case DOWN_RIGHT:
                return 270;
            case DOWN_LEFT:
            default:
                return 0;
        }
    }

    public static Circle random() {
        return randomEnum(Circle.class);
    }

    private static final Random random = new Random();

    private static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    public static int answer(List<Circle> circles, Circle circle) {
        int count = 0;
        for (Circle c : circles) {
            if (circle.equals(c)) {
                count++;
            }
        }

        return count;
    }
}