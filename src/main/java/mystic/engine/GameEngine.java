package mystic.engine;

import java.util.EnumMap;
import java.util.Map;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import mystic.common.Constants;
import mystic.common.GameState;

/** The heart of the game: the loop and the screen switching. */
public class GameEngine {
    private final GraphicsContext gc;
    private final InputManager input;
    private final Map<GameState, Screen> screens = new EnumMap<>(GameState.class);
    private GameState state = GameState.MENU;

    /** 1.0 = normal speed. Below 1.0 = slow motion (used by the ultimate later). */
    private double timeScale = 1.0;

    private long lastNanos = 0;

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
        this.timeScale = timeScale;
    }

    /** Start the game loop. */
    public void start() {
        changeState(GameState.MENU);
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (lastNanos == 0) { // first frame: no time has passed yet
                    lastNanos = now;
                    return;
                }
                double dt = (now - lastNanos) / 1_000_000_000.0;
                lastNanos = now;
                dt = Math.min(dt, 0.05); // never jump more than 50 ms (e.g. after a lag)
                tick(dt);
            }
        }.start();
    }

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