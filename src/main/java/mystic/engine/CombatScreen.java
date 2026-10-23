
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
        private final PauseMenuScreen pauseMenu = new PauseMenuScreen();
        private boolean paused = false;

        private CombatSystem combat;
        private Fighter p1;
        private Fighter p2;

        private double timeRemaining;
        private boolean roundOver;
        private String roundMessage = "";

        private int p1RoundsWon;
        private int p2RoundsWon;

        // Animated HUD values.
        private double p1DisplayedHealth;
        private double p2DisplayedHealth;
        private double hudPulseTime;

        private static final double HUD_ANIMATION_SPEED = 8.0;
        private static final double HEALTH_BAR_WIDTH = 420;
        private static final double KI_BAR_WIDTH = 300;

        // Ultimate cinematic settings.
        private static final double ULTIMATE_CINEMATIC_DURATION = 1.25;
        private static final double ULTIMATE_FLASH_DURATION = 0.16;

        private double ultimateCinematicTimer;
        private double ultimateFlashTimer;

        private String ultimateFighterName = "";
        private int ultimatePlayer;

        public CombatScreen(GameEngine engine) {
                this.engine = engine;
        }

        @Override
        public void onEnter() {
                paused = false;
                pauseMenu.reset();

                p1RoundsWon = 0;
                p2RoundsWon = 0;

                combat = new CombatSystem();

                p1 = engine.createFighter(
                                1,
                                Constants.WIDTH * 0.25);

                p2 = engine.createFighter(
                                2,
                                Constants.WIDTH * 0.75);

                p1DisplayedHealth = p1.getHealth();
                p2DisplayedHealth = p2.getHealth();

                hudPulseTime = 0;

                ultimateCinematicTimer = 0;
                ultimateFlashTimer = 0;
                ultimateFighterName = "";
                ultimatePlayer = 0;

                startRound();
        }

        private void startRound() {
                paused = false;
                pauseMenu.reset();

                p1.resetForRound();
                p2.resetForRound();

                combat.resetCombos();
                combat.resetEffects();

                timeRemaining = Constants.ROUND_SECONDS;
                roundOver = false;
                roundMessage = "";

                p1DisplayedHealth = p1.getHealth();
                p2DisplayedHealth = p2.getHealth();

                ultimateCinematicTimer = 0;
                ultimateFlashTimer = 0;
                ultimateFighterName = "";
                ultimatePlayer = 0;
        }

        @Override
        public void update(double dt, InputManager input) {
                if (p1 == null || p2 == null || combat == null) {
                        return;
                }

                double safeDt = Math.max(
                                0,
                                Math.min(dt, 0.05));

                // Handle pause input before any gameplay timers or fighters update.
                if (paused) {
                        PauseMenuScreen.Choice choice = pauseMenu.update(input);

                        if (choice == PauseMenuScreen.Choice.RESUME) {
                                paused = false;
                        } else if (choice == PauseMenuScreen.Choice.RESTART_ROUND) {
                                startRound();
                        } else if (choice == PauseMenuScreen.Choice.MAIN_MENU) {
                                paused = false;
                                engine.changeState(GameState.MENU);
                        }

                        return;
                }

                // Pause only during an active round, not on the round-result screen.
                if (!roundOver && input.wasPressed(KeyCode.ESCAPE)) {
                        paused = true;
                        pauseMenu.reset();
                        return;
                }

                hudPulseTime += safeDt;

                updateDisplayedHealth(safeDt);
                combat.update(safeDt);
                updateUltimateCinematic(safeDt);

                // Wait for the player to continue after a round.
                if (roundOver) {
                        if (input.wasPressed(KeyCode.ENTER)
                                        || input.wasPressed(KeyCode.SPACE)) {

                                if (p1RoundsWon >= 2 || p2RoundsWon >= 2) {
                                        engine.changeState(GameState.RESULT);
                                } else {
                                        startRound();
                                }
                        }

                        return;
                }

                // Player 1 Ultimate Incantation: H.
                if (input.wasPressed(1, Action.ULTIMATE)) {
                        if (combat.activateUltimate(p1, p2)) {
                                startUltimateCinematic(p1, 1);
                        }
                }

                // Player 2 Ultimate Incantation: semicolon.
                if (input.wasPressed(2, Action.ULTIMATE)) {
                        if (combat.activateUltimate(p2, p1)) {
                                startUltimateCinematic(p2, 2);
                        }
                }

                // Update fighters using the existing Fighter API.
                p1.update(safeDt, input, p2);
                p2.update(safeDt, input, p1);

                // Resolve attacks from both players.
                combat.resolve(p1, p2);
                combat.resolve(p2, p1);

                keepApart();

                timeRemaining -= safeDt;

                // Determine the round winner.
                if (p1.isKO()) {
                        finishRound(2);
                } else if (p2.isKO()) {
                        finishRound(1);
                } else if (timeRemaining <= 0) {
                        if (p1.getHealth() > p2.getHealth()) {
                                finishRound(1);
                        } else if (p2.getHealth() > p1.getHealth()) {
                                finishRound(2);
                        } else {
                                finishRound(0);
                        }
                }
        }

        // Start the ultimate cinematic only after successful activation.
        private void startUltimateCinematic(Fighter fighter, int player) {
                ultimateCinematicTimer = ULTIMATE_CINEMATIC_DURATION;
                ultimateFlashTimer = ULTIMATE_FLASH_DURATION;

                ultimateFighterName = fighter.getName();
                ultimatePlayer = player;
        }

        // Fade the cinematic and flash independently.
        private void updateUltimateCinematic(double dt) {
                ultimateCinematicTimer = Math.max(
                                0,
                                ultimateCinematicTimer - dt);

                ultimateFlashTimer = Math.max(
                                0,
                                ultimateFlashTimer - dt);
        }

        // Smooth the health-bar animation after damage.
        private void updateDisplayedHealth(double dt) {
                p1DisplayedHealth = approachHealth(
                                p1DisplayedHealth,
                                p1.getHealth(),
                                dt);

                p2DisplayedHealth = approachHealth(
                                p2DisplayedHealth,
                                p2.getHealth(),
                                dt);
        }

        private double approachHealth(
                        double displayed,
                        double actual,
                        double dt) {

                double difference = actual - displayed;

                if (Math.abs(difference) < 0.1) {
                        return actual;
                }

                double blend = Math.min(
                                1.0,
                                dt * HUD_ANIMATION_SPEED);

                return displayed + difference * blend;
        }

        private void finishRound(int winner) {
                if (roundOver) {
                        return;
                }

                roundOver = true;

                if (winner == 1) {
                        p1RoundsWon++;
                        roundMessage = "PLAYER 1 WINS THE ROUND!";
                } else if (winner == 2) {
                        p2RoundsWon++;
                        roundMessage = "PLAYER 2 WINS THE ROUND!";
                } else {
                        roundMessage = "ROUND DRAW!";
                }

                // A best-of-three match ends when either player wins two rounds.
                if (p1RoundsWon >= 2 || p2RoundsWon >= 2) {
                        String matchWinner = p1RoundsWon > p2RoundsWon
                                        ? p1.getName() + " WINS!"
                                        : p2.getName() + " WINS!";

                        engine.setMatchResult(
                                        matchWinner,
                                        p1RoundsWon,
                                        p2RoundsWon);

                        roundMessage = matchWinner;
                }
        }

        // Prevent fighters from walking through one another.
        private void keepApart() {
                double distance = Math.abs(
                                p1.getX() - p2.getX());

                double minimumDistance = Fighter.WIDTH;

                if (distance < minimumDistance) {
                        double push = (minimumDistance - distance) / 2.0;

                        if (p1.getX() < p2.getX()) {
                                p1.shiftX(-push);
                                p2.shiftX(push);
                        } else {
                                p1.shiftX(push);
                                p2.shiftX(-push);
                        }
                }
        }

        @Override
        public void render(GraphicsContext gc) {
                // The arena remains stable while the fighters shake.
                drawArena(gc);

                // Apply screen shake only to the combat layer.
                gc.save();

                try {
                        if (combat != null) {
                                gc.translate(
                                                combat.getScreenShakeX(),
                                                combat.getScreenShakeY());
                        }

                        if (p1 != null) {
                                p1.render(gc, false);
                        }

                        if (p2 != null) {
                                p2.render(gc, false);
                        }

                        if (combat != null) {
                                combat.renderEffects(gc);
                        }
                } finally {
                        gc.restore();
                }

                // Keep the HUD stable during screen shake.
                drawHUD(gc);

                // Show the round result after the cinematic finishes.
                if (roundOver && ultimateCinematicTimer <= 0) {
                        drawRoundMessage(gc);
                }

                // Render the cinematic above the arena and HUD.
                drawUltimateCinematic(gc);

                // The pause overlay must be the topmost layer.
                if (paused) {
                        pauseMenu.render(gc);
                }
        }

        private void drawArena(GraphicsContext gc) {
                double width = Constants.WIDTH;
                double height = Constants.HEIGHT;
                double ground = Constants.GROUND_Y;

                // Dark multiverse arena background.
                gc.setFill(Color.rgb(9, 11, 25));
                gc.fillRect(0, 0, width, height);

                // Ambient blue and purple lighting.
                gc.setFill(Color.rgb(35, 55, 125, 0.24));
                gc.fillOval(
                                width * 0.08,
                                height * 0.12,
                                width * 0.34,
                                height * 0.50);

                gc.setFill(Color.rgb(115, 35, 105, 0.20));
                gc.fillOval(
                                width * 0.58,
                                height * 0.12,
                                width * 0.34,
                                height * 0.50);

                // Floor.
                gc.setFill(Color.rgb(19, 22, 39));
                gc.fillRect(
                                0,
                                ground,
                                width,
                                height - ground);

                gc.setStroke(Color.rgb(212, 175, 55, 0.8));
                gc.setLineWidth(2);
                gc.strokeLine(0, ground, width, ground);

                // Floor perspective lines.
                gc.setStroke(Color.rgb(100, 115, 160, 0.18));
                gc.setLineWidth(1);

                for (int x = 0; x < width; x += 50) {
                        gc.strokeLine(
                                        x,
                                        ground,
                                        x - 40,
                                        height);
                }

                // Center arena line.
                gc.setStroke(Color.rgb(212, 175, 55, 0.22));
                gc.strokeLine(
                                width / 2.0,
                                ground,
                                width / 2.0,
                                height);
        }

        private void drawHUD(GraphicsContext gc) {
                if (p1 == null || p2 == null) {
                        return;
                }

                double width = Constants.WIDTH;
                double center = width / 2.0;

                // Player health bars.
                drawHealthBar(
                                gc,
                                p1,
                                40,
                                35,
                                HEALTH_BAR_WIDTH,
                                p1DisplayedHealth,
                                false);

                drawHealthBar(
                                gc,
                                p2,
                                width - 460,
                                35,
                                HEALTH_BAR_WIDTH,
                                p2DisplayedHealth,
                                true);

                // Mystic Energy meters.
                drawKiBar(
                                gc,
                                p1,
                                40,
                                91,
                                KI_BAR_WIDTH,
                                false);

                drawKiBar(
                                gc,
                                p2,
                                width - 340,
                                91,
                                KI_BAR_WIDTH,
                                true);

                // Round timer.
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(Font.font("Arial", 30));
                gc.setFill(Color.WHITE);

                gc.fillText(
                                String.format(
                                                "%02d",
                                                Math.max(
                                                                0,
                                                                (int) Math.ceil(timeRemaining))),
                                center,
                                52);

                // Current match score.
                gc.setFont(Font.font("Arial", 17));
                gc.setFill(Color.rgb(230, 230, 245));

                gc.fillText(
                                p1RoundsWon + "  -  " + p2RoundsWon,
                                center,
                                79);

                // Two round-win pips per player.
                drawRoundPips(
                                gc,
                                center - 45,
                                98,
                                p1RoundsWon,
                                false);

                drawRoundPips(
                                gc,
                                center + 45,
                                98,
                                p2RoundsWon,
                                true);

                // Ultimate readiness indicators.
                drawUltimateLabel(
                                gc,
                                p1,
                                40,
                                128,
                                false);

                drawUltimateLabel(
                                gc,
                                p2,
                                width - 340,
                                128,
                                true);

                // Combo counters.
                drawComboCounter(
                                gc,
                                combat.getComboCount(p1),
                                40,
                                151,
                                false);

                drawComboCounter(
                                gc,
                                combat.getComboCount(p2),
                                width - 40,
                                151,
                                true);
        }

        private void drawHealthBar(
                        GraphicsContext gc,
                        Fighter fighter,
                        double x,
                        double y,
                        double barWidth,
                        double displayedHealth,
                        boolean reverse) {

                double barHeight = 22;

                double maxHealth = Math.max(
                                1,
                                fighter.getMaxHealth());

                double actualRatio = clamp(
                                fighter.getHealth() / maxHealth,
                                0,
                                1);

                double displayedRatio = clamp(
                                displayedHealth / maxHealth,
                                0,
                                1);

                // Health-bar background.
                gc.setFill(Color.rgb(35, 35, 48));
                gc.fillRoundRect(
                                x,
                                y,
                                barWidth,
                                barHeight,
                                8,
                                8);

                // Delayed red trail for recently lost health.
                double trailWidth = barWidth * displayedRatio;

                gc.setFill(Color.rgb(155, 35, 48));

                if (reverse) {
                        gc.fillRoundRect(
                                        x + barWidth - trailWidth,
                                        y,
                                        trailWidth,
                                        barHeight,
                                        8,
                                        8);
                } else {
                        gc.fillRoundRect(
                                        x,
                                        y,
                                        trailWidth,
                                        barHeight,
                                        8,
                                        8);
                }

                // Actual health fill.
                Color healthColor;

                if (actualRatio > 0.60) {
                        healthColor = Color.rgb(50, 220, 125);
                } else if (actualRatio > 0.30) {
                        healthColor = Color.rgb(255, 175, 55);
                } else {
                        healthColor = Color.rgb(255, 65, 75);
                }

                double healthWidth = barWidth * actualRatio;

                gc.setFill(healthColor);

                if (reverse) {
                        gc.fillRoundRect(
                                        x + barWidth - healthWidth,
                                        y,
                                        healthWidth,
                                        barHeight,
                                        8,
                                        8);
                } else {
                        gc.fillRoundRect(
                                        x,
                                        y,
                                        healthWidth,
                                        barHeight,
                                        8,
                                        8);
                }

                gc.setStroke(Color.rgb(220, 225, 245, 0.7));
                gc.setLineWidth(1);

                gc.strokeRoundRect(
                                x,
                                y,
                                barWidth,
                                barHeight,
                                8,
                                8);

                // Display actual health numbers.
                gc.setFont(Font.font("Arial", 13));
                gc.setFill(Color.WHITE);

                gc.setTextAlign(
                                reverse
                                                ? TextAlignment.RIGHT
                                                : TextAlignment.LEFT);

                gc.fillText(
                                fighter.getName()
                                                + "  "
                                                + fighter.getHealth()
                                                + " / "
                                                + fighter.getMaxHealth(),
                                reverse ? x + barWidth : x,
                                y - 7);
        }

        private void drawKiBar(
                        GraphicsContext gc,
                        Fighter fighter,
                        double x,
                        double y,
                        double barWidth,
                        boolean reverse) {

                double barHeight = 13;

                double maxKi = Math.max(1, fighter.getMaxKi());

                double kiRatio = clamp(
                                fighter.getKi() / maxKi,
                                0,
                                1);

                boolean ready = fighter.isUltimateReady();

                // Pulsing golden outline when the ultimate is ready.
                if (ready) {
                        double pulse = 0.45 + 0.45
                                        * (0.5 + 0.5 * Math.sin(hudPulseTime * 5));

                        gc.setStroke(Color.rgb(255, 205, 65, pulse));
                        gc.setLineWidth(4);

                        gc.strokeRoundRect(
                                        x - 2,
                                        y - 2,
                                        barWidth + 4,
                                        barHeight + 4,
                                        9,
                                        9);
                }

                gc.setFill(Color.rgb(30, 32, 48));
                gc.fillRoundRect(
                                x,
                                y,
                                barWidth,
                                barHeight,
                                7,
                                7);

                gc.setFill(
                                ready
                                                ? Color.rgb(255, 205, 65)
                                                : Color.rgb(65, 170, 255));

                double fillWidth = barWidth * kiRatio;

                if (reverse) {
                        gc.fillRoundRect(
                                        x + barWidth - fillWidth,
                                        y,
                                        fillWidth,
                                        barHeight,
                                        7,
                                        7);
                } else {
                        gc.fillRoundRect(
                                        x,
                                        y,
                                        fillWidth,
                                        barHeight,
                                        7,
                                        7);
                }

                gc.setStroke(
                                ready
                                                ? Color.rgb(255, 225, 130)
                                                : Color.rgb(130, 190, 255, 0.8));

                gc.setLineWidth(1);

                gc.strokeRoundRect(
                                x,
                                y,
                                barWidth,
                                barHeight,
                                7,
                                7);

                gc.setFont(Font.font("Arial", 10));
                gc.setFill(Color.WHITE);

                gc.setTextAlign(
                                reverse
                                                ? TextAlignment.RIGHT
                                                : TextAlignment.LEFT);

                gc.fillText(
                                "MYSTIC ENERGY  "
                                                + (int) fighter.getKi() + "%",
                                reverse ? x + barWidth : x,
                                y - 4);
        }

        private void drawRoundPips(
                        GraphicsContext gc,
                        double centerX,
                        double y,
                        int roundsWon,
                        boolean reverse) {

                double radius = 5;
                double spacing = 15;

                for (int i = 0; i < 2; i++) {
                        double offset = i * spacing;

                        double x = reverse
                                        ? centerX + offset
                                        : centerX - offset;

                        if (i < roundsWon) {
                                gc.setFill(Color.rgb(255, 205, 65));
                                gc.setStroke(Color.rgb(255, 235, 160));
                        } else {
                                gc.setFill(Color.rgb(45, 48, 65));
                                gc.setStroke(Color.rgb(130, 135, 160));
                        }

                        gc.setLineWidth(1.2);

                        gc.fillOval(
                                        x - radius,
                                        y - radius,
                                        radius * 2,
                                        radius * 2);

                        gc.strokeOval(
                                        x - radius,
                                        y - radius,
                                        radius * 2,
                                        radius * 2);
                }
        }

        private void drawUltimateLabel(
                        GraphicsContext gc,
                        Fighter fighter,
                        double x,
                        double y,
                        boolean rightAligned) {

                boolean ready = fighter.isUltimateReady();

                gc.setFont(Font.font("Arial", 12));

                gc.setTextAlign(
                                rightAligned
                                                ? TextAlignment.RIGHT
                                                : TextAlignment.LEFT);

                if (ready) {
                        double pulse = 0.65 + 0.35
                                        * (0.5 + 0.5 * Math.sin(hudPulseTime * 5));

                        gc.setFill(Color.rgb(255, 215, 80, pulse));

                        gc.fillText(
                                        rightAligned
                                                        ? "ULTIMATE READY  [;]"
                                                        : "ULTIMATE READY  [H]",
                                        x,
                                        y);
                } else {
                        gc.setFill(Color.rgb(175, 185, 210));

                        gc.fillText(
                                        rightAligned
                                                        ? "ULTIMATE  [;]"
                                                        : "ULTIMATE  [H]",
                                        x,
                                        y);
                }
        }

        /**
         * Display the current combo once a player lands
         * at least two consecutive hits.
         */
        private void drawComboCounter(
                        GraphicsContext gc,
                        int comboCount,
                        double x,
                        double y,
                        boolean rightAligned) {

                if (comboCount < 2) {
                        return;
                }

                gc.save();

                try {
                        gc.setTextAlign(
                                        rightAligned
                                                        ? TextAlignment.RIGHT
                                                        : TextAlignment.LEFT);

                        gc.setFont(Font.font("Arial", 17));
                        gc.setFill(Color.rgb(255, 205, 65));

                        gc.fillText(
                                        comboCount + " HIT COMBO!",
                                        x,
                                        y);
                } finally {
                        gc.restore();
                }
        }

        /**
         * Draw the Ultimate Incantation flash and title panel.
         */
        private void drawUltimateCinematic(GraphicsContext gc) {
                if (ultimateCinematicTimer <= 0
                                && ultimateFlashTimer <= 0) {
                        return;
                }

                gc.save();

                try {
                        double width = Constants.WIDTH;
                        double height = Constants.HEIGHT;
                        double centerX = width / 2.0;

                        // Gold-white activation flash.
                        if (ultimateFlashTimer > 0) {
                                double flashProgress = ultimateFlashTimer
                                                / ULTIMATE_FLASH_DURATION;

                                double flashAlpha = 0.38 * flashProgress;

                                gc.setFill(Color.rgb(
                                                255,
                                                238,
                                                175,
                                                flashAlpha));

                                gc.fillRect(
                                                0,
                                                0,
                                                width,
                                                height);
                        }

                        if (ultimateCinematicTimer <= 0) {
                                return;
                        }

                        // Smooth fade-in and fade-out.
                        double fadeIn = Math.min(
                                        1.0,
                                        (ULTIMATE_CINEMATIC_DURATION
                                                        - ultimateCinematicTimer) / 0.12);

                        double fadeOut = Math.min(
                                        1.0,
                                        ultimateCinematicTimer / 0.25);

                        double alpha = Math.max(
                                        0,
                                        Math.min(fadeIn, fadeOut));

                        // Subtle dark overlay.
                        gc.setFill(Color.rgb(
                                        4,
                                        5,
                                        16,
                                        0.24 * alpha));

                        gc.fillRect(
                                        0,
                                        0,
                                        width,
                                        height);

                        // Cinematic top and bottom borders.
                        gc.setFill(Color.rgb(
                                        5,
                                        6,
                                        18,
                                        0.88 * alpha));

                        gc.fillRect(0, 0, width, 24);
                        gc.fillRect(0, height - 24, width, 24);

                        gc.setFill(Color.rgb(
                                        255,
                                        205,
                                        65,
                                        0.85 * alpha));

                        gc.fillRect(0, 23, width, 2);
                        gc.fillRect(0, height - 25, width, 2);

                        // Central announcement panel.
                        double panelWidth = Math.min(
                                        width - 60,
                                        760);

                        double panelHeight = 112;

                        double panelX = centerX - panelWidth / 2.0;
                        double panelY = height * 0.20;

                        gc.setFill(Color.rgb(
                                        12,
                                        13,
                                        30,
                                        0.94 * alpha));

                        gc.fillRoundRect(
                                        panelX,
                                        panelY,
                                        panelWidth,
                                        panelHeight,
                                        18,
                                        18);

                        gc.setStroke(Color.rgb(
                                        255,
                                        205,
                                        65,
                                        alpha));

                        gc.setLineWidth(2);

                        gc.strokeRoundRect(
                                        panelX,
                                        panelY,
                                        panelWidth,
                                        panelHeight,
                                        18,
                                        18);

                        // Decorative gold accents.
                        gc.setFill(Color.rgb(
                                        255,
                                        220,
                                        110,
                                        alpha));

                        gc.fillRect(
                                        panelX + 28,
                                        panelY + 18,
                                        5,
                                        panelHeight - 36);

                        gc.fillRect(
                                        panelX + panelWidth - 33,
                                        panelY + 18,
                                        5,
                                        panelHeight - 36);

                        // Ultimate title.
                        gc.setTextAlign(TextAlignment.CENTER);

                        gc.setFont(Font.font("Arial", 30));
                        gc.setFill(Color.rgb(
                                        255,
                                        218,
                                        105,
                                        alpha));

                        gc.fillText(
                                        "ULTIMATE INCANTATION",
                                        centerX,
                                        panelY + 43);

                        // Fighter name.
                        gc.setFont(Font.font("Arial", 17));
                        gc.setFill(Color.rgb(
                                        245,
                                        245,
                                        255,
                                        alpha));

                        gc.fillText(
                                        "PLAYER " + ultimatePlayer
                                                        + "  -  " + ultimateFighterName,
                                        centerX,
                                        panelY + 76);

                        // Subtitle.
                        gc.setFont(Font.font("Arial", 11));
                        gc.setFill(Color.rgb(
                                        175,
                                        185,
                                        220,
                                        alpha));

                        gc.fillText(
                                        "MYSTIC ENERGY RELEASED",
                                        centerX,
                                        panelY + 96);

                } finally {
                        gc.restore();
                }
        }

        private void drawRoundMessage(GraphicsContext gc) {
                gc.setFill(Color.rgb(0, 0, 0, 0.78));

                gc.fillRect(
                                0,
                                0,
                                Constants.WIDTH,
                                Constants.HEIGHT);

                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFont(Font.font("Arial", 34));
                gc.setFill(Color.rgb(255, 215, 90));

                gc.fillText(
                                roundMessage,
                                Constants.WIDTH / 2.0,
                                Constants.HEIGHT / 2.0 - 25);

                gc.setFont(Font.font("Arial", 18));
                gc.setFill(Color.WHITE);

                boolean matchFinished = p1RoundsWon >= 2 || p2RoundsWon >= 2;

                gc.fillText(
                                matchFinished
                                                ? "Press ENTER to view results"
                                                : "Press ENTER to continue",
                                Constants.WIDTH / 2.0,
                                Constants.HEIGHT / 2.0 + 25);
        }

        private double clamp(
                        double value,
                        double min,
                        double max) {

                return Math.max(
                                min,
                                Math.min(max, value));
        }
}
