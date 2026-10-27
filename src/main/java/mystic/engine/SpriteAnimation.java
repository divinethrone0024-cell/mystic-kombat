
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.Objects;

/**
 * Draws a horizontal sprite sheet onto a JavaFX Canvas.
 *
 * Each frame occupies frameWidth x frameHeight pixels.
 */
public final class SpriteAnimation {

    private final Image image;
    private final int frameWidth;
    private final int frameHeight;
    private final int frameCount;
    private final double frameDuration;

    private int currentFrame = 0;
    private double elapsed = 0;

    public SpriteAnimation(
            String resourcePath,
            int frameWidth,
            int frameHeight,
            int frameCount,
            double framesPerSecond) {
        if (frameWidth <= 0 || frameHeight <= 0 || frameCount <= 0) {
            throw new IllegalArgumentException(
                    "Frame dimensions and frame count must be positive.");
        }

        if (framesPerSecond <= 0) {
            throw new IllegalArgumentException(
                    "Frames per second must be positive.");
        }

        var stream = getClass().getResourceAsStream(resourcePath);

        if (stream == null) {
            throw new IllegalArgumentException(
                    "Sprite sheet not found: " + resourcePath);
        }

        image = new Image(stream);

        if (image.isError()) {
            throw new IllegalArgumentException(
                    "Could not load sprite sheet: " + resourcePath,
                    image.getException());
        }

        if (image.getWidth() < (double) frameWidth * frameCount
                || image.getHeight() < frameHeight) {
            throw new IllegalArgumentException(
                    "Sprite sheet is too small for the configured frames: "
                            + resourcePath);
        }

        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.frameCount = frameCount;
        this.frameDuration = 1.0 / framesPerSecond;
    }

    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0) {
            return;
        }

        elapsed += Math.min(deltaSeconds, 0.25);

        while (elapsed >= frameDuration) {
            elapsed -= frameDuration;
            currentFrame = (currentFrame + 1) % frameCount;
        }
    }

    public void reset() {
        currentFrame = 0;
        elapsed = 0;
    }

    public void draw(
            GraphicsContext gc,
            double x,
            double y,
            double width,
            double height,
            boolean flipHorizontally) {
        double sourceX = (double) currentFrame * frameWidth;

        gc.save();

        if (flipHorizontally) {
            gc.translate(x + width, 0);
            gc.scale(-1, 1);

            gc.drawImage(
                    image,
                    sourceX, 0,
                    frameWidth, frameHeight,
                    0, y,
                    width, height);
        } else {
            gc.drawImage(
                    image,
                    sourceX, 0,
                    frameWidth, frameHeight,
                    x, y,
                    width, height);
        }

        gc.restore();
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public Image getImage() {
        return image;
    }
}
