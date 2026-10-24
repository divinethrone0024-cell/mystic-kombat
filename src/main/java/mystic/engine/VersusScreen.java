
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;

public class VersusScreen implements Screen {

    private final GameEngine engine;

    private double animationTime;
    private double transitionTimer;

    private static final String[] NAMES = {
            "MYSTIC MAGE",
            "SHADOW VIGILANTE"
    };

    private static final String[] TITLES = {
            "MASTER OF THE ARCANE",
            "THE SILENT EXECUTIONER"
    };

    private static final Color[] COLORS = {
            Color.rgb(177, 104, 255),
            Color.rgb(255, 72, 85)
    };

    public VersusScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        animationTime = 0;
        transitionTimer = 0;
    }

    @Override
    public void update(double dt, InputManager input) {
        animationTime += dt;

        if (input.wasPressed(KeyCode.ESCAPE)) {
            engine.changeState(GameState.CHARACTER_SELECT);
            return;
        }

        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {
            engine.changeState(GameState.COMBAT);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH;
        double h = Constants.HEIGHT;

        int p1 = Math.floorMod(engine.getPlayer1Choice(), NAMES.length);
        int p2 = Math.floorMod(engine.getPlayer2Choice(), NAMES.length);

        Color c1 = COLORS[p1];
        Color c2 = COLORS[p2];

        // Dark multiverse background.
        gc.setFill(Color.rgb(7, 8, 20));
        gc.fillRect(0, 0, w, h);

        // Purple and red energy glows.
        gc.setFill(Color.color(
                c1.getRed(), c1.getGreen(), c1.getBlue(), 0.12));
        gc.fillOval(-w * 0.16, h * 0.08, w * 0.65, h * 0.85);

        gc.setFill(Color.color(
                c2.getRed(), c2.getGreen(), c2.getBlue(), 0.12));
        gc.fillOval(w * 0.51, h * 0.08, w * 0.65, h * 0.85);

        // Energy particles.
        for (int i = 0; i < 48; i++) {
            double baseX = (i * 97.0) % w;
            double baseY = (i * 61.0) % h;
            double x = Math.floorMod(
                    (int) (baseX + Math.sin(animationTime + i) * 18),
                    (int) w);
            double y = Math.floorMod(
                    (int) (baseY - animationTime * (18 + i % 18)),
                    (int) h);

            gc.setFill(i % 2 == 0
                    ? Color.rgb(183, 133, 255, 0.65)
                    : Color.rgb(255, 100, 110, 0.65));
            gc.fillOval(x, y, 2 + i % 3, 2 + i % 3);
        }

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.rgb(238, 207, 137));
        gc.setFont(Font.font("Serif", 15));
        gc.fillText("THE MULTIVERSE COLLIDES", w / 2, h * 0.10);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", 42));
        gc.fillText("PREPARE FOR BATTLE", w / 2, h * 0.18);

        double centerY = h * 0.46;
        double portraitSize = Math.min(155, h * 0.25);
        double leftX = w * 0.25;
        double rightX = w * 0.75;

        drawFighter(gc, leftX, centerY, portraitSize, p1, c1);
        drawFighter(gc, rightX, centerY, portraitSize, p2, c2);

        // Versus emblem.
        double pulse = 1 + Math.sin(animationTime * 4) * 0.06;
        double versusSize = 76 * pulse;

        gc.setFill(Color.rgb(5, 6, 15, 0.9));
        gc.fillOval(
                w / 2 - versusSize / 2,
                centerY - versusSize / 2,
                versusSize,
                versusSize);

        gc.setStroke(Color.GOLD);
        gc.setLineWidth(2.5);
        gc.strokeOval(
                w / 2 - versusSize / 2,
                centerY - versusSize / 2,
                versusSize,
                versusSize);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Serif", 30));
        gc.fillText("VS", w / 2, centerY + 10);
        gc.setLineWidth(1);

        // Fighter names and titles.
        gc.setFill(c1);
        gc.setFont(Font.font("Serif", 25));
        gc.fillText(NAMES[p1], leftX, centerY + portraitSize * 0.78);

        gc.setFill(c2);
        gc.fillText(NAMES[p2], rightX, centerY + portraitSize * 0.78);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText(TITLES[p1], leftX, centerY + portraitSize * 0.78 + 24);
        gc.fillText(TITLES[p2], rightX, centerY + portraitSize * 0.78 + 24);

        gc.setFill(Color.rgb(15, 17, 33, 0.94));
        gc.fillRoundRect(
                w * 0.20, h * 0.80,
                w * 0.60, 55,
                14, 14);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 16));
        gc.fillText("ENTER  /  SPACE  —  FIGHT",
                w / 2, h * 0.80 + 23);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText("ESC — CHANGE FIGHTERS",
                w / 2, h * 0.80 + 42);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawFighter(
            GraphicsContext gc,
            double centerX,
            double centerY,
            double size,
            int fighter,
            Color accent) {

        double aura = size * (1.25 + Math.sin(animationTime * 3) * 0.05);

        // Outer aura.
        gc.setFill(Color.color(
                accent.getRed(),
                accent.getGreen(),
                accent.getBlue(),
                0.12));
        gc.fillOval(
                centerX - aura / 2,
                centerY - aura / 2,
                aura,
                aura);

        // Portrait frame.
        gc.setFill(Color.rgb(18, 21, 39));
        gc.fillRoundRect(
                centerX - size / 2 - 9,
                centerY - size / 2 - 9,
                size + 18,
                size + 18,
                20, 20);

        gc.setStroke(accent);
        gc.setLineWidth(3);
        gc.strokeRoundRect(
                centerX - size / 2 - 9,
                centerY - size / 2 - 9,
                size + 18,
                size + 18,
                20, 20);
        gc.setLineWidth(1);

        // Stylized character silhouette.
        gc.setFill(accent);
        gc.fillOval(
                centerX - size * 0.19,
                centerY - size * 0.34,
                size * 0.38,
                size * 0.38);

        gc.fillRoundRect(
                centerX - size * 0.30,
                centerY + size * 0.04,
                size * 0.60,
                size * 0.37,
                20, 20);

        // Face highlight.
        gc.setStroke(Color.rgb(255, 255, 255, 0.8));
        gc.strokeLine(
                centerX - size * 0.12,
                centerY - size * 0.16,
                centerX + size * 0.12,
                centerY - size * 0.16);

        gc.setLineWidth(1);
    }
}
