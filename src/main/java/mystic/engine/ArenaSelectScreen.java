package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import mystic.common.Arenas;
import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class ArenaSelectScreen implements Screen {

    public static final String[] NAMES = Arenas.NAMES;
    private static final String[] DESC = Arenas.DESC;

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(30, Constants.WIDTH, Constants.HEIGHT);

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
            engine.changeState(engine.getMode() == mystic.common.GameMode.TRAINING
                    ? GameState.TRAINING
                    : GameState.VERSUS);
        } else if (in.wasPressed(KeyCode.ESCAPE) || in.wasPressed(KeyCode.BACK_SPACE)) {
            leaving = true;
            engine.changeState(GameState.CHARACTER_SELECT);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        // Plain background only: the arena preview covers most of the screen,
        // so extra layers here would just cost frame time.
        MysticTheme.drawBackground(gc, w, h);
        embers.draw(gc);

        MysticTheme.drawGlowText(gc, "CHOOSE YOUR BATTLEFIELD", w / 2, 62,
                MysticTheme.title(48), MysticTheme.GOLD, clamp(t / 0.5));

        // ---- ONE arena preview (the selected one) ----
        double px = w * 0.08, pw = w * 0.84, py = 88, ph = h * 0.52;

        // cheap glow border (no DropShadow effect)
        for (int i = 3; i >= 1; i--) {
            gc.setStroke(Color.web("#FFB347", 0.07 * (4 - i)));
            gc.setLineWidth(i * 5);
            gc.strokeRect(px - 2, py - 2, pw + 4, ph + 4);
        }
        gc.setFill(Color.BLACK);
        gc.fillRect(px, py, pw, ph);
        drawArena(gc, sel, px, py, pw, ph, t);

        if (flash > 0) {
            gc.setFill(Color.color(1, 1, 1, flash * 0.35));
            gc.fillRect(px, py, pw, ph);
        }
        gc.setStroke(MysticTheme.GOLD);
        gc.setLineWidth(4);
        gc.strokeRect(px, py, pw, ph);

        // arrows
        double pulse = 0.5 + 0.5 * Math.sin(t * 5);
        gc.setFill(Color.web("#FFD27A", 0.75 + 0.25 * pulse));
        double my = py + ph / 2;
        gc.fillPolygon(new double[] { px - 14, px - 14, px - 42 }, new double[] { my - 26, my + 26, my }, 3);
        gc.fillPolygon(new double[] { px + pw + 14, px + pw + 14, px + pw + 42 },
                new double[] { my - 26, my + 26, my }, 3);

        // ---- stats panel for the selected arena only ----
        double sy = py + ph + 16;
        double sh = h - sy - 52;
        drawStats(gc, px, sy, pw, sh);

        // page dots
        int n = NAMES.length;
        for (int i = 0; i < n; i++) {
            double dx = w / 2 + (i - (n - 1) / 2.0) * 26;
            double dy = h - 36;
            gc.setFill(i == sel ? MysticTheme.GOLD : Color.web("#8C6A2B", 0.7));
            double r = i == sel ? 7 : 5;
            gc.fillOval(dx - r, dy - r, r * 2, r * 2);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", 16));
        gc.setFill(Color.web("#C9A24B", 0.9));
        gc.fillText("A/D or Left/Right choose  -  Enter fight  -  Esc back", w / 2, h - 12);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    /** Name, description, bonus and position of the selected arena. */
    private void drawStats(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.web("#140304", 0.9));
        gc.fillRoundRect(x, y, w, h, 16, 16);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, 16, 16);

        double pad = 24;
        double leftW = w * 0.58;
        double midX = x + leftW + 10;

        // left: name + description
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(MysticTheme.title(Math.min(38, h * 0.36)));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText(NAMES[sel], x + pad, y + h * 0.42, leftW - pad);
        gc.setFont(Font.font("Serif", 18));
        gc.setFill(Color.web("#FFE9A0", 0.9));
        gc.fillText(DESC[sel], x + pad, y + h * 0.72, leftW - pad);

        // divider
        gc.setStroke(Color.web("#8C6A2B", 0.6));
        gc.setLineWidth(1.5);
        gc.strokeLine(midX - 6, y + 14, midX - 6, y + h - 14);

        // right: bonus + index
        double rx = midX + 10, rw = x + w - pad - rx;
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        gc.setFill(MysticTheme.GOLD_D);
        gc.fillText("ARENA BONUS", rx, y + h * 0.32);
        gc.setFont(MysticTheme.title(Math.min(26, h * 0.26)));
        gc.setFill(MysticTheme.SPARK);
        gc.fillText(Arenas.EFFECT[sel], rx, y + h * 0.60, rw);
        gc.setFont(Font.font("Serif", 14));
        gc.setFill(Color.web("#C9A24B", 0.85));
        gc.fillText("Arena " + (sel + 1) + " of " + NAMES.length, rx, y + h * 0.86);
    }

    // ------------------------------------------------------------ arenas

    /**
     * Draws an animated battlefield into a box. Reusable as a combat background.
     */
    public static void drawArena(GraphicsContext gc, int idx, double x, double y,
            double w, double h, double t) {
        drawArena(gc, idx, x, y, w, h, t, y + h * 0.80);
    }

    /** Same as above, but the floor line (horizon) is placed at an absolute Y. */
    public static void drawArena(GraphicsContext gc, int idx, double x, double y,
            double w, double h, double t, double floorY) {
        gc.save();
        gc.beginPath();
        gc.rect(x, y, w, h);
        gc.closePath();
        gc.clip();

        double cx = x + w / 2, cy = y + h * 0.42;
        double r = Math.min(w, h) * 0.42;
        int a = Math.floorMod(idx, 4);

        switch (a) {
            case 0 -> sanctum(gc, x, y, w, h, cx, cy, floorY, r, t);
            case 1 -> mirror(gc, x, y, w, h, cx, cy, floorY, r, t);
            case 2 -> gotham(gc, x, y, w, h, cx, cy, floorY, r, t);
            default -> cosmic(gc, x, y, w, h, cx, cy, floorY, r, t);
        }

        // Drifting embers (not in the rain-soaked arena)
        if (a != 2) {
            gc.setFill(Color.web(a == 1 ? "#BFE6FF" : "#FF8A1F", 0.6));
            for (int i = 0; i < 18; i++) {
                double sx = x + rnd(i * 3.1) * w + Math.sin(t + i) * 8;
                double sy = y + h - ((t * 30 + i * 53) % h);
                gc.fillOval(sx, sy, 2.5, 2.5);
            }
        }

        // Soft vignette to pull the eye to the centre
        gc.setFill(new RadialGradient(0, 0, cx, y + h * 0.5, Math.max(w, h) * 0.75, false,
                CycleMethod.NO_CYCLE,
                new Stop(0.55, Color.color(0, 0, 0, 0)), new Stop(1, Color.color(0, 0, 0, 0.55))));
        gc.fillRect(x, y, w, h);
        gc.restore();
    }

    // ---- 0: KAMAR-TAJ SANCTUM ------------------------------------------------

    private static void sanctum(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double floorY, double r, double t) {
        gc.setFill(new LinearGradient(0, y, 0, floorY, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3B0709")), new Stop(1, Color.web("#140304"))));
        gc.fillRect(x, y, w, h);

        // God rays from the ceiling
        for (int i = 0; i < 5; i++) {
            double o = (i - 2) * w * 0.03;
            double e = (i - 2) * w * 0.16;
            gc.setFill(Color.web("#FFD27A", 0.04 + 0.03 * Math.sin(t * 1.3 + i)));
            gc.fillPolygon(new double[] { cx + o, cx + o + w * 0.03, cx + e + w * 0.09, cx + e - w * 0.07 },
                    new double[] { y, y, floorY, floorY }, 4);
        }

        // Stone arches along the back wall
        double aw = w * 0.15;
        for (int i = 0; i < 5; i++) {
            if (i == 2)
                continue;
            double ax = x + (i + 0.5) * w / 5 - aw / 2;
            double ah = h * (i == 1 || i == 3 ? 0.62 : 0.52);
            arch(gc, ax, floorY, aw, ah, "#1C0507", "#8C6A2B");
            // warm glow inside each arch
            glow(gc, ax + aw / 2, floorY - ah * 0.45, aw * 0.75, "#FF8A1F", 0.18 + 0.06 * Math.sin(t * 2 + i));
        }

        // Hanging lanterns
        for (int i = 0; i < 4; i++) {
            double lx = x + w * (0.14 + i * 0.24) + Math.sin(t * 1.2 + i) * w * 0.008;
            double ly = y + h * (0.16 + 0.05 * (i % 2));
            gc.setStroke(Color.web("#8C6A2B", 0.8));
            gc.setLineWidth(1.5);
            gc.strokeLine(lx, y, lx, ly);
            glow(gc, lx, ly, h * 0.08, "#FF8A1F", 0.55);
            gc.setFill(Color.web("#FFC46B"));
            gc.fillOval(lx - h * 0.014, ly - h * 0.02, h * 0.028, h * 0.04);
        }

        // Sling-ring mandala
        glow(gc, cx, cy, r * 1.7, "#FF8A1F", 0.35);
        gc.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF3C4")), new Stop(1, Color.web("#D9A94E"))));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        gc.setStroke(Color.web("#7A0F12", 0.85));
        gc.setLineWidth(Math.max(2, r * 0.05));
        for (int i = 0; i < 8; i++) {
            double ang = Math.toRadians(i * 45 + t * 9);
            gc.strokeLine(cx + Math.cos(ang) * r * 0.18, cy + Math.sin(ang) * r * 0.18,
                    cx + Math.cos(ang) * r, cy + Math.sin(ang) * r);
        }
        gc.setLineWidth(Math.max(1.5, r * 0.03));
        gc.strokeOval(cx - r * 0.62, cy - r * 0.62, r * 1.24, r * 1.24);
        gc.strokeOval(cx - r * 0.30, cy - r * 0.30, r * 0.60, r * 0.60);
        MysticTheme.drawMandala(gc, cx, cy, r * 1.15, t * 10, 0.9);

        // Orbiting spark trails
        for (int i = 0; i < 14; i++) {
            double base = t * 1.4 + i * Math.PI * 2 / 14;
            for (int k = 0; k < 5; k++) {
                double ang = base - k * 0.07;
                double sx = cx + Math.cos(ang) * r * 1.22;
                double sy = cy + Math.sin(ang) * r * 1.22;
                double d = (5 - k) * r * 0.012 + 1;
                gc.setFill(Color.web("#FFB347", 0.9 - k * 0.17));
                gc.fillOval(sx - d, sy - d, d * 2, d * 2);
            }
        }

        // Floating runes
        for (int i = 0; i < 10; i++) {
            double ang = t * 0.3 + i * 0.63;
            double rx = cx + Math.cos(ang) * r * 1.8;
            double ry = cy + Math.sin(ang) * r * 0.6 + Math.sin(t * 2 + i) * 4;
            double s = r * 0.05;
            gc.setFill(Color.web("#FFD27A", 0.55 + 0.3 * Math.sin(t * 3 + i)));
            gc.fillPolygon(new double[] { rx, rx + s, rx, rx - s },
                    new double[] { ry - s * 1.6, ry, ry + s * 1.6, ry }, 4);
        }

        // Floor with glowing summoning ellipse
        drawFloor(gc, x, y, w, h, floorY, cx, "#2E0C0D", "#0B0203", "#C9A24B");
        double fh = y + h - floorY;
        gc.setStroke(Color.web("#FFB347", 0.6 + 0.2 * Math.sin(t * 3)));
        gc.setLineWidth(2);
        gc.strokeOval(cx - w * 0.26, floorY + fh * 0.15, w * 0.52, fh * 0.6);
        gc.setStroke(Color.web("#FFB347", 0.35));
        gc.strokeOval(cx - w * 0.19, floorY + fh * 0.27, w * 0.38, fh * 0.36);
        glow(gc, cx, floorY + fh * 0.45, w * 0.3, "#FF8A1F", 0.18);
    }

    // ---- 1: MIRROR DIMENSION -------------------------------------------------

    private static void mirror(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double floorY, double r, double t) {
        gc.setFill(new LinearGradient(0, y, 0, y + h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#120A2E")), new Stop(0.55, Color.web("#1B2A5A")),
                new Stop(1, Color.web("#0A0F1C"))));
        gc.fillRect(x, y, w, h);

        glow(gc, cx, cy, r * 1.8, "#9CC8FF", 0.55);
        glow(gc, cx, cy, r * 0.8, "#FFFFFF", 0.7);

        // Four folded copies of the city, rotated around the centre
        String[] fills = { "#3A4F8A", "#5B3A8A", "#2E6F8A", "#7A3A6A" };
        for (int k = 0; k < 4; k++) {
            gc.save();
            gc.translate(cx, cy);
            gc.rotate(k * 90 + Math.sin(t * 0.5 + k) * 4);
            gc.translate(-cx, -cy);
            skyline(gc, cx - w * 0.5, cy + h * 0.2, w, h * 0.5, 10 + k * 17, fills[k], "#CFEAFF", 0.55);
            gc.restore();
        }

        // Expanding sling-ring ripples
        for (int i = 0; i < 3; i++) {
            double ph = (t * 0.35 + i / 3.0) % 1.0;
            double rr = r * (0.2 + 1.3 * ph);
            gc.setStroke(Color.web("#FFB347", (1 - ph) * 0.7));
            gc.setLineWidth(3 * (1 - ph) + 1);
            gc.strokeOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }

        // Drifting glass shards
        for (int i = 0; i < 14; i++) {
            double sx = x + rnd(i * 5.3) * w;
            double sy = y + ((rnd(i * 2.9) * h + t * (6 + i)) % h);
            double s = 10 + rnd(i * 7.7) * 28 * (w / 700.0 + 0.4);
            double rot = t * (0.3 + rnd(i) * 0.6) + i;
            double[] px = new double[3], py = new double[3];
            for (int v = 0; v < 3; v++) {
                double ang = rot + v * 2.1 + rnd(i + v) * 0.8;
                px[v] = sx + Math.cos(ang) * s;
                py[v] = sy + Math.sin(ang) * s * 1.3;
            }
            gc.setFill(Color.web("#DDEBFF", 0.14));
            gc.fillPolygon(px, py, 3);
            gc.setStroke(Color.web("#FFFFFF", 0.55));
            gc.setLineWidth(1);
            gc.strokePolygon(px, py, 3);
        }

        // Glossy floor
        drawFloor(gc, x, y, w, h, floorY, cx, "#1C2750", "#070B18", "#9CC8FF");
        double fh = y + h - floorY;
        for (int i = 0; i < 7; i++) {
            double lx = x + rnd(i * 4.4) * w;
            double sh = 0.15 + 0.15 * Math.sin(t * 2 + i);
            gc.setFill(Color.web("#FFFFFF", sh));
            gc.fillRect(lx, floorY + 2, 2, fh * 0.9);
        }
        glow(gc, cx, floorY + fh * 0.3, w * 0.35, "#9CC8FF", 0.3);
    }

    // ---- 2: GOTHAM BLOOD MOON ------------------------------------------------

    private static void gotham(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double floorY, double r, double t) {
        gc.setFill(new LinearGradient(0, y, 0, floorY, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#08050F")), new Stop(0.6, Color.web("#2A0A18")),
                new Stop(1, Color.web("#4A1220"))));
        gc.fillRect(x, y, w, h);

        // Blood moon
        double mx = x + w * 0.68, my = y + h * 0.26, mr = Math.min(w, h) * 0.17;
        glow(gc, mx, my, mr * 3.4, "#FF2A2A", 0.35);
        gc.setFill(new RadialGradient(0, 0, mx - mr * 0.3, my - mr * 0.3, mr * 1.4, false,
                CycleMethod.NO_CYCLE, new Stop(0, Color.web("#FF9A78")), new Stop(1, Color.web("#9E1414"))));
        gc.fillOval(mx - mr, my - mr, mr * 2, mr * 2);
        gc.setFill(Color.web("#6E0C0C", 0.30));
        gc.fillOval(mx - mr * 0.5, my - mr * 0.1, mr * 0.45, mr * 0.35);
        gc.fillOval(mx + mr * 0.1, my - mr * 0.6, mr * 0.35, mr * 0.28);
        gc.fillOval(mx + mr * 0.15, my + mr * 0.25, mr * 0.4, mr * 0.3);

        // Clouds drifting across the moon
        for (int i = 0; i < 4; i++) {
            double clx = x + ((i * 0.37 * w + t * 10 * (1 + i * 0.3)) % (w * 1.5)) - w * 0.25;
            gc.setFill(Color.web("#12060C", 0.55));
            gc.fillOval(clx, y + h * (0.10 + 0.07 * i), w * 0.34, h * 0.07);
        }

        // Far skyline
        skyline(gc, x, floorY - h * 0.14, w, h * 0.30, 3, "#1B0B18", "#FFC96B", 0.95);

        // Mid layer: gothic spires with lit windows
        for (int i = 0; i < 9; i++) {
            double bw = w * (0.07 + 0.03 * rnd(i * 2.2));
            double bx = x + i * w / 8.5 - bw / 2;
            double bh = h * (0.25 + 0.20 * rnd(i * 4.1));
            double by = floorY - h * 0.10 - bh;
            gc.setFill(Color.web("#0C0610"));
            gc.fillRect(bx, by, bw, bh);
            gc.fillPolygon(new double[] { bx - 2, bx + bw / 2, bx + bw + 2 },
                    new double[] { by, by - bw * 1.7, by }, 3);
            gc.setFill(Color.web("#FFC96B", 0.65 + 0.25 * Math.sin(t * 3 + i)));
            for (int row = 0; row < 4; row++) {
                if ((i + row) % 3 != 0)
                    gc.fillRect(bx + bw * 0.3, by + 8 + row * h * 0.045, Math.max(2, bw * 0.14),
                            Math.max(3, h * 0.022));
            }
        }

        // Neon sign flicker
        double flick = 0.55 + 0.4 * Math.abs(Math.sin(t * 11));
        gc.setStroke(Color.rgb(255, 42, 190, flick));
        gc.setLineWidth(Math.max(2, w * 0.007));
        gc.strokeRoundRect(x + w * 0.14, y + h * 0.40, w * 0.12, h * 0.07, 8, 8);
        gc.setStroke(Color.rgb(66, 221, 255, 0.8));
        gc.strokeRoundRect(x + w * 0.34, y + h * 0.48, w * 0.09, h * 0.06, 8, 8);

        // Rooftop parapet with crenellations
        double pTop = floorY - h * 0.10;
        gc.setFill(Color.web("#07050B"));
        gc.fillRect(x, pTop, w, floorY - pTop);
        int cren = 14;
        double cw = w / cren;
        for (int i = 0; i < cren; i += 2)
            gc.fillRect(x + i * cw, pTop - h * 0.03, cw, h * 0.03);

        // Gargoyles on both corners
        gargoyle(gc, x + w * 0.10, pTop, h * 0.30, 1, t);
        gargoyle(gc, x + w * 0.90, pTop, h * 0.30, -1, t);

        // Wet floor with moon reflection
        drawFloor(gc, x, y, w, h, floorY, cx, "#1E0A12", "#07030A", "#8AB8E8");
        double fh = y + h - floorY;
        for (int i = 0; i < 9; i++) {
            double rw = (3 + 12 * rnd(i * 1.9)) * (1 + 0.3 * Math.sin(t * 3 + i));
            gc.setFill(Color.web("#FF4A3A", 0.30 - i * 0.02));
            gc.fillRect(mx - rw / 2 + Math.sin(t * 2 + i) * 4, floorY + i * fh / 9.5, rw, fh / 11);
        }
        for (int i = 0; i < 6; i++) {
            double ph = (t * 0.8 + rnd(i * 3.3)) % 1.0;
            double rx = x + rnd(i * 6.1) * w;
            double ry = floorY + fh * (0.2 + 0.7 * rnd(i * 8.2));
            double rr = ph * w * 0.03;
            gc.setStroke(Color.web("#BFD8F5", (1 - ph) * 0.5));
            gc.setLineWidth(1);
            gc.strokeOval(rx - rr, ry - rr * 0.3, rr * 2, rr * 0.6);
        }

        // Ground fog
        gc.setFill(new LinearGradient(0, floorY - h * 0.1, 0, floorY + fh * 0.4, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#C9A0B0", 0)), new Stop(1, Color.web("#C9A0B0", 0.18))));
        gc.fillRect(x, floorY - h * 0.1, w, h * 0.1 + fh * 0.4);

        // Rain
        gc.setStroke(Color.web("#9FC4EE", 0.45));
        gc.setLineWidth(1);
        for (int i = 0; i < 90; i++) {
            double rx = x + ((i * 53 + t * 110) % (w + 20)) - 10;
            double ry = y + ((i * 37 + t * 260) % h);
            gc.strokeLine(rx, ry, rx - 5, ry + 14);
        }

        // Occasional lightning flash
        double fl = Math.pow(Math.max(0, Math.sin(t * 0.7)), 30);
        if (fl > 0.02) {
            gc.setFill(Color.web("#E6D8FF", fl * 0.4));
            gc.fillRect(x, y, w, h);
        }
    }

    private static void gargoyle(GraphicsContext gc, double gx, double baseY, double s, int dir, double t) {
        gc.setFill(Color.web("#050308"));
        // wings
        gc.fillPolygon(new double[] { gx, gx - dir * s * 0.55, gx - dir * s * 0.30, gx - dir * s * 0.62,
                gx - dir * s * 0.10 },
                new double[] { baseY - s * 0.55, baseY - s * 1.05, baseY - s * 0.72, baseY - s * 0.55,
                        baseY - s * 0.35 },
                5);
        // body + head
        gc.fillPolygon(new double[] { gx - dir * s * 0.12, gx + dir * s * 0.12, gx + dir * s * 0.20,
                gx + dir * s * 0.08, gx - dir * s * 0.18 },
                new double[] { baseY, baseY, baseY - s * 0.45, baseY - s * 0.62, baseY - s * 0.50 }, 5);
        gc.fillPolygon(new double[] { gx + dir * s * 0.02, gx + dir * s * 0.07, gx + dir * s * 0.12 },
                new double[] { baseY - s * 0.60, baseY - s * 0.80, baseY - s * 0.60 }, 3);
        double eye = 0.6 + 0.4 * Math.sin(t * 4);
        gc.setFill(Color.web("#FF2A2A", eye));
        gc.fillOval(gx + dir * s * 0.09, baseY - s * 0.55, s * 0.035, s * 0.035);
    }

    // ---- 3: MULTIVERSE BREAKDOWN ---------------------------------------------

    private static void cosmic(GraphicsContext gc, double x, double y, double w, double h,
            double cx, double cy, double floorY, double r, double t) {
        gc.setFill(Color.web("#05020F"));
        gc.fillRect(x, y, w, h);

        glow(gc, cx - w * 0.22, cy, w * 0.5, "#7A3CFF", 0.55);
        glow(gc, cx + w * 0.25, cy + h * 0.1, w * 0.4, "#4FE3E0", 0.40);
        glow(gc, cx, y + h * 0.15, w * 0.35, "#C21F3A", 0.30);

        // Stars
        gc.setFill(Color.WHITE);
        for (int i = 0; i < 90; i++) {
            double sx = x + rnd(i * 1.7) * w;
            double sy = y + rnd(i * 2.3 + 5) * h * 0.8;
            double tw = 1 + 1.6 * Math.abs(Math.sin(t * 1.3 + i));
            gc.fillOval(sx, sy, tw, tw);
        }

        // Spiral galaxy arms
        Color[] arm = { Color.web("#B07CFF", 0.8), Color.web("#4FE3E0", 0.8), Color.web("#FF8A1F", 0.8) };
        for (int a = 0; a < 3; a++) {
            gc.setFill(arm[a]);
            for (int s = 0; s < 46; s++) {
                double f = s / 46.0;
                double ang = a * 2.094 + f * 5 + t * 0.6;
                double rad = f * r * 1.15;
                double d = 1.5 + 3 * f;
                gc.fillOval(cx + Math.cos(ang) * rad - d / 2, cy + Math.sin(ang) * rad * 0.55 - d / 2, d, d);
            }
        }

        // Three reality portals
        String[] pc = { "#FF8A1F", "#4FE3E0", "#C21F3A" };
        for (int p = 0; p < 3; p++) {
            double px = x + w * (0.17 + 0.33 * p);
            double py = y + h * (p == 1 ? 0.22 : 0.42);
            double pr = r * 0.42;
            glow(gc, px, py, pr * 1.8, pc[p], 0.35);
            gc.setFill(Color.web("#05020F", 0.85));
            gc.fillOval(px - pr * 0.8, py - pr * 0.8, pr * 1.6, pr * 1.6);
            for (int k = 0; k < 3; k++) {
                double rr = pr * (1 - k * 0.2);
                gc.setStroke(Color.web(pc[p], 0.9 - k * 0.2));
                gc.setLineWidth(3 - k * 0.7);
                gc.strokeOval(px - rr, py - rr, rr * 2, rr * 2);
            }
            for (int i = 0; i < 12; i++) {
                double ang = t * (p % 2 == 0 ? 1.6 : -1.6) + i * Math.PI / 6;
                double ex = px + Math.cos(ang) * pr * 1.1, ey = py + Math.sin(ang) * pr * 1.1;
                gc.setFill(Color.web(pc[p], 0.9));
                gc.fillOval(ex - 2, ey - 2, 4, 4);
            }
        }

        // Fractured reality panels
        for (int i = 0; i < 6; i++) {
            double sx = x + w * (0.1 + 0.16 * i) + Math.sin(t * 0.6 + i) * 8;
            double sy = y + h * (0.12 + 0.50 * rnd(i * 3.7)) + Math.sin(t * 0.9 + i * 2) * 10;
            double pw = w * 0.07, ph = h * 0.12;
            gc.save();
            gc.translate(sx, sy);
            gc.rotate(-25 + i * 11 + Math.sin(t + i) * 6);
            gc.setFill(new LinearGradient(0, 0, pw, ph, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(i % 2 == 0 ? "#7A0F12" : "#0F4F5A", 0.55)),
                    new Stop(1, Color.web("#D9A94E", 0.45))));
            gc.fillRect(-pw / 2, -ph / 2, pw, ph);
            gc.setStroke(Color.web("#4FE3E0", 0.8));
            gc.setLineWidth(1.5);
            gc.strokeRect(-pw / 2, -ph / 2, pw, ph);
            gc.restore();
        }

        // Floating rock islands
        for (int i = 0; i < 4; i++) {
            double ix = x + w * (0.12 + 0.25 * i) + Math.sin(t * 0.5 + i) * w * 0.01;
            double iy = y + h * 0.62 + Math.sin(t * 0.7 + i * 2) * h * 0.02;
            double iw = w * (0.10 + 0.03 * (i % 2)), ih = h * 0.09;
            glow(gc, ix, iy + ih * 0.8, iw * 0.9, "#7A3CFF", 0.25);
            gc.setFill(Color.web("#1A0E38"));
            gc.fillPolygon(new double[] { ix - iw / 2, ix + iw / 2, ix + iw * 0.15, ix - iw * 0.08 },
                    new double[] { iy, iy, iy + ih, iy + ih * 0.75 }, 4);
            gc.setFill(Color.web("#3A1F70"));
            gc.fillOval(ix - iw / 2, iy - ih * 0.18, iw, ih * 0.36);
            gc.setStroke(Color.web("#B07CFF", 0.85));
            gc.setLineWidth(1.5);
            gc.strokeOval(ix - iw / 2, iy - ih * 0.18, iw, ih * 0.36);
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

        // Glitch bars
        int gs = (int) Math.floor(t * 8);
        for (int i = 0; i < 3; i++) {
            double gy = y + rnd(gs * 3 + i) * h * 0.75;
            double gx = x + rnd(gs * 5 + i) * w * 0.8;
            gc.setFill(Color.web(i == 1 ? "#4FE3E0" : "#C21F3A", 0.28));
            gc.fillRect(gx, gy, w * 0.2, 2 + rnd(gs + i) * 4);
        }

        // Energy-grid floor
        drawFloor(gc, x, y, w, h, floorY, cx, "#1A0B3A", "#07031A", "#B07CFF");
        glow(gc, cx, floorY + (y + h - floorY) * 0.4, w * 0.32, "#4FE3E0", 0.25);
    }

    // ---- helpers ---------------------------------------------------------------

    /** Perspective floor: gradient + vanishing-point grid + bright horizon line. */
    private static void drawFloor(GraphicsContext gc, double x, double y, double w, double h,
            double floorY, double cx, String top, String bottom, String line) {
        double bot = y + h;
        gc.setFill(new LinearGradient(0, floorY, 0, bot, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(top)), new Stop(1, Color.web(bottom))));
        gc.fillRect(x, floorY, w, bot - floorY);

        gc.setStroke(Color.web(line, 0.30));
        gc.setLineWidth(1);
        for (int i = -10; i <= 10; i++)
            gc.strokeLine(cx + i * w * 0.03, floorY, cx + i * w * 0.14, bot);
        for (int j = 1; j <= 5; j++) {
            double f = j / 5.0;
            double yy = floorY + (bot - floorY) * f * f;
            gc.strokeLine(x, yy, x + w, yy);
        }
        gc.setStroke(Color.web(line, 0.9));
        gc.setLineWidth(2);
        gc.strokeLine(x, floorY, x + w, floorY);
    }

    /** Gothic arch: rectangle body topped by a half-circle. */
    private static void arch(GraphicsContext gc, double ax, double baseY, double aw, double ah,
            String fill, String stroke) {
        gc.setFill(Color.web(fill, 0.92));
        gc.fillRect(ax, baseY - ah + aw / 2, aw, ah - aw / 2);
        gc.fillArc(ax, baseY - ah, aw, aw, 0, 180, ArcType.ROUND);
        gc.setStroke(Color.web(stroke, 0.85));
        gc.setLineWidth(2);
        gc.strokeLine(ax, baseY, ax, baseY - ah + aw / 2);
        gc.strokeLine(ax + aw, baseY, ax + aw, baseY - ah + aw / 2);
        gc.strokeArc(ax, baseY - ah, aw, aw, 0, 180, ArcType.OPEN);
    }

    /** Row of deterministic buildings with lit windows. */
    private static void skyline(GraphicsContext gc, double left, double baseY, double width,
            double maxH, double seed, String fill, String win, double alpha) {
        double xx = left;
        for (int i = 0; i < 40 && xx < left + width; i++) {
            double bw = width * (0.04 + 0.05 * rnd(seed + i));
            double bh = maxH * (0.30 + 0.70 * rnd(seed * 3 + i * 7));
            gc.setFill(Color.web(fill, alpha));
            gc.fillRect(xx, baseY - bh, bw, bh);
            gc.setFill(Color.web(win, alpha * 0.85));
            for (double wy = baseY - bh + 6; wy < baseY - 8; wy += 10) {
                for (double wx = xx + 3; wx < xx + bw - 4; wx += 8) {
                    if (rnd(seed + wx * 0.13 + wy * 0.71) > 0.55)
                        gc.fillRect(wx, wy, 2.5, 4);
                }
            }
            xx += bw + 1;
        }
    }

    private static void glow(GraphicsContext gc, double cx, double cy, double r, String hex, double a) {
        gc.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(hex, clamp(a))), new Stop(1, Color.web(hex, 0))));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    /** Deterministic pseudo-random in [0,1). */
    private static double rnd(double s) {
        double v = Math.sin(s * 12.9898) * 43758.5453;
        return v - Math.floor(v);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}