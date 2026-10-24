
package mystic.engine;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import mystic.common.Action;

/**
 * Tracks keyboard input for both players.
 * Supports movement, jumping, crouching, blocking, attacks,
 * dashing and dodging.
 */
public class InputManager {

    private final Set<KeyCode> held = new HashSet<>();
    private final Set<KeyCode> pressedThisFrame = new HashSet<>();

    private final Map<Action, KeyCode> p1 = new EnumMap<>(Action.class);
    private final Map<Action, KeyCode> p2 = new EnumMap<>(Action.class);

    public InputManager() {
        // Player 1 controls
        p1.put(Action.LEFT, KeyCode.A);
        p1.put(Action.RIGHT, KeyCode.D);
        p1.put(Action.JUMP, KeyCode.W);
        p1.put(Action.CROUCH, KeyCode.S);
        p1.put(Action.BLOCK, KeyCode.Q);
        p1.put(Action.LIGHT, KeyCode.F);
        p1.put(Action.HEAVY, KeyCode.G);
        p1.put(Action.ULTIMATE, KeyCode.H);
        p1.put(Action.DASH, KeyCode.E);
        p1.put(Action.DODGE, KeyCode.R);

        // Player 2 controls
        p2.put(Action.LEFT, KeyCode.LEFT);
        p2.put(Action.RIGHT, KeyCode.RIGHT);
        p2.put(Action.JUMP, KeyCode.UP);
        p2.put(Action.CROUCH, KeyCode.DOWN);
        p2.put(Action.BLOCK, KeyCode.PERIOD);
        p2.put(Action.LIGHT, KeyCode.K);
        p2.put(Action.HEAVY, KeyCode.L);
        p2.put(Action.ULTIMATE, KeyCode.SEMICOLON);
        p2.put(Action.DASH, KeyCode.SHIFT);
        p2.put(Action.DODGE, KeyCode.SLASH);
    }

    /**
     * Registers keyboard press and release events.
     */
    public void attach(Scene scene) {
        scene.setOnKeyPressed(event -> {
            KeyCode key = event.getCode();

            if (held.add(key)) {
                pressedThisFrame.add(key);
            }
        });

        scene.setOnKeyReleased(event -> {
            held.remove(event.getCode());
        });
    }

    /**
     * Checks whether a player is holding an action key.
     *
     * @param player player number: 1 or 2
     * @param action action to check
     */
    public boolean isDown(int player, Action action) {
        KeyCode key = getControls(player).get(action);
        return key != null && held.contains(key);
    }

    /**
     * Checks whether a keyboard key is currently held.
     */
    public boolean isKeyDown(KeyCode key) {
        return held.contains(key);
    }

    /**
     * Checks whether a key was pressed during this frame.
     */
    public boolean wasPressed(KeyCode key) {
        return pressedThisFrame.contains(key);
    }

    /**
     * Checks whether a player's action key was newly pressed.
     */
    public boolean wasPressed(int player, Action action) {
        KeyCode key = getControls(player).get(action);
        return key != null && pressedThisFrame.contains(key);
    }

    /**
     * Returns the correct control map for a player.
     */
    private Map<Action, KeyCode> getControls(int player) {
        return player == 1 ? p1 : p2;
    }

    /**
     * Clears input when the game needs to reset keyboard state.
     */
    public void clear() {
        held.clear();
        pressedThisFrame.clear();
    }

    /**
     * Call once at the end of every game frame.
     */
    public void endFrame() {
        pressedThisFrame.clear();
    }
}
