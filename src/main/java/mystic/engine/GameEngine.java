
package mystic.engine;

import java.util.EnumMap;
import java.util.Map;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.fighters.Fighter;
import mystic.fighters.MysticMage;
import mystic.fighters.ShadowVigilante;

/**
 * The heart of the game: manages the game loop,
 * screen switching, and selected fighters.
 */
public class GameEngine {

    private final GraphicsContext gc;
    private final InputManager input;

    private final Map<GameState, Screen> screens = new EnumMap<>(GameState.class);

    private GameState state = GameState.MENU;

    /** 1.0 = normal speed; below 1.0 = slow motion. */
    private double timeScale = 1.0;

    private long lastNanos = 0;

    // Character selection:
    // 0 = Mystic Mage
    // 1 = Shadow Vigilante
    private int player1Choice = 0;
    private int player2Choice = 1;

    public GameEngine(GraphicsContext gc, InputManager input) {
        this.gc = gc;
        this.input = input;
    }

    /** Register a screen for a particular game state. */
    public void register(GameState state, Screen screen) {
        screens.put(state, screen);
    }

    public GameState getState() {
        return state;
    }

    /** Switch to another screen and initialize it. */
    public void changeState(GameState next) {
        state = next;

        Screen screen = screens.get(state);

        if (screen != null) {
            screen.onEnter();
        }
    }

    public double getTimeScale() {
        return timeScale;
    }

    public void setTimeScale(double timeScale) {
        this.timeScale = Math.max(0.0, timeScale);
    }

    // ---------------- CHARACTER SELECTION ----------------

    public int getPlayer1Choice() {
        return player1Choice;
    }

    public int getPlayer2Choice() {
        return player2Choice;
    }

    public void setPlayer1Choice(int choice) {
        player1Choice = Math.floorMod(choice, 2);
    }

    public void setPlayer2Choice(int choice) {
        player2Choice = Math.floorMod(choice, 2);
    }

    /**
     * Creates a fresh fighter using the selected character.
     *
     * @param player 1 or 2
     * @param startX starting horizontal position
     */
    public Fighter createFighter(int player, double startX) {
        int choice = (player == 1)
                ? player1Choice
                : player2Choice;

        if (choice == 0) {
            return new MysticMage(player, startX);
        }

        return new ShadowVigilante(player, startX);
    }

    // ---------------- GAME LOOP ----------------

    /** Start the JavaFX game loop. */
    public void start() {
        changeState(GameState.MENU);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) {
                    lastNanos = now;
                    return;
                }

                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;

                // Avoid a large update after a pause or lag spike.
                dt = Math.min(dt, 0.05);

                tick(dt);
            }
        }.start();
    }

    /** Update and render the currently active screen. */
    private void tick(double dt) {
        Screen screen = screens.get(state);

        if (screen != null) {
            screen.update(dt * timeScale, input);
        }

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);

        if (screen != null) {
            screen.render(gc);
        }

        input.endFrame();
    }
}
