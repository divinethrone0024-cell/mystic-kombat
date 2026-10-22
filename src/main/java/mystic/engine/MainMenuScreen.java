package mystic.engine;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.common.Constants;
import mystic.common.GameState;

public class MainMenuScreen implements Screen {

    private final GameEngine engine;
    private int selectedOption = 0;

    private static final String[] OPTIONS = {
        "START GAME",
        "QUIT GAME"
    };

    public MainMenuScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        selectedOption = 0;
    }

    @Override
    public void update(double dt, InputManager input) {

        if (input.wasPressed(KeyCode.UP)
                || input.wasPressed(KeyCode.W)) {
            selectedOption = (selectedOption - 1 + OPTIONS.length)
                    % OPTIONS.length;
        }

        if (input.wasPressed(KeyCode.DOWN)
                || input.wasPressed(KeyCode.S)) {
            selectedOption = (selectedOption + 1) % OPTIONS.length;
        }

        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {

            if (selectedOption == 0) {
                engine.changeState(GameState.CHARACTER_SELECT);
            } else {
                Platform.exit();
            }
        }

        if (input.wasPressed(KeyCode.ESCAPE)) {
            Platform.exit();
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = Constants.WIDTH;
        double height = Constants.HEIGHT;
        double centerX = width / 2.0;

        // Background
        gc.setFill(Color.rgb(7, 8, 20));
        gc.fillRect(0, 0, width, height);

        // Mystical ambient glow
        gc.setFill(Color.rgb(37, 20, 76, 0.30));
        gc.fillOval(centerX - 270, 65, 540, 430);

        gc.setFill(Color.rgb(15, 72, 100, 0.16));
        gc.fillOval(centerX - 400, 220, 800, 360);

        // Decorative horizontal lines
        gc.setStroke(Color.rgb(210, 166, 75, 0.55));
        gc.setLineWidth(2);
        gc.strokeLine(80, 95, width - 80, 95);
        gc.strokeLine(80, height - 75, width - 80, height - 75);

        // Small mystical particles
        gc.setFill(Color.rgb(235, 195, 112, 0.75));
        for (int i = 0; i < 35; i++) {
            double x = (i * 137.0 + 43) % width;
            double y = (i * 83.0 + 29) % height;
            double size = (i % 3 == 0) ? 3.5 : 1.8;
            gc.fillOval(x, y, size, size);
        }

        // Game title
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.rgb(244, 202, 115));
        gc.setFont(Font.font("Serif", 62));
        gc.fillText("MYSTIC KOMBAT", centerX, height * 0.30);

        gc.setFill(Color.rgb(177, 165, 205));
        gc.setFont(Font.font("Serif", 19));
        gc.fillText(
                "WHEN WORLDS COLLIDE, LEGENDS AWAKEN",
                centerX,
                height * 0.30 + 42
        );

        // Menu options
        double firstOptionY = height * 0.56;
        double optionSpacing = 65;

        for (int i = 0; i < OPTIONS.length; i++) {
            double y = firstOptionY + i * optionSpacing;
            boolean selected = (i == selectedOption);

            if (selected) {
                gc.setFill(Color.rgb(205, 158, 64, 0.18));
                gc.fillRoundRect(
                        centerX - 175, y - 35,
                        350, 48, 14, 14
                );

                gc.setStroke(Color.rgb(238, 190, 91));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(
                        centerX - 175, y - 35,
                        350, 48, 14, 14
                );

                gc.setFill(Color.rgb(255, 218, 139));
                gc.setFont(Font.font("Serif", 25));
                gc.fillText("> " + OPTIONS[i] + " <", centerX, y);
            } else {
                gc.setFill(Color.rgb(174, 176, 196));
                gc.setFont(Font.font("Serif", 23));
                gc.fillText(OPTIONS[i], centerX, y);
            }
        }

        // Controls
        gc.setFill(Color.rgb(145, 147, 170));
        gc.setFont(Font.font("Arial", 14));
        gc.fillText(
                "W / S or UP / DOWN  •  ENTER to select  •  ESC to quit",
                centerX,
                height - 38
        );

        gc.setTextAlign(TextAlignment.LEFT);
    }
}
