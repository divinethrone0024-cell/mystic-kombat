
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;

public class ResultScreen implements Screen {

    private final GameEngine engine;

    public ResultScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void update(double dt, InputManager input) {
        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {
            // Start a fresh match with the same selected fighters.
            engine.changeState(GameState.COMBAT);
        }

        if (input.wasPressed(KeyCode.ESCAPE)
                || input.wasPressed(KeyCode.M)) {
            engine.changeState(GameState.MENU);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = Constants.WIDTH;
        double height = Constants.HEIGHT;
        double centerX = width / 2.0;

        // Background.
        gc.setFill(Color.rgb(8, 10, 25));
        gc.fillRect(0, 0, width, height);

        // Mystic glow.
        gc.setFill(Color.rgb(91, 54, 150, 0.20));
        gc.fillOval(centerX - 300, 75, 600, 430);

        gc.setStroke(Color.rgb(220, 177, 81, 0.75));
        gc.setLineWidth(2);
        gc.strokeLine(100, 115, width - 100, 115);
        gc.strokeLine(100, height - 100, width - 100, height - 100);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Serif", 48));
        gc.fillText("MATCH COMPLETE", centerX, 190);

        String winner = engine.getMatchWinner();

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", 34));
        gc.fillText(
                winner.isBlank() ? "MATCH FINISHED" : winner,
                centerX,
                290);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 23));
        gc.fillText(
                engine.getPlayer1MatchWins()
                        + "     -     "
                        + engine.getPlayer2MatchWins(),
                centerX,
                360);

        gc.setFont(Font.font("Arial", 17));
        gc.setFill(Color.rgb(195, 190, 215));
        gc.fillText("PLAYER 1          FINAL SCORE          PLAYER 2",
                centerX, 395);

        // Rematch option.
        gc.setFill(Color.rgb(255, 218, 139));
        gc.setFont(Font.font("Arial", 21));
        gc.fillText("ENTER / SPACE  —  REMATCH", centerX, 490);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 18));
        gc.fillText("M / ESC  —  MAIN MENU", centerX, 535);

        gc.setTextAlign(TextAlignment.LEFT);
    }
}
