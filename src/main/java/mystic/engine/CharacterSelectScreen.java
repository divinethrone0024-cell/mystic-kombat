
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.Roster;
import mystic.common.Roster.Hero;

public class CharacterSelectScreen implements Screen {

        private static final Color BACKGROUND = Color.rgb(8, 10, 23);
        private static final Color PANEL = Color.rgb(19, 23, 43);
        private static final Color MUTED = Color.rgb(157, 165, 190);
        private static final Color P1 = Color.rgb(100, 210, 255);
        private static final Color P2 = Color.rgb(255, 190, 95);

        private final GameEngine engine;

        private int p1Choice;
        private int p2Choice;
        private int choosingPlayer = 1;
        private double animationTime;

        public CharacterSelectScreen(GameEngine engine) {
                this.engine = engine;
        }

        @Override
        public void onEnter() {
                int n = Roster.HEROES.length;

                p1Choice = Math.floorMod(engine.getPlayer1Choice(), n);
                p2Choice = Math.floorMod(engine.getPlayer2Choice(), n);

                // Keep the selected characters valid if a previously selected
                // character is now locked.
                if (!isUnlocked(p1Choice)) {
                        p1Choice = ProgressionManager.STARTER_CHARACTER;
                }

                if (!isUnlocked(p2Choice)) {
                        p2Choice = ProgressionManager.STARTER_CHARACTER;
                }

                choosingPlayer = 1;
                animationTime = 0;
        }

        private boolean isUnlocked(int index) {
                return Roster.isPlayable(index)
                                && engine.getProgression().isCharacterUnlocked(index);
        }

        @Override
        public void update(double dt, InputManager input) {
                animationTime += dt;

                if (input.wasPressed(KeyCode.ESCAPE)) {
                        engine.changeState(GameState.MENU);
                        return;
                }

                // Open Sorcery Upgradation.
                if (input.wasPressed(KeyCode.U)) {
                        engine.changeState(GameState.SORCERY_UPGRADATION);
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

                        int current = choosingPlayer == 1
                                        ? p1Choice
                                        : p2Choice;

                        // Never allow a locked character to be confirmed.
                        if (!isUnlocked(current)) {
                                return;
                        }

                        if (choosingPlayer == 1) {
                                engine.setPlayer1Choice(p1Choice);
                                choosingPlayer = 2;
                        } else {
                                engine.setPlayer2Choice(p2Choice);
                                engine.changeState(GameState.VERSUS);
                        }
                }
        }

        /** Moves the cursor only to unlocked, playable characters. */
        private void changeChoice(int direction) {
                int n = Roster.HEROES.length;

                int current = choosingPlayer == 1
                                ? p1Choice
                                : p2Choice;

                for (int step = 0; step < n; step++) {
                        current = Math.floorMod(current + direction, n);

                        if (isUnlocked(current)) {
                                break;
                        }
                }

                if (choosingPlayer == 1) {
                        p1Choice = current;
                } else {
                        p2Choice = current;
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

                // Roster strip.
                int n = Roster.HEROES.length;
                double margin = Math.max(24, w * 0.05);
                double gap = 8;
                double tileW = (w - 2 * margin - (n - 1) * gap) / n;
                double tileH = 58;
                double stripY = 105;

                for (int i = 0; i < n; i++) {
                        drawTile(
                                        gc,
                                        margin + i * (tileW + gap),
                                        stripY,
                                        tileW,
                                        tileH,
                                        i);
                }

                // Selected character details.
                int current = choosingPlayer == 1
                                ? p1Choice
                                : p2Choice;

                double cardY = stripY + tileH + 16;
                double cardH = h - cardY - 66;

                drawDetail(
                                gc,
                                margin,
                                cardY,
                                w - 2 * margin,
                                cardH,
                                current);

                // Footer.
                gc.setFill(Color.rgb(10, 12, 25, 0.97));
                gc.fillRoundRect(18, h - 53, w - 36, 40, 12, 12);

                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", 10));

                gc.fillText(
                                "A / D OR ARROWS: CHANGE"
                                                + "  •  ENTER: CONFIRM"
                                                + "  •  U: SORCERY UPGRADATION"
                                                + "  •  ESC: BACK",
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
                        GraphicsContext gc,
                        double w,
                        double h) {
                gc.setFill(BACKGROUND);
                gc.fillRect(0, 0, w, h);

                for (int i = 0; i < 9; i++) {
                        double x = i * w / 8.0;

                        gc.setStroke(Color.rgb(100, 95, 180, 0.08));
                        gc.setLineWidth(1);
                        gc.strokeLine(x, 0, x - 120, h);
                }

                for (int i = 0; i < 35; i++) {
                        double x = Math.floorMod(i * 137, (int) w);
                        double baseY = Math.floorMod(i * 83, (int) h);

                        double y = Math.floorMod(
                                        (int) (baseY + animationTime * (12 + i % 9)),
                                        (int) h);

                        gc.setFill(Color.rgb(171, 143, 255, 0.3));
                        gc.fillOval(x, y, 2 + i % 3, 2 + i % 3);
                }
        }

        private void drawTile(
                        GraphicsContext gc,
                        double x,
                        double y,
                        double w,
                        double h,
                        int i) {
                Hero hero = Roster.HEROES[i];
                Color color = hero.color();

                boolean locked = !isUnlocked(i);
                boolean by1 = p1Choice == i;
                boolean by2 = p2Choice == i;

                boolean active = choosingPlayer == 1
                                ? by1
                                : by2;

                // Tile background and border.
                gc.setFill(PANEL);
                gc.fillRoundRect(x, y, w, h, 12, 12);

                gc.setStroke(active
                                ? color
                                : Color.rgb(65, 73, 105));

                gc.setLineWidth(active ? 3 : 1);
                gc.strokeRoundRect(x, y, w, h, 12, 12);
                gc.setLineWidth(1);

                // Character emblem.
                double d = Math.min(w * 0.6, h * 0.55);

                gc.setFill(Color.color(
                                color.getRed(),
                                color.getGreen(),
                                color.getBlue(),
                                locked ? 0.25 : 0.95));

                gc.fillOval(
                                x + w / 2 - d / 2,
                                y + 6,
                                d,
                                d);

                // Character number.
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(Font.font("Arial", 9));
                gc.setFill(locked ? MUTED : Color.WHITE);

                gc.fillText(
                                String.valueOf(i + 1),
                                x + w / 2,
                                y + h - 6);

                // Dark overlay for locked characters.
                if (locked) {
                        gc.setFill(Color.rgb(5, 7, 16, 0.78));

                        gc.fillRoundRect(
                                        x + 2,
                                        y + 2,
                                        w - 4,
                                        h - 4,
                                        10,
                                        10);

                        gc.setFill(Color.GOLD);
                        gc.setFont(Font.font("Arial", 9));

                        gc.fillText(
                                        "LOCKED",
                                        x + w / 2,
                                        y + h / 2 + 3);
                }

                // Player markers.
                if (by1) {
                        gc.setFill(P1);
                        gc.setFont(Font.font("Arial", 9));
                        gc.fillText("P1", x + 12, y + 12);
                }

                if (by2) {
                        gc.setFill(P2);
                        gc.setFont(Font.font("Arial", 9));
                        gc.fillText("P2", x + w - 12, y + 12);
                }
        }

        private void drawDetail(
                        GraphicsContext gc,
                        double x,
                        double y,
                        double w,
                        double h,
                        int idx) {
                Hero hero = Roster.HEROES[idx];
                Color color = hero.color();

                boolean locked = !isUnlocked(idx);
                double pulse = 1.0 + Math.sin(animationTime * 2.5) * 0.04;

                gc.setFill(PANEL);
                gc.fillRoundRect(x, y, w, h, 18, 18);

                gc.setStroke(color);
                gc.setLineWidth(2);
                gc.strokeRoundRect(x, y, w, h, 18, 18);
                gc.setLineWidth(1);

                gc.setFill(color);
                gc.fillRoundRect(x + 2, y + 2, w - 4, 4, 4, 4);

                // Glowing emblem.
                double ex = x + w * 0.2;
                double ey = y + h * 0.45;
                double r = Math.min(w * 0.12, h * 0.3) * pulse;

                gc.setFill(Color.color(
                                color.getRed(),
                                color.getGreen(),
                                color.getBlue(),
                                0.2));

                gc.fillOval(
                                ex - r * 1.4,
                                ey - r * 1.4,
                                r * 2.8,
                                r * 2.8);

                gc.setFill(Color.color(
                                color.getRed(),
                                color.getGreen(),
                                color.getBlue(),
                                locked ? 0.25 : 0.95));

                gc.fillOval(ex - r, ey - r, r * 2, r * 2);

                // Character information.
                double tx = x + w * 0.42;

                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Serif", 26));
                gc.fillText(hero.name(), tx, y + 40);

                gc.setFill(color);
                gc.setFont(Font.font("Arial", 12));
                gc.fillText(
                                hero.title().toUpperCase(),
                                tx,
                                y + 60);

                gc.setFill(MUTED);
                gc.setFont(Font.font("Arial", 11));

                double ly = y + 86;

                for (String power : hero.powers()) {
                        gc.fillText("•  " + power, tx, ly);
                        ly += 17;
                }

                double sy = Math.max(ly + 14, y + h - 62);
                double sw = w - (tx - x) - 24;

                drawStat(gc, tx, sy, sw, "HP", hero.health(), color);
                drawStat(gc, tx, sy + 18, sw, "SPD", hero.speed(), color);
                drawStat(gc, tx, sy + 36, sw, "DMG", hero.damage(), color);

                // Ownership status.
                gc.setFont(Font.font("Arial", 12));

                if (locked) {
                        gc.setFill(Color.GOLD);
                        gc.fillText(
                                        "LOCKED — Visit Sorcery Upgradation (U) to unlock this fighter.",
                                        tx,
                                        y + h - 8);
                } else {
                        gc.setFill(Color.LIGHTGREEN);
                        gc.fillText("UNLOCKED", tx, y + h - 8);
                }
        }

        private void drawStat(
                        GraphicsContext gc,
                        double x,
                        double y,
                        double width,
                        String label,
                        int value,
                        Color accent) {
                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFill(MUTED);
                gc.setFont(Font.font("Arial", 10));
                gc.fillText(label, x, y);

                double barX = x + 40;
                double barW = Math.max(10, width - 80);

                gc.setFill(Color.rgb(42, 47, 70));
                gc.fillRoundRect(barX, y - 8, barW, 6, 6, 6);

                gc.setFill(accent);
                gc.fillRoundRect(
                                barX,
                                y - 8,
                                barW * Math.max(0, Math.min(1, value / 10.0)),
                                6,
                                6,
                                6);

                gc.setFill(Color.WHITE);
                gc.fillText(
                                value + " / 10",
                                x + 40 + barW + 8,
                                y);
        }
}
