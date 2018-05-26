package main.model;


public enum Difficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3);

    private int level;

    Difficulty(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static Difficulty create(int level) {
        switch (level) {
            case 1:
                return EASY;
            case 2:
                return MEDIUM;
            case 3:
                return HARD;

            default:
                return MEDIUM;
        }
    }
}
