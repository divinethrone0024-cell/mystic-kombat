package mystic.engine;

import javafx.scene.canvas.GraphicsContext;

/** One screen of the game: menu, character select, combat or result. */
public interface Screen {

    /** Called once each time this screen becomes the active one. */
    default void onEnter() {
    }

    /** Update the screen. dt is the time since the last frame, in seconds. */
    void update(double dt, InputManager input);

    /** Draw the screen. */
    void render(GraphicsContext gc);
}