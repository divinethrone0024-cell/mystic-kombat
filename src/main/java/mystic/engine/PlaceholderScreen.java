package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.common.Constants;
import mystic.common.GameState;

/**
 * A temporary screen: shows its name and moves to the next screen on ENTER.
 * We replace these one by one with the real screens.
 */
public class PlaceholderScreen implements Screen {
    private final String title;
    private final GameEngine engine;
    private final GameState next;

    public PlaceholderScreen(String title, GameEngine engine, GameState next) {
        this.title = title;
        this.engine = engine;
        this.next = next;
    }

    @Override
    public void update(double dt, InputManager input) {
        if (input.wasPressed(KeyCode.ENTER)) {
            engine.changeState(next);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 56));
        gc.fillText(title, Constants.WIDTH / 2.0, Constants.HEIGHT / 2.0 - 20);
        gc.setFont(Font.font("Arial", 24));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Press ENTER to continue", Constants.WIDTH / 2.0, Constants.HEIGHT / 2.0 + 30);
    }
}