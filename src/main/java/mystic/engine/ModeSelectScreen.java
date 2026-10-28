package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.ai.Difficulty;
import mystic.common.GameMode;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class ModeSelectScreen implements Screen {

    private record Card(String title, String emblem, String desc, GameMode mode, boolean enabled) {
    }

    private static final Card[] CARDS = {
            new Card("SOLO", "VS AI",
                    "Duel the machine.\nChoose how fierce it is.", GameMode.SOLO, true),
            new Card("2 PLAYERS", "1 v 1",
                    "Two sorcerers,\none keyboard.", GameMode.LOCAL_2P, true),
            new Card("ARCADE", "LADDER",
                    "Climb 10 trials.\nEvery foe is stronger.\nFace the boss.", GameMode.ARCADE, true),
            new Card("TRAINING", "MIRROR",
                    "Practise combos and\nsee the hitboxes.\nA dummy that fights back.", GameMode.TRAINING, true),
    };

    private static final Difficulty[] LEVELS = Difficulty.values();
    private static final String[] LEVEL_TEXT = {
            "Slow reactions. Good for learning.",
            "A fair fight.",
            "Fast, punishing, and relentless."
    };

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(60, Constants.WIDTH, Constants.HEIGHT);

    private double t;
    private int card;
    private int level = 1;
    private boolean pickingDifficulty;

    public ModeSelectScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        card = 0;
        level = 1;
        pickingDifficulty = false;
    }

    @Override
    public void update(double dt, InputManager input) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);

        boolean left = input.wasPressed(KeyCode.LEFT) || input.wasPressed(KeyCode.A);
        boolean right = input.wasPressed(KeyCode.RIGHT) || input.wasPressed(KeyCode.D);
        boolean up = input.wasPressed(KeyCode.UP) || input.wasPressed(KeyCode.W);
        boolean down = input.wasPressed(KeyCode.DOWN) || input.wasPressed(KeyCode.S);
        boolean ok = input.wasPressed(KeyCode.ENTER) || input.wasPressed(KeyCode.SPACE);
        boolean back = input.wasPressed(KeyCode.ESCAPE) || input.wasPressed(KeyCode.BACK_SPACE);

        if (pickingDifficulty) {
            if (up)
                level = Math.floorMod(level - 1, LEVELS.length);
            if (down)
                level = Math.floorMod(level + 1, LEVELS.length);
            if (back)
                pickingDifficulty = false;
            if (ok) {
                engine.setMode(GameMode.SOLO);
                engine.setDifficulty(LEVELS[level]);
                engine.changeState(GameState.CHARACTER_SELECT);
            }
            return;
        }

        if (left)
            moveCard(-1);
        if (right)
            moveCard(1);
        if (back)
            engine.changeState(GameState.MENU);

        if (ok) {
            Card c = CARDS[card];
            if (!c.enabled())
                return;
            if (c.mode() == GameMode.SOLO) {
                pickingDifficulty = true;
            } else if (c.mode() == GameMode.ARCADE) {
                engine.setMode(GameMode.ARCADE);
                engine.changeState(GameState.ARCADE);
            } else {
                engine.setMode(c.mode());
                engine.changeState(GameState.CHARACTER_SELECT);
            }
        }
    }

    private void moveCard(int dir) {
        for (int i = 0; i < CARDS.length; i++) {
            card = Math.floorMod(card + dir, CARDS.length);
            return; // disabled cards can be looked at, just not chosen
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        MysticTheme.drawBackground(gc, w, h);
        MysticTheme.drawMandala(gc, w / 2, h * 0.55, 420, t * 6, 0.35);
        MysticTheme.drawShards(gc, w, h, t);
        embers.draw(gc);

        MysticTheme.drawGlowText(gc, "CHOOSE YOUR PATH", w / 2, 110,
                MysticTheme.title(64), MysticTheme.GOLD, clamp(t / 0.5));

        double cw = 250, ch = 380, gap = 30;
        double total = CARDS.length * cw + (CARDS.length - 1) * gap;
        double x0 = (w - total) / 2, y0 = 190;

        for (int i = 0; i < CARDS.length; i++) {
            double local = clamp((t - 0.2 - i * 0.12) / 0.4);
            double ease = 1 - Math.pow(1 - local, 3);
            double x = x0 + i * (cw + gap);
            double y = y0 + (1 - ease) * 80;
            drawCard(gc, CARDS[i], x, y, cw, ch, i == card && !pickingDifficulty, ease);
        }

        gc.save();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", 16));
        gc.setFill(Color.web("#C9A24B", 0.85));
        gc.fillText(pickingDifficulty
                ? "W/S choose difficulty  -  Enter confirm  -  Esc back"
                : "A/D or Left/Right choose  -  Enter select  -  Esc back",
                w / 2, h - 30);
        gc.restore();

        if (pickingDifficulty)
            drawDifficulty(gc, w, h);
    }

    private void drawCard(GraphicsContext gc, Card c, double x, double y,
            double cw, double ch, boolean sel, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha * (c.enabled() ? 1 : 0.5));

        double lift = sel ? -14 : 0;
        y += lift;

        if (sel)
            gc.setEffect(new DropShadow(30, MysticTheme.SPARK));
        gc.setFill(Color.web(sel ? "#3A0B0D" : "#220708", 0.95));
        gc.fillRoundRect(x, y, cw, ch, 22, 22);
        gc.setEffect(null);

        gc.setStroke(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
        gc.setLineWidth(sel ? 4 : 2);
        gc.strokeRoundRect(x, y, cw, ch, 22, 22);

        double cx = x + cw / 2;
        MysticTheme.drawMandala(gc, cx, y + 110, 62, t * (sel ? 40 : 15), sel ? 1 : 0.6);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(30));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText(c.emblem(), cx, y + 120);

        gc.setFont(MysticTheme.title(40));
        gc.setFill(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
        gc.fillText(c.title(), cx, y + 235);

        gc.setFont(Font.font("Serif", 18));
        gc.setFill(Color.web("#FFE9A0", 0.85));
        double ty = y + 275;
        for (String line : c.desc().split("\n")) {
            gc.fillText(line, cx, ty);
            ty += 24;
        }
        gc.restore();
    }

    private void drawDifficulty(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.color(0, 0, 0, 0.78));
        gc.fillRect(0, 0, w, h);

        double pw = 640, ph = 420, px = (w - pw) / 2, py = (h - ph) / 2;
        gc.setFill(Color.web("#2A0809", 0.97));
        gc.fillRoundRect(px, py, pw, ph, 22, 22);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(3);
        gc.strokeRoundRect(px, py, pw, ph, 22, 22);

        MysticTheme.drawGlowText(gc, "CHOOSE DIFFICULTY", w / 2, py + 70,
                MysticTheme.title(44), MysticTheme.GOLD, 1);

        for (int i = 0; i < LEVELS.length; i++) {
            double y = py + 150 + i * 90;
            boolean sel = i == level;

            gc.save();
            gc.setTextAlign(TextAlignment.CENTER);
            if (sel) {
                gc.setFill(Color.web("#C21E1E", 0.30));
                gc.fillRoundRect(px + 50, y - 42, pw - 100, 70, 14, 14);
                gc.setStroke(MysticTheme.GOLD);
                gc.setLineWidth(2);
                gc.strokeRoundRect(px + 50, y - 42, pw - 100, 70, 14, 14);
                gc.setEffect(new DropShadow(16, MysticTheme.SPARK));
            }
            gc.setFont(MysticTheme.title(36));
            gc.setFill(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
            gc.fillText(LEVELS[i].name(), w / 2, y);
            gc.setEffect(null);
            gc.setFont(Font.font("Serif", 16));
            gc.setFill(Color.web("#FFE9A0", 0.8));
            gc.fillText(LEVEL_TEXT[i], w / 2, y + 20);
            gc.restore();
        }
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}