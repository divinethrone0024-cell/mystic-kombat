
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
                        "SHADOW VIGILANTE",
                        "SPEEDSTER SONIC",
                        "PLUMBER BRAWLER"
        };

        private static final String[] TITLES = {
                        "MASTER OF THE ARCANE",
                        "THE SILENT EXECUTIONER",
                        "THE LIGHTNING FIGHTER",
                        "THE HEAVY HITTER"
        };

        private static final String[] DESCRIPTIONS = {
                        "Wields ancient magic in battle.",
                        "A relentless close-range warrior.",
                        "Uses speed and dash attacks.",
                        "Uses powerful, heavy strikes."
        };

        private static final String[] SPECIALS = {
                        "ARCANE BLAST",
                        "SHADOW STRIKE",
                        "LIGHTNING RUSH",
                        "POWER SMASH"
        };

        private static final String[] ULTIMATES = {
                        "MYSTIC NOVA",
                        "ETERNAL NIGHT",
                        "SONIC OVERDRIVE",
                        "TITAN IMPACT"
        };

        private static final int[] HEALTH = {
                        100, 120, 95, 130
        };

        private static final int[] SPEED = {
                        5, 4, 7, 3
        };

        private static final int[] DAMAGE = {
                        12, 15, 10, 17
        };

        private static final Color[] COLORS = {
                        Color.rgb(176, 108, 255),
                        Color.rgb(255, 75, 85),
                        Color.rgb(50, 205, 255),
                        Color.rgb(255, 180, 65)
        };

        private static final Color BACKGROUND = Color.rgb(8, 10, 23);

        private static final Color PANEL = Color.rgb(19, 23, 43);

        private static final Color MUTED = Color.rgb(157, 165, 190);

        private static final int CHARACTER_COUNT = NAMES.length;

        public CharacterSelectScreen(GameEngine engine) {
                this.engine = engine;
        }

        @Override
        public void onEnter() {
                p1Choice = Math.floorMod(
                                engine.getPlayer1Choice(), CHARACTER_COUNT);

                p2Choice = Math.floorMod(
                                engine.getPlayer2Choice(), CHARACTER_COUNT);

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
                                || input.wasPressed(KeyCode.A)
                                || input.wasPressed(KeyCode.UP)
                                || input.wasPressed(KeyCode.W)) {
                        changeChoice(-1);
                }

                if (input.wasPressed(KeyCode.RIGHT)
                                || input.wasPressed(KeyCode.D)
                                || input.wasPressed(KeyCode.DOWN)
                                || input.wasPressed(KeyCode.S)) {
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
                        p1Choice = Math.floorMod(
                                        p1Choice + direction, CHARACTER_COUNT);
                } else {
                        p2Choice = Math.floorMod(
                                        p2Choice + direction, CHARACTER_COUNT);
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
                gc.fillText(
                                "M U L T I V E R S E   C H A M P I O N S H I P",
                                w / 2, 27);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Serif", 32));
                gc.fillText("CHOOSE YOUR FIGHTER", w / 2, 65);

                gc.setFill(MUTED);
                gc.setFont(Font.font("Arial", 12));
                gc.fillText(
                                choosingPlayer == 1
                                                ? "PLAYER 1 — SELECT YOUR CHAMPION"
                                                : "PLAYER 2 — SELECT YOUR CHAMPION",
                                w / 2, 88);

                double marginX = Math.max(24, w * 0.07);
                double gapX = 18;
                double gapY = 12;

                double cardWidth = (w - 2 * marginX - gapX) / 2.0;
                double cardHeight = (h - 145 - 70 - gapY) / 2.0;

                double topY = 105;
                double bottomY = topY + cardHeight + gapY;

                double leftX = marginX;
                double rightX = marginX + cardWidth + gapX;

                // Draw all four selectable fighters.
                for (int i = 0; i < CHARACTER_COUNT; i++) {
                        double x = (i % 2 == 0) ? leftX : rightX;
                        double y = (i < 2) ? topY : bottomY;

                        drawFighterCard(
                                        gc, x, y, cardWidth, cardHeight, i);
                }

                // Instructions footer.
                gc.setFill(Color.rgb(10, 12, 25, 0.97));
                gc.fillRoundRect(
                                18, h - 53, w - 36, 40, 12, 12);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", 11));
                gc.fillText(
                                "A / D OR ARROWS: CHANGE FIGHTER"
                                                + "     •     ENTER: CONFIRM"
                                                + "     •     ESC: BACK",
                                w / 2, h - 36);

                gc.setFill(Color.GOLD);
                gc.setFont(Font.font("Arial", 11));
                gc.fillText(
                                choosingPlayer == 1
                                                ? "PLAYER 1: CONFIRM YOUR FIGHTER"
                                                : "PLAYER 2: CONFIRM TO START",
                                w / 2, h - 19);

                gc.setTextAlign(TextAlignment.LEFT);
        }

        private void drawBackground(
                        GraphicsContext gc, double w, double h) {

                gc.setFill(BACKGROUND);
                gc.fillRect(0, 0, w, h);

                // Subtle vertical light beams.
                for (int i = 0; i < 9; i++) {
                        double x = i * w / 8.0;

                        gc.setStroke(Color.rgb(100, 95, 180, 0.08));
                        gc.setLineWidth(1);
                        gc.strokeLine(x, 0, x - 120, h);
                }

                // Moving multiverse particles.
                for (int i = 0; i < 35; i++) {
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
                        int index) {

                Color accent = COLORS[index];

                boolean selectedByP1 = p1Choice == index;
                boolean selectedByP2 = p2Choice == index;

                boolean active = choosingPlayer == 1
                                ? selectedByP1
                                : selectedByP2;

                gc.setFill(Color.rgb(0, 0, 0, 0.25));
                gc.fillRoundRect(
                                x + 4, y + 5, width, height, 18, 18);

                gc.setFill(PANEL);
                gc.fillRoundRect(x, y, width, height, 18, 18);

                gc.setStroke(active
                                ? accent
                                : Color.rgb(65, 73, 105));

                gc.setLineWidth(active ? 3 : 1);
                gc.strokeRoundRect(x, y, width, height, 18, 18);
                gc.setLineWidth(1);

                // Accent strip.
                gc.setFill(accent);
                gc.fillRoundRect(
                                x + 2, y + 2, width - 4, 4, 4, 4);

                gc.setTextAlign(TextAlignment.CENTER);

                // Show which players have selected this character.
                gc.setFont(Font.font("Arial", 10));

                if (selectedByP1) {
                        gc.setFill(Color.rgb(100, 210, 255));
                        gc.fillText("P1", x + 25, y + 22);
                }

                if (selectedByP2) {
                        gc.setFill(Color.rgb(255, 190, 95));
                        gc.fillText("P2", x + width - 25, y + 22);
                }

                double centerX = x + width / 2;
                double artY = y + 22;
                double artHeight = Math.min(60, height * 0.30);

                double pulse = 1.0
                                + Math.sin(animationTime * 2.5 + index) * 0.04;

                double aura = artHeight * 0.95 * pulse;

                // Glowing placeholder portrait.
                // Real character sprites can be drawn here later.
                gc.setFill(Color.color(
                                accent.getRed(),
                                accent.getGreen(),
                                accent.getBlue(),
                                active ? 0.20 : 0.09));

                gc.fillOval(
                                centerX - aura / 2,
                                artY,
                                aura,
                                aura);

                gc.setFill(Color.color(
                                accent.getRed(),
                                accent.getGreen(),
                                accent.getBlue(),
                                0.95));

                double headSize = Math.min(31, artHeight * 0.48);

                gc.fillOval(
                                centerX - headSize / 2,
                                artY + 5,
                                headSize,
                                headSize);

                double bodyWidth = Math.min(54, width * 0.25);

                gc.fillRoundRect(
                                centerX - bodyWidth / 2,
                                artY + headSize + 6,
                                bodyWidth,
                                Math.max(14, artHeight * 0.35),
                                12, 12);

                gc.setStroke(Color.rgb(255, 255, 255, 0.75));
                gc.setLineWidth(2);

                gc.strokeLine(
                                centerX - 9, artY + 17,
                                centerX + 9, artY + 17);

                gc.setLineWidth(1);

                double textY = artY + artHeight + 15;

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font(
                                "Serif", width < 300 ? 15 : 19));

                gc.fillText(NAMES[index], centerX, textY);

                gc.setFill(accent);
                gc.setFont(Font.font("Arial", 9));
                gc.fillText(TITLES[index], centerX, textY + 16);

                gc.setFill(MUTED);
                gc.setFont(Font.font("Arial", 10));
                gc.fillText(DESCRIPTIONS[index], centerX, textY + 31);

                double statsY = textY + 48;

                drawStat(
                                gc, x + 16, statsY, width - 32,
                                "HP", HEALTH[index] / 130.0, accent);

                drawStat(
                                gc, x + 16, statsY + 16, width - 32,
                                "SPD", SPEED[index] / 7.0, accent);

                drawStat(
                                gc, x + 16, statsY + 32, width - 32,
                                "DMG", DAMAGE[index] / 17.0, accent);

                double abilityY = statsY + 51;

                gc.setFill(MUTED);
                gc.setFont(Font.font("Arial", 9));
                gc.fillText(
                                SPECIALS[index] + "  •  " + ULTIMATES[index],
                                centerX, abilityY);

                if (active) {
                        gc.setFill(accent);
                        gc.setFont(Font.font("Arial", 9));
                        gc.fillText(
                                        choosingPlayer == 1
                                                        ? "PLAYER 1 — CURRENT SELECTION"
                                                        : "PLAYER 2 — CURRENT SELECTION",
                                        centerX, y + height - 9);
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

                gc.setFill(MUTED);
                gc.setFont(Font.font("Arial", 9));
                gc.fillText(label, x, y);

                double barX = x + 36;
                double barWidth = Math.max(10, width - 66);

                gc.setFill(Color.rgb(42, 47, 70));
                gc.fillRoundRect(
                                barX, y - 7, barWidth, 5, 5, 5);

                gc.setFill(accent);
                gc.fillRoundRect(
                                barX, y - 7,
                                barWidth * Math.max(0, Math.min(1, value)),
                                5, 5, 5);

                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", 9));
                gc.fillText(
                                String.valueOf((int) Math.round(value * 100)),
                                x + width, y);
        }
}
