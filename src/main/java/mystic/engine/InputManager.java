package mystic.engine;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import mystic.common.Action;

/**
 * Remembers which keys are held down. The game asks it every frame.
 * Player 1 and Player 2 have separate key maps.
 */
public class InputManager {
    private final Set<KeyCode> held = new HashSet<>();
    private final Set<KeyCode> pressedThisFrame = new HashSet<>();
    private final Map<Action, KeyCode> p1 = new EnumMap<>(Action.class);
    private final Map<Action, KeyCode> p2 = new EnumMap<>(Action.class);

    public InputManager() {
        p1.put(Action.LEFT, KeyCode.A);
        p1.put(Action.RIGHT, KeyCode.D);
        p1.put(Action.JUMP, KeyCode.W);
        p1.put(Action.CROUCH, KeyCode.S);
        p1.put(Action.LIGHT, KeyCode.F);
        p1.put(Action.HEAVY, KeyCode.G);
        p1.put(Action.ULTIMATE, KeyCode.H);

        p2.put(Action.LEFT, KeyCode.LEFT);
        p2.put(Action.RIGHT, KeyCode.RIGHT);
        p2.put(Action.JUMP, KeyCode.UP);
        p2.put(Action.CROUCH, KeyCode.DOWN);
        p2.put(Action.LIGHT, KeyCode.K);
        p2.put(Action.HEAVY, KeyCode.L);
        p2.put(Action.ULTIMATE, KeyCode.SEMICOLON);
    }

    /** Connect to a scene so key presses and releases are recorded. */
    public void attach(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (held.add(e.getCode())) { // true only on the first press
                pressedThisFrame.add(e.getCode());
            }
        });
        scene.setOnKeyReleased(e -> held.remove(e.getCode()));
    }

    /**
     * Is this player holding the key for this action right now? (player is 1 or 2)
     */
    public boolean isDown(int player, Action action) {
        KeyCode key = (player == 1 ? p1 : p2).get(action);
        return key != null && held.contains(key);
    }

    /** Is this raw key held down? */
    public boolean isKeyDown(KeyCode key) {
        return held.contains(key);
    }

    /**
     * Was this key pressed for the first time during this frame? (good for menus)
     */
    public boolean wasPressed(KeyCode key) {
        return pressedThisFrame.contains(key);
    }

    /** Forget all held keys, e.g. when the window loses focus. */
    public void clear() {
        held.clear();
        pressedThisFrame.clear();
    }

    /** Call once at the end of every frame. */
    public void endFrame() {
        pressedThisFrame.clear();
    }
}