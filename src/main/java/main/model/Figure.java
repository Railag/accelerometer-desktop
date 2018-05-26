package main.model;


import java.util.Random;



public enum Figure {
    OVAL,
    RECTANGLE,
    ROUND,
    SQUARE,
    STAR,
    TRAPEZIUM,
    TRIANGLE;

    public String getDrawableString() {
        switch (this) {
            case OVAL:
                return "oval.png";
            case RECTANGLE:
                return "rectangle.png";
            case ROUND:
                return "round.png";
            case SQUARE:
                return "rectangle2.png";
            case STAR:
                return "star.png";
            case TRAPEZIUM:
                return "trapezium.png";
            case TRIANGLE:
            default:
                return "triangle.png";
        }
    }

    public static Figure random() {
        return randomEnum(Figure.class);
    }

    private static final Random random = new Random();

    private static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }
}