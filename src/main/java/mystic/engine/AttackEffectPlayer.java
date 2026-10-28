package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;

/**
 * Plays the 5 x 5 slash effect sheet (24 used frames, 192 x 192 each).
 * The sheet is loaded once and shared by every fighter.
 */
public final class AttackEffectPlayer {

    private static final String SHEET_PATH = "/assets/effects/slash_effect_sheet.png";

    private static final int COLUMNS = 5;
    private static final int FRAME_COUNT = 24;
    private static final double CELL = 192.0;

    private static Image sheet;
    private static boolean loadAttempted;

    private boolean playing;
    private double time; // starts negative while waiting for the delay
    private double duration;
    private int firstFrame;
    private double hue;

    private static Image getSheet() {
        if (!loadAttempted) {
            loadAttempted = true;

            var stream = AttackEffectPlayer.class
                    .getResourceAsStream(SHEET_PATH);

            if (stream == null) {
                System.err.println("Effect sheet not found: " + SHEET_PATH);
            } else {
                Image image = new Image(stream);
                sheet = image.isError() ? null : image;
            }
        }

        return sheet;
    }

    /**
     * @param duration   seconds the animation takes to play
     * @param delay      seconds to wait before it becomes visible
     * @param firstFrame first frame index to show (0-23). Frames 0-2 are a
     *                   starburst flash, 3+ are the slash itself.
     * @param hue        ColorAdjust hue shift (-1..1), 0 keeps the red look
     */
    public void play(double duration, double delay, int firstFrame, double hue) {
        this.duration = Math.max(0.05, duration);
        this.time = -Math.max(0.0, delay);
        this.firstFrame = Math.max(0, Math.min(FRAME_COUNT - 1, firstFrame));
        this.hue = hue;
        this.playing = true;
    }

    public void stop() {
        playing = false;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void update(double dt) {
        if (!playing || !Double.isFinite(dt) || dt <= 0) {
            return;
        }

        time += dt;

        if (time >= duration) {
            playing = false;
        }
    }

    /** Draws the effect centred on (centerX, centerY). */
    public void draw(
            GraphicsContext gc,
            double centerX,
            double centerY,
            double size,
            boolean flipHorizontally) {

        if (!playing || time < 0) {
            return;
        }

        Image image = getSheet();

        if (image == null) {
            return;
        }

        int frames = FRAME_COUNT - firstFrame;
        int index = firstFrame
                + Math.min(frames - 1, (int) (time / duration * frames));

        double sx = (index % COLUMNS) * CELL;
        double sy = (index / COLUMNS) * CELL;

        gc.save();

        try {
            gc.translate(centerX, centerY);

            if (flipHorizontally) {
                gc.scale(-1, 1);
            }

            if (hue != 0.0) {
                gc.setEffect(new ColorAdjust(hue, 0, 0, 0));
            }

            gc.drawImage(
                    image,
                    sx, sy, CELL, CELL,
                    -size / 2.0, -size / 2.0, size, size);
        } finally {
            gc.restore();
        }
    }
}