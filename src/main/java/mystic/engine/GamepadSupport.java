package mystic.engine;

// If these two imports don't resolve, check the package inside the jamepad jar
// (it is com.studiohartman.jamepad in the versions I know) and fix them here.
// This is the ONLY file that touches the controller library.
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

/** Thin wrapper around Jamepad (SDL2 game controllers). */
final class GamepadSupport {

    static final int MAX = 4;

    /** Plain snapshot of one controller. */
    static final class Pad {
        boolean connected;
        boolean a, b, x, y, lb, rb, back, start;
        boolean dUp, dDown, dLeft, dRight;
        double lx, ly, lt, rt;

        void clear() {
            connected = false;
            a = b = x = y = lb = rb = back = start = false;
            dUp = dDown = dLeft = dRight = false;
            lx = ly = lt = rt = 0;
        }
    }

    private final ControllerManager manager;
    private final Pad[] pads = new Pad[MAX];
    private int count;

    GamepadSupport() {
        manager = new ControllerManager();
        manager.initSDLGamepad();
        for (int i = 0; i < MAX; i++) {
            pads[i] = new Pad();
        }
    }

    void poll() {
        count = Math.min(MAX, manager.getNumControllers());
        for (int i = 0; i < MAX; i++) {
            Pad p = pads[i];
            if (i >= count) {
                p.clear();
                continue;
            }
            ControllerState s = manager.getState(i);
            if (s == null || !s.isConnected) {
                p.clear();
                continue;
            }
            p.connected = true;
            p.a = s.a;
            p.b = s.b;
            p.x = s.x;
            p.y = s.y;
            p.lb = s.lb;
            p.rb = s.rb;
            p.back = s.back;
            p.start = s.start;
            p.dUp = s.dpadUp;
            p.dDown = s.dpadDown;
            p.dLeft = s.dpadLeft;
            p.dRight = s.dpadRight;
            p.lx = s.leftStickX;
            p.ly = s.leftStickY;
            p.lt = s.leftTrigger;
            p.rt = s.rightTrigger;
        }
    }

    int count() {
        return count;
    }

    Pad get(int i) {
        return pads[Math.max(0, Math.min(MAX - 1, i))];
    }

    void close() {
        try {
            manager.quitSDLGamepad();
        } catch (Throwable ignored) {
        }
    }
}