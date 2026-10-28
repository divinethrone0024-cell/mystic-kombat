package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.EnumMap;
import java.util.Map;

public final class CharacterAnimationController {

    public enum Pose {
        IDLE,
        WALK,
        JUMP,
        CROUCH,
        LIGHT_ATTACK,
        HEAVY_ATTACK,
        HIT,
        SPECIAL,
        ULTIMATE,
        KO
    }

    public record Frame(
            double x, double y,
            double width, double height) {
    }

    /** Height of the standing frame; every pose is scaled by the same factor. */
    private static final double MAGE_REFERENCE_HEIGHT = 194.0;

    private final Image sheet;
    private final Map<Pose, Frame> frames = new EnumMap<>(Pose.class);

    private double referenceHeight = MAGE_REFERENCE_HEIGHT;
    private boolean pixelArt;

    private Pose currentPose = Pose.IDLE;
    private double elapsed;
    private double glowTime;

    /** Sprite set for a fighter other than the mage (see SpriteSets). */
    public static CharacterAnimationController forSet(String id) {
        return new CharacterAnimationController(
                "/assets/characters/" + id + "/sheet.png",
                SpriteSets.get(id),
                true);
    }

    /** The mage's sheet and crop table. */
    public CharacterAnimationController(String resourcePath) {
        this(resourcePath, null, false);
    }

    private CharacterAnimationController(
            String resourcePath,
            Map<Pose, Frame> custom,
            boolean pixelArt) {

        var stream = getClass().getResourceAsStream(resourcePath);

        if (stream == null) {
            throw new IllegalArgumentException(
                    "Character sheet not found: " + resourcePath);
        }

        sheet = new Image(stream);

        if (sheet.isError()) {
            throw new IllegalArgumentException(
                    "Could not load character sheet: " + resourcePath,
                    sheet.getException());
        }

        if (custom != null) {
            frames.putAll(custom);
            this.pixelArt = pixelArt;
            this.referenceHeight = frames.get(Pose.IDLE).height();
            return;
        }

        /*
         * Approximate crop regions for the supplied 1024 x 1024 sheet.
         * Adjust these rectangles after checking the cropped poses.
         */
        frames.put(Pose.IDLE,
                new Frame(0, 5, 118, 191));

        frames.put(Pose.WALK,
                new Frame(0, 414, 118, 194));

        frames.put(Pose.JUMP,
                new Frame(718, 414, 116, 194));

        frames.put(Pose.CROUCH,
                new Frame(20, 889, 203, 128));

        frames.put(Pose.LIGHT_ATTACK,
                new Frame(502, 7, 204, 188));

        frames.put(Pose.HEAVY_ATTACK,
                new Frame(715, 207, 153, 195));

        frames.put(Pose.HIT,
                new Frame(614, 414, 150, 194));

        frames.put(Pose.SPECIAL,
                new Frame(422, 869, 130, 54));

        frames.put(Pose.ULTIMATE,
                new Frame(267, 621, 237, 201));

        // NOTE: KO currently shares the crouch rectangle. Replace with a
        // real knocked-down frame from the sheet when you find one.
        frames.put(Pose.KO,
                new Frame(20, 889, 203, 128));
    }

    public void setPose(Pose pose) {
        if (pose == null) {
            return;
        }

        if (pose != currentPose) {
            currentPose = pose;
            elapsed = 0;
        }
    }

    public Pose getCurrentPose() {
        return currentPose;
    }

    public void update(double dt) {
        if (Double.isFinite(dt) && dt > 0) {
            elapsed += Math.min(dt, 0.1);
            glowTime += dt;
        }
    }

    /**
     * Draws the current pose. The box (x, y, width, height) describes the
     * fighter's area; the sprite is anchored to its bottom centre and scaled
     * uniformly so no pose is stretched.
     */
    public void draw(
            GraphicsContext gc,
            double x,
            double y,
            double width,
            double height,
            boolean flipHorizontally) {

        Frame frame = frames.get(currentPose);

        if (frame == null) {
            frame = frames.get(Pose.IDLE);
        }

        double scale = height / referenceHeight;
        double dw = frame.width() * scale;
        double dh = frame.height() * scale;

        // Procedural motion, because each pose is a single still image.
        double bobY = 0;
        double lunge = 0;
        double shake = 0;
        double tiltDegrees = 0;
        double squash = 1.0;

        switch (currentPose) {
            case IDLE -> {
                bobY = Math.sin(elapsed * 4.0) * 2.0;
                squash = 1.0 + Math.sin(elapsed * 4.0) * 0.015;
            }
            case WALK -> {
                bobY = -Math.abs(Math.sin(elapsed * 12.0)) * 6.0;
                tiltDegrees = Math.sin(elapsed * 12.0) * 3.0;
            }
            case LIGHT_ATTACK, HEAVY_ATTACK -> {
                double p = Math.min(elapsed / 0.25, 1.0);
                lunge = Math.sin(p * Math.PI) * 14.0;
            }
            case HIT -> shake = Math.sin(elapsed * 70.0) * 4.0;
            case SPECIAL, ULTIMATE -> bobY = Math.sin(glowTime * 10.0) * 3.0;
            default -> {
            }
        }

        double anchorX = x + width / 2.0;
        double anchorY = y + height;

        gc.save();

        try {
            gc.translate(anchorX + shake, anchorY + bobY);

            if (flipHorizontally) {
                gc.scale(-1, 1);
            }

            gc.rotate(tiltDegrees);
            gc.translate(lunge, 0);
            gc.scale(1, squash);

            // Magical glow for special and ultimate poses.
            if (currentPose == Pose.SPECIAL
                    || currentPose == Pose.ULTIMATE) {

                Color glow = currentPose == Pose.ULTIMATE
                        ? Color.DEEPSKYBLUE
                        : Color.MEDIUMPURPLE;

                double pulse = 0.18
                        + 0.12 * Math.abs(Math.sin(glowTime * 8));

                gc.setGlobalAlpha(pulse);
                gc.setFill(glow);

                gc.fillOval(
                        -dw * 0.62,
                        -dh * 1.03,
                        dw * 1.24,
                        dh * 1.06);

                gc.setGlobalAlpha(1.0);
            }

            gc.setImageSmoothing(!pixelArt);

            gc.drawImage(
                    sheet,
                    frame.x(), frame.y(),
                    frame.width(), frame.height(),
                    -dw / 2.0, -dh, dw, dh);
        } finally {
            gc.setImageSmoothing(true);
            gc.restore();
        }
    }
}