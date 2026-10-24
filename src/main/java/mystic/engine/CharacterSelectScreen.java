
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

    private double animationTime;

    private static final String[] NAMES = {
            "MYSTIC MAGE",
            "SHADOW VIGILANTE"
    };

    private static final String[] TITLES = {
            "MASTER OF THE ARCANE",
            "THE SILENT EXECUTIONER"
    };

    private static final String[] DESCRIPTIONS = {
            "Wields ancient magic to overwhelm opponents.",
            "A relentless warrior built for devastating strikes."
    };

    private static final String[] SPECIALS = {
            "ARCANE BLAST",
            "SHADOW STRIKE"
    };

    private static final String[] ULTIMATES = {
            "MYSTIC NOVA",
            "ETERNAL NIGHT"
    };

    private static final int[] HEALTH = { 100, 120 };
    private static final int[] SPEED = { 5, 4 };
    private static final int[] DAMAGE = { 12, 15 };

    private static final Color[] COLORS = {
            Color.rgb(176, 108, 255),
            Color.rgb(255, 75, 85)
    };

    private static final Color BACKGROUND = Color.rgb(8, 10, 23);

    private static final Color PANEL = Color.rgb(19, 23, 43);

    private static final Color MUTED = Color.rgb(157, 165, 190);

    public CharacterSelectScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        p1Choice = engine.getPlayer1Choice();
        p2Choice = engine.getPlayer2Choice();
        choosingPlayer = 1;
        animationTime = 0;
    }

    @Override
    public void update(double dt, InputManager input) {
        animationTime += dt;

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
                engine.changeState(GameState.VERSUS);
            }
        }
    }

    private void changeChoice(int direction) {
        if (choosingPlayer == 1) {
            p1Choice = Math.floorMod(p1Choice + direction, NAMES.length);
        } else {
            p2Choice = Math.floorMod(p2Choice + direction, NAMES.length);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH;
        double h = Constants.HEIGHT;

        drawBackground(gc, w, h);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(Color.rgb(244, 218, 157));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("M U L T I V E R S E   C H A M P I O N S H I P",
                w / 2, 32);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", 39));
        gc.fillText("CHOOSE YOUR FIGHTER", w / 2, 79);

        gc.setFill(MUTED);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText("SELECT YOUR CHAMPION • CLAIM YOUR DESTINY",
                w / 2, 108);

        double margin = Math.max(30, w * 0.055);
        double gap = Math.max(24, w * 0.035);
        double cardWidth = (w - 2 * margin - gap) / 2;
        double cardY = 135;
        double cardHeight = h - 230;

        drawFighterCard(gc, margin, cardY, cardWidth, cardHeight,
                p1Choice, choosingPlayer == 1, "PLAYER 1");

        drawFighterCard(gc, margin + cardWidth + gap, cardY,
                cardWidth, cardHeight, p2Choice,
                choosingPlayer == 2, "PLAYER 2");

        // Versus emblem between the two cards.
        double cx = w / 2;
        double cy = cardY + cardHeight * 0.36;

        gc.setFill(Color.rgb(5, 7, 17, 0.95));
        gc.fillOval(cx - 30, cy - 30, 60, 60);

        gc.setStroke(Color.rgb(244, 197, 104, 0.9));
        gc.setLineWidth(2);
        gc.strokeOval(cx - 30, cy - 30, 60, 60);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Serif", 22));
        gc.fillText("VS", cx, cy + 8);

        gc.setLineWidth(1);

        gc.setFill(Color.rgb(10, 12, 25, 0.96));
        gc.fillRoundRect(18, h - 75, w - 36, 56, 15, 15);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText(
                choosingPlayer == 1
                        ? "PLAYER 1: A / D OR ← / → TO SELECT"
                        : "PLAYER 2: A / D OR ← / → TO SELECT",
                w / 2, h - 51);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 13));
        gc.fillText(
                choosingPlayer == 1
                        ? "ENTER: CONFIRM PLAYER 1"
                        : "ENTER: START BATTLE",
                w / 2, h - 31);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawBackground(
            GraphicsContext gc, double w, double h) {

        gc.setFill(BACKGROUND);
        gc.fillRect(0, 0, w, h);

        // Subtle vertical light beams.
        for (int i = 0; i < 9; i++) {
            double x = (i * w / 8.0);
            gc.setStroke(Color.rgb(100, 95, 180, 0.08));
            gc.setLineWidth(1);
            gc.strokeLine(x, 0, x - 120, h);
        }

        // Moving multiverse energy particles.
        for (int i = 0; i < 30; i++) {
            double x = Math.floorMod(i * 137, (int) w);
            double baseY = Math.floorMod(i * 83, (int) h);
            double y = Math.floorMod(
                    (int) (baseY + animationTime * (12 + i % 9)),
                    (int) h);

            gc.setFill(Color.rgb(171, 143, 255, 0.3));
            gc.fillOval(x, y, 2 + i % 3, 2 + i % 3);
        }

        gc.setLineWidth(1);
    }

    private void drawFighterCard(
            GraphicsContext gc,
            double x,
            double y,
            double width,
            double height,
            int selected,
            boolean active,
            String playerLabel) {

        Color accent = COLORS[selected];

        gc.setFill(Color.rgb(0, 0, 0, 0.22));
        gc.fillRoundRect(x + 5, y + 7, width, height, 22, 22);

        gc.setFill(PANEL);
        gc.fillRoundRect(x, y, width, height, 22, 22);

        gc.setStroke(active ? accent : Color.rgb(65, 73, 105));
        gc.setLineWidth(active ? 3 : 1);
        gc.strokeRoundRect(x, y, width, height, 22, 22);
        gc.setLineWidth(1);

        // Accent strip.
        gc.setFill(accent);
        gc.fillRoundRect(x + 2, y + 2, width - 4, 5, 5, 5);

        gc.setTextAlign(TextAlignment.CENTER);

        gc.setFill(active ? Color.GOLD : MUTED);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText(playerLabel, x + width / 2, y + 30);

        double centerX = x + width / 2;
        double artY = y + 47;
        double artHeight = Math.min(145, height * 0.29);
        double pulse = 1.0 + Math.sin(animationTime * 2.5) * 0.035;
        double aura = artHeight * 0.62 * pulse;

        // Character aura and stylized silhouette.
        gc.setFill(Color.color(
                accent.getRed(),
                accent.getGreen(),
                accent.getBlue(),
                active ? 0.15 : 0.07));

        gc.fillOval(
                centerX - aura / 2,
                artY + 5,
                aura,
                aura);

        gc.setFill(Color.color(
                accent.getRed(),
                accent.getGreen(),
                accent.getBlue(),
                0.95));

        gc.fillOval(
                centerX - 28, artY + 10,
                56, 56);

        gc.fillRoundRect(
                centerX - 42, artY + 65,
                84, artHeight * 0.48,
                22, 22);

        // Eye line adds a small character detail.
        gc.setStroke(Color.rgb(255, 255, 255, 0.75));
        gc.setLineWidth(2);
        gc.strokeLine(
                centerX - 17, artY + 37,
                centerX + 17, artY + 37);
        gc.setLineWidth(1);

        double nameY = artY + artHeight + 12;

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Serif", width < 360 ? 20 : 25));
        gc.fillText(NAMES[selected], centerX, nameY);

        gc.setFill(accent);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText(TITLES[selected], centerX, nameY + 22);

        gc.setFill(MUTED);
        gc.setFont(Font.font("Arial", 11));
        gc.fillText(DESCRIPTIONS[selected], centerX, nameY + 42);

        double statY = nameY + 67;
        double statGap = 24;

        drawStat(gc, x + 25, statY, width - 50,
                "HEALTH", HEALTH[selected] / 120.0, accent);

        drawStat(gc, x + 25, statY + statGap, width - 50,
                "SPEED", SPEED[selected] / 5.0, accent);

        drawStat(gc, x + 25, statY + statGap * 2, width - 50,
                "DAMAGE", DAMAGE[selected] / 15.0, accent);

        double abilityY = statY + statGap * 3 + 12;

        gc.setStroke(Color.rgb(80, 87, 118));
        gc.strokeLine(x + 25, abilityY, x + width - 25, abilityY);

        gc.setFill(MUTED);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("SIGNATURE SPECIAL", centerX, abilityY + 18);

        gc.setFill(accent);
        gc.setFont(Font.font("Arial", 13));
        gc.fillText(SPECIALS[selected], centerX, abilityY + 37);

        gc.setFill(MUTED);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText("ULTIMATE INCANTATION", centerX, abilityY + 57);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 13));
        gc.fillText(ULTIMATES[selected], centerX, abilityY + 76);

        if (active) {
            gc.setFill(accent);
            gc.setFont(Font.font("Arial", 11));
            gc.fillText("◀  CHANGE FIGHTER  ▶",
                    centerX, y + height - 12);
        } else {
            gc.setFill(MUTED);
            gc.setFont(Font.font("Arial", 10));
            gc.fillText("WAITING FOR SELECTION",
                    centerX, y + height - 12);
        }

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawStat(
            GraphicsContext gc,
            double x,
            double y,
            double width,
            String label,
            double value,
            Color accent) {

        double labelWidth = 65;
        double barX = x + labelWidth;
        double barWidth = width - labelWidth - 35;

        gc.setFill(MUTED);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText(label, x, y);

        gc.setFill(Color.rgb(42, 47, 70));
        gc.fillRoundRect(barX, y - 8, barWidth, 7, 7, 7);

        gc.setFill(accent);
        gc.fillRoundRect(
                barX, y - 8,
                barWidth * Math.max(0, Math.min(1, value)),
                7, 7, 7);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 10));
        gc.fillText(
                String.valueOf((int) Math.round(value * 100)),
                x + width, y);
    }
}
