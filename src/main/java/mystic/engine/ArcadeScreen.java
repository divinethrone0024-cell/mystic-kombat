package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
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

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;
import mystic.common.Roster;
import mystic.common.Roster.Hero;

public class ArcadeScreen implements Screen {

    private enum Phase {
        COVER, OPENING, SELECT
    }

    private enum Sub {
        PICK, MAP
    }

    private static final double OPEN_TIME = 1.3;
    private static final double FLIP_TIME = 1.0;
    private static final String[] DIFF = { "EASY", "MEDIUM", "HARD" };

    private static final Color INK = Color.web("#3A0B0D");
    private static final Color PAGE_LINE = Color.web("#8A6A1E");
    private static final Color LEATHER_TOP = Color.web("#4A2A15");
    private static final Color LEATHER_BOT = Color.web("#120A05");
    private static final Color BACK_COVER = Color.web("#1E1008");
    private static final Color INSIDE_COVER = Color.web("#3B2210");

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(50, Constants.WIDTH, Constants.HEIGHT);

    private Phase phase = Phase.COVER;
    private Sub sub = Sub.PICK;
    private double t, openT, flipT;
    private boolean flipping, leaving;
    private boolean seenBook; // skip the intro when coming back from a fight
    private int pick, selected;
    private String msg = "";
    private double msgT;

    public ArcadeScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        openT = 0;
        flipT = 0;
        flipping = false;
        leaving = false;
        msg = "";
        msgT = 0;

        if (!engine.isChoiceUnlocked(engine.getPlayer1Choice())) {
            engine.setPlayer1Choice(0);
        }
        pick = engine.getPlayer1Choice();
        selected = Math.min(engine.getArcadeUnlocked(), GameEngine.ARCADE_TRIALS - 1);

        if (seenBook) {
            phase = Phase.SELECT;
            sub = Sub.MAP;
        } else {
            phase = Phase.COVER;
            sub = Sub.PICK;
        }
    }

    private int playableLimit() {
        return Math.min(GameEngine.ARCADE_TRIALS - 1, engine.getArcadeUnlocked());
    }

    private boolean confirm(InputManager in) {
        return in.wasPressed(KeyCode.ENTER) || in.wasPressed(KeyCode.SPACE);
    }

    private void flash(String s) {
        msg = s;
        msgT = 2.2;
    }

    private void leave() {
        leaving = true;
        seenBook = false;
        engine.changeState(GameState.MODE_SELECT);
    }

    // ------------------------------------------------------------ update

    @Override
    public void update(double dt, InputManager in) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);
        if (msgT > 0)
            msgT -= dt;
        if (leaving || t < 0.3)
            return;

        switch (phase) {
            case COVER -> {
                if (in.wasPressed(KeyCode.ESCAPE)) {
                    leave();
                } else if (in.anyKeyPressed()) {
                    phase = Phase.OPENING;
                    openT = 0;
                }
            }
            case OPENING -> {
                openT += dt;
                if (in.anyKeyPressed())
                    openT = OPEN_TIME;
                if (openT >= OPEN_TIME)
                    phase = Phase.SELECT;
            }
            case SELECT -> updateSelect(dt, in);
        }
    }

    private void updateSelect(double dt, InputManager in) {
        if (flipping) {
            flipT += dt / FLIP_TIME;
            if (flipT >= 1) {
                flipping = false;
                sub = Sub.MAP;
                seenBook = true;
            }
            return;
        }

        if (in.wasPressed(KeyCode.ESCAPE)) {
            leave();
            return;
        }
        if (in.wasPressed(KeyCode.U)) {
            engine.changeState(GameState.SORCERY_UPGRADATION);
            return;
        }

        if (sub == Sub.PICK)
            updatePick(in);
        else
            updateMap(in);
    }

    private void updatePick(InputManager in) {
        int n = Roster.HEROES.length;
        if (in.wasPressed(KeyCode.LEFT) || in.wasPressed(KeyCode.A))
            pick = Math.floorMod(pick - 1, n);
        if (in.wasPressed(KeyCode.RIGHT) || in.wasPressed(KeyCode.D))
            pick = Math.floorMod(pick + 1, n);
        if (in.wasPressed(KeyCode.UP) || in.wasPressed(KeyCode.W))
            pick = Math.floorMod(pick - 5, n);
        if (in.wasPressed(KeyCode.DOWN) || in.wasPressed(KeyCode.S))
            pick = Math.floorMod(pick + 5, n);

        if (confirm(in)) {
            if (engine.isChoiceUnlocked(pick)) {
                engine.setPlayer1Choice(pick);
                if (seenBook) {
                    sub = Sub.MAP;
                } else {
                    flipping = true;
                    flipT = 0;
                }
            } else {
                flash("Locked - unlock it in Sorcery Upgradation (U) for "
                        + engine.getProgression().getFighterPrice() + " XP");
            }
        }
    }

    private void updateMap(InputManager in) {
        if (in.wasPressed(KeyCode.LEFT) || in.wasPressed(KeyCode.A)) {
            selected = Math.max(0, selected - 1);
        }
        if (in.wasPressed(KeyCode.RIGHT) || in.wasPressed(KeyCode.D)) {
            selected = Math.min(GameEngine.ARCADE_TRIALS - 1, selected + 1);
        }
        if (in.wasPressed(KeyCode.Q))
            cycleChampion(-1);
        if (in.wasPressed(KeyCode.E))
            cycleChampion(1);

        if (in.wasPressed(KeyCode.C)) {
            pick = engine.getPlayer1Choice();
            sub = Sub.PICK;
            return;
        }

        if (confirm(in)) {
            if (selected <= playableLimit()) {
                engine.setArcadeTrial(selected);
                engine.configureArcadeTrial();
                engine.changeState(GameState.VERSUS);
            } else {
                flash("Locked - break the previous chain first");
            }
        }
    }

    private void cycleChampion(int dir) {
        int n = Roster.HEROES.length;
        int c = engine.getPlayer1Choice();
        for (int i = 0; i < n; i++) {
            c = Math.floorMod(c + dir, n);
            if (engine.isChoiceUnlocked(c)) {
                engine.setPlayer1Choice(c);
                return;
            }
        }
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
        double p = raw * raw * (3 - 2 * raw);

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
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", 15));
            gc.setFill(Color.web("#C9A24B", 0.9));
            gc.fillText(sub == Sub.PICK
                    ? "A/D/W/S move  -  ENTER choose  -  U Sorcery Upgradation  -  ESC back"
                    : "A/D select trial  -  ENTER begin  -  Q/E champion  -  C champion book  -  U Sorcery  -  ESC back",
                    cx, h - 16);

            if (msgT > 0) {
                gc.setFont(Font.font("Serif", 19));
                gc.setFill(Color.web("#FF6B5E"));
                gc.fillText(msg, cx, h - 40);
            }
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawCoverFace(GraphicsContext gc, double cx, double cy, double pw,
            double ph, double top, double bh) {
        gc.setFill(new LinearGradient(cx, top, cx, top + bh, false, CycleMethod.NO_CYCLE,
                new Stop(0, LEATHER_TOP), new Stop(1, LEATHER_BOT)));
        gc.fillRoundRect(cx, top, pw, bh, 18, 18);

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

        gc.setFill(MysticTheme.GOLD_D);
        double[][] studs = { { cx + 16, top + 16 }, { cx + pw - 16, top + 16 },
                { cx + 16, top + bh - 16 }, { cx + pw - 16, top + bh - 16 } };
        for (double[] s : studs)
            gc.fillOval(s[0] - 5, s[1] - 5, 10, 10);

        double mx = cx + pw / 2;
        MysticTheme.drawMandala(gc, mx, cy - ph * 0.12, pw * 0.26, t * 20, 0.95);

        MysticTheme.drawGlowText(gc, "ARCADE", mx, cy + ph * 0.22,
                MysticTheme.title(58), MysticTheme.GOLD, 1);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(2);
        gc.strokeLine(mx - pw * 0.32, cy + ph * 0.22 + 18, mx + pw * 0.32, cy + ph * 0.22 + 18);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("WONDERS OF THE MULTIVERSE", mx, cy + ph * 0.22 + 52);
    }

    // ------------------------------------------------------------ pages

    private void drawPages(GraphicsContext gc, double cx, double cy, double pw, double ph) {
        double lx = cx - pw + 10, rx = cx, pageW = pw - 10, py = cy - ph / 2;

        if (!flipping) {
            if (sub == Sub.PICK) {
                drawPickLeft(gc, lx, py, pageW, ph);
                drawPickRight(gc, rx, py, pageW, ph);
            } else {
                drawMapPage(gc, 0, lx, py, pageW, ph);
                drawMapPage(gc, 1, rx, py, pageW, ph);
                drawSpineChain(gc, lx, rx, py, pageW, ph);
            }
            return;
        }

        // Paper page turn: the roster page lifts off the spine, the trial page lands on
        // the left.
        double f = clamp(flipT);
        f = f * f * (3 - 2 * f);
        double sx = Math.cos(Math.PI * f);
        double sin = Math.sin(Math.PI * f);

        drawPickLeft(gc, lx, py, pageW, ph);
        drawMapPage(gc, 1, rx, py, pageW, ph);

        // Shadow cast on the page underneath.
        double edge = cx + pageW * sx;
        double sw = 80 * sin;
        if (sw > 1) {
            if (sx >= 0) {
                gc.setFill(new LinearGradient(edge, 0, edge + sw, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#000000", 0.38 * sin)),
                        new Stop(1, Color.web("#000000", 0))));
                gc.fillRect(edge, py, sw, ph);
            } else {
                gc.setFill(new LinearGradient(edge, 0, edge - sw, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#000000", 0.38 * sin)),
                        new Stop(1, Color.web("#000000", 0))));
                gc.fillRect(edge - sw, py, sw, ph);
            }
        }

        gc.save();
        gc.translate(cx, 0);
        gc.scale(Math.max(0.001, Math.abs(sx)), 1);
        gc.translate(-cx, 0);

        if (sx >= 0) {
            drawPickRight(gc, rx, py, pageW, ph);
            gc.setFill(new LinearGradient(rx, 0, rx + pageW, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#000000", 0)),
                    new Stop(1, Color.web("#3A2008", 0.35 * sin))));
            gc.fillRect(rx, py, pageW, ph);
        } else {
            // Back of the page, which is the left trial page, drawn mirrored onto the left
            // side.
            gc.translate(cx, 0);
            gc.scale(-1, 1);
            gc.translate(-cx, 0);
            drawMapPage(gc, 0, rx, py, pageW, ph);
            gc.setFill(new LinearGradient(rx, 0, rx + pageW, 0, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#000000", 0)),
                    new Stop(1, Color.web("#3A2008", 0.35 * sin))));
            gc.fillRect(rx, py, pageW, ph);
        }
        gc.restore();
    }

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

    // ------------------------------------------------------ champion pages

    private void drawPickLeft(GraphicsContext gc, double x, double y, double w, double h) {
        Hero hd = Roster.HEROES[pick];
        boolean unlocked = engine.isChoiceUnlocked(pick);
        parchment(gc, x, y, w, h);

        double pcx = x + w / 2, pcy = y + h * 0.40;
        Color c = hd.color();

        glow(gc, pcx, pcy, w * 0.36, c, 0.55);

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

        gc.save();
        if (!unlocked)
            gc.setGlobalAlpha(gc.getGlobalAlpha() * 0.35);
        drawArt(gc, pick, pcx, pcy, w * 0.62, c);
        gc.restore();
        if (!unlocked)
            drawPadlock(gc, pcx, pcy + 10, 3.0);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(34));
        gc.setFill(INK);
        gc.fillText(hd.name(), pcx, y + h * 0.74);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 19));
        gc.setFill(Color.web("#7A0F12"));
        gc.fillText(hd.title(), pcx, y + h * 0.74 + 28);

        gc.setFont(Font.font("Serif", FontWeight.BOLD, 17));
        if (unlocked) {
            gc.setFill(Color.web("#2E7D32"));
            gc.fillText(pick == engine.getPlayer1Choice() ? "YOUR CHAMPION" : "UNLOCKED",
                    pcx, y + h * 0.90);
        } else {
            gc.setFill(Color.web("#9A1015"));
            gc.fillText("LOCKED  -  " + engine.getProgression().getFighterPrice() + " XP",
                    pcx, y + h * 0.90);
        }
    }

    private void drawPickRight(GraphicsContext gc, double x, double y, double w, double h) {
        parchment(gc, x, y, w, h);
        double cx = x + w / 2;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(30));
        gc.setFill(INK);
        gc.fillText("CHOOSE YOUR CHAMPION", cx, y + 52);
        gc.setStroke(PAGE_LINE);
        gc.setLineWidth(2);
        gc.strokeLine(x + 40, y + 66, x + w - 40, y + 66);

        int n = Roster.HEROES.length;
        double gap = 10;
        double tw = (w - 60 - 4 * gap) / 5.0;
        double th = tw * 1.25;
        double gx = x + 30;
        double gy = y + 92;

        for (int i = 0; i < n; i++) {
            int col = i % 5, row = i / 5;
            double tx = gx + col * (tw + gap) + tw / 2;
            double ty = gy + row * (th + gap + 20) + th / 2;
            boolean u = engine.isChoiceUnlocked(i);
            portraitTile(gc, i, tx, ty, tw, th, !u);

            if (i == pick) {
                gc.save();
                gc.setEffect(new DropShadow(16 + 6 * Math.sin(t * 6), MysticTheme.SPARK));
                gc.setStroke(Color.web("#B8860B"));
                gc.setLineWidth(4);
                gc.strokeRoundRect(tx - tw / 2 - 3, ty - th / 2 - 3, tw + 6, th + 6, 12, 12);
                gc.restore();
            }
            if (i == engine.getPlayer1Choice()) {
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFill(Color.web("#2E7D32"));
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 12));
                gc.fillText("SELECTED", tx, ty + th / 2 + 15);
            }
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 17));
        gc.setFill(INK);
        gc.fillText("XP: " + engine.getProgression().getXp(), cx, y + h - 56);
        gc.setFont(Font.font("Serif", 15));
        gc.setFill(Color.web("#7A0F12"));
        gc.fillText("Locked fighters are unlocked with XP in Sorcery Upgradation (U)",
                cx, y + h - 30);
    }

    private void portraitTile(GraphicsContext gc, int idx, double cx, double cy,
            double tw, double th, boolean locked) {
        Color accent = Roster.HEROES[idx].color();
        double x = cx - tw / 2, y = cy - th / 2;

        gc.setFill(Color.web("#0A0203"));
        gc.fillRoundRect(x, y, tw, th, 12, 12);

        gc.save();
        gc.beginPath();
        gc.rect(x, y, tw, th);
        gc.closePath();
        gc.clip();
        gc.setFill(Color.color(accent.getRed() * 0.35, accent.getGreen() * 0.35,
                accent.getBlue() * 0.35));
        gc.fillRect(x, y, tw, th);
        glow(gc, cx, cy, tw * 0.8, accent, 0.35);
        if (!Portraits.draw(gc, idx, cx, cy + th * 0.04, tw * 1.15, th * 0.92, false)) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFill(accent);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, Math.max(14, tw * 0.5)));
            gc.fillText(String.valueOf(idx + 1), cx, cy + tw * 0.15);
        }
        if (locked) {
            gc.setFill(Color.web("#050710", 0.78));
            gc.fillRect(x, y, tw, th);
        }
        gc.restore();

        gc.setStroke(locked ? Color.web("#6B6560") : accent);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, tw, th, 12, 12);
        if (locked)
            drawPadlock(gc, cx, cy + 4, 1.2);
    }

    // --------------------------------------------------- trial map pages

    private double[] nodePos(double x, double y, double w, double h, int k) {
        return new double[] { x + w * (0.12 + 0.19 * k), y + h * (k % 2 == 0 ? 0.24 : 0.40) };
    }

    private void drawMapPage(GraphicsContext gc, int pg, double x, double y, double w, double h) {
        parchment(gc, x, y, w, h);
        double cx = x + w / 2;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(30));
        gc.setFill(INK);
        gc.fillText(pg == 0 ? "TRIALS 1 - 5" : "TRIALS 6 - 10", cx, y + 52);
        gc.setStroke(PAGE_LINE);
        gc.setLineWidth(2);
        gc.strokeLine(x + 40, y + 66, x + w - 40, y + 66);

        int cleared = engine.getArcadeUnlocked();
        int limit = playableLimit();

        for (int k = 0; k < 4; k++) {
            int i = pg * 5 + k;
            double[] a = nodePos(x, y, w, h, k);
            double[] b = nodePos(x, y, w, h, k + 1);
            drawChain(gc, a[0], a[1], b[0], b[1], i < cleared, i);
        }
        for (int k = 0; k < 5; k++) {
            int i = pg * 5 + k;
            drawEmblem(gc, i, nodePos(x, y, w, h, k), i > limit, i < cleared, i == selected);
        }

        if (pg == 0)
            drawChampPanel(gc, x, y, w, h);
        else
            drawTrialDetails(gc, x, y, w, h, limit);
    }

    /** The chain that links trial 5 (left page) to trial 6 (right page). */
    private void drawSpineChain(GraphicsContext gc, double lx, double rx, double py,
            double pageW, double ph) {
        double[] a = nodePos(lx, py, pageW, ph, 4);
        double[] b = nodePos(rx, py, pageW, ph, 0);
        drawChain(gc, a[0], a[1], b[0], b[1], 4 < engine.getArcadeUnlocked(), 4);
        // Redraw the two end emblems over the chain ends.
        int cleared = engine.getArcadeUnlocked();
        int limit = playableLimit();
        drawEmblem(gc, 4, a, 4 > limit, 4 < cleared, selected == 4);
        drawEmblem(gc, 5, b, 5 > limit, 5 < cleared, selected == 5);
    }

    private void drawChampPanel(GraphicsContext gc, double x, double y, double w, double h) {
        int c = engine.getPlayer1Choice();
        Hero hd = Roster.HEROES[c];
        double cx = x + w / 2;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        gc.setFill(Color.web("#7A0F12"));
        gc.fillText("YOUR CHAMPION", cx, y + h * 0.52);

        double cy = y + h * 0.68;
        glow(gc, cx, cy, w * 0.22, hd.color(), 0.5);
        drawArt(gc, c, cx, cy, w * 0.30, hd.color());

        gc.setFont(MysticTheme.title(26));
        gc.setFill(INK);
        gc.fillText(hd.name(), cx, y + h * 0.88);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        gc.setFill(Color.web("#2E7D32"));
        gc.fillText("XP: " + engine.getProgression().getXp(), cx, y + h * 0.94);
    }

    private void drawTrialDetails(GraphicsContext gc, double x, double y, double w, double h,
            int limit) {
        int opp = engine.getArcadeOpponent(selected);
        Hero oh = Roster.HEROES[opp];
        int d = selected < 3 ? 0 : selected < 7 ? 1 : 2;
        boolean playable = selected <= limit;
        double cx = x + w / 2;

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
        gc.setFill(Color.web("#7A0F12"));
        gc.fillText("TRIAL " + (selected + 1), cx, y + h * 0.52);
        gc.setFont(MysticTheme.title(28));
        gc.setFill(INK);
        gc.fillText(GameEngine.TRIAL_NAMES[selected], cx, y + h * 0.58);

        double pcy = y + h * 0.73;
        glow(gc, x + 85, pcy, 70, oh.color(), 0.5);
        gc.save();
        if (!playable)
            gc.setGlobalAlpha(gc.getGlobalAlpha() * 0.4);
        drawArt(gc, opp, x + 85, pcy, 110, oh.color());
        gc.restore();

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 17));
        gc.setFill(INK);
        gc.fillText("Opponent: " + oh.name(), x + 160, y + h * 0.69);
        gc.fillText("Difficulty: " + DIFF[d], x + 160, y + h * 0.74);
        gc.fillText("Reward: +" + ProgressionManager.ARCADE_WIN_XP + " XP", x + 160, y + h * 0.79);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 19));
        gc.setFill(playable ? Color.web("#7A0F12") : Color.web("#6B5B3A"));
        gc.fillText(playable ? "ENTER - BEGIN TRIAL" : "LOCKED - break the previous chain",
                cx, y + h * 0.92);
    }

    /**
     * Glowing rotating emblem: gold when cleared, red-orange when open, dim when
     * locked.
     */
    private void drawEmblem(GraphicsContext gc, int i, double[] p,
            boolean locked, boolean done, boolean sel) {
        double pulse = 0.5 + 0.5 * Math.sin(t * 4 + i);
        double r = 26 + (sel ? 4 * Math.sin(t * 6) : 0);

        Color core = locked ? Color.web("#8A7B5A")
                : done ? Color.web("#C9A24B") : Color.web("#E0541E");

        if (!locked)
            glow(gc, p[0], p[1], r * (2.2 + 0.4 * pulse), core, sel ? 0.75 : 0.5);

        gc.save();
        if (!locked)
            gc.setEffect(new DropShadow(sel ? 26 : 14, core));
        gc.setFill(locked ? Color.web("#4A4238") : Color.web("#1B0708"));
        gc.fillOval(p[0] - r, p[1] - r, r * 2, r * 2);
        gc.restore();

        if (!locked) {
            MysticTheme.drawMandala(gc, p[0], p[1], r * 1.05,
                    t * 20 * (i % 2 == 0 ? 1 : -1), sel ? 0.95 : 0.65);
        }

        gc.setStroke(sel ? Color.web("#FFE9A0") : core);
        gc.setLineWidth(sel ? 5 : 3);
        gc.strokeOval(p[0] - r, p[1] - r, r * 2, r * 2);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(locked ? Color.web("#B8AA88") : Color.web("#FFE9A0"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 21));
        gc.fillText(String.valueOf(i + 1), p[0], p[1] + 7);

        if (done) {
            gc.setStroke(Color.web("#2E7D32"));
            gc.setLineWidth(4);
            gc.strokeLine(p[0] + 14, p[1] - 26, p[0] + 20, p[1] - 18);
            gc.strokeLine(p[0] + 20, p[1] - 18, p[0] + 32, p[1] - 36);
        } else if (locked) {
            drawPadlock(gc, p[0], p[1] + r + 16, 1.0);
        }
    }

    /**
     * Animated chain: gold and shimmering when open, dark and swaying when locked.
     */
    private void drawChain(GraphicsContext gc, double x1, double y1, double x2, double y2,
            boolean open, int seed) {
        double dx = x2 - x1, dy = y2 - y1;
        double dist = Math.hypot(dx, dy);
        if (dist < 1)
            return;
        double ang = Math.toDegrees(Math.atan2(dy, dx));
        double nx = -dy / dist, ny = dx / dist;
        int links = Math.max(2, (int) (dist / 14));

        gc.setLineWidth(3);
        for (int j = 1; j < links; j++) {
            double f = j / (double) links;
            double sway = open ? 0 : Math.sin(t * 2.2 + j * 0.8 + seed) * 2.5;
            double x = x1 + dx * f + nx * sway;
            double y = y1 + dy * f + ny * sway;

            if (open) {
                double g = 0.5 + 0.5 * Math.sin(t * 5 - j * 0.7);
                gc.setStroke(Color.web("#B07A1A").interpolate(Color.web("#FFE9A0"), g));
            } else {
                gc.setStroke(Color.web("#4B4540"));
            }

            gc.save();
            gc.translate(x, y);
            gc.rotate(ang);
            if (j % 2 == 0)
                gc.strokeOval(-8, -5, 16, 10);
            else
                gc.strokeOval(-5, -3, 10, 6);
            gc.restore();
        }

        if (!open)
            drawPadlock(gc, x1 + dx / 2, y1 + dy / 2, 1.0);
    }

    private void drawPadlock(GraphicsContext gc, double x, double y, double s) {
        gc.save();
        gc.translate(x, y);
        gc.scale(s, s);
        gc.setStroke(Color.web("#3A3530"));
        gc.setLineWidth(2.5);
        gc.strokeArc(-5, -12, 10, 12, 0, 180, ArcType.OPEN);
        gc.setFill(Color.web("#6B6560"));
        gc.fillRoundRect(-8, -5, 16, 12, 3, 3);
        gc.setStroke(Color.web("#3A3530"));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(-8, -5, 16, 12, 3, 3);
        gc.restore();
    }

    // ------------------------------------------------------------ helpers

    private static void glow(GraphicsContext gc, double cx, double cy, double r,
            Color c, double a) {
        double alpha = clamp(a);
        gc.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), alpha)),
                new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}