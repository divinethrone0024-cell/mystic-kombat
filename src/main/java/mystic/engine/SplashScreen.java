package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class SplashScreen implements Screen {

    private static final double TITLE_DONE = 3.0;
    private static final double LEAVE_TIME = 0.5;

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(60, Constants.WIDTH, Constants.HEIGHT);

    private double t, leaveT;
    private boolean leaving;

    public SplashScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        leaveT = 0;
        leaving = false;
    }

    @Override
    public void update(double dt, InputManager input) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);

        if (leaving) {
            leaveT += dt;
            if (leaveT >= LEAVE_TIME)
                engine.changeState(GameState.MENU);
            return;
        }

        if (input.anyKeyPressed()) {
            if (t < TITLE_DONE)
                t = TITLE_DONE; // skip animation
            else
                leaving = true; // go to menu
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        MysticTheme.drawBackground(gc, w, h);
        MysticTheme.drawShards(gc, w, h, t);
        embers.draw(gc);

        double cx = w / 2, cy = h / 2 - 40;

        double trace = clamp(t / 1.2);
        gc.setStroke(MysticTheme.SPARK);
        gc.setLineWidth(5);
        gc.strokeArc(cx - 150, cy - 150, 300, 300, 90, -360 * trace, ArcType.OPEN);
        if (trace < 1) {
            double a = Math.toRadians(90 - 360 * trace);
            gc.setFill(Color.WHITE);
            gc.fillOval(cx + 150 * Math.cos(a) - 6, cy - 150 * Math.sin(a) - 6, 12, 12);
        }

        double m = clamp((t - 1.0) / 1.2);
        if (m > 0)
            MysticTheme.drawMandala(gc, cx, cy, 150 + 120 * m, t * 20, m);

        MysticTheme.drawGlowText(gc, "MYSTIC KOMBAT", cx, cy + 20,
                MysticTheme.title(96), MysticTheme.GOLD, clamp((t - 1.8) / 1.2));
        MysticTheme.drawGlowText(gc, "Enter the Multiverse", cx, cy + 70,
                MysticTheme.title(28), MysticTheme.GOLD_D, clamp((t - 2.4) / 0.6));

        if (t >= TITLE_DONE && !leaving) {
            double blink = 0.5 + 0.5 * Math.sin(t * 4);
            MysticTheme.drawGlowText(gc, "PRESS ANY KEY", cx, h - 90,
                    MysticTheme.title(30), MysticTheme.GOLD, 0.35 + 0.65 * blink);
        }

        if (leaving) {
            gc.setFill(Color.color(0, 0, 0, clamp(leaveT / LEAVE_TIME)));
            gc.fillRect(0, 0, w, h);
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}