package mystic.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import mystic.common.Action;

/**
 * Keyboard + game-controller input with rebindable controls.
 *
 * - Every action can be bound to ANY keyboard key and/or a controller button.
 * - Bindings are saved to ~/.mystic-kombat-controls.properties.
 * - If the controller library is missing, the game still runs (keyboard only).
 */
public class InputManager {

    /** Controller buttons that can be bound to actions. */
    public enum PadButton {
        A, B, X, Y, LB, RB, LT, RT, BACK
    }

    private static final Path SAVE_FILE = Path.of(System.getProperty("user.home"),
            ".mystic-kombat-controls.properties");

    private static final double STICK_THRESHOLD = 0.5;
    private static final double TRIGGER_THRESHOLD = 0.5;
    /** Flip this if pushing the stick UP makes the character crouch. */
    private static final boolean STICK_Y_UP_IS_POSITIVE = true;

    private final Set<KeyCode> held = new HashSet<>();
    private final Set<KeyCode> pressedThisFrame = new LinkedHashSet<>();

    private final Map<Action, KeyCode> p1 = new EnumMap<>(Action.class);
    private final Map<Action, KeyCode> p2 = new EnumMap<>(Action.class);

    private final Map<Action, PadButton> pad1 = new EnumMap<>(Action.class);
    private final Map<Action, PadButton> pad2 = new EnumMap<>(Action.class);

    // Which controller drives which player (-1 = keyboard only). Index 0 unused.
    private final int[] padIndex = { -1, 0, 1 };

    // AI/controller-generated actions, separate from physical keyboard input.
    private final Map<Action, Boolean> virtualP1 = new EnumMap<>(Action.class);
    private final Map<Action, Boolean> virtualP2 = new EnumMap<>(Action.class);

    private final Map<Action, Boolean> virtualPressedP1 = new EnumMap<>(Action.class);
    private final Map<Action, Boolean> virtualPressedP2 = new EnumMap<>(Action.class);

    // Controller state.
    private GamepadSupport pads;
    private final boolean[][] padHeld = new boolean[3][Action.values().length];
    private final boolean[][] padPressed = new boolean[3][Action.values().length];
    private final boolean[][] captureHeld = new boolean[GamepadSupport.MAX][PadButton.values().length];
    private PadButton padCaptured;
    private boolean mUp, mDown, mLeft, mRight, mA, mB, mStart;

    /** When true, controller d-pad/stick/A/B act like menu keys. */
    private boolean padMenuMode = true;
    /** While rebinding, controller buttons must not act as menu keys. */
    private boolean captureMode = false;

    public InputManager() {
        applyDefaults(1);
        applyDefaults(2);
        loadBindings();

        try {
            pads = new GamepadSupport();
        } catch (Throwable t) {
            pads = null;
            System.err.println("Controller support disabled: " + t);
        }
    }

    // ------------------------------------------------------------ defaults

    private void applyDefaults(int player) {
        Map<Action, KeyCode> k = getControls(player);
        Map<Action, PadButton> b = getPadMap(player);
        k.clear();
        b.clear();

        if (player == 1) {
            k.put(Action.LEFT, KeyCode.A);
            k.put(Action.RIGHT, KeyCode.D);
            k.put(Action.JUMP, KeyCode.W);
            k.put(Action.CROUCH, KeyCode.S);
            k.put(Action.LIGHT, KeyCode.J);
            k.put(Action.HEAVY, KeyCode.K);
            k.put(Action.BLOCK, KeyCode.F);
            k.put(Action.DASH, KeyCode.L);
            k.put(Action.DODGE, KeyCode.I);
            k.put(Action.LAUNCHER, KeyCode.H);
            k.put(Action.ULTIMATE, KeyCode.U);
            k.put(Action.SPECIAL, KeyCode.O);
        } else {
            k.put(Action.LEFT, KeyCode.LEFT);
            k.put(Action.RIGHT, KeyCode.RIGHT);
            k.put(Action.JUMP, KeyCode.UP);
            k.put(Action.CROUCH, KeyCode.DOWN);
            k.put(Action.LIGHT, KeyCode.DIGIT1);
            k.put(Action.HEAVY, KeyCode.DIGIT2);
            k.put(Action.BLOCK, KeyCode.DIGIT4);
            k.put(Action.DASH, KeyCode.DIGIT3);
            k.put(Action.DODGE, KeyCode.DIGIT6);
            k.put(Action.LAUNCHER, KeyCode.DIGIT5);
            k.put(Action.ULTIMATE, KeyCode.DIGIT0);
            k.put(Action.SPECIAL, KeyCode.DIGIT7);
        }

        // Controller defaults (d-pad / left stick always move, jump and crouch too).
        b.put(Action.JUMP, PadButton.A);
        b.put(Action.LIGHT, PadButton.X);
        b.put(Action.HEAVY, PadButton.Y);
        b.put(Action.LAUNCHER, PadButton.B);
        b.put(Action.BLOCK, PadButton.LT);
        b.put(Action.DASH, PadButton.RB);
        b.put(Action.DODGE, PadButton.LB);
        b.put(Action.ULTIMATE, PadButton.RT);
        b.put(Action.SPECIAL, PadButton.BACK);
    }

    /** Restore default keys and controller buttons for one player. */
    public void resetDefaults(int player) {
        applyDefaults(player);
        padIndex[player] = player - 1;
        saveBindings();
    }

    // ------------------------------------------------------------ attach

    private final ChangeListener<Boolean> focusListener = (observable, wasFocused, isFocused) -> {
        if (!isFocused) {
            clear();
        }
    };

    public void attach(Scene scene) {
        if (scene == null) {
            throw new IllegalArgumentException("Scene cannot be null.");
        }

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (held.add(event.getCode())) {
                pressedThisFrame.add(event.getCode());
            }
        });

        scene.addEventFilter(KeyEvent.KEY_RELEASED,
                event -> held.remove(event.getCode()));

        scene.windowProperty().addListener((observable, oldWindow, newWindow) -> {
            if (oldWindow != null) {
                oldWindow.focusedProperty().removeListener(focusListener);
            }

            if (newWindow != null) {
                newWindow.focusedProperty().addListener(focusListener);
            }

            clear();
        });

        if (scene.getWindow() != null) {
            scene.getWindow().focusedProperty().addListener(focusListener);
        }
    }

    // ------------------------------------------------------------ controller
    // polling

    /** Call once per frame BEFORE screens update. */
    public void poll() {
        if (pads == null) {
            return;
        }

        try {
            pads.poll();
        } catch (Throwable t) {
            System.err.println("Controller polling failed, disabling controllers: " + t);
            pads = null;
            return;
        }

        int n = pads.count();

        // Per-player action state from the assigned controller.
        for (int pl = 1; pl <= 2; pl++) {
            int idx = padIndex[pl];
            GamepadSupport.Pad s = (idx >= 0 && idx < n) ? pads.get(idx) : null;
            for (Action a : Action.values()) {
                int o = a.ordinal();
                boolean now = s != null && s.connected && padActionDown(pl, a, s);
                if (now && !padHeld[pl][o]) {
                    padPressed[pl][o] = true;
                }
                padHeld[pl][o] = now;
            }
        }

        // Any controller: menu navigation + "press a button" capture.
        boolean up = false, down = false, left = false, right = false;
        boolean a = false, b = false, start = false;

        for (int i = 0; i < n; i++) {
            GamepadSupport.Pad s = pads.get(i);
            if (!s.connected) {
                continue;
            }

            up |= s.dUp || stickUp(s);
            down |= s.dDown || stickDown(s);
            left |= s.dLeft || s.lx < -STICK_THRESHOLD;
            right |= s.dRight || s.lx > STICK_THRESHOLD;
            a |= s.a;
            b |= s.b;
            start |= s.start;

            for (PadButton pb : PadButton.values()) {
                boolean d = buttonDown(pb, s);
                if (d && !captureHeld[i][pb.ordinal()] && padCaptured == null) {
                    padCaptured = pb;
                }
                captureHeld[i][pb.ordinal()] = d;
            }
        }

        if (start && !mStart) {
            pressedThisFrame.add(KeyCode.ESCAPE); // pause / back
        }

        if (padMenuMode && !captureMode) {
            if (up && !mUp)
                pressedThisFrame.add(KeyCode.UP);
            if (down && !mDown)
                pressedThisFrame.add(KeyCode.DOWN);
            if (left && !mLeft)
                pressedThisFrame.add(KeyCode.LEFT);
            if (right && !mRight)
                pressedThisFrame.add(KeyCode.RIGHT);
            if (a && !mA)
                pressedThisFrame.add(KeyCode.ENTER);
            if (b && !mB)
                pressedThisFrame.add(KeyCode.ESCAPE);
        }

        mUp = up;
        mDown = down;
        mLeft = left;
        mRight = right;
        mA = a;
        mB = b;
        mStart = start;
    }

    private boolean padActionDown(int player, Action action, GamepadSupport.Pad s) {
        PadButton mapped = getPadMap(player).get(action);
        if (mapped != null && buttonDown(mapped, s)) {
            return true;
        }
        return switch (action) {
            case LEFT -> s.dLeft || s.lx < -STICK_THRESHOLD;
            case RIGHT -> s.dRight || s.lx > STICK_THRESHOLD;
            case JUMP -> s.dUp || stickUp(s);
            case CROUCH -> s.dDown || stickDown(s);
            default -> false;
        };
    }

    private static boolean stickUp(GamepadSupport.Pad s) {
        return STICK_Y_UP_IS_POSITIVE ? s.ly > STICK_THRESHOLD : s.ly < -STICK_THRESHOLD;
    }

    private static boolean stickDown(GamepadSupport.Pad s) {
        return STICK_Y_UP_IS_POSITIVE ? s.ly < -STICK_THRESHOLD : s.ly > STICK_THRESHOLD;
    }

    private static boolean buttonDown(PadButton b, GamepadSupport.Pad s) {
        return switch (b) {
            case A -> s.a;
            case B -> s.b;
            case X -> s.x;
            case Y -> s.y;
            case LB -> s.lb;
            case RB -> s.rb;
            case LT -> s.lt > TRIGGER_THRESHOLD;
            case RT -> s.rt > TRIGGER_THRESHOLD;
            case BACK -> s.back;
        };
    }

    public void setPadMenuMode(boolean on) {
        padMenuMode = on;
    }

    public void setCaptureMode(boolean on) {
        captureMode = on;
    }

    // ------------------------------------------------------------ queries

    public boolean isDown(int player, Action action) {
        Map<Action, KeyCode> controls = getControls(player);
        return held.contains(controls.get(action))
                || padHeld[player][action.ordinal()]
                || getVirtualHeld(player).getOrDefault(action, false);
    }

    public boolean isKeyDown(KeyCode key) {
        return held.contains(key);
    }

    public boolean wasPressed(KeyCode key) {
        return pressedThisFrame.contains(key);
    }

    public boolean wasPressed(int player, Action action) {
        KeyCode key = getControls(player).get(action);
        return (key != null && pressedThisFrame.contains(key))
                || padPressed[player][action.ordinal()]
                || getVirtualPressed(player).getOrDefault(action, false);
    }

    /**
     * Set or release a virtual action, for example an AI-controlled action.
     * A press is registered only when the action changes from up to down.
     */
    public void setVirtualDown(int player, Action action, boolean down) {
        if (action == null) {
            throw new IllegalArgumentException("Action cannot be null.");
        }

        Map<Action, Boolean> heldActions = getVirtualHeld(player);
        Map<Action, Boolean> pressedActions = getVirtualPressed(player);

        boolean wasDown = heldActions.getOrDefault(action, false);

        if (down && !wasDown) {
            pressedActions.put(action, true);
        }

        if (down) {
            heldActions.put(action, true);
        } else {
            heldActions.remove(action);
        }
    }

    /** Releases every virtual action for the specified player. */
    public void clearVirtual(int player) {
        getVirtualHeld(player).clear();
        getVirtualPressed(player).clear();
    }

    public boolean anyKeyPressed() {
        return !pressedThisFrame.isEmpty();
    }

    // ------------------------------------------------------------ rebinding

    /** First keyboard key pressed this frame (or null). */
    public KeyCode firstPressedKey() {
        for (KeyCode k : pressedThisFrame) {
            return k;
        }
        return null;
    }

    /** First controller button newly pressed this frame on any pad (or null). */
    public PadButton consumePadButtonPressed() {
        PadButton b = padCaptured;
        padCaptured = null;
        return b;
    }

    /**
     * Bind ANY key to an action. If another action already uses that key,
     * the two swap so nothing is left unbound. ESC is reserved.
     */
    public boolean rebindKey(int player, Action action, KeyCode key) {
        if (action == null || key == null
                || key == KeyCode.ESCAPE || key == KeyCode.UNDEFINED) {
            return false;
        }

        Map<Action, KeyCode> mine = getControls(player);
        KeyCode old = mine.get(action);

        int swapPlayer = 0;
        Action swapAction = null;
        outer: for (int pl = 1; pl <= 2; pl++) {
            Map<Action, KeyCode> m = getControls(pl);
            for (Action a : Action.values()) {
                if (m.get(a) == key && !(pl == player && a == action)) {
                    swapPlayer = pl;
                    swapAction = a;
                    break outer;
                }
            }
        }

        if (swapAction != null) {
            if (old != null) {
                getControls(swapPlayer).put(swapAction, old);
            } else {
                getControls(swapPlayer).remove(swapAction);
            }
        }

        mine.put(action, key);
        saveBindings();
        return true;
    }

    /**
     * Bind a controller button to an action (swaps with a clash for the same
     * player).
     */
    public boolean rebindPad(int player, Action action, PadButton button) {
        if (action == null || button == null) {
            return false;
        }

        Map<Action, PadButton> m = getPadMap(player);
        PadButton old = m.get(action);

        for (Action a : Action.values()) {
            if (a != action && m.get(a) == button) {
                if (old != null) {
                    m.put(a, old);
                } else {
                    m.remove(a);
                }
                break;
            }
        }

        m.put(action, button);
        saveBindings();
        return true;
    }

    public void setControl(int player, Action action, KeyCode key) {
        if (action == null || key == null) {
            throw new IllegalArgumentException("Action and key cannot be null.");
        }
        getControls(player).put(action, key);
    }

    public KeyCode getControl(int player, Action action) {
        return getControls(player).get(action);
    }

    public PadButton getPadBinding(int player, Action action) {
        return getPadMap(player).get(action);
    }

    // ------------------------------------------------------------ controller
    // assignment

    public boolean isGamepadSupported() {
        return pads != null;
    }

    public int getPadCount() {
        return pads == null ? 0 : pads.count();
    }

    public int getPadIndex(int player) {
        return padIndex[player];
    }

    /** Cycle: KEYBOARD ONLY -> PAD 1 -> PAD 2 ... -> KEYBOARD ONLY. */
    public void cyclePadIndex(int player) {
        int max = Math.max(1, getPadCount() - 1);
        int next = padIndex[player] + 1;
        padIndex[player] = next > max ? -1 : next;
        saveBindings();
    }

    // ------------------------------------------------------------ labels for UI

    public String keyLabel(int player, Action action) {
        KeyCode k = getControls(player).get(action);
        return k == null ? "-" : k.getName().toUpperCase();
    }

    public String padLabel(int player, Action action) {
        PadButton b = getPadMap(player).get(action);
        if (b != null) {
            return b.name();
        }
        return switch (action) {
            case LEFT, RIGHT, JUMP, CROUCH -> "D-PAD";
            default -> "-";
        };
    }

    /** Short text such as "U / RT" for HUD hints. */
    public String label(int player, Action action) {
        String key = keyLabel(player, action);
        PadButton b = getPadMap(player).get(action);
        return b == null ? key : key + " / " + b.name();
    }

    // ------------------------------------------------------------ persistence

    public void saveBindings() {
        Properties props = new Properties();
        for (int pl = 1; pl <= 2; pl++) {
            for (Action a : Action.values()) {
                KeyCode k = getControls(pl).get(a);
                if (k != null) {
                    props.setProperty("p" + pl + ".key." + a.name(), k.name());
                }
                PadButton b = getPadMap(pl).get(a);
                props.setProperty("p" + pl + ".pad." + a.name(), b == null ? "NONE" : b.name());
            }
            props.setProperty("p" + pl + ".padIndex", Integer.toString(padIndex[pl]));
        }

        try (var out = Files.newBufferedWriter(SAVE_FILE)) {
            props.store(out, "Mystic Kombat controls");
        } catch (Exception ignored) {
        }
    }

    private void loadBindings() {
        if (!Files.exists(SAVE_FILE)) {
            return;
        }

        Properties props = new Properties();
        try (var in = Files.newBufferedReader(SAVE_FILE)) {
            props.load(in);
        } catch (Exception e) {
            return;
        }

        for (int pl = 1; pl <= 2; pl++) {
            for (Action a : Action.values()) {
                String k = props.getProperty("p" + pl + ".key." + a.name());
                if (k != null) {
                    try {
                        getControls(pl).put(a, KeyCode.valueOf(k));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                String b = props.getProperty("p" + pl + ".pad." + a.name());
                if (b != null) {
                    if (b.equals("NONE")) {
                        getPadMap(pl).remove(a);
                    } else {
                        try {
                            getPadMap(pl).put(a, PadButton.valueOf(b));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }

            String idx = props.getProperty("p" + pl + ".padIndex");
            if (idx != null) {
                try {
                    padIndex[pl] = Integer.parseInt(idx.trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    // ------------------------------------------------------------ internals

    private Map<Action, KeyCode> getControls(int player) {
        if (player == 1)
            return p1;
        if (player == 2)
            return p2;

        throw new IllegalArgumentException("Player must be 1 or 2, got: " + player);
    }

    private Map<Action, PadButton> getPadMap(int player) {
        if (player == 1)
            return pad1;
        if (player == 2)
            return pad2;

        throw new IllegalArgumentException("Player must be 1 or 2, got: " + player);
    }

    private Map<Action, Boolean> getVirtualHeld(int player) {
        if (player == 1)
            return virtualP1;
        if (player == 2)
            return virtualP2;

        throw new IllegalArgumentException("Player must be 1 or 2, got: " + player);
    }

    private Map<Action, Boolean> getVirtualPressed(int player) {
        if (player == 1)
            return virtualPressedP1;
        if (player == 2)
            return virtualPressedP2;

        throw new IllegalArgumentException("Player must be 1 or 2, got: " + player);
    }

    public void clear() {
        held.clear();
        pressedThisFrame.clear();
        clearVirtual(1);
        clearVirtual(2);
        for (int pl = 1; pl <= 2; pl++) {
            java.util.Arrays.fill(padHeld[pl], false);
            java.util.Arrays.fill(padPressed[pl], false);
        }
    }

    /** Call once after processing each game frame. */
    public void endFrame() {
        pressedThisFrame.clear();
        virtualPressedP1.clear();
        virtualPressedP2.clear();
        java.util.Arrays.fill(padPressed[1], false);
        java.util.Arrays.fill(padPressed[2], false);
        padCaptured = null;
    }

    /** Release the controller library on shutdown. */
    public void shutdown() {
        if (pads != null) {
            pads.close();
            pads = null;
        }
    }
}