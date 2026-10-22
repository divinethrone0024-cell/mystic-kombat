package mystic.common;

/** Numbers shared by the whole game. Change them here, nowhere else. */
public final class Constants {
    private Constants() {
    }

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    public static final double GROUND_Y = 600; // y of the floor
    public static final double GRAVITY = 2200; // pixels per second squared
    public static final int ROUNDS_TO_WIN = 2; // best of three
    public static final double ROUND_SECONDS = 60;
}