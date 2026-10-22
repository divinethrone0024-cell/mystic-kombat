package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.common.Constants;
import mystic.common.GameState;
import mystic.fighters.Fighter;
import mystic.fighters.MysticMage;
import mystic.fighters.ShadowVigilante;

/** The fight. For now: two rectangle fighters that can move. */
public class CombatScreen implements Screen {
    private final GameEngine engine;
    private Fighter p1;
    private Fighter p2;

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
        keepApart();

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

        p1.render(gc);
        p2.render(gc);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText("P1: A/D move, W jump, S crouch     P2: arrows move, Up jump, Down crouch     ENTER: result",
                Constants.WIDTH / 2.0, 40);
    }
}