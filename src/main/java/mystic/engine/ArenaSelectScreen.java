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
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class ArenaSelectScreen implements Screen {

    public static final String[] NAMES = {
            "KAMAR-TAJ: THE SANCTUM", "CRIMSON MIRROR", "GOTHAM: BLOOD MOON", "MULTIVERSE BREAKDOWN"
    };
    private static final String[] DESC = {
            "Golden mandalas awaken beneath crimson sanctum arches.",
            "Black-glass shards fracture a realm of red lightning and gold.",
            "A blood moon burns over rain-dark rooftops and gothic stone.",
            "A broken multiverse floats in a storm of crimson and molten gold."
    };

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(40, Constants.WIDTH, Constants.HEIGHT);

    private double t, flash;
    private int sel;
    private boolean leaving;

    public ArenaSelectScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        flash = 0;
        sel = Math.min(engine.getArena(), NAMES.length - 1);
        leaving = false;
    }

    @Override
    public void update(double dt, InputManager in) {
        t += dt;
        flash = Math.max(0, flash - dt * 3);
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);
        if (leaving)
            return;

        int n = NAMES.length;
        if (in.wasPressed(KeyCode.LEFT) || in.wasPressed(KeyCode.A)) {
            sel = Math.floorMod(sel - 1, n);
            flash = 1;
        }
        if (in.wasPressed(KeyCode.RIGHT) || in.wasPressed(KeyCode.D)) {
            sel = Math.floorMod(sel + 1, n);
            flash = 1;
        }

        if (in.wasPressed(KeyCode.ENTER) || in.wasPressed(KeyCode.SPACE)) {
            leaving = true;
            engine.setArena(sel);
            engine.changeState(GameState.VERSUS);
        } else if (in.wasPressed(KeyCode.ESCAPE) || in.wasPressed(KeyCode.BACK_SPACE)) {
            leaving = true;
            engine.changeState(GameState.CHARACTER_SELECT);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        MysticTheme.drawBackground(gc, w, h);
        MysticTheme.drawShards(gc, w, h, t);
        embers.draw(gc);

        MysticTheme.drawGlowText(gc, "CHOOSE YOUR BATTLEFIELD", w / 2, 62,
                MysticTheme.title(48), MysticTheme.GOLD, clamp(t / 0.5));

        // Large animated preview
        double px = w * 0.08, pw = w * 0.84, py = 88, ph = h * 0.46;
        gc.save();
        gc.setEffect(new DropShadow(28, MysticTheme.SPARK));
        gc.setFill(Color.BLACK);
        gc.fillRect(px, py, pw, ph);
        gc.restore();
        drawArena(gc, sel, px, py, pw, ph, t);

        // name bar
        gc.setFill(new LinearGradient(0, py + ph - 90, 0, py + ph, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0, 0, 0, 0)), new Stop(1, Color.color(0, 0, 0, 0.85))));
        gc.fillRect(px, py + ph - 90, pw, 90);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(MysticTheme.title(38));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText(NAMES[sel], px + 24, py + ph - 38);
        gc.setFont(Font.font("Serif", 18));
        gc.setFill(Color.web("#FFE9A0", 0.9));
        gc.fillText(DESC[sel], px + 26, py + ph - 14);

        if (flash > 0) {
            gc.setFill(Color.color(1, 1, 1, flash * 0.35));
            gc.fillRect(px, py, pw, ph);
        }
        gc.setStroke(MysticTheme.GOLD);
        gc.setLineWidth(4);
        gc.strokeRect(px, py, pw, ph);

        // Arrows
        double pulse = 0.5 + 0.5 * Math.sin(t * 5);
        gc.save();
        gc.setEffect(new DropShadow(10 + 8 * pulse, MysticTheme.SPARK));
        gc.setFill(MysticTheme.GOLD);
        double my = py + ph / 2;
        gc.fillPolygon(new double[] { px - 14, px - 14, px - 42 }, new double[] { my - 26, my + 26, my }, 3);
        gc.fillPolygon(new double[] { px + pw + 14, px + pw + 14, px + pw + 42 }, new double[] { my - 26, my + 26, my },
                3);
        gc.restore();

        // Thumbnails
        int n = NAMES.length;
        double gap = 18, tw = (pw - (n - 1) * gap) / n;
        double ty = py + ph + 22, th = Math.max(40, h - ty - 80);
        for (int i = 0; i < n; i++) {
            double x = px + i * (tw + gap);
            double local = clamp((t - 0.15 - i * 0.1) / 0.4);
            gc.save();
            gc.setGlobalAlpha(local);
            drawArena(gc, i, x, ty, tw, th, t);
            if (i != sel) {
                gc.setFill(Color.color(0, 0, 0, 0.45));
                gc.fillRect(x, ty, tw, th);
            }
            gc.setStroke(i == sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
            gc.setLineWidth(i == sel ? 4 : 2);
            gc.strokeRect(x, ty, tw, th);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(MysticTheme.title(20));
            gc.setFill(i == sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
            gc.fillText(NAMES[i], x + tw / 2, ty + th + 24);
            gc.restore();
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", 16));
        gc.setFill(Color.web("#C9A24B", 0.9));
        gc.fillText("A/D or Left/Right choose  -  Enter fight  -  Esc back", w / 2, h - 22);
    }

    // ------------------------------------------------------------ arenas

    /**
     * Draws an animated battlefield into a box. Reusable as a combat background.
     */
    public static void drawArena(GraphicsContext gc, int idx, double x, double y,
            double w, double h, double t) {
        gc.save();
        gc.beginPath();
        gc.rect(x, y, w, h);
        gc.closePath();
        gc.clip();

        double cx = x + w / 2, cy = y + h * 0.42;
        double floorY = y + h * 0.80;
        double r = Math.min(w, h) * 0.42;

        switch (Math.floorMod(idx, 4)) {
            case 0 -> sanctum(gc, x, y, w, h, cx, cy, r, t);
            case 1 -> mirror(gc, x, y, w, h, cx, cy, floorY, r, t);
            case 2 -> crimson(gc, x, y, w, h, cx, cy, r, t);
            default -> cosmic(gc, x, y, w, h, cx, cy, floorY, r, t);
        }

        // Floor
        gc.setFill(Color.web("#120405", 0.9));
        gc.fillRect(x, floorY, w, y + h - floorY);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(2);
        gc.strokeLine(x, floorY, x + w, floorY);

        // Drifting sparks
        gc.setFill(Color.web("#FF8A1F", 0.6));
        for (int i = 0; i < 16; i++) {
            double sx = x + ((i * 97) % 100) / 100.0 * w + Math.sin(t + i) * 8;
            double sy = y + h - ((t * 30 + i * 53) % h);
            gc.fillOval(sx, sy, 2.5, 2.5);
        }
        gc.restore();
    }

    private static void sanctum(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double r, double t) {
        gc.setFill(new LinearGradient(0, y, 0, y + h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5A0C0F")), new Stop(1, Color.web("#1A0405"))));
        gc.fillRect(x, y, w, h);

        gc.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF3C4")), new Stop(1, Color.web("#D9A94E"))));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        gc.setStroke(Color.web("#7A0F12", 0.85));
        gc.setLineWidth(Math.max(2, r * 0.05));
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + t * 9);
            gc.strokeLine(cx + Math.cos(a) * r * 0.18, cy + Math.sin(a) * r * 0.18,
                    cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
        gc.setLineWidth(Math.max(1.5, r * 0.03));
        gc.strokeOval(cx - r * 0.62, cy - r * 0.62, r * 1.24, r * 1.24);
        gc.strokeOval(cx - r * 0.30, cy - r * 0.30, r * 0.60, r * 0.60);

        MysticTheme.drawMandala(gc, cx, cy, r * 1.15, t * 10, 0.9);

        for (int i = 0; i < 2; i++) {
            double a = t * 1.5 + i * Math.PI;
            double sx = cx + Math.cos(a) * r * 0.95, sy = cy + Math.sin(a) * r * 0.40;
            gc.setFill(Color.web("#FF8A1F", 0.30));
            gc.fillOval(sx - r * 0.12, sy - r * 0.12, r * 0.24, r * 0.24);
            gc.setFill(Color.web("#FFB347"));
            gc.fillOval(sx - r * 0.05, sy - r * 0.05, r * 0.10, r * 0.10);
        }
    }

    private static void mirror(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double floorY, double r, double t) {
        gc.setFill(new LinearGradient(0, y, 0, y + h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0A0F1C")), new Stop(1, Color.web("#1B2438"))));
        gc.fillRect(x, y, w, h);

        gc.setFill(new RadialGradient(0, 0, cx, cy, r * 0.9, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFFFFF", 0.85)), new Stop(1, Color.web("#FFFFFF", 0))));
        gc.fillOval(cx - r * 0.9, cy - r * 0.9, r * 1.8, r * 1.8);

        for (int k = 0; k < 4; k++) {
            gc.save();
            gc.translate(cx, cy);
            gc.rotate(k * 90 + Math.sin(t * 0.6) * 5);
            gc.translate(-cx, -cy);
            double bw = (w * 0.84) / 12;
            for (int j = 0; j < 12; j++) {
                double bh = 25 + ((j * 37 + 11) % 7) * h * 0.05;
                double bx = cx - w * 0.42 + j * bw;
                gc.setFill(Color.web("#C9D2E6", 0.40));
                gc.fillRect(bx, floorY - bh, bw - 3, bh);
                gc.setStroke(Color.web("#FFFFFF", 0.5));
                gc.setLineWidth(1);
                gc.strokeRect(bx, floorY - bh, bw - 3, bh);
            }
            gc.restore();
        }
    }

    private static void crimson(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double r, double t) {
        // Gotham Alley / Midnight Rooftop preview.
        gc.setFill(new LinearGradient(0, y, 0, y + h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#070B1A")), new Stop(1, Color.web("#28152E"))));
        gc.fillRect(x, y, w, h);

        // Foggy moon behind the skyline.
        gc.setFill(new RadialGradient(0, 0, cx + w * 0.18, cy - h * 0.08, r * 0.85,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#C6D8FF", 0.55)),
                new Stop(1, Color.web("#7387D8", 0))));
        gc.fillOval(cx + w * 0.18 - r * 0.85, cy - h * 0.08 - r * 0.85,
                r * 1.7, r * 1.7);

        // Distant city buildings.
        for (int i = 0; i < 22; i++) {
            double bw = w * (0.025 + (i % 4) * 0.008);
            double bh = h * (0.14 + ((i * 17) % 9) * 0.045);
            double bx = x + i * w / 21.0 - bw / 2;
            double by = y + h * 0.69 - bh;
            gc.setFill(Color.web(i % 2 == 0 ? "#101222" : "#171327"));
            gc.fillRect(bx, by, bw, bh);
            gc.setFill(Color.web("#FFC96B", 0.65));
            for (int row = 0; row < 5; row++) {
                if ((i + row) % 3 != 0) {
                    gc.fillRect(bx + bw * 0.28, by + 7 + row * h * 0.027,
                            Math.max(2, bw * 0.18), Math.max(2, h * 0.012));
                }
            }
        }

        // Neon signs flicker at different frequencies.
        double flicker = 0.65 + 0.30 * Math.abs(Math.sin(t * 11));
        gc.setStroke(Color.rgb(255, 42, 190, flicker));
        gc.setLineWidth(Math.max(2, w * 0.008));
        gc.strokeRoundRect(x + w * 0.15, y + h * 0.25, w * 0.18, h * 0.10, 10, 10);
        gc.setStroke(Color.rgb(66, 221, 255, 0.75));
        gc.strokeRoundRect(x + w * 0.69, y + h * 0.33, w * 0.15, h * 0.09, 10, 10);

        // Rooftop parapet and gargoyle silhouettes.
        gc.setFill(Color.web("#080911"));
        gc.fillRect(x, y + h * 0.67, w, h * 0.16);
        for (int i = 0; i < 7; i++) {
            double gx = x + i * w / 6.0;
            gc.fillPolygon(new double[] { gx - w * 0.025, gx, gx + w * 0.025, gx + w * 0.012 },
                    new double[] { y + h * 0.68, y + h * 0.55, y + h * 0.68, y + h * 0.72 }, 4);
        }

        // Animated rain streaks in the foreground.
        gc.setStroke(Color.web("#8AB8E8", 0.42));
        gc.setLineWidth(1);
        for (int i = 0; i < 65; i++) {
            double rx = x + ((i * 53 + t * 95) % w);
            double ry = y + ((i * 37 + t * 180) % h);
            gc.strokeLine(rx, ry, rx - 5, ry + 13);
        }
        gc.setFill(Color.web("#060710", 0.25));
        gc.fillRect(x, y, w, h);
    }

    private static void cosmic(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double floorY, double r, double t) {
        gc.setFill(Color.web("#05020F"));
        gc.fillRect(x, y, w, h);

        gc.setFill(new RadialGradient(0, 0, cx - w * 0.2, cy, w * 0.5, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7A3CFF", 0.55)), new Stop(1, Color.web("#7A3CFF", 0))));
        gc.fillRect(x, y, w, h);
        gc.setFill(new RadialGradient(0, 0, cx + w * 0.25, cy + h * 0.1, w * 0.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#4FE3E0", 0.40)), new Stop(1, Color.web("#4FE3E0", 0))));
        gc.fillRect(x, y, w, h);

        gc.setFill(Color.WHITE);
        for (int i = 0; i < 70; i++) {
            double sx = x + ((i * 73) % 100) / 100.0 * w;
            double sy = y + ((i * 47 + 13) % 100) / 100.0 * h * 0.8;
            double tw = 1 + 1.6 * Math.abs(Math.sin(t * 1.3 + i));
            gc.fillOval(sx, sy, tw, tw);
        }

        Color[] arm = { Color.web("#B07CFF", 0.8), Color.web("#4FE3E0", 0.8), Color.web("#FF8A1F", 0.8) };
        for (int a = 0; a < 3; a++) {
            gc.setFill(arm[a]);
            for (int s = 0; s < 40; s++) {
                double f = s / 40.0;
                double ang = a * 2.094 + f * 5 + t * 0.6;
                double rad = f * r * 1.1;
                double d = 1.5 + 3 * f;
                gc.fillOval(cx + Math.cos(ang) * rad - d / 2, cy + Math.sin(ang) * rad * 0.55 - d / 2, d, d);
            }
        }

        // The rift: a jagged crack through the centre
        gc.beginPath();
        for (int s = 0; s <= 10; s++) {
            double py = y + (floorY - y) * s / 10.0;
            double off = ((s % 2 == 0) ? -1 : 1) * (6 + 10 * Math.abs(Math.sin(t * 2 + s))) * (w / 600.0);
            if (s == 0)
                gc.moveTo(cx + off, py);
            else
                gc.lineTo(cx + off, py);
        }
        gc.setStroke(Color.web("#4FE3E0", 0.35));
        gc.setLineWidth(12);
        gc.stroke();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(3);
        gc.stroke();
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}