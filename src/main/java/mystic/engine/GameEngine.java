package mystic.engine;

import java.util.EnumMap;
import java.util.Map;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.common.Constants;
import mystic.common.Difficulty;
import mystic.common.GameMode;
import mystic.common.GameState;
import mystic.common.Roster;
import mystic.fighters.Fighter;
import mystic.fighters.MysticMage;
import mystic.fighters.ShadowVigilante;
import mystic.fighters.SpeedsterSonic;

/**
 * Manages the game loop, screen switching (with transitions),
 * game mode, difficulty, arena and character selection,
 * time scaling, and match results.
 */
public class GameEngine {

    private final GraphicsContext gc;
    private final InputManager input;

    private final Map<GameState, Screen> screens = new EnumMap<>(GameState.class);
    private final Transition transition = new Transition();

    private GameState state = GameState.MENU;

    // ---------------- MODE / DIFFICULTY / ARENA ----------------
    private GameMode mode = GameMode.LOCAL_2P;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private int arena = 0;

    // ---------------- MATCH RESULTS ----------------
    private String matchWinner = "";
    private int player1MatchWins;
    private int player2MatchWins;

    /** 1.0 = normal speed; 0.0 = frozen. */
    private double timeScale = 1.0;

    private long lastNanos = 0;
    private AnimationTimer gameTimer;

    // 0 = Mystic Mage, 1 = Shadow Vigilante, 2 = Speedster Sonic, 3 = Plumber
    // Brawler
    private int player1Choice = 0;
    private int player2Choice = 1;

    public GameEngine(GraphicsContext gc, InputManager input) {
        this.gc = gc;
        this.input = input;
    }

    public void register(GameState state, Screen screen) {
        screens.put(state, screen);
    }

    public GameState getState() {
        return state;
    }

    // ---------------- SCREEN SWITCHING ----------------

    /** Switch screens with the shard-wipe transition. */
    public void changeState(GameState next) {
        if (next == null || transition.isActive()) {
            return;
        }
        // At startup there is nothing to wipe from.
        if (gameTimer == null || screens.get(state) == null) {
            applyState(next);
            return;
        }
        transition.start(() -> applyState(next));
    }

    private void applyState(GameState next) {
        state = next;

        // Prevent slow motion from leaking into another screen.
        timeScale = 1.0;
        lastNanos = 0;

        Screen screen = screens.get(state);
        if (screen != null) {
            screen.onEnter();
        }
    }

    // ---------------- MODE / DIFFICULTY / ARENA API ----------------

    public GameMode getMode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = (mode == null) ? GameMode.LOCAL_2P : mode;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = (difficulty == null) ? Difficulty.MEDIUM : difficulty;
    }

    /** True when player 2 should be controlled by the AI. */
    public boolean isVsAI() {
        return mode == GameMode.SOLO;
    }

    public int getArena() {
        return arena;
    }

    public void setArena(int arena) {
        this.arena = Math.max(0, arena);
    }

    // ---------------- TIME SCALE ----------------

    public double getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(double timeScale) {
        if (!Double.isFinite(timeScale)) {
            this.timeScale = 1.0;
            return;
        }
        this.timeScale = Math.max(0.0, Math.min(timeScale, 1.0));
    }

    // ---------------- MATCH RESULTS ----------------

    public void setMatchResult(String winner, int player1Wins, int player2Wins) {
        this.matchWinner = winner;
        this.player1MatchWins = player1Wins;
        this.player2MatchWins = player2Wins;
    }

    public String getMatchWinner() {
        return matchWinner;
    }

    public int getPlayer1MatchWins() {
        return player1MatchWins;
    }

    public int getPlayer2MatchWins() {
        return player2MatchWins;
    }

    // ---------------- CHARACTER SELECTION ----------------

    public int getPlayer1Choice() {
        return player1Choice;
    }

    public int getPlayer2Choice() {
        return player2Choice;
    }

    public void setPlayer1Choice(int choice) {
        player1Choice = Math.floorMod(choice, Roster.PLAYABLE);
    }

    public void setPlayer2Choice(int choice) {
        player2Choice = Math.floorMod(choice, Roster.PLAYABLE);
    }

    public Fighter createFighter(int player, double startX) {
        int choice;

        if (player == 1) {
            choice = player1Choice;
        } else if (player == 2) {
            choice = player2Choice;
        } else {
            throw new IllegalArgumentException(
                    "Player must be 1 or 2, got: " + player);
        }

        if (choice == 0) {
            return new MysticMage(player, startX);
        }
        if (choice == 1) {
            return new ShadowVigilante(player, startX);
        }
        if (choice == 2) {
            return new SpeedsterSonic(player, startX);
        }

        // Choice 3 (Plumber Brawler): enable once PlumberBrawler.java exists
        // and Roster.PLAYABLE is 4. Until then, fall back safely.
        // return new mystic.fighters.PlumberBrawler(player, startX);
        return new MysticMage(player, startX);
    }

    // ---------------- GAME LOOP ----------------

    public void start() {
        if (gameTimer != null) {
            return;
        }

        changeState(GameState.SPLASH);

        gameTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                    return;
                }

                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;

                // Avoid large physics steps after lag or pauses.
                dt = Math.max(0.0, Math.min(dt, 0.05));

                tick(dt);
            }
        };

        gameTimer.start();
    }

    private void tick(double dt) {
        Screen screen = screens.get(state);

        // Freeze screen logic during a transition so keys can't fire twice.
        if (screen != null && !transition.isActive()) {
            screen.update(dt * timeScale, input);
        }
        transition.update(dt);

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        // Render using real frames even during slow motion.
        if (screen != null) {
            screen.render(gc);
        }
        transition.render(gc);

        input.endFrame();
    }

    /** Stop the game loop when shutting down the game. */
    public void stop() {
        if (gameTimer != null) {
            gameTimer.stop();
            gameTimer = null;
        }

        input.clear();
        lastNanos = 0;
        timeScale = 1.0;
    }
}