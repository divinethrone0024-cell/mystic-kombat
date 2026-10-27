package mystic.engine;

import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class MainMenuScreen implements Screen {

    private record Item(String label, boolean enabled) {
    }

    private static final Item[] ITEMS = {
            new Item("START GAME", true),
            new Item("HOW TO PLAY", true),
            new Item("SETTINGS", true),
            new Item("ARCADE  (soon)", false),
            new Item("TRAINING  (soon)", false),
            new Item("QUIT", true),
    };

    private static final String[] RULES = {
            "GOAL",
            "Drain your rival's health. First to win 2 rounds wins the duel.",
            "",
            "MYSTIC ENERGY (KI)",
            "Dealing and taking damage fills the Eye of Agamotto.",
            "When it is full, unleash your Ultimate Incantation.",
            "",
            "PLAYER 1",
            "A / D move   W jump   S crouch   J light   K heavy   F block",
            "L dash   I dodge   H launcher   O special   U ultimate",
            "",
            "PLAYER 2",
            "Arrows move/jump/crouch   1 light   2 heavy   4 block",
            "3 dash   6 dodge   5 launcher   7 special   0 ultimate",
            "",
            "Press ESC to return",
    };

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(70, Constants.WIDTH, Constants.HEIGHT);

    private double t;
    private int selected;
    private boolean showRules;

    public MainMenuScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        selected = 0;
        showRules = false;
    }

    @Override
    public void update(double dt, InputManager input) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);

        if (showRules) {
            if (input.wasPressed(KeyCode.ESCAPE)
                    || input.wasPressed(KeyCode.ENTER)
                    || input.wasPressed(KeyCode.BACK_SPACE)) {
                showRules = false;
            }
            return;
        }

        if (input.wasPressed(KeyCode.UP) || input.wasPressed(KeyCode.W))
            move(-1);
        if (input.wasPressed(KeyCode.DOWN) || input.wasPressed(KeyCode.S))
            move(1);

        if (input.wasPressed(KeyCode.ENTER) || input.wasPressed(KeyCode.SPACE)) {
            activate();
        }
        if (input.wasPressed(KeyCode.ESCAPE)) {
            Platform.exit();
        }
    }

    private void move(int dir) {
        int n = ITEMS.length;
        for (int i = 0; i < n; i++) {
            selected = Math.floorMod(selected + dir, n);
            if (ITEMS[selected].enabled())
                return;
        }
    }

    private void activate() {
        switch (selected) {
            case 0 -> engine.changeState(GameState.MODE_SELECT);
            case 1 -> showRules = true;
            case 2 -> engine.changeState(GameState.SETTINGS);
            case 5 -> Platform.exit();
            default -> {
            }
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        MysticTheme.drawBackground(gc, w, h);
        MysticTheme.drawMandala(gc, w * 0.72, h * 0.52, 330, t * 8, 0.55);
        MysticTheme.drawShards(gc, w, h, t);
        embers.draw(gc);

        gc.save();
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setEffect(new DropShadow(26, MysticTheme.SPARK));
        gc.setFont(MysticTheme.title(84));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("MYSTIC KOMBAT", 70, 130);
        gc.restore();

        double y0 = 230, gap = 68;
        for (int i = 0; i < ITEMS.length; i++) {
            double local = clamp((t - 0.25 - i * 0.1) / 0.4);
            double ease = 1 - Math.pow(1 - local, 3);
            drawItem(gc, ITEMS[i], 90 - (1 - ease) * 320, y0 + i * gap,
                    i == selected, ease);
        }

        gc.save();
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", 16));
        gc.setFill(Color.web("#C9A24B", 0.8));
        gc.fillText("W/S or Up/Down to move  -  Enter to choose  -  Esc to quit", 70, h - 30);
        gc.restore();

        if (showRules)
            drawRules(gc, w, h);
    }

    private void drawItem(GraphicsContext gc, Item item, double x, double y,
            boolean sel, double alpha) {
        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(MysticTheme.title(44));

        if (!item.enabled()) {
            gc.setFill(Color.web("#C9A24B", 0.35));
        } else if (sel) {
            double pulse = 0.5 + 0.5 * Math.sin(t * 6);
            gc.setEffect(new DropShadow(18 + 10 * pulse, MysticTheme.SPARK));
            gc.setFill(MysticTheme.GOLD);
            gc.fillText(">", x - 38, y);
            gc.setStroke(MysticTheme.GOLD_D);
            gc.setLineWidth(2);
            gc.strokeLine(x, y + 10, x + 340, y + 10);
        } else {
            gc.setFill(MysticTheme.GOLD_D);
        }
        gc.fillText(item.label(), x, y);
        gc.restore();
    }

    private void drawRules(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.color(0, 0, 0, 0.78));
        gc.fillRect(0, 0, w, h);

        double pw = 960, ph = 600, px = (w - pw) / 2, py = (h - ph) / 2;
        gc.setFill(Color.web("#2A0809", 0.96));
        gc.fillRoundRect(px, py, pw, ph, 20, 20);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(3);
        gc.strokeRoundRect(px, py, pw, ph, 20, 20);

        MysticTheme.drawGlowText(gc, "HOW TO PLAY", w / 2, py + 58,
                MysticTheme.title(46), MysticTheme.GOLD, 1);

        gc.save();
        gc.setTextAlign(TextAlignment.LEFT);
        double y = py + 105;
        for (String line : RULES) {
            boolean heading = !line.isEmpty() && line.equals(line.toUpperCase())
                    && !line.startsWith("Press");
            gc.setFont(heading ? MysticTheme.title(24) : Font.font("Serif", 19));
            gc.setFill(heading ? MysticTheme.SPARK : MysticTheme.GOLD);
            gc.fillText(line, px + 40, y);
            y += heading ? 28 : 26;
        }
        gc.restore();
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}