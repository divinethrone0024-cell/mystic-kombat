package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.io.InputStream;
import java.util.Random;

/**
 * Animated Sanctum Sanctorum arena.
 *
 * Same public API as before: draw(gc, width, height), so CombatScreen
 * needs no changes.
 *
 * Optional artwork (put PNGs in src/main/resources/bg/):
 * stage.png : full Sanctum Sanctorum background cropped from the asset sheet.
 * If stage.png is missing, a procedural background is drawn instead.
 * Animated effects (rotating runes, candle flicker, floor circle, embers)
 * are always drawn on top.
 */
public final class MysticArenaRenderer {

    // Candle positions as fractions of screen (x, y). Tune these to match
    // stage.png.
    private static final double[][] CANDLES = {
            { 0.07, 0.60 }, { 0.16, 0.55 }, { 0.29, 0.64 },
            { 0.71, 0.64 }, { 0.84, 0.55 }, { 0.93, 0.60 }
    };

    private static final int EMBERS = 70;

    private final Image stage = load("/bg/stage.png");

    private final double[] ex = new double[EMBERS];
    private final double[] ey = new double[EMBERS];
    private final double[] espeed = new double[EMBERS];
    private final double[] ephase = new double[EMBERS];
    private final Random random = new Random(21);

    private long lastNanos = 0;
    private double time = 0;

    public MysticArenaRenderer() {
        for (int i = 0; i < EMBERS; i++) {
            ex[i] = random.nextDouble();
            ey[i] = random.nextDouble();
            espeed[i] = 0.03 + random.nextDouble() * 0.07;
            ephase[i] = random.nextDouble() * Math.PI * 2;
        }
    }

    private static Image load(String path) {
        try (InputStream in = MysticArenaRenderer.class.getResourceAsStream(path)) {
            return in == null ? null : new Image(in);
        } catch (Exception e) {
            return null;
        }
    }

    public void draw(GraphicsContext gc, double w, double h) {
        if (w <= 0 || h <= 0) {
            return;
        }

        // Real-time clock so animation speed does not depend on the caller.
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : (now - lastNanos) / 1e9;
        lastNanos = now;
        dt = Math.min(dt, 0.05);
        time += dt;

        gc.save();
        try {
            if (stage != null) {
                gc.drawImage(stage, 0, 0, w, h);
            } else {
                drawSky(gc, w, h);
                drawCity(gc, w, h);
                drawPillars(gc, w, h);
                drawFloor(gc, w, h);
            }

            drawRuneWindow(gc, w, h);
            drawFloorCircle(gc, w, h);
            drawCandles(gc, w, h);
            drawEmbers(gc, w, h, dt);
            drawLightPulse(gc, w, h);
            drawVignette(gc, w, h);
        } finally {
            gc.restore();
        }
    }

    // ---------------- procedural backdrop (used when no stage.png)
    // ----------------

    private void drawSky(GraphicsContext gc, double w, double h) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0B0824")),
                new Stop(1, Color.web("#5A2A45"))));
        gc.fillRect(0, 0, w, h);

        Random r = new Random(1);
        for (int i = 0; i < 90; i++) {
            double sx = r.nextDouble() * w;
            double sy = r.nextDouble() * h * 0.5;
            double a = 0.3 + 0.7 * Math.abs(Math.sin(time * 1.4 + i));
            gc.setFill(Color.rgb(255, 250, 220, a));
            gc.fillOval(sx, sy, 2, 2);
        }
    }

    private void drawCity(GraphicsContext gc, double w, double h) {
        Random r = new Random(3);
        double base = h * 0.66;

        for (double x = -20; x < w + 20; x += 46) {
            double bh = h * (0.10 + r.nextDouble() * 0.25);
            gc.setFill(Color.web("#120E24"));
            gc.fillRect(x, base - bh, 42, bh);

            gc.setFill(Color.rgb(255, 185, 90, 0.5));
            for (double wy = base - bh + 8; wy < base - 8; wy += 16) {
                if (r.nextBoolean()) {
                    gc.fillRect(x + 8, wy, 5, 7);
                }
            }
        }
    }

    private void drawPillars(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.web("#1A1226"));
        gc.fillRect(0, 0, w * 0.09, h * 0.78);
        gc.fillRect(w * 0.91, 0, w * 0.09, h * 0.78);

        gc.setStroke(Color.web("#A16C43", 0.7));
        gc.setLineWidth(2);
        for (int i = 0; i < 7; i++) {
            double y = h * i / 7.0;
            gc.strokeLine(0, y, w * 0.09, y);
            gc.strokeLine(w * 0.91, y, w, y);
        }
    }

    private void drawFloor(GraphicsContext gc, double w, double h) {
        double fy = h * 0.72;
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2A1A2C")),
                new Stop(1, Color.web("#0C0812"))));
        gc.fillRect(0, fy, w, h - fy);

        gc.setStroke(Color.web("#4A3350", 0.6));
        gc.setLineWidth(1);
        for (int i = 0; i <= 12; i++) {
            gc.strokeLine(w * 0.5, fy, w * i / 12.0, h);
        }
        for (int i = 1; i <= 6; i++) {
            double t = i / 6.0;
            double y = fy + (h - fy) * t * t;
            gc.strokeLine(0, y, w, y);
        }
    }

    // ---------------- animated layers ----------------

    private void drawRuneWindow(GraphicsContext gc, double w, double h) {
        double cx = w * 0.5;
        double cy = h * 0.30;
        double r = Math.min(w * 0.13, h * 0.24);

        // Pulsing glow behind the window.
        double glow = 0.30 + 0.12 * Math.sin(time * 1.6);
        gc.setFill(new RadialGradient(0, 0, cx, cy, r * 1.6, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 160, 60, glow)),
                new Stop(1, Color.rgb(255, 160, 60, 0))));
        gc.fillOval(cx - r * 1.6, cy - r * 1.6, r * 3.2, r * 3.2);

        gc.setStroke(Color.rgb(255, 190, 90, 0.9));
        gc.setLineWidth(3);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Outer ring rotates clockwise, inner ring counter-clockwise.
        drawRotatingRing(gc, cx, cy, r * 0.86, 24, time * 20, 6);
        drawRotatingRing(gc, cx, cy, r * 0.60, 12, -time * 35, 8);

        // Spokes.
        gc.setStroke(Color.rgb(255, 190, 90, 0.5));
        gc.setLineWidth(1.5);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * i / 4.0;
            gc.strokeLine(cx - Math.cos(a) * r, cy - Math.sin(a) * r,
                    cx + Math.cos(a) * r, cy + Math.sin(a) * r);
        }
    }

    private void drawRotatingRing(GraphicsContext gc, double cx, double cy,
            double radius, int marks, double degrees, double size) {
        gc.save();
        gc.translate(cx, cy);
        gc.rotate(degrees);

        gc.setStroke(Color.rgb(255, 210, 120, 0.8));
        gc.setLineWidth(1.5);
        gc.strokeOval(-radius, -radius, radius * 2, radius * 2);

        gc.setFill(Color.rgb(255, 215, 130, 0.9));
        for (int i = 0; i < marks; i++) {
            gc.save();
            gc.rotate(i * 360.0 / marks);
            gc.fillRect(radius - size / 2, -size / 2, size, size);
            gc.restore();
        }
        gc.restore();
    }

    private void drawFloorCircle(GraphicsContext gc, double w, double h) {
        double cx = w * 0.5;
        double cy = h * 0.82;
        double r = w * 0.24;
        double squash = 0.30; // perspective: flatten the circle

        double pulse = 0.55 + 0.35 * Math.sin(time * 2.0);

        gc.save();
        gc.translate(cx, cy);
        gc.scale(1, squash);

        // Soft glow.
        gc.setFill(new RadialGradient(0, 0, 0, 0, r * 1.2, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 120, 30, 0.22 * pulse)),
                new Stop(1, Color.rgb(255, 120, 30, 0))));
        gc.fillOval(-r * 1.2, -r * 1.2, r * 2.4, r * 2.4);

        gc.setStroke(Color.rgb(255, 150, 50, 0.4 + 0.5 * pulse));
        gc.setLineWidth(3);
        gc.strokeOval(-r, -r, r * 2, r * 2);
        gc.strokeOval(-r * 0.8, -r * 0.8, r * 1.6, r * 1.6);

        // Slowly rotating sigil (two triangles + ticks).
        gc.rotate(time * 12);
        gc.setStroke(Color.rgb(255, 200, 100, 0.35 + 0.45 * pulse));
        gc.setLineWidth(2);
        for (int t = 0; t < 2; t++) {
            double[] px = new double[3];
            double[] py = new double[3];
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(t * 60 + i * 120 - 90);
                px[i] = Math.cos(a) * r * 0.78;
                py[i] = Math.sin(a) * r * 0.78;
            }
            gc.strokePolygon(px, py, 3);
        }
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24.0;
            gc.strokeLine(Math.cos(a) * r * 0.82, Math.sin(a) * r * 0.82,
                    Math.cos(a) * r * 0.98, Math.sin(a) * r * 0.98);
        }
        gc.restore();
    }

    private void drawCandles(GraphicsContext gc, double w, double h) {
        for (int i = 0; i < CANDLES.length; i++) {
            double x = CANDLES[i][0] * w;
            double y = CANDLES[i][1] * h;

            // Cheap flicker: several sine waves at different speeds.
            double f = 0.75
                    + 0.15 * Math.sin(time * 9 + i * 2)
                    + 0.10 * Math.sin(time * 23 + i * 5);

            double gr = 70 * f;
            gc.setFill(new RadialGradient(0, 0, x, y, gr, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 200, 100, 0.55)),
                    new Stop(1, Color.rgb(255, 140, 40, 0))));
            gc.fillOval(x - gr, y - gr, gr * 2, gr * 2);

            if (stage == null) {
                gc.setFill(Color.rgb(235, 220, 190));
                gc.fillRect(x - 3, y, 6, 24);
            }

            gc.setFill(Color.rgb(255, 225, 120));
            gc.fillOval(x - 3, y - 10 * f, 6, 12 * f);
        }
    }

    private void drawEmbers(GraphicsContext gc, double w, double h, double dt) {
        for (int i = 0; i < EMBERS; i++) {
            ey[i] -= espeed[i] * dt;
            ex[i] += Math.sin(time + ephase[i]) * 0.01 * dt;

            if (ey[i] < 0) {
                ey[i] = 1;
                ex[i] = random.nextDouble();
            }

            double a = Math.max(0, Math.min(1, ey[i] * 1.3));
            double size = 2 + (i % 3);

            gc.setFill(i % 4 == 0
                    ? Color.rgb(255, 170, 60, 0.8 * a)
                    : Color.rgb(255, 120, 40, 0.6 * a));
            gc.fillOval(ex[i] * w, ey[i] * h, size, size);
        }
    }

    // Subtle global brightness breathing.
    private void drawLightPulse(GraphicsContext gc, double w, double h) {
        double a = 0.03 + 0.02 * Math.sin(time * 1.2);
        gc.setFill(Color.rgb(255, 150, 60, a));
        gc.fillRect(0, 0, w, h);
    }

    private void drawVignette(GraphicsContext gc, double w, double h) {
        double r = Math.max(w, h) * 0.75;
        gc.setFill(new RadialGradient(0, 0, w / 2, h / 2, r, false, CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.rgb(2, 4, 11, 0)),
                new Stop(1, Color.rgb(2, 4, 11, 0.55))));
        gc.fillRect(0, 0, w, h);

        gc.setStroke(Color.web("#C38A45", 0.45));
        gc.setLineWidth(2);
        gc.strokeRect(4, 4, w - 8, h - 8);
    }
}