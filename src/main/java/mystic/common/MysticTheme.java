package mystic.common;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.Random;

public final class MysticTheme {
    private MysticTheme() {
    }

    // Palette
    public static final Color BG = Color.web("#120405");
    public static final Color CRIMSON = Color.web("#7A0F12");
    public static final Color RED = Color.web("#C21E1E");
    public static final Color GOLD = Color.web("#FFE9A0");
    public static final Color GOLD_D = Color.web("#C9A24B");
    public static final Color SPARK = Color.web("#FF8A1F");
    public static final Color EYE = Color.web("#4FE3E0");

    // Font (loaded once, falls back to Serif if file is missing)
    private static String family;

    public static Font title(double size) {
        if (family == null) {
            family = "Serif";
            try (var in = MysticTheme.class.getResourceAsStream(
                    "/assets/ui/fonts/UnifrakturCook-Bold.ttf")) {
                if (in != null) {
                    Font f = Font.loadFont(in, 12);
                    if (f != null)
                        family = f.getFamily();
                }
            } catch (Exception ignored) {
            }
        }
        return Font.font(family, size);
    }

    // Background: radial crimson glow on near-black
    public static void drawBackground(GraphicsContext gc, double w, double h) {
        gc.setFill(BG);
        gc.fillRect(0, 0, w, h);
        gc.setFill(new javafx.scene.paint.RadialGradient(0, 0, w / 2, h / 2, Math.max(w, h) * 0.7,
                false, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0, Color.web("#7A0F12", 0.85)),
                new javafx.scene.paint.Stop(1, Color.web("#120405", 1))));
        gc.fillRect(0, 0, w, h);
    }

    // Rotating golden rings with rune ticks (Sanctum window look)
    public static void drawMandala(GraphicsContext gc, double cx, double cy,
            double r, double rot, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.translate(cx, cy);
        gc.setStroke(GOLD_D);
        gc.setLineWidth(1.5);
        for (int ring = 0; ring < 4; ring++) {
            double rr = r * (1 - ring * 0.18);
            gc.save();
            gc.rotate(rot * (ring % 2 == 0 ? 1 : -1) * (1 + ring * 0.3));
            gc.strokeOval(-rr, -rr, rr * 2, rr * 2);
            for (int i = 0; i < 24; i++) {
                gc.rotate(15);
                gc.strokeLine(rr - 8, 0, rr, 0);
            }
            gc.restore();
        }
        gc.restore();
    }

    // Glowing centered text
    public static void drawGlowText(GraphicsContext gc, String text, double x, double y,
            Font font, Color color, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setEffect(new DropShadow(24, SPARK));
        gc.setFont(font);
        gc.setFill(color);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x, y);
        gc.restore();
    }

    // Drifting glass shards
    private static final int SHARDS = 14;
    private static final double[][] SH = new double[SHARDS][7];
    static {
        Random r = new Random(7);
        for (double[] s : SH)
            for (int i = 0; i < s.length; i++)
                s[i] = r.nextDouble();
    }

    public static void drawShards(GraphicsContext gc, double w, double h, double t) {
        gc.save();
        for (int i = 0; i < SHARDS; i++) {
            double[] s = SH[i];
            double cx = (s[0] * w + Math.sin(t * 0.2 + i) * 20) % w;
            double cy = (s[1] * h + Math.cos(t * 0.15 + i) * 20) % h;
            double size = 40 + s[2] * 120;
            gc.setFill(Color.color(0.05, 0.05, 0.07, 0.35));
            gc.setStroke(Color.web("#FFE9A0", 0.12));
            double[] xs = { cx, cx + size * s[3], cx + size * (s[4] - 0.4) };
            double[] ys = { cy, cy + size * (s[5] - 0.2), cy + size * s[6] };
            gc.fillPolygon(xs, ys, 3);
            gc.strokePolygon(xs, ys, 3);
        }
        gc.restore();
    }

    // Rising embers
    public static final class Embers {
        private final double[] x, y, sp, sz;
        private final Random rnd = new Random();

        public Embers(int n, double w, double h) {
            x = new double[n];
            y = new double[n];
            sp = new double[n];
            sz = new double[n];
            for (int i = 0; i < n; i++)
                reset(i, w, h, true);
        }

        private void reset(int i, double w, double h, boolean anywhere) {
            x[i] = rnd.nextDouble() * w;
            y[i] = anywhere ? rnd.nextDouble() * h : h + 10;
            sp[i] = 20 + rnd.nextDouble() * 50;
            sz[i] = 1 + rnd.nextDouble() * 2.5;
        }

        public void update(double dt, double w, double h) {
            for (int i = 0; i < x.length; i++) {
                y[i] -= sp[i] * dt;
                x[i] += Math.sin(y[i] * 0.02 + i) * 0.4;
                if (y[i] < -10)
                    reset(i, w, h, false);
            }
        }

        public void draw(GraphicsContext gc) {
            gc.setFill(SPARK);
            for (int i = 0; i < x.length; i++) {
                gc.setGlobalAlpha(0.4 + 0.4 * Math.sin(i + y[i] * 0.05));
                gc.fillOval(x[i], y[i], sz[i], sz[i]);
            }
            gc.setGlobalAlpha(1);
        }
    }
}