package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;
import mystic.common.Roster;
import mystic.engine.ProgressionManager.Stat;

public class SorceryUpgradeScreen implements Screen {

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(50, Constants.WIDTH, Constants.HEIGHT);
    private final Stat[] stats = Stat.values();

    private double t;
    private int fighter;
    private int row;
    private String message = "";

    public SorceryUpgradeScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        row = 0;
        message = "";
        fighter = engine.getPlayer1Choice();
    }

    @Override
    public void update(double dt, InputManager input) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);
        if (t < 0.3)
            return;

        ProgressionManager pm = engine.getProgression();
        int n = Roster.HEROES.length;

        if (input.wasPressed(KeyCode.ESCAPE)) {
            engine.changeState(GameState.MENU);
            return;
        }
        if (input.wasPressed(KeyCode.LEFT) || input.wasPressed(KeyCode.A)) {
            fighter = Math.floorMod(fighter - 1, n);
            message = "";
        }
        if (input.wasPressed(KeyCode.RIGHT) || input.wasPressed(KeyCode.D)) {
            fighter = Math.floorMod(fighter + 1, n);
            message = "";
        }
        if (input.wasPressed(KeyCode.UP) || input.wasPressed(KeyCode.W)) {
            row = Math.floorMod(row - 1, stats.length);
        }
        if (input.wasPressed(KeyCode.DOWN) || input.wasPressed(KeyCode.S)) {
            row = Math.floorMod(row + 1, stats.length);
        }

        String id = GameEngine.fighterId(fighter);

        if (input.wasPressed(KeyCode.ENTER) || input.wasPressed(KeyCode.SPACE)) {
            if (!pm.isUnlocked(id)) {
                message = pm.unlockFighter(id)
                        ? Roster.HEROES[fighter].name() + " UNLOCKED!"
                        : "Not enough XP (" + pm.getFighterPrice() + " needed)";
            } else {
                Stat s = stats[row];
                if (pm.getLevel(id, s) >= ProgressionManager.MAX_LEVEL) {
                    message = s.label + " is at max level";
                } else if (pm.upgrade(id, s)) {
                    message = s.label + " upgraded!";
                } else {
                    message = "Not enough XP";
                }
            }
        }

        // Use this fighter in battle.
        if (input.wasPressed(KeyCode.E) && pm.isUnlocked(id)) {
            engine.setPlayer1Choice(fighter);
            message = Roster.HEROES[fighter].name() + " selected for battle";
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT, cx = w / 2;
        ProgressionManager pm = engine.getProgression();
        String id = GameEngine.fighterId(fighter);
        boolean unlocked = pm.isUnlocked(id);

        MysticTheme.drawBackground(gc, w, h);
        embers.draw(gc);
        gc.setTextAlign(TextAlignment.CENTER);

        MysticTheme.drawGlowText(gc, "SORCERY UPGRADATION", cx, h * 0.12,
                MysticTheme.title(44), MysticTheme.GOLD, 1);

        gc.setFill(Color.LIGHTGREEN);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
        gc.fillText("XP: " + pm.getXp(), cx, h * 0.19);

        Roster.Hero hero = Roster.HEROES[fighter];
        gc.setFill(hero.color());
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 34));
        gc.fillText("<  " + hero.name() + "  >", cx, h * 0.29);

        gc.setFill(MysticTheme.GOLD_D);
        gc.setFont(Font.font("Serif", 16));
        gc.fillText(hero.title(), cx, h * 0.33);

        if (!unlocked) {
            gc.setFill(Color.web("#FF8A8A"));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 24));
            gc.fillText("LOCKED  -  " + pm.getFighterPrice() + " XP", cx, h * 0.48);
            gc.setFill(Color.web("#D8C7A0"));
            gc.setFont(Font.font("Serif", 16));
            gc.fillText("Press ENTER to unlock", cx, h * 0.53);
        } else {
            for (int i = 0; i < stats.length; i++) {
                Stat s = stats[i];
                double y = h * 0.43 + i * 70;
                int lvl = pm.getLevel(id, s);
                boolean sel = i == row;

                gc.setFill(sel ? Color.web("#4A0E12", 0.9) : Color.web("#1B0708", 0.8));
                gc.fillRoundRect(cx - 330, y - 30, 660, 56, 14, 14);
                gc.setStroke(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
                gc.setLineWidth(sel ? 3 : 1.5);
                gc.strokeRoundRect(cx - 330, y - 30, 660, 56, 14, 14);

                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFill(MysticTheme.GOLD);
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
                gc.fillText(s.label.toUpperCase(), cx - 310, y + 6);

                // Level pips.
                for (int p = 0; p < ProgressionManager.MAX_LEVEL; p++) {
                    gc.setFill(p < lvl ? MysticTheme.GOLD : Color.web("#2D1114"));
                    gc.fillOval(cx - 190 + p * 22, y - 8, 14, 14);
                }

                gc.setFill(Color.web("#D8C7A0"));
                gc.setFont(Font.font("Serif", 15));
                String cost = lvl >= ProgressionManager.MAX_LEVEL
                        ? "MAX"
                        : pm.getUpgradeCost(id, s) + " XP";
                gc.fillText("+" + (int) Math.round((pm.getMultiplier(id, s) - 1) * 100)
                        + "%    next: " + cost, cx + 50, y + 6);
                gc.setTextAlign(TextAlignment.CENTER);
            }
        }

        gc.setFill(Color.web("#FFE9A0"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.fillText(message, cx, h * 0.80);

        gc.setFill(Color.web("#D8C7A0"));
        gc.setFont(Font.font("Serif", 14));
        gc.fillText("A/D: Fighter    W/S: Stat    ENTER: Buy    E: Use in battle    ESC: Menu",
                cx, h * 0.93);
        gc.setTextAlign(TextAlignment.LEFT);
    }
}