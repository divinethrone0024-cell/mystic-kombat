
package mystic.engine;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import mystic.common.Action;

/**
 * Tracks keyboard input for both players.
 *
 * Supports movement, jumping, crouching, blocking, attacks,
 * dashing, dodging, launcher attacks, and ultimate attacks.
 *
 * Player 1 uses WASD and letter keys.
 * Player 2 uses arrow keys and the number row.
 */
public class InputManager {

    private final Set<KeyCode> held = new HashSet<>();
    private final Set<KeyCode> pressedThisFrame = new HashSet<>();

    private final Map<Action, KeyCode> p1 = new EnumMap<>(Action.class);

    private final Map<Action, KeyCode> p2 = new EnumMap<>(Action.class);

    public InputManager() {
        configureDefaultControls();
    }

    /**
     * Default controls for both players.
     */
    private void configureDefaultControls() {

        // ================ PLAYER 1 ================
        // A / D : Move
        // W : Jump
        // S : Crouch
        // J : Light attack
        // K : Heavy attack
        // F : Block
        // L : Dash
        // I : Dodge
        // H : Launcher
        // U : Ultimate

        p1.put(Action.LEFT, KeyCode.A);
        p1.put(Action.RIGHT, KeyCode.D);
        p1.put(Action.JUMP, KeyCode.W);
        p1.put(Action.CROUCH, KeyCode.S);

        p1.put(Action.LIGHT, KeyCode.J);
        p1.put(Action.HEAVY, KeyCode.K);
        p1.put(Action.BLOCK, KeyCode.F);

        p1.put(Action.DASH, KeyCode.L);
        p1.put(Action.DODGE, KeyCode.I);
        p1.put(Action.LAUNCHER, KeyCode.H);
        p1.put(Action.ULTIMATE, KeyCode.U);

        // ================ PLAYER 2 ================
        // LEFT / RIGHT : Move
        // UP : Jump
        // DOWN : Crouch
        // 1 : Light attack
        // 2 : Heavy attack
        // 4 : Block
        // 3 : Dash
        // 6 : Dodge
        // 5 : Launcher
        // 0 : Ultimate
        //
        // Uses the number row above the letters, not the numpad.

        p2.put(Action.LEFT, KeyCode.LEFT);
        p2.put(Action.RIGHT, KeyCode.RIGHT);
        p2.put(Action.JUMP, KeyCode.UP);
        p2.put(Action.CROUCH, KeyCode.DOWN);

        p2.put(Action.LIGHT, KeyCode.DIGIT1);
        p2.put(Action.HEAVY, KeyCode.DIGIT2);
        p2.put(Action.BLOCK, KeyCode.DIGIT4);

        p2.put(Action.DASH, KeyCode.DIGIT3);
        p2.put(Action.DODGE, KeyCode.DIGIT6);
        p2.put(Action.LAUNCHER, KeyCode.DIGIT5);
        p2.put(Action.ULTIMATE, KeyCode.DIGIT0);
    }

    /**
     * Registers keyboard press and release events.
     *
     * Event filters allow this manager to track input without
     * replacing existing Scene key handlers.
     */
    public void attach(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException(
                    "Scene cannot be null.");
        }

        scene.addEventFilter(
                KeyEvent.KEY_PRESSED,
                event -> {
                    KeyCode key = event.getCode();

                    // Add to pressedThisFrame only on the initial press.
                    if (held.add(key)) {
                        pressedThisFrame.add(key);
                    }
                });

        scene.addEventFilter(
                KeyEvent.KEY_RELEASED,
                event -> {
                    held.remove(event.getCode());
                });

        // Prevent stuck keys when the window loses focus.
        scene.windowProperty().addListener(
                (observable, oldWindow, newWindow) -> {
                    if (oldWindow != null) {
                        oldWindow.focusedProperty()
                                .removeListener(focusListener);
                    }

                    if (newWindow != null) {
                        newWindow.focusedProperty()
                                .addListener(focusListener);
                    }

                    clear();
                });

        if (scene.getWindow() != null) {
            scene.getWindow().focusedProperty()
                    .addListener(focusListener);
        }
    }

    private final javafx.beans.value.ChangeListener<Boolean> focusListener = (observable, wasFocused, isFocused) -> {
        if (!isFocused) {
            clear();
        }
    };

    /**
     * Checks whether an action's key is currently held.
     */
    public boolean isDown(int player, Action action) {
        KeyCode key = getControls(player).get(action);
        return key != null && held.contains(key);
    }

    /**
     * Checks whether a raw keyboard key is held.
     */
    public boolean isKeyDown(KeyCode key) {
        return held.contains(key);
    }

    /**
     * Checks whether a raw keyboard key was newly pressed this frame.
     */
    public boolean wasPressed(KeyCode key) {
        return pressedThisFrame.contains(key);
    }

    /**
     * Checks whether an action key was newly pressed this frame.
     */
    public boolean wasPressed(int player, Action action) {
        KeyCode key = getControls(player).get(action);
        return key != null && pressedThisFrame.contains(key);
    }

    /**
     * Gets the control map for the requested player.
     */
    private Map<Action, KeyCode> getControls(int player) {
        if (player == 1) {
            return p1;
        }

        if (player == 2) {
            return p2;
        }

        throw new IllegalArgumentException(
                "Player must be 1 or 2, got: " + player);
    }

    /**
     * Reassigns a control.
     */
    public void setControl(
            int player,
            Action action,
            KeyCode key) {

        if (action == null || key == null) {
            throw new IllegalArgumentException(
                    "Action and key cannot be null.");
        }

        getControls(player).put(action, key);
    }

    /**
     * Gets the key assigned to an action.
     */
    public KeyCode getControl(int player, Action action) {
        return getControls(player).get(action);
    }

    /**
     * Clears all held and newly pressed keys.
     */
    public void clear() {
        held.clear();
        pressedThisFrame.clear();
    }

    /**
     * Call exactly once after processing each game frame.
     */
    public void endFrame() {
        pressedThisFrame.clear();
    }
}
