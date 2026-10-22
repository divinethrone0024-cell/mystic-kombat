
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

/**
 * Main two-player combat screen.
 * Includes health, Mystic Energy, ultimates, round timer,
 * and a best-of-three match system.
 */
public class CombatScreen implements Screen {

    private static final double ROUND_DURATION = 99.0;
    private static final int ROUNDS_TO_WIN = 2;

    private static final double P1_START_X = 300;
    private static final double P2_START_X = Constants.WIDTH - 300;

    private final GameEngine engine;
    private final CombatSystem combat = new CombatSystem();

    private Fighter p1;
    private Fighter p2;

    private boolean debug = false;
    private boolean roundOver = false;
    private boolean matchOver = false;

    private double timeRemaining = ROUND_DURATION;

    private int p1RoundsWon = 0;
    private int p2RoundsWon = 0;
    private int roundNumber = 1;

    private String roundMessage = "";

    public CombatScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        p1 = new MysticMage(1, P1_START_X);
        p2 = new ShadowVigilante(2, P2_START_X);

        p1RoundsWon = 0;
        p2RoundsWon = 0;
        roundNumber = 1;
        debug = false;
        matchOver = false;

        startRound();
    }

    /**
     * Resets round state and fighter stats.
     * Fighter.resetForRound() must restore health and Ki.
     */
    private void startRound() {
        p1.resetForRound();
        p2.resetForRound();

        timeRemaining = ROUND_DURATION;
        roundOver = false;
        roundMessage = "";
    }

    @Override
    public void update(double dt, InputManager input) {
        if (p1 == null || p2 == null || input == null) {
            return;
        }

        // Toggle hitbox/debug drawing.
        if (input.wasPressed(KeyCode.F1)) {
            debug = !debug;
        }

        // Restart the complete match.
        if (input.wasPressed(KeyCode.R)) {
            p1RoundsWon = 0;
            p2RoundsWon = 0;
            roundNumber = 1;
            matchOver = false;
            startRound();
            return;
        }

        // Freeze combat after the round ends.
        if (roundOver) {
            if (input.wasPressed(KeyCode.ENTER)) {
                if (matchOver) {
                    engine.changeState(GameState.RESULT);
                } else {
                    roundNumber++;
                    startRound();
                }
            }
            return;
        }

        // Count down using delta time.
        timeRemaining = Math.max(0, timeRemaining - dt);

        // Update both fighters.
        p1.update(dt, input, p2);
        p2.update(dt, input, p1);

        // Resolve normal attacks.
        combat.resolve(p1, p2);
        combat.resolve(p2, p1);

        // Player 1 ultimate: H.
        if (input.wasPressed(KeyCode.H) && !p1.isKO()) {
            combat.activateUltimate(p1, p2);
        }

        // Player 2 ultimate: O.
        if (input.wasPressed(KeyCode.O) && !p2.isKO()) {
            combat.activateUltimate(p2, p1);
        }

        // Prevent horizontal overlap.
        keepApart();

        // A knockout takes priority over timeout.
        if (p1.isKO() || p2.isKO()) {
            if (p1.isKO() && p2.isKO()) {
                endRound(0);
            } else if (p1.isKO()) {
                endRound(2);
            } else {
                endRound(1);
            }

            return;
        }

        // If time runs out, compare remaining health.
        if (timeRemaining <= 0) {
            if (p1.getHealth() > p2.getHealth()) {
                endRound(1);
            } else if (p2.getHealth() > p1.getHealth()) {
                endRound(2);
            } else {
                endRound(0);
            }
        }
    }

    /**
     * winner:
     * 1 = Player 1
     * 2 = Player 2
     * 0 = Draw
     */
    private void endRound(int winner) {
        if (roundOver) {
            return;
        }

        roundOver = true;

        if (winner == 1) {
            p1RoundsWon++;
            roundMessage = p1.getName()
                    + " WINS ROUND " + roundNumber + "!";
        } else if (winner == 2) {
            p2RoundsWon++;
            roundMessage = p2.getName()
                    + " WINS ROUND " + roundNumber + "!";
        } else {
            roundMessage = "ROUND DRAW!";
        }

        // First player to win two rounds wins the match.
        if (p1RoundsWon >= ROUNDS_TO_WIN) {
            matchOver = true;
            roundMessage = p1.getName() + " WINS THE MATCH!";
        } else if (p2RoundsWon >= ROUNDS_TO_WIN) {
            matchOver = true;
            roundMessage = p2.getName() + " WINS THE MATCH!";
        }
    }

    /**
     * Prevents fighters from overlapping horizontally
     * while they are approximately at the same height.
     */
    private void keepApart() {
        double dx = p2.getX() - p1.getX();
        double overlap = Fighter.WIDTH - Math.abs(dx);

        boolean sameHeight = Math.abs(p1.getY() - p2.getY()) < Fighter.STAND_HEIGHT;

        if (overlap > 0 && sameHeight) {
            double direction = dx >= 0 ? 1 : -1;
            double push = overlap / 2.0;

            p1.shiftX(-push * direction);
            p2.shiftX(push * direction);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        if (p1 == null || p2 == null) {
            return;
        }

        // Arena background.
        gc.setFill(Color.rgb(12, 15, 30));
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        // Ground.
        gc.setStroke(Color.GRAY);
        gc.setLineWidth(2);
        gc.strokeLine(
                0,
                Constants.GROUND_Y,
                Constants.WIDTH,
                Constants.GROUND_Y);

        // Render fighters.
        p1.render(gc, debug);
        p2.render(gc, debug);

        // Player 1 health.
        drawBar(
                gc,
                40, 65, 450, 24,
                ratio(p1.getHealth(), p1.getMaxHealth()),
                Color.LIMEGREEN,
                p1.getName() + " HP");

        // Player 2 health.
        drawBar(
                gc,
                Constants.WIDTH - 490, 65, 450, 24,
                ratio(p2.getHealth(), p2.getMaxHealth()),
                Color.ORANGE,
                p2.getName() + " HP");

        // Player 1 Mystic Energy.
        drawBar(
                gc,
                40, 105, 450, 15,
                ratio(p1.getKi(), p1.getMaxKi()),
                Color.DEEPSKYBLUE,
                "P1 MYSTIC ENERGY");

        // Player 2 Mystic Energy.
        drawBar(
                gc,
                Constants.WIDTH - 490, 105, 450, 15,
                ratio(p2.getKi(), p2.getMaxKi()),
                Color.MEDIUMPURPLE,
                "P2 MYSTIC ENERGY");

        drawMatchInfo(gc);
        drawUltimateStatus(gc);

        if (roundOver) {
            drawRoundOverlay(gc);
        }
    }

    private void drawMatchInfo(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.WHITE);

        // Timer.
        gc.setFont(Font.font("Arial", 22));
        gc.fillText(
                String.format(
                        "TIME: %02d",
                        (int) Math.ceil(timeRemaining)),
                Constants.WIDTH / 2.0,
                75);

        // Round number and match score.
        gc.setFont(Font.font("Arial", 16));
        gc.fillText(
                "ROUND " + roundNumber
                        + "     " + p1RoundsWon + " - " + p2RoundsWon
                        + "     FIRST TO 2",
                Constants.WIDTH / 2.0,
                100);

        // Controls.
        gc.setFont(Font.font("Arial", 13));
        gc.setFill(Color.LIGHTGRAY);
        gc.fillText(
                "P1: A/D move, W jump, S crouch, F light, G heavy, H ultimate"
                        + " | P2: Arrows, K light, L heavy, O ultimate",
                Constants.WIDTH / 2.0,
                30);

        gc.fillText(
                "F1: Debug     R: Restart Match",
                Constants.WIDTH / 2.0,
                48);
    }

    private void drawUltimateStatus(GraphicsContext gc) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 15));
        gc.setFill(Color.GOLD);

        if (p1.isUltimateReady() && !p1.isKO()) {
            gc.fillText(
                    "P1 ULTIMATE READY - H",
                    Constants.WIDTH / 2.0,
                    155);
        }

        if (p2.isUltimateReady() && !p2.isKO()) {
            gc.fillText(
                    "P2 ULTIMATE READY - O",
                    Constants.WIDTH / 2.0,
                    177);
        }
    }

    private void drawRoundOverlay(GraphicsContext gc) {
        gc.setFill(Color.color(0, 0, 0, 0.82));
        gc.fillRoundRect(
                Constants.WIDTH / 2.0 - 300,
                Constants.HEIGHT / 2.0 - 100,
                600,
                200,
                24,
                24);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.GOLD);
        gc.setFont(Font.font("Arial", 32));

        gc.fillText(
                roundMessage,
                Constants.WIDTH / 2.0,
                Constants.HEIGHT / 2.0 - 25);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 18));

        String instruction = matchOver
                ? "Press ENTER for the Result Screen"
                : "Press ENTER for the Next Round";

        gc.fillText(
                instruction,
                Constants.WIDTH / 2.0,
                Constants.HEIGHT / 2.0 + 25);

        gc.setFont(Font.font("Arial", 14));
        gc.fillText(
                "Match score: " + p1RoundsWon + " - " + p2RoundsWon,
                Constants.WIDTH / 2.0,
                Constants.HEIGHT / 2.0 + 60);
    }

    private double ratio(double value, double maximum) {
        if (maximum <= 0) {
            return 0;
        }

        return Math.max(0, Math.min(1, value / maximum));
    }

    private void drawBar(
            GraphicsContext gc,
            double x,
            double y,
            double width,
            double height,
            double value,
            Color fillColor,
            String label) {
        // Background.
        gc.setFill(Color.DARKSLATEGRAY);
        gc.fillRect(x, y, width, height);

        // Filled portion.
        gc.setFill(fillColor);
        gc.fillRect(x, y, width * value, height);

        // Border.
        gc.setStroke(Color.WHITE);
        gc.strokeRect(x, y, width, height);

        // Label.
        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText(label, x, y - 5);
    }
}
