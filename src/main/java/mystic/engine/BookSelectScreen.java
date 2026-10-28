package mystic.engine;

import java.util.HashMap;
import java.util.Map;

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
import javafx.scene.text.Text;
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
    private static final double EMERGE_TIME = 0.9;

    private static final Color P1 = Color.web("#FF4B3E");
    private static final Color P2 = Color.web("#1FB5B0");
    private static final Color INK = Color.web("#3A0B0D");
    private static final Color PAGE_LINE = Color.web("#8A6A1E");

    // Brown / black leather palette
    private static final Color LEATHER_TOP = Color.web("#4A2A15");
    private static final Color LEATHER_BOT = Color.web("#120A05");
    private static final Color BACK_COVER = Color.web("#1E1008");
    private static final Color INSIDE_COVER = Color.web("#3B2210");
    private static final Color CARD_TOP = Color.web("#3A2110");
    private static final Color CARD_BOT = Color.web("#150C06");

    /**
     * Cache for fitted fonts so the overview does not rebuild Text nodes every
     * frame.
     */
    private static final Map<String, Font> FONT_CACHE = new HashMap<>();

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

    // "All fighters" overview
    private boolean overview;
    private double ov; // 0 = closed, 1 = fully fanned out

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
        overview = false;
        ov = 0;
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

        // animate the overview in / out
        if (overview) {
            ov = Math.min(1, ov + dt / EMERGE_TIME);
        } else {
            ov = Math.max(0, ov - dt / (EMERGE_TIME * 0.6));
        }

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

        if (in.wasPressed(KeyCode.V)) {
            overview = !overview;
            return;
        }

        if (in.wasPressed(KeyCode.ESCAPE)) {
            if (overview) {
                overview = false;
                return;
            }
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
                focus(cur[who]);
            }
        }

        if (in.wasPressed(KeyCode.TAB)) {
            who = 1 - who;
            focus(cur[who]);
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
                        focus(cur[who]);
                    }
                } else {
                    flash("This champion is sealed - not summoned yet");
                }
            } else {
                who = 1 - who; // this one is locked, help the other
                focus(cur[who]);
            }
        }
    }

    /** Shows a hero: page flip in the book, instant swap in the overview. */
    private void focus(int idx) {
        if (overview) {
            shown = target = idx;
            anim = 0;
        } else {
            startFlip(idx);
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
        gc.setFill(BACK_COVER);
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
            gc.setFill(INSIDE_COVER);
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
            drawOverview(gc, w, h);
            drawTabs(gc, w);
            if (ov < 0.05) {
                drawArrows(gc, cx, cy, pw);
                drawDots(gc, cx, h);
            }
            drawAllButton(gc, h);
            drawNext(gc, w, h);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", 15));
            gc.setFill(Color.web("#C9A24B", 0.9));
            gc.fillText(
                    "A/D change  -  TAB switch player  -  ENTER lock  -  BACKSPACE unlock  -  V all fighters  -  ESC back",
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
                new Stop(0, LEATHER_TOP), new Stop(1, LEATHER_BOT)));
        gc.fillRoundRect(cx, top, pw, bh, 18, 18);

        // faint leather grain
        gc.setStroke(Color.web("#000000", 0.14));
        gc.setLineWidth(1);
        for (double gy = top + 14; gy < top + bh - 10; gy += 9) {
            gc.strokeLine(cx + 14, gy, cx + pw - 14, gy);
        }

        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(4);
        gc.strokeRoundRect(cx + 10, top + 10, pw - 20, bh - 20, 12, 12);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(cx + 22, top + 22, pw - 44, bh - 44, 8, 8);

        // gold corner studs
        gc.setFill(MysticTheme.GOLD_D);
        double[][] studs = { { cx + 16, top + 16 }, { cx + pw - 16, top + 16 },
                { cx + 16, top + bh - 16 }, { cx + pw - 16, top + bh - 16 } };
        for (double[] s : studs)
            gc.fillOval(s[0] - 5, s[1] - 5, 10, 10);

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

    /** Draws the portrait image (or a letter fallback) centred on (cx, cy). */
    private void drawArt(GraphicsContext gc, int idx, double cx, double cy, double box, Color c) {
        if (!Portraits.draw(gc, idx, cx, cy, box, box, false)) {
            String letter = Roster.HEROES[idx].name().substring(0, 1);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(MysticTheme.title(box * 0.42));
            gc.setFill(c);
            gc.fillText(letter, cx, cy + box * 0.15);
            gc.setStroke(INK);
            gc.setLineWidth(2);
            gc.strokeText(letter, cx, cy + box * 0.15);
        }
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

        drawArt(gc, idx, pcx, pcy, w * 0.62, c);

        gc.setTextAlign(TextAlignment.CENTER);
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

    // ------------------------------------------------------------ overview

    /** All fighters emerge from the book's centre and line up as cards. */
    private void drawOverview(GraphicsContext gc, double w, double h) {
        if (ov <= 0.001)
            return;

        int n = Roster.HEROES.length;

        // dim the book behind the cards
        gc.setFill(Color.web("#0B0603", 0.92 * ov));
        gc.fillRect(0, 0, w, h);

        gc.save();
        gc.setGlobalAlpha(ov);
        gc.setTextAlign(TextAlignment.CENTER);
        MysticTheme.drawGlowText(gc, "ALL CHAMPIONS", w / 2, 112,
                MysticTheme.title(34), MysticTheme.GOLD, 1);
        gc.restore();

        // layout: one row if cards stay wide enough, otherwise two rows
        double margin = 22, gap = 8;
        int perRow = n;
        double cardW = (w - 2 * margin - (perRow - 1) * gap) / perRow;
        if (cardW < 100) {
            perRow = (n + 1) / 2;
            cardW = (w - 2 * margin - (perRow - 1) * gap) / perRow;
        }
        int rows = (n + perRow - 1) / perRow;

        double areaTop = 130, areaBottom = h - 130;
        double avail = areaBottom - areaTop;
        double cardH = rows == 1 ? Math.min(avail, 440) : (avail - gap) / 2;
        double blockH = rows * cardH + (rows - 1) * gap;
        double y0 = areaTop + (avail - blockH) / 2;

        // draw the active card last so it sits on top
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < n; i++) {
                boolean active = cur[who] == i;
                if ((pass == 1) != active)
                    continue;

                int row = i / perRow, col = i % perRow;
                int inRow = Math.min(perRow, n - row * perRow);
                double rowW = inRow * cardW + (inRow - 1) * gap;
                double sx = (w - rowW) / 2 + col * (cardW + gap);
                double sy = y0 + row * (cardH + gap);
                double scx = sx + cardW / 2, scy = sy + cardH / 2;

                // centre cards emerge first, outer ones follow
                double d = Math.abs(i - (n - 1) / 2.0) / Math.max(1, (n - 1) / 2.0);
                double local = clamp((ov - d * 0.45) / 0.55);
                double e = local * local * (3 - 2 * local);
                if (e <= 0.001)
                    continue;

                double px = w / 2 + (scx - w / 2) * e;
                double py = h / 2 + (scy - h / 2) * e;
                double lift = active ? -10 * e : 0;

                gc.save();
                gc.setGlobalAlpha(e);
                gc.translate(px, py + lift);
                gc.rotate((1 - e) * (i - (n - 1) / 2.0) * 7);
                double sc = 0.2 + 0.8 * e;
                gc.scale(sc, sc);
                drawCard(gc, i, -cardW / 2, -cardH / 2, cardW, cardH, active);
                gc.restore();
            }
        }
    }

    private void drawCard(GraphicsContext gc, int idx, double x, double y,
            double cw, double ch, boolean active) {
        Hero hd = Roster.HEROES[idx];
        Color c = hd.color();
        boolean sealed = !hd.playable();
        double fs = Math.max(0.7, Math.min(1.2, cw / 116.0));
        double cx = x + cw / 2;

        // body
        gc.save();
        if (active)
            gc.setEffect(new DropShadow(22, cur[who] == idx ? (who == 0 ? P1 : P2) : c));
        gc.setFill(new LinearGradient(x, y, x, y + ch, false, CycleMethod.NO_CYCLE,
                new Stop(0, CARD_TOP), new Stop(1, CARD_BOT)));
        gc.fillRoundRect(x, y, cw, ch, 14, 14);
        gc.restore();

        // border: player colours if picked, gold otherwise
        Color border = MysticTheme.GOLD_D;
        if (cur[0] == idx && cur[1] == idx)
            border = MysticTheme.GOLD;
        else if (cur[0] == idx)
            border = P1;
        else if (cur[1] == idx)
            border = P2;
        gc.setStroke(border);
        gc.setLineWidth(active ? 4 : 2);
        gc.strokeRoundRect(x, y, cw, ch, 14, 14);

        // hero colour strip
        gc.setFill(c);
        gc.fillRoundRect(x + 6, y + 6, cw - 12, 4, 4, 4);

        // portrait
        double box = Math.min(cw * 0.82, ch * 0.34);
        double pcy = y + 18 + box / 2;
        gc.setFill(new RadialGradient(0, 0, cx, pcy, box * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.5)),
                new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
        gc.fillOval(cx - box * 0.7, pcy - box * 0.7, box * 1.4, box * 1.4);

        gc.save();
        if (sealed)
            gc.setGlobalAlpha(gc.getGlobalAlpha() * 0.45);
        drawArt(gc, idx, cx, pcy, box, c);
        gc.restore();

        // name + title
        double ty = y + 18 + box + 22 * fs;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(fit(hd.name(), FontWeight.BOLD, 15 * fs, cw - 12));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText(hd.name(), cx, ty);
        gc.setFont(fit(hd.title(), FontWeight.NORMAL, 11 * fs, cw - 12));
        gc.setFill(Color.web("#C9A24B", 0.9));
        gc.fillText(hd.title(), cx, ty + 15 * fs);

        // stats
        double sy = ty + 38 * fs;
        double step = 19 * fs;
        miniStat(gc, "HP", hd.health(), x + 10, sy, cw - 20, c, fs);
        miniStat(gc, "SPD", hd.speed(), x + 10, sy + step, cw - 20, c, fs);
        miniStat(gc, "DMG", hd.damage(), x + 10, sy + 2 * step, cw - 20, c, fs);

        // special + ultimate (strip the "Special:" / "Ultimate:" prefixes)
        double ay = sy + 3 * step + 10 * fs;
        String[] pw = hd.powers();
        if (pw.length > 0)
            abilityLine(gc, "SP", pw[0], cx, x, ay, cw, fs, c);
        if (pw.length > 1)
            abilityLine(gc, "ULT", pw[1], cx, x, ay + 30 * fs, cw, fs, c);

        // bottom badges
        double by = y + ch - 12;
        if (sealed) {
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 12 * fs));
            gc.setFill(Color.web("#FF6B5E"));
            gc.fillText("SEALED", cx, by);
        } else {
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 12 * fs));
            boolean b1 = cur[0] == idx, b2 = cur[1] == idx;
            if (b1 && b2) {
                gc.setFill(P1);
                gc.fillText("P1" + (lock[0] ? "*" : ""), cx - 18 * fs, by);
                gc.setFill(P2);
                gc.fillText("P2" + (lock[1] ? "*" : ""), cx + 18 * fs, by);
            } else if (b1) {
                gc.setFill(P1);
                gc.fillText("P1" + (lock[0] ? " LOCKED" : ""), cx, by);
            } else if (b2) {
                gc.setFill(P2);
                gc.fillText("P2" + (lock[1] ? " LOCKED" : ""), cx, by);
            }
        }
    }

    private void miniStat(GraphicsContext gc, String label, int v, double x, double y,
            double w, Color c, double fs) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 10 * fs));
        gc.setFill(Color.web("#C9A24B"));
        gc.fillText(label, x, y);

        double bx = x + 28 * fs, bw = w - 28 * fs - 16 * fs, bh = 7;
        gc.setFill(Color.web("#000000", 0.55));
        gc.fillRoundRect(bx, y - 8, bw, bh, 6, 6);
        gc.setFill(c);
        gc.fillRoundRect(bx, y - 8, bw * Math.min(1, v / 10.0), bh, 6, 6);

        gc.setFill(Color.WHITE);
        gc.fillText(String.valueOf(v), bx + bw + 4, y);
    }

    private void abilityLine(GraphicsContext gc, String tag, String line, double cx,
            double x, double y, double cw, double fs, Color c) {
        String text = line.contains(":") ? line.substring(line.indexOf(':') + 1).trim() : line;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 9 * fs));
        gc.setFill(c);
        gc.fillText(tag, cx, y);
        gc.setFont(fit(text, FontWeight.NORMAL, 11 * fs, cw - 12));
        gc.setFill(Color.web("#E8D8B0"));
        gc.fillText(text, cx, y + 13 * fs);
    }

    /** Shrinks the font until the text fits in maxW. Results are cached. */
    private static Font fit(String s, FontWeight weight, double size, double maxW) {
        String key = s + '|' + weight + '|' + Math.round(size * 2) + '|' + Math.round(maxW);
        Font cached = FONT_CACHE.get(key);
        if (cached != null)
            return cached;

        double sz = size;
        Font f = Font.font("Serif", weight, sz);
        Text probe = new Text(s);
        while (sz > 7) {
            probe.setFont(f);
            if (probe.getLayoutBounds().getWidth() <= maxW)
                break;
            sz -= 0.5;
            f = Font.font("Serif", weight, sz);
        }
        FONT_CACHE.put(key, f);
        return f;
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
            gc.setFill(Color.web("#1A0E07", 0.95));
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

    /** Button (bottom-left) that fans every fighter out as cards, or closes it. */
    private void drawAllButton(GraphicsContext gc, double h) {
        double bx = 40, by = h - 84, bw = 210, bh = 54;
        double pulse = 0.5 + 0.5 * Math.sin(t * 4);
        gc.save();
        gc.setEffect(new DropShadow(overview ? 16 : 8 + 8 * pulse, MysticTheme.SPARK));
        gc.setFill(Color.web("#4A2A14", 0.97));
        gc.fillRoundRect(bx, by, bw, bh, 16, 16);
        gc.restore();
        gc.setStroke(MysticTheme.GOLD);
        gc.setLineWidth(3);
        gc.strokeRoundRect(bx, by, bw, bh, 16, 16);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(overview ? 24 : 26));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText(overview ? "BACK TO BOOK" : "ALL FIGHTERS", bx + bw / 2, by + 36);
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("press V", bx + bw / 2, by + bh + 16);
    }

    private void drawNext(GraphicsContext gc, double w, double h) {
        if (!(lock[0] && lock[1]))
            return;
        double pulse = 0.5 + 0.5 * Math.sin(t * 6);
        double bx = w - 230, by = h - 84, bw = 190, bh = 54;
        gc.save();
        gc.setEffect(new DropShadow(14 + 14 * pulse, MysticTheme.SPARK));
        gc.setFill(Color.web("#5A3416", 0.97));
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