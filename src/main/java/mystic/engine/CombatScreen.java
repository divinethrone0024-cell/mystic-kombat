package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.combat.CombatSystem;
import mystic.common.Constants;
import mystic.common.GameState;
import mystic.fighters.Fighter;
import mystic.fighters.MysticMage;
import mystic.fighters.ShadowVigilante;

/** The fight. Rectangles for now: real sprites, HUD and rounds come later. */
public class CombatScreen implements Screen {
    private final GameEngine engine;
    private final CombatSystem combat = new CombatSystem();
    private Fighter p1;
    private Fighter p2;
    private boolean debug = false;

    public CombatScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        p1 = new MysticMage(1, 300);
        p2 = new ShadowVigilante(2, Constants.WIDTH - 300);
    }

    @Override
    public void update(double dt, InputManager input) {
        p1.update(dt, input, p2);
        p2.update(dt, input, p1);

        combat.resolve(p1, p2);
        combat.resolve(p2, p1);
        keepApart();

        if (input.wasPressed(KeyCode.F1)) {
            debug = !debug; // show or hide the hurtboxes
        }
        if (input.wasPressed(KeyCode.R)) {
            p1.resetForRound(); // quick reset for testing
            p2.resetForRound();
        }
        if (input.wasPressed(KeyCode.ENTER)) {
            engine.changeState(GameState.RESULT);
        }
    }

    /** Stop the two fighters from standing inside each other. */
    private void keepApart() {
        double dx = p2.getX() - p1.getX();
        double overlap = Fighter.WIDTH - Math.abs(dx);
        boolean sameHeight = Math.abs(p1.getY() - p2.getY()) < Fighter.STAND_HEIGHT;
        if (overlap > 0 && sameHeight) {
            double push = (overlap / 2) * (dx >= 0 ? 1 : -1);
            p1.shiftX(-push);
            p2.shiftX(push);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setStroke(Color.GRAY);
        gc.strokeLine(0, Constants.GROUND_Y, Constants.WIDTH, Constants.GROUND_Y);

        p1.render(gc, debug);
        p2.render(gc, debug);

        // temporary health bars (Harsh builds the real HUD later)
        drawBar(gc, 40, 60, 500, 24, p1.getHealth() / (double) p1.getMaxHealth(), p1.getName());
        drawBar(gc, Constants.WIDTH - 540, 60, 500, 24, p2.getHealth() / (double) p2.getMaxHealth(), p2.getName());

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("Arial", 15));
        gc.fillText("P1: A/D move, W jump, S crouch, F light, G heavy      "
                + "P2: arrows, K light, L heavy      F1: boxes   R: reset   ENTER: result",
                Constants.WIDTH / 2.0, 30);

        if (p1.isKO() || p2.isKO()) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 90));
            gc.fillText("K.O.!", Constants.WIDTH / 2.0, 300);
        }
    }

    private void drawBar(GraphicsContext gc, double x, double y, double w, double h,
            double ratio, String label) {
        gc.setFill(Color.DARKRED);
        gc.fillRect(x, y, w, h);
        gc.setFill(Color.LIMEGREEN);
        gc.fillRect(x, y, w * Math.max(0, ratio), h);
        gc.setStroke(Color.WHITE);
        gc.strokeRect(x, y, w, h);
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText(label, x, y - 6);
    }
}