
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;

public class SettingsScreen implements Screen {

    private final GameEngine engine;

    private double musicVolume = 0.70;
    private double sfxVolume = 0.80;

    private boolean musicMuted = false;
    private boolean sfxMuted = false;
    private boolean showingControls = false;

    private int selectedOption = 0;

    private final String[] options = {
            "MUSIC VOLUME",
            "SOUND EFFECTS",
            "MUTE MUSIC",
            "MUTE SOUND EFFECTS",
            "KEYBOARD CONTROLS",
            "BACK TO MAIN MENU"
    };

    public SettingsScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        selectedOption = 0;
        showingControls = false;
    }

    @Override
    public void update(double dt, InputManager input) {

        if (showingControls) {
            if (input.wasPressed(KeyCode.ESCAPE)
                    || input.wasPressed(KeyCode.ENTER)
                    || input.wasPressed(KeyCode.BACK_SPACE)) {
                showingControls = false;
            }
            return;
        }

        if (input.wasPressed(KeyCode.ESCAPE)) {
            engine.changeState(GameState.MENU);
            return;
        }

        if (input.wasPressed(KeyCode.UP)
                || input.wasPressed(KeyCode.W)) {
            selectedOption = Math.floorMod(selectedOption - 1, options.length);
        }

        if (input.wasPressed(KeyCode.DOWN)
                || input.wasPressed(KeyCode.S)) {
            selectedOption = Math.floorMod(selectedOption + 1, options.length);
        }

        if (input.wasPressed(KeyCode.LEFT)) {
            adjustOption(-0.05);
        }

        if (input.wasPressed(KeyCode.RIGHT)) {
            adjustOption(0.05);
        }

        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {
            activateOption();
        }
    }

    private void adjustOption(double amount) {
        if (selectedOption == 0) {
            musicVolume = clamp(musicVolume + amount);
        } else if (selectedOption == 1) {
            sfxVolume = clamp(sfxVolume + amount);
        }
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private void activateOption() {
        switch (selectedOption) {
            case 2:
                musicMuted = !musicMuted;
                break;

            case 3:
                sfxMuted = !sfxMuted;
                break;

            case 4:
                showingControls = true;
                break;

            case 5:
                engine.changeState(GameState.MENU);
                break;

            default:
                break;
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = Constants.WIDTH;
        double height = Constants.HEIGHT;

        gc.setFill(Color.rgb(8, 6, 22));
        gc.fillRect(0, 0, width, height);

        gc.setStroke(Color.rgb(116, 70, 190));
        gc.setLineWidth(2);
        gc.strokeRect(20, 20, width - 40, height - 40);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.rgb(244, 202, 115));
        gc.setFont(Font.font("Serif", 38));
        gc.fillText("MYSTIC KOMBAT", width / 2, 78);

        if (showingControls) {
            renderControls(gc, width, height);
            gc.setTextAlign(TextAlignment.LEFT);
            return;
        }

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", 25));
        gc.fillText("SETTINGS", width / 2, 122);

        double startY = 190;
        double rowGap = 52;

        for (int i = 0; i < options.length; i++) {
            double y = startY + i * rowGap;

            if (i == selectedOption) {
                gc.setFill(Color.rgb(91, 49, 139, 0.65));
                gc.fillRoundRect(
                        width / 2 - 255, y - 30,
                        510, 40, 12, 12);

                gc.setStroke(Color.GOLD);
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(
                        width / 2 - 255, y - 30,
                        510, 40, 12, 12);
            }

            gc.setFill(
                    i == selectedOption ? Color.GOLD : Color.LIGHTGRAY);
            gc.setFont(Font.font("Serif", 18));
            gc.fillText(getOptionLabel(i), width / 2, y);
        }

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 13));
        gc.fillText(
                "UP / DOWN: Select     LEFT / RIGHT: Adjust",
                width / 2, height - 58);
        gc.fillText(
                "ENTER: Toggle / Select     ESC: Main Menu",
                width / 2, height - 36);
    }

    private void renderControls(
            GraphicsContext gc, double width, double height) {

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", 25));
        gc.fillText("KEYBOARD CONTROLS", width / 2, 125);

        gc.setFont(Font.font("Serif", 21));
        gc.setFill(Color.rgb(244, 202, 115));
        gc.fillText("PLAYER 1", width * 0.29, 185);
        gc.fillText("PLAYER 2", width * 0.71, 185);

        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.LIGHTGRAY);

        String[] actions = {
                "Move Left / Right",
                "Jump",
                "Crouch",
                "Light Attack",
                "Heavy Attack",
                "Ultimate Incantation"
        };

        String[] player1Keys = {
                "A / D", "W", "S", "F", "G", "H"
        };

        String[] player2Keys = {
                "LEFT / RIGHT", "UP", "DOWN", "K", "L", ";"
        };

        double startY = 230;
        double rowGap = 42;

        for (int i = 0; i < actions.length; i++) {
            double y = startY + i * rowGap;

            gc.setFill(Color.rgb(177, 165, 205));
            gc.fillText(actions[i], width / 2, y);

            gc.setFill(Color.WHITE);
            gc.fillText(player1Keys[i], width * 0.29, y + 22);
            gc.fillText(player2Keys[i], width * 0.71, y + 22);
        }

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText(
                "Press ESC, ENTER, or BACKSPACE to return",
                width / 2, height - 45);
    }

    private String getOptionLabel(int index) {
        switch (index) {
            case 0:
                return options[index] + "   "
                        + (int) (musicVolume * 100) + "%";

            case 1:
                return options[index] + "   "
                        + (int) (sfxVolume * 100) + "%";

            case 2:
                return options[index] + "   "
                        + (musicMuted ? "MUTED" : "ON");

            case 3:
                return options[index] + "   "
                        + (sfxMuted ? "MUTED" : "ON");

            case 4:
                return options[index] + "   (VIEW)";

            default:
                return options[index];
        }
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public boolean isSfxMuted() {
        return sfxMuted;
    }
}
