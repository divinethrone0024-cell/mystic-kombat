package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import mystic.common.Arenas;
import mystic.common.Constants;
import mystic.common.GameMode;
import mystic.common.GameState;
import mystic.common.MysticTheme;
import mystic.common.Roster;

public class VersusScreen implements Screen {

        private final GameEngine engine;
        private final MysticTheme.Embers embers = new MysticTheme.Embers(46, Constants.WIDTH, Constants.HEIGHT);

        private double animationTime;

        public VersusScreen(GameEngine engine) {
                this.engine = engine;
        }

        @Override
        public void onEnter() {
                animationTime = 0;
        }

        @Override
        public void update(double dt, InputManager input) {
                animationTime += dt;
                embers.update(dt, Constants.WIDTH, Constants.HEIGHT);

                if (input.wasPressed(KeyCode.ESCAPE)) {
                        engine.changeState(
                                        engine.getMode() == GameMode.ARCADE
                                                        ? GameState.ARCADE
                                                        : GameState.CHARACTER_SELECT);
                        return;
                }

                if (input.wasPressed(KeyCode.ENTER) || input.wasPressed(KeyCode.SPACE)) {
                        engine.changeState(GameState.COMBAT);
                }
        }

        @Override
        public void render(GraphicsContext gc) {
                double w = Constants.WIDTH;
                double h = Constants.HEIGHT;
                double t = animationTime;

                int n = Roster.HEROES.length;
                int p1 = Math.floorMod(engine.getPlayer1Choice(), n);
                int p2 = Math.floorMod(engine.getPlayer2Choice(), n);
                Roster.Hero hero1 = Roster.HEROES[p1];
                Roster.Hero hero2 = Roster.HEROES[p2];
                Color c1 = hero1.color();
                Color c2 = hero2.color();

                // --- Mystic background ---
                MysticTheme.drawBackground(gc, w, h);
                glow(gc, w * 0.22, h * 0.48, w * 0.50, c1, 0.30);
                glow(gc, w * 0.78, h * 0.48, w * 0.50, c2, 0.30);
                MysticTheme.drawShards(gc, w, h, t);

                // Diagonal clash light
                double sl = 0.05 + 0.03 * Math.sin(t * 2);
                gc.setFill(Color.web("#FFE9A0", sl));
                gc.fillPolygon(new double[] { w * 0.47, w * 0.53, w * 0.45, w * 0.39 },
                                new double[] { 0, 0, h, h }, 4);

                MysticTheme.drawMandala(gc, w / 2, h * 0.46, h * 0.44, t * 10, 0.20);
                embers.draw(gc);

                // --- Header ---
                double a0 = clamp(t / 0.5);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setGlobalAlpha(a0);
                gc.setFill(MysticTheme.GOLD_D);
                gc.setFont(Font.font("Serif", 16));
                gc.fillText("THE MULTIVERSE COLLIDES", w / 2, h * 0.075);
                gc.setGlobalAlpha(1);
                MysticTheme.drawGlowText(gc, "PREPARE FOR BATTLE", w / 2, h * 0.15,
                                MysticTheme.title(48), MysticTheme.GOLD, a0);

                // Arcade: show which trial this is.
                if (engine.getMode() == GameMode.ARCADE) {
                        int trial = engine.getArcadeTrial();
                        gc.setGlobalAlpha(a0);
                        gc.setFill(MysticTheme.GOLD);
                        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
                        gc.fillText("TRIAL " + (trial + 1) + ":  "
                                        + GameEngine.TRIAL_NAMES[trial]
                                        + "   -   " + engine.getDifficulty().name(),
                                        w / 2, h * 0.205);
                        gc.setGlobalAlpha(1);
                }

                // --- Fighters slide in from the sides ---
                double ease = ease(clamp((t - 0.1) / 0.6));
                double slide = (1 - ease) * w * 0.45;

                double cardH = h * 0.44;
                double cardW = cardH * 0.74;
                double centerY = h * 0.46;
                double leftX = w * 0.25;
                double rightX = w * 0.75;

                drawFighter(gc, leftX - slide, centerY, cardW, cardH, p1, c1, 1);
                drawFighter(gc, rightX + slide, centerY, cardW, cardH, p2, c2, -1);

                // Names + titles
                double textA = clamp((t - 0.55) / 0.4);
                gc.setGlobalAlpha(textA);
                double ny = centerY + cardH / 2 + 38;
                gc.save();
                gc.setEffect(new DropShadow(14, c1));
                gc.setFill(Color.WHITE);
                gc.setFont(MysticTheme.title(34));
                gc.fillText(hero1.name(), leftX, ny);
                gc.restore();
                gc.save();
                gc.setEffect(new DropShadow(14, c2));
                gc.setFill(Color.WHITE);
                gc.setFont(MysticTheme.title(34));
                gc.fillText(hero2.name(), rightX, ny);
                gc.restore();

                gc.setFill(MysticTheme.GOLD_D);
                gc.setFont(Font.font("Serif", 14));
                gc.fillText(hero1.title().toUpperCase(), leftX, ny + 24);
                gc.fillText(hero2.title().toUpperCase(), rightX, ny + 24);
                gc.setGlobalAlpha(1);

                // --- VS emblem ---
                double vs = ease(clamp((t - 0.35) / 0.35));
                double pulse = 1 + Math.sin(t * 4) * 0.05;
                double vr = Math.min(h * 0.11, 78) * pulse * (0.6 + 0.4 * vs);
                glow(gc, w / 2, centerY, vr * 3, MysticTheme.SPARK, 0.45 * vs);
                gc.setFill(Color.web("#0A0203", 0.92 * vs));
                gc.fillOval(w / 2 - vr, centerY - vr, vr * 2, vr * 2);
                gc.setStroke(Color.web("#FFE9A0", vs));
                gc.setLineWidth(3);
                gc.strokeOval(w / 2 - vr, centerY - vr, vr * 2, vr * 2);
                MysticTheme.drawMandala(gc, w / 2, centerY, vr * 1.25, -t * 25, 0.8 * vs);
                MysticTheme.drawGlowText(gc, "VS", w / 2, centerY + vr * 0.32,
                                MysticTheme.title(vr * 0.95), MysticTheme.GOLD, vs);

                // --- Battlefield banner ---
                double bw = w * 0.30, bh = h * 0.12;
                double bx = w / 2 - bw / 2, by = h * 0.755;
                double ba = clamp((t - 0.7) / 0.4);
                gc.setGlobalAlpha(ba);
                int arena = engine.getArena();
                ArenaSelectScreen.drawArena(gc, arena, bx, by, bw, bh, t);
                gc.setStroke(MysticTheme.GOLD_D);
                gc.setLineWidth(2);
                gc.strokeRect(bx, by, bw, bh);
                gc.setFill(Color.color(0, 0, 0, 0.6));
                gc.fillRect(bx, by + bh - 24, bw, 24);
                gc.setFill(MysticTheme.GOLD);
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
                gc.fillText(Arenas.NAMES[Math.floorMod(arena, Arenas.NAMES.length)], w / 2, by + bh - 7);
                gc.setGlobalAlpha(1);

                // --- Prompt ---
                double pa = 0.65 + 0.35 * Math.sin(t * 4);
                gc.setGlobalAlpha(clamp((t - 0.9) / 0.4) * pa);
                gc.setFill(MysticTheme.GOLD);
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
                gc.fillText("ENTER / SPACE  -  FIGHT", w / 2, h * 0.935);
                gc.setGlobalAlpha(clamp((t - 0.9) / 0.4));
                gc.setFill(MysticTheme.GOLD_D);
                gc.setFont(Font.font("Serif", 14));
                gc.fillText(engine.getMode() == GameMode.ARCADE
                                ? "ESC  -  BACK TO TRIALS"
                                : "ESC  -  CHANGE FIGHTERS", w / 2, h * 0.965);
                gc.setGlobalAlpha(1);

                gc.setTextAlign(TextAlignment.LEFT);
        }

        // ------------------------------------------------------------ fighter card

        private void drawFighter(GraphicsContext gc, double cx, double cy,
                        double cw, double ch, int fighter, Color accent, int dir) {
                double x = cx - cw / 2, y = cy - ch / 2;
                double t = animationTime;
                double bob = Math.sin(t * 2 + fighter) * 4;

                // outer aura
                glow(gc, cx, cy, ch * 0.85, accent, 0.30 + 0.08 * Math.sin(t * 3));

                gc.save();
                gc.setEffect(new DropShadow(26, accent));
                gc.setFill(Color.web("#0A0203"));
                gc.fillRoundRect(x, y, cw, ch, 18, 18);
                gc.restore();

                // clipped interior
                gc.save();
                gc.beginPath();
                gc.rect(x, y, cw, ch);
                gc.closePath();
                gc.clip();

                gc.setFill(new LinearGradient(0, y, 0, y + ch, false, CycleMethod.NO_CYCLE,
                                new Stop(0, darken(accent, 0.45)), new Stop(1, Color.web("#0A0203"))));
                gc.fillRect(x, y, cw, ch);

                // rotating light rays
                gc.save();
                gc.translate(cx, cy - ch * 0.05);
                gc.rotate(t * 12 * dir);
                gc.setFill(Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), 0.13));
                for (int i = 0; i < 8; i++) {
                        gc.rotate(45);
                        gc.fillPolygon(new double[] { 0, ch * 0.9, ch * 0.9 },
                                        new double[] { 0, -ch * 0.07, ch * 0.07 }, 3);
                }
                gc.restore();

                MysticTheme.drawMandala(gc, cx, cy - ch * 0.08 + bob, cw * 0.55, t * 14 * dir, 0.45);

                // soft backlight behind the character so the portrait pops
                glow(gc, cx, cy + ch * 0.02, cw * 0.75, accent, 0.35);

                // The character portrait fills the card (clipped to the frame).
                // dir < 0 flips player 2 so both fighters face the centre.
                boolean drawn = Portraits.draw(gc, fighter, cx, cy + ch * 0.04 + bob,
                                cw * 1.15, ch * 0.92, dir < 0);
                if (!drawn) {
                        // Only used when the PNG is missing.
                        silhouette(gc, cx, cy + ch * 0.12 + bob, cw * 0.58, fighter % 2, accent, t);
                }

                // floor fade
                gc.setFill(new LinearGradient(0, y + ch * 0.7, 0, y + ch, false, CycleMethod.NO_CYCLE,
                                new Stop(0, Color.web("#000000", 0)), new Stop(1, Color.web("#000000", 0.75))));
                gc.fillRect(x, y + ch * 0.7, cw, ch * 0.3);
                gc.restore();

                // frame
                gc.setStroke(accent);
                gc.setLineWidth(3);
                gc.strokeRoundRect(x, y, cw, ch, 18, 18);
                gc.setStroke(Color.web("#FFE9A0", 0.5));
                gc.setLineWidth(1);
                gc.strokeRoundRect(x + 7, y + 7, cw - 14, ch - 14, 12, 12);

                // corner studs
                gc.setFill(MysticTheme.GOLD);
                for (double[] p : new double[][] { { x, y }, { x + cw, y }, { x, y + ch }, { x + cw, y + ch } }) {
                        gc.fillPolygon(new double[] { p[0], p[0] + 6, p[0], p[0] - 6 },
                                        new double[] { p[1] - 6, p[1], p[1] + 6, p[1] }, 4);
                }
        }

        /** Fallback figure, used only when a portrait PNG is missing. */
        private void silhouette(GraphicsContext gc, double cx, double cy, double s,
                        int idx, Color accent, double t) {
                Color dark = Color.web("#07030A");

                double[] sx = { cx - s * 0.62, cx - s * 0.46, cx - s * 0.18, cx + s * 0.18, cx + s * 0.46,
                                cx + s * 0.62 };
                double[] sy = { cy + s * 0.85, cy + s * 0.30, cy + s * 0.16, cy + s * 0.16, cy + s * 0.30,
                                cy + s * 0.85 };
                gc.setFill(dark);
                gc.fillPolygon(sx, sy, 6);
                gc.setStroke(accent);
                gc.setLineWidth(2);
                gc.strokePolyline(sx, sy, 6);

                double[] hx = { cx - s * 0.28, cx - s * 0.25, cx, cx + s * 0.25, cx + s * 0.28 };
                double[] hy = { cy + s * 0.22, cy - s * 0.10, cy - s * 0.52, cy - s * 0.10, cy + s * 0.22 };
                if (idx == 1) {
                        hx = new double[] { cx - s * 0.27, cx - s * 0.30, cx - s * 0.12, cx, cx + s * 0.12,
                                        cx + s * 0.30, cx + s * 0.27 };
                        hy = new double[] { cy + s * 0.22, cy - s * 0.12, cy - s * 0.30, cy - s * 0.46, cy - s * 0.30,
                                        cy - s * 0.12, cy + s * 0.22 };
                }
                gc.setFill(dark);
                gc.fillPolygon(hx, hy, hx.length);
                gc.setStroke(accent);
                gc.strokePolygon(hx, hy, hx.length);

                gc.setFill(Color.web("#000000"));
                gc.fillOval(cx - s * 0.15, cy - s * 0.20, s * 0.30, s * 0.34);

                double blink = 0.75 + 0.25 * Math.sin(t * 3 + idx);
                gc.save();
                gc.setEffect(new DropShadow(10, idx == 0 ? MysticTheme.EYE : Color.web("#FF2A2A")));
                gc.setFill(idx == 0 ? MysticTheme.EYE : Color.web("#FF4A4A"));
                gc.setGlobalAlpha(blink);
                if (idx == 0) {
                        gc.fillOval(cx - s * 0.10, cy - s * 0.08, s * 0.06, s * 0.06);
                        gc.fillOval(cx + s * 0.04, cy - s * 0.08, s * 0.06, s * 0.06);
                } else {
                        gc.fillPolygon(new double[] { cx - s * 0.12, cx - s * 0.02, cx - s * 0.04 },
                                        new double[] { cy - s * 0.10, cy - s * 0.06, cy - s * 0.03 }, 3);
                        gc.fillPolygon(new double[] { cx + s * 0.12, cx + s * 0.02, cx + s * 0.04 },
                                        new double[] { cy - s * 0.10, cy - s * 0.06, cy - s * 0.03 }, 3);
                }
                gc.restore();
        }

        // ------------------------------------------------------------ helpers

        private static void glow(GraphicsContext gc, double cx, double cy, double r, Color c, double a) {
                gc.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                                new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), clamp(a))),
                                new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
                gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        }

        private static Color darken(Color c, double f) {
                return Color.color(c.getRed() * f, c.getGreen() * f, c.getBlue() * f);
        }

        private static double clamp(double v) {
                return Math.max(0, Math.min(1, v));
        }

        private static double ease(double x) {
                return 1 - Math.pow(1 - x, 3);
        }
}