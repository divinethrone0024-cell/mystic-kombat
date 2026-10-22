
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Action;
import mystic.common.Constants;
import mystic.common.GameState;
import mystic.combat.CombatSystem;
import mystic.fighters.Fighter;

public class CombatScreen implements Screen {

    private final GameEngine engine;
    private final CombatSystem combat = new CombatSystem();

    private Fighter p1;
    private Fighter p2;

    private double timeRemaining;
    private boolean roundOver;
    private String roundMessage = "";

    private int p1RoundsWon;
    private int p2RoundsWon;

    public CombatScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        p1RoundsWon = 0;
        p2RoundsWon = 0;

        combat.resetCombos();
        combat.resetEffects();

        p1 = engine.createFighter(
                1,
                Constants.WIDTH * 0.25);

        p2 = engine.createFighter(
                2,
                Constants.WIDTH * 0.75);

        startRound();
    }

    private void startRound() {
        p1.resetForRound();
        p2.resetForRound();

        combat.resetCombos();
        combat.resetEffects();

        timeRemaining = Constants.ROUND_SECONDS;
        roundOver = false;
        roundMessage = "";
    }

    @Override
    public void update(double dt, InputManager input) {
        if (p1 == null || p2 == null) {
            return;
        }

        // Update combo timers and impact animations.
        combat.update(dt);

        if (roundOver) {
            if (input.wasPressed(KeyCode.ENTER)
                    || input.wasPressed(KeyCode.SPACE)) {

                if (p1RoundsWon >= Constants.ROUNDS_TO_WIN
                        || p2RoundsWon >= Constants.ROUNDS_TO_WIN) {

                    engine.changeState(GameState.RESULT);

                } else {
                    startRound();
                }
            }

            return;
        }

        // Activate Ultimate Incantations.
        if (input.wasPressed(1, Action.ULTIMATE)) {
            combat.activateUltimate(p1, p2);
        }

        if (input.wasPressed(2, Action.ULTIMATE)) {
            combat.activateUltimate(p2, p1);
        }

        // Update both fighters.
        p1.update(dt, input, p2);
        p2.update(dt, input, p1);

        // Resolve normal attacks.
        combat.resolve(p1, p2);
        combat.resolve(p2, p1);

        // Prevent fighters from overlapping.
        keepApart();

        timeRemaining = Math.max(
                0,
                timeRemaining - dt);

        // Check for knockouts.
        if (p1.isKO()) {
            finishRound(2);
            return;
        }

        if (p2.isKO()) {
            finishRound(1);
            return;
        }

        // Decide the round winner when time expires.
        if (timeRemaining <= 0) {
            if (p1.getHealth() > p2.getHealth()) {
                finishRound(1);

            } else if (p2.getHealth() > p1.getHealth()) {
                finishRound(2);

            } else {
                finishRound(0);
            }
        }
    }

    /**
     * Separate fighters when their horizontal positions overlap.
     */
    private void keepApart() {
        double distance = p2.getX() - p1.getX();
        double minimumDistance = Fighter.WIDTH;

        if (Math.abs(distance) >= minimumDistance) {
            return;
        }

        double overlap = minimumDistance - Math.abs(distance);

        double separation = overlap / 2.0;

        if (distance >= 0) {
            p1.shiftX(-separation);
            p2.shiftX(separation);

        } else {
            p1.shiftX(separation);
            p2.shiftX(-separation);
        }
    }

    /**
     * Finish the current round and update the score.
     *
     * winner = 1: Player 1
     * winner = 2: Player 2
     * winner = 0: Draw
     */
    private void finishRound(int winner) {
        roundOver = true;

        if (winner == 1) {
            p1RoundsWon++;
            roundMessage = "PLAYER 1 WINS THE ROUND!";

        } else if (winner == 2) {
            p2RoundsWon++;
            roundMessage = "PLAYER 2 WINS THE ROUND!";

        } else {
            roundMessage = "DRAW!";
        }

        if (p1RoundsWon >= Constants.ROUNDS_TO_WIN) {
            roundMessage = "PLAYER 1 WINS THE MATCH!";

        } else if (p2RoundsWon >= Constants.ROUNDS_TO_WIN) {
            roundMessage = "PLAYER 2 WINS THE MATCH!";
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        drawArena(gc);

        if (p1 == null || p2 == null) {
            return;
        }

        // Draw fighters first.
        p1.render(gc, false);
        p2.render(gc, false);

        // Draw impact effects above the fighters.
        combat.renderEffects(gc);

        // Draw HUD and round overlay last.
        drawHUD(gc);
        drawRoundMessage(gc);
    }

    /**
     * Draw the fighting arena.
     */
    private void drawArena(GraphicsContext gc) {
        gc.setFill(Color.rgb(10, 13, 32));
        gc.fillRect(
                0, 0,
                Constants.WIDTH,
                Constants.HEIGHT);

        // Background decorations.
        gc.setFill(Color.rgb(36, 43, 76));
        gc.fillOval(80, 80, 180, 180);

        gc.fillOval(
                Constants.WIDTH - 260,
                80,
                180,
                180);

        // Ground.
        gc.setFill(Color.rgb(28, 31, 48));
        gc.fillRect(
                0,
                Constants.GROUND_Y,
                Constants.WIDTH,
                Constants.HEIGHT - Constants.GROUND_Y);

        gc.setStroke(Color.rgb(100, 105, 145));
        gc.setLineWidth(2);

        gc.strokeLine(
                0,
                Constants.GROUND_Y,
                Constants.WIDTH,
                Constants.GROUND_Y);

        gc.setStroke(Color.rgb(65, 70, 100));
        gc.setLineWidth(1);

        gc.strokeLine(
                Constants.WIDTH / 2.0,
                Constants.GROUND_Y,
                Constants.WIDTH / 2.0,
                Constants.HEIGHT);
    }

    /**
     * Draw health, Ki, timer, round score, and ultimate status.
     */
    private void drawHUD(GraphicsContext gc) {
        drawHealthBar(
                gc, p1,
                40, 35, 420,
                false);

        drawHealthBar(
                gc, p2,
                Constants.WIDTH - 460,
                35, 420,
                true);

        drawKiBar(
                gc, p1,
                40, 88, 300);

        drawKiBar(
                gc, p2,
                Constants.WIDTH - 340,
                88, 300);

        // Round timer.
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 34));
        gc.setTextAlign(TextAlignment.CENTER);

        gc.fillText(
                String.format(
                        "%02d",
                        (int) Math.ceil(timeRemaining)),
                Constants.WIDTH / 2.0,
                55);

        // Match score.
        gc.setFont(Font.font("Arial", 16));

        gc.fillText(
                p1RoundsWon + "  -  " + p2RoundsWon,
                Constants.WIDTH / 2.0,
                82);

        // Player 1 ultimate status.
        gc.setFont(Font.font("Arial", 12));
        gc.setFill(
                p1.isUltimateReady()
                        ? Color.GOLD
                        : Color.LIGHTGRAY);

        gc.setTextAlign(TextAlignment.LEFT);

        gc.fillText(
                p1.isUltimateReady()
                        ? "ULTIMATE READY [H]"
                        : "ULTIMATE [H]",
                40,
                115);

        // Player 2 ultimate status.
        gc.setFill(
                p2.isUltimateReady()
                        ? Color.GOLD
                        : Color.LIGHTGRAY);

        gc.setTextAlign(TextAlignment.RIGHT);

        gc.fillText(
                p2.isUltimateReady()
                        ? "ULTIMATE READY [;]"
                        : "ULTIMATE [;]",
                Constants.WIDTH - 40,
                115);
    }

    /**
     * Draw a health bar.
     */
    private void drawHealthBar(
            GraphicsContext gc,
            Fighter fighter,
            double x,
            double y,
            double width,
            boolean reverse) {

        double height = 28;

        double ratio = (double) fighter.getHealth()
                / fighter.getMaxHealth();

        ratio = Math.max(0, Math.min(1, ratio));

        gc.setFill(Color.rgb(55, 35, 48));
        gc.fillRoundRect(
                x, y,
                width, height,
                8, 8);

        gc.setFill(
                ratio > 0.5
                        ? Color.LIMEGREEN
                        : ratio > 0.25
                                ? Color.ORANGE
                                : Color.RED);

        double filledWidth = width * ratio;

        double filledX = reverse
                ? x + width - filledWidth
                : x;

        gc.fillRoundRect(
                filledX, y,
                filledWidth, height,
                8, 8);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);

        gc.strokeRoundRect(
                x, y,
                width, height,
                8, 8);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 15));

        gc.setTextAlign(
                reverse
                        ? TextAlignment.RIGHT
                        : TextAlignment.LEFT);

        gc.fillText(
                fighter.getName()
                        + "  "
                        + fighter.getHealth()
                        + "/"
                        + fighter.getMaxHealth(),
                reverse ? x + width : x,
                y - 7);
    }

    /**
     * Draw the Mystic Energy / Ki meter.
     */
    private void drawKiBar(
            GraphicsContext gc,
            Fighter fighter,
            double x,
            double y,
            double width) {

        double height = 12;

        double ratio = fighter.getKi() / fighter.getMaxKi();

        ratio = Math.max(0, Math.min(1, ratio));

        gc.setFill(Color.rgb(38, 39, 57));

        gc.fillRoundRect(
                x, y,
                width, height,
                5, 5);

        gc.setFill(
                fighter.isUltimateReady()
                        ? Color.GOLD
                        : Color.rgb(40, 190, 255));

        gc.fillRoundRect(
                x, y,
                width * ratio,
                height,
                5, 5);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(1);

        gc.strokeRoundRect(
                x, y,
                width, height,
                5, 5);
    }

    /**
     * Draw the round result overlay.
     */
    private void drawRoundMessage(GraphicsContext gc) {
        if (!roundOver) {
            return;
        }

        gc.setFill(Color.rgb(0, 0, 0, 0.72));

        gc.fillRect(
                0,
                Constants.HEIGHT / 2.0 - 75,
                Constants.WIDTH,
                150);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 32));
        gc.setFill(Color.GOLD);

        gc.fillText(
                roundMessage,
                Constants.WIDTH / 2.0,
                Constants.HEIGHT / 2.0 - 5);

        gc.setFont(Font.font("Arial", 16));
        gc.setFill(Color.WHITE);

        boolean matchFinished = p1RoundsWon >= Constants.ROUNDS_TO_WIN
                || p2RoundsWon >= Constants.ROUNDS_TO_WIN;

        gc.fillText(
                matchFinished
                        ? "Press ENTER to continue"
                        : "Press ENTER to start the next round",
                Constants.WIDTH / 2.0,
                Constants.HEIGHT / 2.0 + 35);
    }
}
