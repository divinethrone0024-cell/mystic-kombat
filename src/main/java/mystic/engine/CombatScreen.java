
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.combat.CombatSystem;
import mystic.combat.MatchManager;
import mystic.common.Constants;
import mystic.fighters.Fighter;
import mystic.fighters.MysticMage;
import mystic.fighters.ShadowVigilante;

/** Combat screen with rounds, timer, health and Ki. */
public class CombatScreen implements Screen {

    private final GameEngine engine;
    private final CombatSystem combat = new CombatSystem();
    private final MatchManager match = new MatchManager();

    private Fighter p1;
    private Fighter p2;

    private boolean debug = false;
    private double transitionDelay = 0;

    public CombatScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        match.resetMatch();
        transitionDelay = 0;
        createFighters();
    }

    private void createFighters() {
        p1 = new MysticMage(1, 300);
        p2 = new ShadowVigilante(2, Constants.WIDTH - 300);
    }

    @Override
    public void update(double dt, InputManager input) {

        // The match has finished.
        if (match.isMatchOver()) {
            if (input.wasPressed(KeyCode.ENTER)) {
                match.resetMatch();
                createFighters();
                transitionDelay = 0;
            }
            return;
        }

        // Wait before starting the next round.
        if (match.isRoundOver()) {
            transitionDelay -= dt;

            if (transitionDelay <= 0 && !match.isMatchOver()) {
                match.nextRound();
                createFighters();
            }
            return;
        }

        // Update fighters and combat.
        p1.update(dt, input, p2);
        p2.update(dt, input, p1);

        combat.resolve(p1, p2);
        combat.resolve(p2, p1);

        keepApart();

        if (input.wasPressed(KeyCode.F1)) {
            debug = !debug;
        }

        // End the round after a knockout.
        if (p1.isKO() || p2.isKO()) {
            int winner;

            if (p1.isKO() && p2.isKO()) {
                winner = 0;
            } else {
                winner = p1.isKO() ? 2 : 1;
            }

            finishRound(winner);
            return;
        }

        // Update the countdown.
        match.update(dt);

        // At timeout, the fighter with more health wins.
        if (match.getTimeRemaining() <= 0) {
            int comparison = Integer.compare(
                    p1.getHealth(),
                    p2.getHealth());

            int winner;

            if (comparison > 0) {
                winner = 1;
            } else if (comparison < 0) {
                winner = 2;
            } else {
                winner = 0;
            }

            finishRound(winner);
        }
    }

    private void finishRound(int winner) {
        match.finishRound(winner);
        transitionDelay = 2.0;
    }

    /** Prevent fighters from overlapping horizontally. */
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

        // Arena background.
        gc.setFill(Color.rgb(12, 15, 32));
        gc.fillRect(0, 0, Constants.WIDTH, Constants.GROUND_Y + 100);

        gc.setStroke(Color.GRAY);
        gc.strokeLine(
                0, Constants.GROUND_Y,
                Constants.WIDTH, Constants.GROUND_Y);

        p1.render(gc, debug);
        p2.render(gc, debug);

        // Health bars.
        drawBar(
                gc, 40, 75, 420, 22,
                p1.getHealth() / (double) p1.getMaxHealth(),
                Color.LIMEGREEN);

        drawBar(
                gc, Constants.WIDTH - 460, 75, 420, 22,
                p2.getHealth() / (double) p2.getMaxHealth(),
                Color.ORANGE);

        // Ki bars.
        drawBar(
                gc, 40, 108, 300, 14,
                p1.getKi() / p1.getMaxKi(),
                Color.DEEPSKYBLUE);

        drawBar(
                gc, Constants.WIDTH - 340, 108, 300, 14,
                p2.getKi() / p2.getMaxKi(),
                Color.MAGENTA);

        // Round, timer and score.
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 22));

        gc.fillText(
                "ROUND " + match.getRoundNumber(),
                Constants.WIDTH / 2.0, 45);

        gc.setFont(Font.font("Arial", 30));
        gc.fillText(
                String.format("%.1f", match.getTimeRemaining()),
                Constants.WIDTH / 2.0, 85);

        gc.setFont(Font.font("Arial", 16));
        gc.fillText(
                "P1: " + match.getPlayer1Wins()
                        + " - " + match.getPlayer2Wins() + " :P2",
                Constants.WIDTH / 2.0, 125);

        // Ki values and ultimate readiness.
        gc.setFont(Font.font("Arial", 13));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(
                p1.isUltimateReady() ? "ULTIMATE READY!" : "KI " + (int) p1.getKi() + "/100",
                40, 140);

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(
                p2.isUltimateReady() ? "ULTIMATE READY!" : "KI " + (int) p2.getKi() + "/100",
                Constants.WIDTH - 40, 140);

        // Controls.
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 15));
        gc.fillText(
                "P1: A/D move, W jump, S crouch, F light, G heavy"
                        + "     P2: Arrows, K light, L heavy"
                        + "     F1: debug",
                Constants.WIDTH / 2.0, Constants.GROUND_Y + 40);

        // Round result.
        if (match.isRoundOver() && !match.isMatchOver()) {
            String message = match.getRoundWinner() == 0
                    ? "ROUND DRAW!"
                    : "PLAYER " + match.getRoundWinner() + " WINS ROUND!";

            drawOverlay(gc, message);
        }

        // Match result.
        if (match.isMatchOver()) {
            drawOverlay(gc, match.getMatchWinner());

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 20));
            gc.setTextAlign(TextAlignment.CENTER);

            gc.fillText(
                    "Press ENTER to play again",
                    Constants.WIDTH / 2.0,
                    Constants.GROUND_Y / 2.0 + 100);
        }
    }

    private void drawBar(
            GraphicsContext gc,
            double x, double y,
            double width, double height,
            double ratio, Color fillColor) {

        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(x, y, width, height);

        gc.setFill(fillColor);
        gc.fillRect(
                x, y,
                width * Math.max(0, Math.min(1, ratio)),
                height);

        gc.setStroke(Color.WHITE);
        gc.strokeRect(x, y, width, height);
    }

    private void drawOverlay(GraphicsContext gc, String message) {
        gc.setFill(Color.rgb(0, 0, 0, 0.78));

        // Use the ground position to avoid requiring Constants.HEIGHT.
        gc.fillRect(
                0, 0,
                Constants.WIDTH, Constants.GROUND_Y + 100);

        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 42));
        gc.setTextAlign(TextAlignment.CENTER);

        gc.fillText(
                message,
                Constants.WIDTH / 2.0,
                Constants.GROUND_Y / 2.0);
    }
}
