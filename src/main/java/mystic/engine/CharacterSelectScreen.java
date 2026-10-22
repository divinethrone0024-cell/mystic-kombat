
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.common.Constants;
import mystic.common.GameState;

public class CharacterSelectScreen implements Screen {

    private final GameEngine engine;

    private int p1Choice;
    private int p2Choice;
    private int choosingPlayer = 1;

    private static final String[] NAMES = {
            "MYSTIC MAGE",
            "SHADOW VIGILANTE"
    };

    private static final int[] HEALTH = { 100, 120 };
    private static final int[] SPEED = { 5, 4 };
    private static final int[] DAMAGE = { 12, 15 };

    private static final Color[] COLORS = {
            Color.MEDIUMPURPLE,
            Color.ORANGERED
    };

    public CharacterSelectScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        p1Choice = engine.getPlayer1Choice();
        p2Choice = engine.getPlayer2Choice();
        choosingPlayer = 1;
    }

    @Override
    public void update(double dt, InputManager input) {
        if (input.wasPressed(KeyCode.ESCAPE)) {
            engine.changeState(GameState.MENU);
            return;
        }

        if (input.wasPressed(KeyCode.LEFT)
                || input.wasPressed(KeyCode.A)) {
            changeChoice(-1);
        }

        if (input.wasPressed(KeyCode.RIGHT)
                || input.wasPressed(KeyCode.D)) {
            changeChoice(1);
        }

        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {

            if (choosingPlayer == 1) {
                engine.setPlayer1Choice(p1Choice);
                choosingPlayer = 2;
            } else {
                engine.setPlayer2Choice(p2Choice);
                engine.changeState(GameState.COMBAT);
            }
        }
    }

    private void changeChoice(int direction) {
        if (choosingPlayer == 1) {
            p1Choice = Math.floorMod(p1Choice + direction, 2);
        } else {
            p2Choice = Math.floorMod(p2Choice + direction, 2);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH;
        double h = Constants.HEIGHT;

        gc.setFill(Color.rgb(10, 12, 27));
        gc.fillRect(0, 0, w, h);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Serif", 42));
        gc.fillText("CHOOSE YOUR FIGHTER", w / 2, 75);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 17));
        gc.fillText(
                choosingPlayer == 1
                        ? "PLAYER 1 — SELECT YOUR FIGHTER"
                        : "PLAYER 2 — SELECT YOUR FIGHTER",
                w / 2, 115);

        drawFighterCard(gc, 0, 230, p1Choice,
                choosingPlayer == 1, "PLAYER 1");

        drawFighterCard(gc, 1, 790, p2Choice,
                choosingPlayer == 2, "PLAYER 2");

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 17));
        gc.fillText(
                "A / D or LEFT / RIGHT: Change Fighter",
                w / 2, h - 82);

        gc.setFill(Color.GOLD);
        gc.fillText(
                "ENTER: Confirm   •   ESC: Main Menu",
                w / 2, h - 48);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawFighterCard(
            GraphicsContext gc,
            int index,
            double x,
            int selected,
            boolean active,
            String playerLabel) {

        double y = 175;
        double width = 410;
        double height = 390;

        gc.setFill(Color.rgb(23, 27, 48));
        gc.fillRoundRect(x, y, width, height, 22, 22);

        gc.setStroke(active ? Color.GOLD : Color.GRAY);
        gc.setLineWidth(active ? 4 : 1);
        gc.strokeRoundRect(x, y, width, height, 22, 22);
        gc.setLineWidth(1);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(active ? Color.GOLD : Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 17));
        gc.fillText(playerLabel, x + width / 2, y + 34);

        double cx = x + width / 2;

        // Simple character silhouette using the existing character colors.
        gc.setFill(COLORS[selected]);
        gc.fillOval(cx - 42, y + 65, 84, 84);
        gc.fillRoundRect(cx - 55, y + 145, 110, 95, 20, 20);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", 27));
        gc.fillText(NAMES[selected], cx, y + 275);

        gc.setFont(Font.font("Arial", 17));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("Health: " + HEALTH[selected], cx, y + 310);
        gc.fillText("Speed: " + SPEED[selected], cx, y + 338);
        gc.fillText("Damage: " + DAMAGE[selected], cx, y + 366);

        if (active) {
            gc.setFill(Color.GOLD);
            gc.setFont(Font.font("Arial", 14));
            gc.fillText("← CHANGE →", cx, y + height + 24);
        }
    }
}
