
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.common.Constants;

public class PauseMenuScreen {

    public enum Choice {
        RESUME,
        RESTART_ROUND,
        CONTROLS,
        MAIN_MENU
    }

    private static final String[] OPTIONS = {
            "RESUME",
            "RESTART ROUND",
            "CONTROLS",
            "RETURN TO MAIN MENU"
    };

    private int selectedOption = 0;
    private boolean showControls = false;

    public void reset() {
        selectedOption = 0;
        showControls = false;
    }

    public Choice update(InputManager input) {

        if (input.wasPressed(KeyCode.UP)
                || input.wasPressed(KeyCode.W)) {
            selectedOption = (selectedOption - 1 + OPTIONS.length)
                    % OPTIONS.length;
        }

        if (input.wasPressed(KeyCode.DOWN)
                || input.wasPressed(KeyCode.S)) {
            selectedOption = (selectedOption + 1) % OPTIONS.length;
        }

        if (input.wasPressed(KeyCode.ESCAPE)) {
            if (showControls) {
                showControls = false;
                return null;
            }
            return Choice.RESUME;
        }

        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {

            if (showControls) {
                showControls = false;
                return null;
            }

            switch (selectedOption) {
                case 0:
                    return Choice.RESUME;

                case 1:
                    return Choice.RESTART_ROUND;

                case 2:
                    showControls = true;
                    return null;

                case 3:
                    return Choice.MAIN_MENU;

                default:
                    return null;
            }
        }

        return null;
    }

    public void render(GraphicsContext gc) {

        double width = Constants.WIDTH;
        double height = Constants.HEIGHT;
        double cx = width / 2.0;
        double cy = height / 2.0;

        // Darken the active combat arena.
        gc.setFill(Color.rgb(3, 4, 15, 0.84));
        gc.fillRect(0, 0, width, height);

        // Outer gold frame.
        gc.setStroke(Color.rgb(214, 172, 79, 0.85));
        gc.setLineWidth(2);
        gc.strokeRoundRect(
                cx - 260, cy - 255,
                520, 510, 22, 22);

        // Inner frame.
        gc.setStroke(Color.rgb(125, 92, 180, 0.65));
        gc.setLineWidth(1);
        gc.strokeRoundRect(
                cx - 248, cy - 243,
                496, 486, 18, 18);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.rgb(245, 204, 117));
        gc.setFont(Font.font("Serif", 42));
        gc.fillText("GAME PAUSED", cx, cy - 175);

        gc.setFill(Color.rgb(168, 155, 198));
        gc.setFont(Font.font("Serif", 14));
        gc.fillText(
                "THE MULTIVERSE AWAITS YOUR RETURN",
                cx, cy - 145);

        if (showControls) {
            drawControls(gc, cx, cy);
        } else {
            drawOptions(gc, cx, cy);
        }

        gc.setFill(Color.rgb(165, 165, 185));
        gc.setFont(Font.font("Arial", 13));
        gc.fillText(
                "W / S or ARROWS  •  ENTER SELECTS  •  ESC RESUMES",
                cx, cy + 215);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawOptions(
            GraphicsContext gc, double cx, double cy) {

        double startY = cy - 75;
        double spacing = 58;

        for (int i = 0; i < OPTIONS.length; i++) {
            double y = startY + i * spacing;
            boolean selected = i == selectedOption;

            if (selected) {
                gc.setFill(Color.rgb(194, 145, 47, 0.20));
                gc.fillRoundRect(
                        cx - 195, y - 28,
                        390, 42, 12, 12);

                gc.setStroke(Color.rgb(239, 192, 91));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(
                        cx - 195, y - 28,
                        390, 42, 12, 12);

                gc.setFill(Color.rgb(255, 218, 139));
                gc.setFont(Font.font("Serif", 22));
                gc.fillText(
                        "> " + OPTIONS[i] + " <", cx, y);
            } else {
                gc.setFill(Color.rgb(195, 191, 210));
                gc.setFont(Font.font("Serif", 20));
                gc.fillText(OPTIONS[i], cx, y);
            }
        }
    }

    private void drawControls(
            GraphicsContext gc, double cx, double cy) {

        gc.setFill(Color.rgb(255, 218, 139));
        gc.setFont(Font.font("Serif", 24));
        gc.fillText("BATTLE CONTROLS", cx, cy - 70);

        gc.setFill(Color.rgb(220, 218, 233));
        gc.setFont(Font.font("Arial", 16));

        gc.fillText("PLAYER 1", cx - 100, cy - 25);
        gc.fillText("PLAYER 2", cx + 100, cy - 25);

        gc.setFill(Color.rgb(190, 184, 212));
        gc.setFont(Font.font("Arial", 14));

        gc.fillText("Move: A / D", cx - 100, cy + 10);
        gc.fillText("Jump: W", cx - 100, cy + 35);
        gc.fillText("Crouch: S", cx - 100, cy + 60);
        gc.fillText("Light: F", cx - 100, cy + 85);
        gc.fillText("Heavy: G", cx - 100, cy + 110);
        gc.fillText("Ultimate: H", cx - 100, cy + 135);

        gc.fillText("Move: LEFT / RIGHT", cx + 100, cy + 10);
        gc.fillText("Jump: UP", cx + 100, cy + 35);
        gc.fillText("Crouch: DOWN", cx + 100, cy + 60);
        gc.fillText("Light: K", cx + 100, cy + 85);
        gc.fillText("Heavy: L", cx + 100, cy + 110);
        gc.fillText("Ultimate: ;", cx + 100, cy + 135);
    }
}
