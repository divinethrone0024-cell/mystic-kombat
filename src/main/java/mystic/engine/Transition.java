package mystic.engine;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.common.Constants;
import mystic.common.MysticTheme;

/** Shard-wipe cover: shards fly in, hold, then fly out. */
public class Transition {

    private static final double IN_TIME = 0.30; // shards cover screen
    private static final double OUT_TIME = 0.35; // shards clear

    private static final int COLS = 8;
    private static final int ROWS = 5;

    private final double[] delay = new double[COLS * ROWS];

    private boolean active;
    private boolean swapped;
    private double t;
    private Runnable onCovered;

    public Transition() {
        Random r = new Random(11);
        for (int i = 0; i < delay.length; i++)
            delay[i] = r.nextDouble() * 0.35;
    }

    /** Start a transition. onCovered runs once, when the screen is fully hidden. */
    public void start(Runnable onCovered) {
        this.onCovered = onCovered;
        this.active = true;
        this.swapped = false;
        this.t = 0;
    }

    public boolean isActive() {
        return active;
    }

    public void update(double dt) {
        if (!active)
            return;
        t += dt;

        if (!swapped && t >= IN_TIME + 0.08) {
            swapped = true;
            if (onCovered != null)
                onCovered.run();
        }
        if (t >= IN_TIME + 0.08 + OUT_TIME) {
            active = false;
        }
    }

    public void render(GraphicsContext gc) {
        if (!active)
            return;

        double w = Constants.WIDTH, h = Constants.HEIGHT;
        double cw = w / COLS, ch = h / ROWS;

        // progress: 0 -> 1 while covering, 1 -> 0 while clearing
        double p;
        if (t < IN_TIME + 0.08)
            p = Math.min(1, t / IN_TIME);
        else
            p = 1 - Math.min(1, (t - IN_TIME - 0.08) / OUT_TIME);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int i = row * COLS + col;
                double local = clamp((p - delay[i]) / (1 - 0.35));
                if (local <= 0)
                    continue;

                double ease = 1 - Math.pow(1 - local, 3);
                double x = col * cw, y = row * ch;
                double cx = x + cw / 2, cy = y + ch / 2;
                double s = ease * 1.15; // slight overlap, no gaps

                double[] xs = { cx - cw / 2 * s, cx + cw / 2 * s, cx + cw / 2 * s * 0.85, cx - cw / 2 * s * 1.1 };
                double[] ys = { cy - ch / 2 * s, cy - ch / 2 * s * 0.9, cy + ch / 2 * s, cy + ch / 2 * s };

                gc.setFill(Color.web("#120405"));
                gc.fillPolygon(xs, ys, 4);
                gc.setStroke(MysticTheme.GOLD_D);
                gc.setLineWidth(1.5);
                gc.strokePolygon(xs, ys, 4);
            }
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}