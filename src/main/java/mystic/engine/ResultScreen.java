
package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameMode;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class ResultScreen implements Screen {

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(60, Constants.WIDTH, Constants.HEIGHT);

    private double t;

    public ResultScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
    }

    @Override
    public void update(double dt, InputManager input) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);

        // Prevent the key used to finish the match from skipping this screen.
        if (t < 0.4) {
            return;
        }

        boolean arcade = engine.getMode() == GameMode.ARCADE;
        boolean playerWon = engine.getPlayer1MatchWins() > engine.getPlayer2MatchWins();

        // U: Sorcery Upgradation.
        if (input.wasPressed(KeyCode.U)) {
            engine.changeState(GameState.SORCERY_UPGRADATION);
            return;
        }

        // Escape or M: Main menu.
        if (input.wasPressed(KeyCode.ESCAPE)
                || input.wasPressed(KeyCode.M)) {
            engine.changeState(GameState.MENU);
            return;
        }

        // N: Advance only after winning an Arcade trial.
        if (input.wasPressed(KeyCode.N) && arcade && playerWon) {
            int currentTrial = engine.getArcadeTrial();

            if (currentTrial + 1 < GameEngine.ARCADE_TRIALS) {
                engine.setArcadeTrial(currentTrial + 1);
                engine.configureArcadeTrial();
                engine.changeState(GameState.COMBAT);
            } else {
                // The final Arcade trial has been completed.
                engine.changeState(GameState.MENU);
            }
            return;
        }

        // Enter/Space: rematch the current fight.
        if (input.wasPressed(KeyCode.ENTER)
                || input.wasPressed(KeyCode.SPACE)) {
            engine.changeState(GameState.COMBAT);
        }
    }

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH;
        double h = Constants.HEIGHT;
        double cx = w / 2.0;

        int wins1 = engine.getPlayer1MatchWins();
        int wins2 = engine.getPlayer2MatchWins();

        boolean arcade = engine.getMode() == GameMode.ARCADE;
        boolean playerWon = wins1 > wins2;

        double a1 = ease(clamp(t / 0.6));
        double a2 = ease(clamp((t - 0.5) / 0.6));
        double a3 = ease(clamp((t - 1.0) / 0.6));

        // Background.
        MysticTheme.drawBackground(gc, w, h);

        for (int i = 0; i < 7; i++) {
            double o = (i - 3) * w * 0.045;
            double e = (i - 3) * w * 0.17;

            gc.setFill(Color.web(
                    "#FFD27A",
                    (0.05 + 0.03 * Math.sin(t * 1.2 + i)) * a1));

            gc.fillPolygon(
                    new double[] {
                            cx + o,
                            cx + o + w * 0.03,
                            cx + e + w * 0.09,
                            cx + e - w * 0.07
                    },
                    new double[] { 0, 0, h, h },
                    4);
        }

        MysticTheme.drawShards(gc, w, h, t);
        MysticTheme.drawMandala(
                gc, cx, h * 0.40, h * 0.52, t * 8, 0.28 * a1);
        embers.draw(gc);

        rule(gc, cx, h * 0.12, w * 0.40 * a1);
        rule(gc, cx, h * 0.90, w * 0.40 * a1);

        gc.setTextAlign(TextAlignment.CENTER);

        // Header.
        gc.setGlobalAlpha(a1);
        gc.setFill(MysticTheme.GOLD_D);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.fillText(
                arcade ? "ARCADE TRIAL COMPLETE" : "MATCH COMPLETE",
                cx, h * 0.095);
        gc.setGlobalAlpha(1);

        // Winner banner.
        String winner = engine.getMatchWinner();
        String resultText = (winner == null || winner.isBlank())
                ? "MATCH FINISHED"
                : winner;

        double sc = 1.0 + (1 - a2) * 0.45;
        double glowPulse = 0.75 + 0.25 * Math.sin(t * 3);

        gc.save();
        gc.translate(cx, h * 0.27);
        gc.scale(sc, sc);

        glowText(
                gc,
                playerWon ? "VICTORY" : "DEFEAT",
                0,
                -h * 0.045,
                MysticTheme.title(Math.min(96, h * 0.12)),
                playerWon ? Color.web("#FF8A1F") : Color.web("#FF4855"),
                a2 * glowPulse);

        MysticTheme.drawGlowText(
                gc,
                resultText,
                0,
                h * 0.06,
                MysticTheme.title(Math.min(52, h * 0.065)),
                MysticTheme.GOLD,
                a2);

        gc.restore();

        // Score panels.
        double py = h * 0.43;
        double ph = h * 0.26;
        double pw = Math.min(250, w * 0.22);
        double gap = Math.min(150, w * 0.12);

        boolean p1Won = wins1 > wins2;
        boolean p2Won = wins2 > wins1;
        double slide = (1 - a3) * w * 0.3;

        scorePanel(
                gc,
                cx - gap / 2 - pw - slide,
                py,
                pw,
                ph,
                "PLAYER 1",
                wins1,
                p1Won,
                Color.web("#B168FF"));

        scorePanel(
                gc,
                cx + gap / 2 + slide,
                py,
                pw,
                ph,
                "PLAYER 2",
                wins2,
                p2Won,
                Color.web("#FF4855"));

        gc.setGlobalAlpha(a3);
        gc.setFill(MysticTheme.GOLD_D);
        gc.setFont(Font.font("Serif", 14));
        gc.fillText("FINAL SCORE", cx, py + ph / 2 - 16);

        gc.setFill(MysticTheme.GOLD);
        gc.setFont(MysticTheme.title(40));
        gc.fillText("-", cx, py + ph / 2 + 22);
        gc.setGlobalAlpha(1);

        // Arcade reward and progression information.
        double infoY = h * 0.735;

        if (arcade) {
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 17));

            if (playerWon) {
                gc.setFill(Color.LIGHTGREEN);
                gc.fillText("+" + engine.getLastXpGained() + " XP EARNED", cx, infoY);

                int nextTrial = engine.getArcadeTrial() + 1;

                gc.setFill(MysticTheme.GOLD);
                gc.setFont(Font.font("Serif", 13));

                if (nextTrial < GameEngine.ARCADE_TRIALS) {
                    gc.fillText(
                            "NEXT TRIAL UNLOCKED  •  PRESS N TO CONTINUE",
                            cx, infoY + 23);
                } else {
                    gc.fillText(
                            "ALL ARCADE TRIALS COMPLETE!",
                            cx, infoY + 23);
                }
            } else {
                gc.setFill(Color.web("#FF8A8A"));
                gc.fillText("TRIAL NOT COMPLETED", cx, infoY);

                gc.setFill(Color.web("#D8C7A0"));
                gc.setFont(Font.font("Serif", 13));
                gc.fillText(
                        "Press ENTER to retry this trial",
                        cx, infoY + 23);
            }
        }

        // Buttons / keyboard controls.
        double buttonY = arcade ? h * 0.83 : h * 0.80;
        double buttonW = Math.min(250, w * 0.22);
        double buttonH = 40;
        double buttonGap = 10;
        double totalW = buttonW * 3 + buttonGap * 2;
        double startX = cx - totalW / 2;
        double pulse = 0.5 + 0.5 * Math.sin(t * 4);

        gc.setGlobalAlpha(a3);

        button(
                gc,
                startX,
                buttonY,
                buttonW,
                buttonH,
                "REMATCH [ENTER]",
                true,
                pulse);

        button(
                gc,
                startX + buttonW + buttonGap,
                buttonY,
                buttonW,
                buttonH,
                "SORCERY [U]",
                false,
                pulse);

        button(
                gc,
                startX + 2 * (buttonW + buttonGap),
                buttonY,
                buttonW,
                buttonH,
                "MENU [ESC]",
                false,
                pulse);

        gc.setGlobalAlpha(1);

        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void scorePanel(
            GraphicsContext gc,
            double x,
            double y,
            double w,
            double h,
            String label,
            int wins,
            boolean winner,
            Color accent) {
        gc.save();

        if (winner) {
            gc.setEffect(new DropShadow(28, MysticTheme.SPARK));
        }

        gc.setFill(new LinearGradient(
                0, y, 0, y + h,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(
                        0,
                        Color.web(winner ? "#4A0E12" : "#25080B", 0.95)),
                new Stop(1, Color.web("#0C0203", 0.95))));

        gc.fillRoundRect(x, y, w, h, 18, 18);
        gc.restore();

        gc.setStroke(winner ? MysticTheme.GOLD : Color.web("#C9A24B", 0.5));
        gc.setLineWidth(winner ? 3 : 1.5);
        gc.strokeRoundRect(x, y, w, h, 18, 18);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(accent);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.fillText(label, x + w / 2, y + 34);

        gc.setStroke(Color.web("#C9A24B", 0.4));
        gc.setLineWidth(1);
        gc.strokeLine(x + 24, y + 46, x + w - 24, y + 46);

        gc.save();

        if (winner) {
            gc.setEffect(new DropShadow(20, MysticTheme.SPARK));
        }

        gc.setFill(winner ? MysticTheme.GOLD : Color.web("#D8C7A0", 0.75));
        gc.setFont(MysticTheme.title(Math.min(110, h * 0.5)));
        gc.fillText(String.valueOf(wins), x + w / 2, y + h * 0.76);
        gc.restore();

        for (int i = 0; i < 2; i++) {
            double px = x + w / 2 + (i - 0.5) * 24;
            double py = y + h - 16;
            boolean on = i < wins;

            gc.setFill(on ? MysticTheme.GOLD : Color.web("#2D1114"));
            gc.setStroke(on ? MysticTheme.GOLD : MysticTheme.GOLD_D);
            gc.setLineWidth(1.2);
            gc.fillOval(px - 6, py - 6, 12, 12);
            gc.strokeOval(px - 6, py - 6, 12, 12);
        }

        if (winner) {
            double crownX = x + w / 2;
            double crownY = y - 14;

            gc.setFill(MysticTheme.GOLD);
            gc.fillPolygon(
                    new double[] {
                            crownX - 22, crownX - 22,
                            crownX - 11, crownX,
                            crownX + 11, crownX + 22,
                            crownX + 22
                    },
                    new double[] {
                            crownY + 14, crownY - 6,
                            crownY + 4, crownY - 12,
                            crownY + 4, crownY - 6,
                            crownY + 14
                    },
                    7);
        }
    }

    private void button(
            GraphicsContext gc,
            double x,
            double y,
            double w,
            double h,
            String label,
            boolean primary,
            double pulse) {
        gc.save();

        if (primary) {
            gc.setEffect(new DropShadow(10 + 12 * pulse, MysticTheme.SPARK));
        }

        gc.setFill(
                primary
                        ? Color.web("#7A0F12", 0.95)
                        : Color.web("#1B0708", 0.9));

        gc.fillRoundRect(x, y, w, h, 14, 14);
        gc.restore();

        gc.setStroke(
                primary
                        ? Color.web("#FFE9A0", 0.6 + 0.4 * pulse)
                        : MysticTheme.GOLD_D);

        gc.setLineWidth(primary ? 2 : 1.5);
        gc.strokeRoundRect(x, y, w, h, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(primary ? MysticTheme.GOLD : Color.web("#D8C7A0"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 15));
        gc.fillText(label, x + w / 2, y + h / 2 + 5);
    }

    private void rule(GraphicsContext gc, double cx, double y, double half) {
        if (half < 2) {
            return;
        }

        gc.setStroke(new LinearGradient(
                cx - half, 0, cx + half, 0,
                false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#C9A24B", 0)),
                new Stop(0.5, Color.web("#FFE9A0", 0.9)),
                new Stop(1, Color.web("#C9A24B", 0))));

        gc.setLineWidth(2);
        gc.strokeLine(cx - half, y, cx + half, y);

        gc.setFill(MysticTheme.GOLD);
        gc.fillPolygon(
                new double[] { cx, cx + 8, cx, cx - 8 },
                new double[] { y - 8, y, y + 8, y },
                4);
    }

    private void glowText(
            GraphicsContext gc,
            String text,
            double x,
            double y,
            Font font,
            Color color,
            double alpha) {
        gc.save();
        gc.setGlobalAlpha(clamp(alpha));
        gc.setEffect(new DropShadow(36, color));
        gc.setFont(font);
        gc.setFill(MysticTheme.GOLD);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(text, x, y);
        gc.restore();
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double ease(double value) {
        return 1 - Math.pow(1 - value, 3);
    }
}
