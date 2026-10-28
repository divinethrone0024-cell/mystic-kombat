package mystic.engine;

import java.util.EnumMap;
import java.util.Map;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.common.Constants;
import mystic.ai.Difficulty;
import mystic.common.GameMode;
import mystic.common.GameState;
import mystic.common.Roster;
import mystic.fighters.CyberHunter;
import mystic.fighters.Fighter;
import mystic.fighters.FlameKarateka;
import mystic.fighters.MysticMage;
import mystic.fighters.PhantomBlade;
import mystic.fighters.PlumberBrawler;
import mystic.fighters.ShadowVigilante;
import mystic.fighters.SpeedsterSonic;
import mystic.fighters.StarSage;
import mystic.fighters.StreetStriker;
import mystic.fighters.ToxicToad;

/**
 * Manages the game loop, screen switching (with transitions),
 * game mode, difficulty, arena and character selection,
 * time scaling, match results and persistent progression.
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

    public static final int ARCADE_TRIALS = 10;
    private int arcadeTrial = 0;

    // Opponent (Roster index) and arena for each Arcade trial. Edit freely.
    private static final int[] ARCADE_OPPONENTS = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 1 };
    private static final int[] ARCADE_ARENAS = { 0, 1, 2, 3, 0, 1, 2, 3, 0, 3 };

    public static final String[] TRIAL_NAMES = {
            "The Mirror Gate", "The Ember Road", "The Silent Vault",
            "The Chaos Rift", "The Crimson Spire", "The Hollow Court",
            "The Storm Bridge", "The Blood Eclipse", "The Last Sanctum",
            "The Final Incursion"
    };

    // ---------------- PROGRESSION ----------------
    private final ProgressionManager progression = new ProgressionManager();
    private int lastXpGained = 0;

    /** Save ids, in the same order as Roster.HEROES. */
    public static final String[] FIGHTER_IDS = {
            "mage", "vigilante", "sonic", "plumber", "karateka",
            "hunter", "striker", "sage", "blade", "toad"
    };

    public ProgressionManager getProgression() {
        return progression;
    }

    public static String fighterId(int choice) {
        return FIGHTER_IDS[Math.floorMod(choice, FIGHTER_IDS.length)];
    }

    public boolean isChoiceUnlocked(int choice) {
        return progression.isUnlocked(fighterId(choice));
    }

    /** XP earned by the most recent Arcade result, for the result screen. */
    public int getLastXpGained() {
        return lastXpGained;
    }

    // ---------------- MATCH RESULTS ----------------
    private String matchWinner = "";
    private int player1MatchWins;
    private int player2MatchWins;

    /** 1.0 = normal speed; 0.0 = frozen. */
    private double timeScale = 1.0;

    private long lastNanos = 0;
    private AnimationTimer gameTimer;

    // Choice index matches Roster.HEROES.
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

    public InputManager getInputManager() {
        return input;
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

        input.setPadMenuMode(true);
        input.setCaptureMode(false);

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
        return mode == GameMode.SOLO || mode == GameMode.ARCADE;
    }

    public int getArena() {
        return arena;
    }

    public void setArena(int arena) {
        this.arena = Math.max(0, arena);
    }

    // ---------------- ARCADE ----------------

    public int getArcadeTrial() {
        return arcadeTrial;
    }

    public void setArcadeTrial(int t) {
        arcadeTrial = Math.max(0, Math.min(t, ARCADE_TRIALS - 1));
    }

    /** Number of trials cleared (the next one is also playable). */
    public int getArcadeUnlocked() {
        return progression.getArcadeCleared();
    }

    public int getArcadeOpponent(int trial) {
        return ARCADE_OPPONENTS[Math.floorMod(trial, ARCADE_TRIALS)];
    }

    public Difficulty arcadeDifficulty(int trial) {
        if (trial < 3)
            return Difficulty.EASY;
        if (trial < 7)
            return Difficulty.MEDIUM;
        return Difficulty.HARD;
    }

    /** Sets mode, opponent, arena and difficulty for the current trial. */
    public void configureArcadeTrial() {
        setMode(GameMode.ARCADE);
        setDifficulty(arcadeDifficulty(arcadeTrial));
        setPlayer2Choice(ARCADE_OPPONENTS[arcadeTrial]);
        setArena(ARCADE_ARENAS[arcadeTrial]);
    }

    /** Call once when an Arcade match finishes. Awards XP and saves. */
    public void reportArcadeResult(boolean playerWon) {
        lastXpGained = playerWon
                ? progression.awardArcadeWin(arcadeTrial)
                : 0;
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
        player1Choice = Math.floorMod(choice, Roster.HEROES.length);
    }

    public void setPlayer2Choice(int choice) {
        player2Choice = Math.floorMod(choice, Roster.HEROES.length);
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

        // Upgrades and locks apply to the human player in Arcade and Solo.
        boolean progressionMode = (player == 1)
                && (mode == GameMode.ARCADE || mode == GameMode.SOLO);

        if (progressionMode && !isChoiceUnlocked(choice)) {
            choice = 0; // locked fighter: fall back to the starter
            player1Choice = 0;
        }

        Fighter f = switch (choice) {
            case 1 -> new ShadowVigilante(player, startX);
            case 2 -> new SpeedsterSonic(player, startX);
            case 3 -> new PlumberBrawler(player, startX);
            case 4 -> new FlameKarateka(player, startX);
            case 5 -> new CyberHunter(player, startX);
            case 6 -> new StreetStriker(player, startX);
            case 7 -> new StarSage(player, startX);
            case 8 -> new PhantomBlade(player, startX);
            case 9 -> new ToxicToad(player, startX);
            default -> new MysticMage(player, startX);
        };

        if (progressionMode) {
            String id = fighterId(choice);
            f.applyUpgrades(
                    progression.getMultiplier(id, ProgressionManager.Stat.HEALTH),
                    progression.getMultiplier(id, ProgressionManager.Stat.DAMAGE),
                    progression.getMultiplier(id, ProgressionManager.Stat.SPEED));
        }

        return f;
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
        input.poll();
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
            input.shutdown();
        }

        input.clear();
        lastNanos = 0;
        timeScale = 1.0;
    }
}