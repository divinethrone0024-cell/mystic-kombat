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

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;
import mystic.common.Roster;
import mystic.common.Roster.Hero;

public class BookSelectScreen implements Screen {

    private enum Phase {
        COVER, OPENING, SELECT
    }

    private static final double OPEN_TIME = 1.3;
    private static final double FLIP_TIME = 0.55;
    private static final Color P1 = Color.web("#FF4B3E");
    private static final Color P2 = Color.web("#1FB5B0");
    private static final Color INK = Color.web("#3A0B0D");
    private static final Color PAGE_LINE = Color.web("#8A6A1E");

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(50, Constants.WIDTH, Constants.HEIGHT);

    private Phase phase = Phase.COVER;
    private double t, openT, flipT, anim;
    private boolean flipping, leaving;
    private int shown, target, who;
    private final int[] cur = { 0, 1 };
    private final boolean[] lock = new boolean[2];
    private String msg = "";
    private double msgT;

    public BookSelectScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        phase = Phase.COVER;
        t = 0;
        openT = 0;
        flipT = 0;
        anim = 0;
        flipping = false;
        leaving = false;
        who = 0;
        int n = Roster.HEROES.length;
        cur[0] = Math.floorMod(engine.getPlayer1Choice(), n);
        cur[1] = Math.floorMod(engine.getPlayer2Choice(), n);
        lock[0] = lock[1] = false;
        shown = target = cur[0];
        msg = "";
        msgT = 0;
    }

    // ------------------------------------------------------------ update

    @Override
    public void update(double dt, InputManager in) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);
        if (msgT > 0)
            msgT -= dt;
        if (leaving)
            return;

        switch (phase) {
            case COVER -> {
                if (in.wasPressed(KeyCode.ESCAPE)) {
                    leaving = true;
                    engine.changeState(GameState.MODE_SELECT);
                } else if (in.anyKeyPressed()) {
                    phase = Phase.OPENING;
                    openT = 0;
                }
            }
            case OPENING -> {
                openT += dt;
                if (in.anyKeyPressed())
                    openT = OPEN_TIME;
                if (openT >= OPEN_TIME) {
                    phase = Phase.SELECT;
                    shown = target = cur[who];
                    anim = 0;
                }
            }
            case SELECT -> updateSelect(dt, in);
        }
    }

    private void updateSelect(double dt, InputManager in) {
        if (flipping) {
            flipT += dt / FLIP_TIME;
            if (flipT >= 1) {
                flipping = false;
                shown = target;
                anim = 0;
            }
            return;
        }
        anim += dt;

        if (in.wasPressed(KeyCode.ESCAPE)) {
            leaving = true;
            engine.changeState(GameState.MODE_SELECT);
            return;
        }

        int n = Roster.HEROES.length;
        boolean left = in.wasPressed(KeyCode.A) || in.wasPressed(KeyCode.LEFT);
        boolean right = in.wasPressed(KeyCode.D) || in.wasPressed(KeyCode.RIGHT);

        if (left || right) {
            if (lock[who]) {
                flash("Unlock first (Backspace) to change fighter");
            } else {
                cur[who] = Math.floorMod(cur[who] + (right ? 1 : -1), n);
                startFlip(cur[who]);
            }
        }

        if (in.wasPressed(KeyCode.TAB)) {
            who = 1 - who;
            startFlip(cur[who]);
        }

        if (in.wasPressed(KeyCode.BACK_SPACE)) {
            lock[who] = false;
        }

        if (in.wasPressed(KeyCode.ENTER) || in.wasPressed(KeyCode.SPACE)) {
            if (lock[0] && lock[1]) {
                leaving = true;
                engine.setPlayer1Choice(cur[0]);
                engine.setPlayer2Choice(cur[1]);
                engine.changeState(GameState.ARENA_SELECT);
            } else if (!lock[who]) {
                if (Roster.HEROES[cur[who]].playable()) {
                    lock[who] = true;
                    if (!lock[1 - who]) { // jump to the other player
                        who = 1 - who;
                        startFlip(cur[who]);
                    }
                } else {
                    flash("This champion is sealed - not summoned yet");
                }
            } else {
                who = 1 - who; // this one is locked, help the other
                startFlip(cur[who]);
            }
        }
    }

    private void startFlip(int idx) {
        if (idx == shown)
            return;
        target = idx;
        flipT = 0;
        flipping = true;
    }

    private void flash(String text) {
        msg = text;
        msgT = 2.0;
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        MysticTheme.drawBackground(gc, w, h);
        MysticTheme.drawShards(gc, w, h, t);
        embers.draw(gc);

        double cx = w / 2, cy = h / 2 + 20;
        double pw = Math.min(520, w / 2 - 70);
        double ph = h - 190;

        double raw = switch (phase) {
            case COVER -> 0;
            case OPENING -> clamp(openT / OPEN_TIME);
            case SELECT -> 1;
        };
        double p = raw * raw * (3 - 2 * raw); // smooth open

        double top = cy - ph / 2 - 14, bh = ph + 28;

        gc.save();
        gc.translate(-pw / 2 * (1 - p), 0); // closed book sits centred

        // back cover
        double baseL = cx - pw * p - 14;
        gc.save();
        gc.setEffect(new DropShadow(30, Color.BLACK));
        gc.setFill(Color.web("#3A0709"));
        gc.fillRoundRect(baseL, top, cx + pw + 14 - baseL, bh, 18, 18);
        gc.restore();
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(3);
        gc.strokeRoundRect(baseL, top, cx + pw + 14 - baseL, bh, 18, 18);

        // blank right page appears while the cover swings
        if (p > 0 && phase != Phase.SELECT) {
            parchment(gc, cx, cy - ph / 2, pw - 10, ph);
        }

        // swinging cover
        double cw = pw * Math.cos(Math.PI * p);
        if (cw > 0.5) {
            gc.save();
            gc.translate(cx, 0);
            gc.scale(Math.max(0.001, cw / pw), 1);
            gc.translate(-cx, 0);
            drawCoverFace(gc, cx, cy, pw, ph, top, bh);
            gc.restore();
        } else if (cw < -0.5) {
            double iw = -cw;
            gc.setFill(Color.web("#5A0E10"));
            gc.fillRoundRect(cx - iw, top, iw, bh, 14, 14);
            gc.setStroke(MysticTheme.GOLD_D);
            gc.setLineWidth(2);
            gc.strokeRoundRect(cx - iw + 6, top + 6, iw - 12, bh - 12, 10, 10);
        }

        if (phase == Phase.SELECT) {
            drawPages(gc, cx, cy, pw, ph);
        }
        gc.restore();

        if (phase == Phase.COVER) {
            double blink = 0.5 + 0.5 * Math.sin(t * 4);
            MysticTheme.drawGlowText(gc, "PRESS ANY KEY", cx, cy + ph / 2 + 52,
                    MysticTheme.title(30), MysticTheme.GOLD, 0.35 + 0.65 * blink);
        }

        if (phase == Phase.SELECT) {
            drawTabs(gc, w);
            drawArrows(gc, cx, cy, pw);
            drawDots(gc, cx, h);
            drawNext(gc, w, h);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", 15));
            gc.setFill(Color.web("#C9A24B", 0.9));
            gc.fillText("A/D flip page  -  TAB switch player  -  ENTER lock  -  BACKSPACE unlock  -  ESC back",
                    cx, h - 16);

            if (msgT > 0) {
                gc.setFont(Font.font("Serif", 19));
                gc.setFill(Color.web("#FF6B5E"));
                gc.fillText(msg, cx, h - 76);
            }
        }
    }

    private void drawPages(GraphicsContext gc, double cx, double cy, double pw, double ph) {
        double lx = cx - pw + 10, rx = cx, pageW = pw - 10, py = cy - ph / 2;

        if (!flipping) {
            drawPortrait(gc, shown, lx, py, pageW, ph);
            drawDetails(gc, shown, rx, py, pageW, ph, clamp(anim / 0.7));
            return;
        }

        double f = clamp(flipT);
        double sx = Math.cos(Math.PI * f);

        if (f < 0.5) {
            drawPortrait(gc, shown, lx, py, pageW, ph);
            gc.save();
            gc.translate(cx, 0);
            gc.scale(Math.max(0.001, sx), 1);
            gc.translate(-cx, 0);
            drawDetails(gc, shown, rx, py, pageW, ph, 1);
            gc.restore();
        } else {
            gc.save();
            gc.setGlobalAlpha(clamp((f - 0.5) / 0.5));
            drawDetails(gc, target, rx, py, pageW, ph, 0);
            gc.restore();

            gc.save();
            gc.translate(cx, 0);
            gc.scale(Math.max(0.001, -sx), 1);
            gc.translate(-cx, 0);
            drawPortrait(gc, target, lx, py, pageW, ph);
            gc.restore();
        }
    }

    private void drawCoverFace(GraphicsContext gc, double cx, double cy, double pw,
            double ph, double top, double bh) {
        gc.setFill(new LinearGradient(cx, top, cx, top + bh, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#5E0F12")), new Stop(1, Color.web("#2A0506"))));
        gc.fillRoundRect(cx, top, pw, bh, 18, 18);

        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(4);
        gc.strokeRoundRect(cx + 10, top + 10, pw - 20, bh - 20, 12, 12);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(cx + 22, top + 22, pw - 44, bh - 44, 8, 8);

        double mx = cx + pw / 2;
        MysticTheme.drawMandala(gc, mx, cy - ph * 0.12, pw * 0.26, t * 20, 0.95);

        MysticTheme.drawGlowText(gc, "KAMAR TAJ", mx, cy + ph * 0.22,
                MysticTheme.title(58), MysticTheme.GOLD, 1);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(2);
        gc.strokeLine(mx - pw * 0.32, cy + ph * 0.22 + 18, mx + pw * 0.32, cy + ph * 0.22 + 18);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("THE MULTIVERSE BREAKDOWN", mx, cy + ph * 0.22 + 52);
    }

    // ------------------------------------------------------------ pages

    private void parchment(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(new LinearGradient(x, y, x, y + h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#F4DE96")), new Stop(1, Color.web("#D4A94F"))));
        gc.fillRoundRect(x, y, w, h, 8, 8);
        gc.setStroke(PAGE_LINE);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, 8, 8);
        gc.setLineWidth(1);
        gc.setGlobalAlpha(gc.getGlobalAlpha() * 0.6);
        gc.strokeRoundRect(x + 10, y + 10, w - 20, h - 20, 6, 6);
        gc.setGlobalAlpha(gc.getGlobalAlpha() / 0.6);
        gc.setFill(PAGE_LINE);
        double[][] c = { { x + 10, y + 10 }, { x + w - 10, y + 10 }, { x + 10, y + h - 10 },
                { x + w - 10, y + h - 10 } };
        for (double[] pt : c)
            gc.fillOval(pt[0] - 4, pt[1] - 4, 8, 8);
    }

    private void drawPortrait(GraphicsContext gc, int idx, double x, double y, double w, double h) {
        Hero hd = Roster.HEROES[idx];
        parchment(gc, x, y, w, h);

        double pcx = x + w / 2, pcy = y + h * 0.40;
        Color c = hd.color();

        double ar = w * 0.36;
        gc.setFill(new RadialGradient(0, 0, pcx, pcy, ar, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.55)),
                new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
        gc.fillOval(pcx - ar, pcy - ar, ar * 2, ar * 2);

        gc.setStroke(Color.web("#7A0F12", 0.85));
        gc.setLineWidth(2);
        double r0 = w * 0.30;
        for (int ring = 0; ring < 3; ring++) {
            double rr = r0 * (1 - ring * 0.16);
            gc.save();
            gc.translate(pcx, pcy);
            gc.rotate(t * (ring % 2 == 0 ? 12 : -12));
            gc.strokeOval(-rr, -rr, rr * 2, rr * 2);
            for (int i = 0; i < 20; i++) {
                gc.rotate(18);
                gc.strokeLine(rr - 7, 0, rr, 0);
            }
            gc.restore();
        }

        // Placeholder portrait: swap for the real sprite once the sheet layout is
        // known.
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(w * 0.26));
        gc.setFill(c);
        gc.fillText(hd.name().substring(0, 1), pcx, pcy + w * 0.09);
        gc.setStroke(INK);
        gc.setLineWidth(2);
        gc.strokeText(hd.name().substring(0, 1), pcx, pcy + w * 0.09);

        gc.setFont(MysticTheme.title(34));
        gc.setFill(INK);
        gc.fillText(hd.name(), pcx, y + h * 0.74);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 19));
        gc.setFill(Color.web("#7A0F12"));
        gc.fillText(hd.title(), pcx, y + h * 0.74 + 28);

        // which player has this page
        int count = (cur[0] == idx ? 1 : 0) + (cur[1] == idx ? 1 : 0);
        int k = 0;
        for (int pl = 0; pl < 2; pl++) {
            if (cur[pl] != idx)
                continue;
            double bx = pcx + (k - (count - 1) / 2.0) * 135 - 60;
            gc.setFill(pl == 0 ? P1 : P2);
            gc.fillRoundRect(bx, y + h * 0.86, 120, 26, 10, 10);
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 15));
            gc.fillText("P" + (pl + 1) + (lock[pl] ? "  LOCKED" : ""), bx + 60, y + h * 0.86 + 18);
            k++;
        }

        gc.setFont(Font.font("Serif", 15));
        gc.setFill(PAGE_LINE);
        gc.fillText("- " + (idx + 1) + " of " + Roster.HEROES.length + " -", pcx, y + h - 20);
    }

    private void drawDetails(GraphicsContext gc, int idx, double x, double y,
            double w, double h, double bars) {
        Hero hd = Roster.HEROES[idx];
        parchment(gc, x, y, w, h);

        double s = Math.min(1.0, h / 520.0);
        double cx = x + w / 2;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(36));
        gc.setFill(INK);
        gc.fillText("ABILITIES", cx, y + 56 * s);
        gc.setStroke(PAGE_LINE);
        gc.setLineWidth(2);
        gc.strokeLine(x + 40, y + 70 * s, x + w - 40, y + 70 * s);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        double yy = y + 106 * s;
        for (String line : hd.powers()) {
            gc.setFill(hd.color());
            gc.fillOval(x + 40, yy - 12, 10, 10);
            gc.setFill(INK);
            gc.fillText(line, x + 62, yy);
            yy += 34 * s;
        }

        yy += 14 * s;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(32));
        gc.setFill(INK);
        gc.fillText("ATTRIBUTES", cx, yy);
        gc.strokeLine(x + 40, yy + 12 * s, x + w - 40, yy + 12 * s);

        yy += 52 * s;
        stat(gc, "HEALTH", hd.health(), x, yy, w, hd.color(), bars);
        stat(gc, "SPEED", hd.speed(), x, yy + 40 * s, w, hd.color(), bars);
        stat(gc, "DAMAGE", hd.damage(), x, yy + 80 * s, w, hd.color(), bars);

        if (!hd.playable()) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 19));
            gc.setFill(Color.web("#9A1015"));
            gc.fillText("SEALED  -  not summoned yet", cx, y + h - 24);
        }
    }

    private void stat(GraphicsContext gc, String label, int v, double x, double y,
            double w, Color c, double prog) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.setFill(INK);
        gc.fillText(label, x + 40, y);

        double bx = x + 135, bw = w - 135 - 85, bh = 16;
        gc.setFill(Color.web("#7A5A18", 0.35));
        gc.fillRoundRect(bx, y - 14, bw, bh, 8, 8);
        gc.setFill(c.darker());
        gc.fillRoundRect(bx, y - 14, bw * Math.min(1, v / 10.0) * prog, bh, 8, 8);
        gc.setStroke(PAGE_LINE);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(bx, y - 14, bw, bh, 8, 8);

        gc.setFill(INK);
        gc.fillText(v + " / 10", bx + bw + 10, y);
    }

    // ------------------------------------------------------------ HUD icons

    private void drawTabs(GraphicsContext gc, double w) {
        double cw = 300, ch = 46, gap = 30;
        double x0 = w / 2 - cw - gap / 2, y = 22;
        for (int i = 0; i < 2; i++) {
            double x = x0 + i * (cw + gap);
            boolean act = who == i;
            Color c = i == 0 ? P1 : P2;

            gc.save();
            if (act)
                gc.setEffect(new DropShadow(18, c));
            gc.setFill(Color.web("#220708", 0.95));
            gc.fillRoundRect(x, y, cw, ch, 14, 14);
            gc.restore();

            gc.setStroke(c);
            gc.setLineWidth(act ? 4 : 2);
            gc.strokeRoundRect(x, y, cw, ch, 14, 14);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 17));
            gc.setFill(act ? MysticTheme.GOLD : MysticTheme.GOLD_D);
            gc.fillText("P" + (i + 1) + "  " + Roster.HEROES[cur[i]].name()
                    + (lock[i] ? "  - LOCKED" : ""), x + cw / 2, y + 29);
        }
        gc.setFont(Font.font("Serif", 14));
        gc.setFill(Color.web("#C9A24B", 0.85));
        gc.fillText("TAB: switch player", w / 2, y + ch + 22);
    }

    private void drawArrows(GraphicsContext gc, double cx, double cy, double pw) {
        double pulse = 0.5 + 0.5 * Math.sin(t * 5);
        gc.save();
        gc.setEffect(new DropShadow(10 + 8 * pulse, MysticTheme.SPARK));
        gc.setFill(MysticTheme.GOLD);
        double lx = cx - pw - 46, rx = cx + pw + 46;
        gc.fillPolygon(new double[] { lx + 14, lx + 14, lx - 14 },
                new double[] { cy - 28, cy + 28, cy }, 3);
        gc.fillPolygon(new double[] { rx - 14, rx - 14, rx + 14 },
                new double[] { cy - 28, cy + 28, cy }, 3);
        gc.restore();
    }

    private void drawDots(GraphicsContext gc, double cx, double h) {
        int n = Roster.HEROES.length;
        for (int i = 0; i < n; i++) {
            double x = cx + (i - (n - 1) / 2.0) * 44, y = h - 46;
            gc.setFill(Roster.HEROES[i].color());
            gc.fillOval(x - 9, y - 9, 18, 18);
            if (i == (flipping ? target : shown)) {
                gc.setStroke(MysticTheme.GOLD);
                gc.setLineWidth(3);
                gc.strokeOval(x - 13, y - 13, 26, 26);
            }
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
            if (cur[0] == i) {
                gc.setFill(P1);
                gc.fillText("1", x - 8, y - 18);
            }
            if (cur[1] == i) {
                gc.setFill(P2);
                gc.fillText("2", x + 8, y - 18);
            }
        }
    }

    private void drawNext(GraphicsContext gc, double w, double h) {
        if (!(lock[0] && lock[1]))
            return;
        double pulse = 0.5 + 0.5 * Math.sin(t * 6);
        double bx = w - 230, by = h - 84, bw = 190, bh = 54;
        gc.save();
        gc.setEffect(new DropShadow(14 + 14 * pulse, MysticTheme.SPARK));
        gc.setFill(Color.web("#7A0F12", 0.95));
        gc.fillRoundRect(bx, by, bw, bh, 16, 16);
        gc.restore();
        gc.setStroke(MysticTheme.GOLD);
        gc.setLineWidth(3);
        gc.strokeRoundRect(bx, by, bw, bh, 16, 16);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(30));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("NEXT  >", bx + bw / 2, by + 36);
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("press ENTER", bx + bw / 2, by + bh + 16);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}