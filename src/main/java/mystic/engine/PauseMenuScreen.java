package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import mystic.common.Action;
import mystic.common.Constants;
import mystic.common.MysticTheme;

public class PauseMenuScreen {

    public enum Choice {
        RESUME,
        RESTART_ROUND,
        RESTART_MATCH,
        CONTROLS,
        MAIN_MENU
    }

    private enum Page {
        MAIN, CONTROLS
    }

    private static final String[] OPTIONS = {
            "RESUME",
            "RESTART ROUND",
            "RESTART MATCH",
            "CONTROLS",
            "RETURN TO MAIN MENU"
    };

    // Rows shown in the control editor (per player column).
    private static final Action[] EDIT_ACTIONS = {
            Action.LEFT, Action.RIGHT, Action.JUMP, Action.CROUCH,
            Action.LIGHT, Action.HEAVY, Action.LAUNCHER, Action.BLOCK,
            Action.DASH, Action.DODGE, Action.ULTIMATE, Action.SPECIAL
    };
    private static final int ROW_PAD = 0;
    private static final int ROW_RESET = EDIT_ACTIONS.length + 1;
    private static final int ROWS = ROW_RESET + 1;

    private Page page = Page.MAIN;
    private int selectedOption = 0;

    // Control editor state.
    private int edCol = 0; // 0 = player 1, 1 = player 2
    private int edRow = 1;
    private boolean capturing = false;
    private int captureGrace = 0;
    private String toast = "";
    private long toastUntil = 0;

    private InputManager input;

    // Self-contained animation clock (update() has no dt).
    private long startNano = System.nanoTime();
    private long lastNano = startNano;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(34, Constants.WIDTH, Constants.HEIGHT);

    /** Optional: lets the first rendered frame show real bindings. */
    public void bind(InputManager input) {
        this.input = input;
    }

    public void reset() {
        if (input != null) {
            input.setCaptureMode(false);
        }
        page = Page.MAIN;
        selectedOption = 0;
        capturing = false;
        captureGrace = 0;
        edCol = 0;
        edRow = 1;
        toast = "";
        startNano = System.nanoTime();
        lastNano = startNano;
    }

    public Choice update(InputManager in) {
        this.input = in;

        if (page == Page.CONTROLS) {
            updateEditor(in);
            return null;
        }

        if (in.wasPressed(KeyCode.UP) || in.wasPressed(KeyCode.W)) {
            selectedOption = (selectedOption - 1 + OPTIONS.length) % OPTIONS.length;
        }

        if (in.wasPressed(KeyCode.DOWN) || in.wasPressed(KeyCode.S)) {
            selectedOption = (selectedOption + 1) % OPTIONS.length;
        }

        if (in.wasPressed(KeyCode.ESCAPE)) {
            return Choice.RESUME;
        }

        if (in.wasPressed(KeyCode.ENTER) || in.wasPressed(KeyCode.SPACE)) {
            switch (selectedOption) {
                case 0:
                    return Choice.RESUME;
                case 1:
                    return Choice.RESTART_ROUND;
                case 2:
                    return Choice.RESTART_MATCH;
                case 3:
                    page = Page.CONTROLS;
                    edRow = 1;
                    capturing = false;
                    return null;
                case 4:
                    return Choice.MAIN_MENU;
                default:
                    return null;
            }
        }

        return null;
    }

    // ------------------------------------------------------------ control editor
    // logic

    private void updateEditor(InputManager in) {
        int player = edCol + 1;

        if (capturing) {
            in.setCaptureMode(true);

            // Skip the frame that started the capture so the selecting press isn't bound.
            if (captureGrace > 0) {
                captureGrace--;
                return;
            }

            if (in.wasPressed(KeyCode.ESCAPE)) {
                capturing = false;
                in.setCaptureMode(false);
                setToast("CANCELLED");
                return;
            }

            Action action = EDIT_ACTIONS[edRow - 1];

            KeyCode key = in.firstPressedKey();
            if (key != null) {
                if (in.rebindKey(player, action, key)) {
                    setToast("P" + player + "  " + action.name() + "  =  " + key.getName().toUpperCase());
                } else {
                    setToast("THAT KEY CAN'T BE USED");
                }
                capturing = false;
                in.setCaptureMode(false);
                return;
            }

            InputManager.PadButton pb = in.consumePadButtonPressed();
            if (pb != null) {
                in.rebindPad(player, action, pb);
                setToast("P" + player + "  " + action.name() + "  =  PAD " + pb.name());
                capturing = false;
                in.setCaptureMode(false);
            }
            return;
        }

        in.setCaptureMode(false);

        if (in.wasPressed(KeyCode.UP) || in.wasPressed(KeyCode.W)) {
            edRow = (edRow - 1 + ROWS) % ROWS;
        }
        if (in.wasPressed(KeyCode.DOWN) || in.wasPressed(KeyCode.S)) {
            edRow = (edRow + 1) % ROWS;
        }
        if (in.wasPressed(KeyCode.LEFT) || in.wasPressed(KeyCode.A)) {
            edCol = 0;
        }
        if (in.wasPressed(KeyCode.RIGHT) || in.wasPressed(KeyCode.D)) {
            edCol = 1;
        }

        if (in.wasPressed(KeyCode.ESCAPE)) {
            page = Page.MAIN;
            return;
        }

        if (in.wasPressed(KeyCode.ENTER) || in.wasPressed(KeyCode.SPACE)) {
            player = edCol + 1;
            if (edRow == ROW_PAD) {
                in.cyclePadIndex(player);
                setToast("P" + player + " CONTROLLER: " + padDeviceText(player));
            } else if (edRow == ROW_RESET) {
                in.resetDefaults(player);
                setToast("PLAYER " + player + " RESET TO DEFAULTS");
            } else {
                capturing = true;
                captureGrace = 1;
            }
        }
    }

    private void setToast(String s) {
        toast = s;
        toastUntil = System.nanoTime() + 2_200_000_000L;
    }

    // ------------------------------------------------------------ rendering

    public void render(GraphicsContext gc) {

        double width = Constants.WIDTH;
        double height = Constants.HEIGHT;
        double cx = width / 2.0;
        double cy = height / 2.0;

        long now = System.nanoTime();
        double t = (now - startNano) / 1e9;
        double dt = Math.min(0.05, (now - lastNano) / 1e9);
        lastNano = now;
        embers.update(dt, width, height);

        double open = ease(clamp(t / 0.28));

        // Dim the frozen arena and tint it crimson.
        gc.setFill(Color.color(0.03, 0.01, 0.02, 0.80 * open));
        gc.fillRect(0, 0, width, height);
        gc.setFill(new RadialGradient(0, 0, cx, cy, Math.max(width, height) * 0.72, false,
                CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#7A0F12", 0.40 * open)),
                new Stop(1, Color.web("#000000", 0.65 * open))));
        gc.fillRect(0, 0, width, height);

        MysticTheme.drawMandala(gc, cx, cy, 290, t * 8, 0.22 * open);
        gc.setGlobalAlpha(open);
        embers.draw(gc);
        gc.setGlobalAlpha(1);

        // Panel pops in with a small scale.
        double sc = 0.92 + 0.08 * open;
        gc.save();
        gc.translate(cx, cy);
        gc.scale(sc, sc);
        gc.translate(-cx, -cy);
        gc.setGlobalAlpha(open);

        if (page == Page.MAIN) {
            renderMain(gc, cx, cy, t, open);
        } else {
            renderEditor(gc, width, height, cx, cy, t, open);
        }

        gc.restore();
        gc.setGlobalAlpha(1);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void renderMain(GraphicsContext gc, double cx, double cy, double t, double open) {
        drawPanel(gc, cx, cy, 520, 510, t);

        gc.setTextAlign(TextAlignment.CENTER);

        MysticTheme.drawGlowText(gc, "GAME PAUSED", cx, cy - 172,
                MysticTheme.title(48), MysticTheme.GOLD, open);

        gc.setFill(Color.web("#C9A24B", 0.95));
        gc.setFont(Font.font("Serif", 14));
        gc.fillText("THE MULTIVERSE AWAITS YOUR RETURN", cx, cy - 143);

        divider(gc, cx, cy - 124, 170);

        double startY = cy - 70;
        double spacing = 52;

        for (int i = 0; i < OPTIONS.length; i++) {
            double y = startY + i * spacing;
            boolean selected = i == selectedOption;

            if (selected) {
                double pulse = 0.5 + 0.5 * Math.sin(t * 5);

                gc.setFill(new LinearGradient(cx - 200, 0, cx + 200, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#7A0F12", 0)),
                        new Stop(0.5, Color.web("#C21E1E", 0.55)),
                        new Stop(1, Color.web("#7A0F12", 0))));
                gc.fillRoundRect(cx - 205, y - 31, 410, 44, 14, 14);

                gc.setStroke(Color.web("#FFE9A0", 0.55 + 0.35 * pulse));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(cx - 205, y - 31, 410, 44, 14, 14);

                double off = 4 * pulse;
                for (int s = -1; s <= 1; s += 2) {
                    double dx = cx + s * (178 + off);
                    gc.save();
                    gc.setEffect(new DropShadow(12, MysticTheme.SPARK));
                    gc.setFill(MysticTheme.SPARK);
                    gc.fillPolygon(new double[] { dx, dx + 8, dx, dx - 8 },
                            new double[] { y - 17, y - 8, y + 1, y - 8 }, 4);
                    gc.restore();
                }

                gc.save();
                gc.setEffect(new DropShadow(14, MysticTheme.SPARK));
                gc.setFill(MysticTheme.GOLD);
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 23));
                gc.fillText(OPTIONS[i], cx, y);
                gc.restore();
            } else {
                gc.setFill(Color.web("#D8C7A0", 0.82));
                gc.setFont(Font.font("Serif", 21));
                gc.fillText(OPTIONS[i], cx, y);
            }
        }

        gc.setFill(Color.web("#C9A24B", 0.9));
        gc.setFont(Font.font("Serif", 14));
        gc.fillText("W / S or ARROWS or D-PAD  -  ENTER / A SELECTS  -  ESC / START RESUMES",
                cx, cy + 222);
    }

    private void renderEditor(GraphicsContext gc, double w, double h,
            double cx, double cy, double t, double open) {
        double pw = Math.min(w - 60, 920);
        double ph = Math.min(h - 30, 620);
        double top = cy - ph / 2;

        drawPanel(gc, cx, cy, pw, ph, t);

        gc.setTextAlign(TextAlignment.CENTER);
        MysticTheme.drawGlowText(gc, "CONTROLS", cx, top + 52,
                MysticTheme.title(40), MysticTheme.GOLD, open);

        gc.setFill(Color.web("#C9A24B", 0.95));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("PICK A ROW, PRESS ENTER, THEN PRESS ANY KEY OR CONTROLLER BUTTON", cx, top + 76);
        divider(gc, cx, top + 88, 200);

        double cw = (pw - 90) / 2.0;
        double x0 = cx - pw / 2 + 30;
        double y0 = top + 112;
        double avail = ph - 112 - 22 - 64;
        double rowH = Math.max(20, Math.min(28, avail / ROWS));

        for (int col = 0; col < 2; col++) {
            drawColumn(gc, col, x0 + col * (cw + 30), y0, cw, rowH, t);
        }

        // Toast and footer.
        gc.setTextAlign(TextAlignment.CENTER);
        if (System.nanoTime() < toastUntil) {
            gc.setFill(MysticTheme.GOLD);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 16));
            gc.fillText(toast, cx, cy + ph / 2 - 40);
        }
        gc.setFill(Color.web("#C9A24B", 0.9));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText(capturing
                ? "PRESS A KEY OR CONTROLLER BUTTON  -  ESC / START CANCELS"
                : "ARROWS / D-PAD MOVE  -  ENTER / A REBINDS  -  ESC / START BACK",
                cx, cy + ph / 2 - 18);
    }

    private void drawColumn(GraphicsContext gc, int col, double x, double y0,
            double cw, double rowH, double t) {
        int player = col + 1;
        String accent = col == 0 ? "#FFB347" : "#4FE3E0";

        // Header
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web(accent));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
        gc.fillText("PLAYER " + player, x + 8, y0);

        double keyX = x + cw * 0.60;
        double padX = x + cw * 0.86;
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#C9A24B", 0.9));
        gc.setFont(Font.font("Serif", 11));
        gc.fillText("KEY", keyX, y0 - 2);
        gc.fillText("PAD", padX, y0 - 2);

        double ry0 = y0 + 12;

        gc.setFill(Color.web("#0C0203", 0.55));
        gc.fillRoundRect(x - 6, ry0 - 4, cw + 12, rowH * ROWS + 8, 12, 12);
        gc.setStroke(Color.web(accent, 0.55));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(x - 6, ry0 - 4, cw + 12, rowH * ROWS + 8, 12, 12);

        for (int row = 0; row < ROWS; row++) {
            double y = ry0 + row * rowH;
            double base = y + rowH * 0.68;
            boolean sel = col == edCol && row == edRow;

            if (sel) {
                double pulse = 0.5 + 0.5 * Math.sin(t * 5);
                gc.setFill(new LinearGradient(x, 0, x + cw, 0, false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#C21E1E", 0.55)),
                        new Stop(1, Color.web("#7A0F12", 0.15))));
                gc.fillRoundRect(x, y, cw, rowH - 2, 8, 8);
                gc.setStroke(Color.web("#FFE9A0", 0.5 + 0.4 * pulse));
                gc.setLineWidth(1.5);
                gc.strokeRoundRect(x, y, cw, rowH - 2, 8, 8);
            }

            if (row == ROW_PAD) {
                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFill(Color.web("#D8C7A0", 0.95));
                gc.setFont(Font.font("Serif", 14));
                gc.fillText("CONTROLLER", x + 12, base);

                gc.setTextAlign(TextAlignment.RIGHT);
                gc.setFill(MysticTheme.GOLD);
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
                gc.fillText(padDeviceText(player), x + cw - 12, base);
            } else if (row == ROW_RESET) {
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setFill(sel ? MysticTheme.GOLD : Color.web("#D8C7A0", 0.8));
                gc.setFont(Font.font("Serif", FontWeight.BOLD, 14));
                gc.fillText("RESET TO DEFAULTS", x + cw / 2, base);
            } else {
                Action action = EDIT_ACTIONS[row - 1];

                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFill(sel ? MysticTheme.GOLD : Color.web("#D8C7A0", 0.95));
                gc.setFont(Font.font("Serif", sel ? FontWeight.BOLD : FontWeight.NORMAL, 15));
                gc.fillText(action.name(), x + 12, base);

                if (sel && capturing) {
                    double a = 0.55 + 0.45 * Math.sin(t * 8);
                    gc.setTextAlign(TextAlignment.RIGHT);
                    gc.setFill(Color.web("#FFE9A0", a));
                    gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
                    gc.fillText("PRESS ANY KEY / BUTTON...", x + cw - 10, base);
                } else {
                    String k = input == null ? "-" : input.keyLabel(player, action);
                    String p = input == null ? "-" : input.padLabel(player, action);
                    chip(gc, keyX, y, rowH, k, "#FFE9A0");
                    chip(gc, padX, y, rowH, p, accent);
                }
            }
        }
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void chip(GraphicsContext gc, double cx, double y, double rowH, String text, String color) {
        double w = 78;
        gc.setFill(Color.web("#000000", 0.45));
        gc.fillRoundRect(cx - w / 2, y + 2, w, rowH - 6, 7, 7);
        gc.setStroke(Color.web(color, 0.45));
        gc.setLineWidth(1);
        gc.strokeRoundRect(cx - w / 2, y + 2, w, rowH - 6, 7, 7);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web(color));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, text.length() > 8 ? 10 : 13));
        gc.fillText(text, cx, y + rowH * 0.66);
    }

    private String padDeviceText(int player) {
        if (input == null) {
            return "-";
        }
        if (!input.isGamepadSupported()) {
            return "NOT AVAILABLE";
        }
        int idx = input.getPadIndex(player);
        if (idx < 0) {
            return "KEYBOARD ONLY";
        }
        return "PAD " + (idx + 1) + (idx < input.getPadCount() ? "  (CONNECTED)" : "  (NOT FOUND)");
    }

    // ------------------------------------------------------------ shared pieces

    private void drawPanel(GraphicsContext gc, double cx, double cy, double w, double h, double t) {
        double x = cx - w / 2, y = cy - h / 2;

        gc.save();
        gc.setEffect(new DropShadow(30, Color.web("#FF8A1F", 0.45)));
        gc.setFill(Color.web("#120405"));
        gc.fillRoundRect(x, y, w, h, 22, 22);
        gc.restore();

        gc.setFill(new LinearGradient(0, y, 0, y + h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#35090C", 0.97)), new Stop(1, Color.web("#120405", 0.98))));
        gc.fillRoundRect(x, y, w, h, 22, 22);

        gc.setFill(new RadialGradient(0, 0, cx, y, w * 0.7, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF8A1F", 0.20)), new Stop(1, Color.web("#FF8A1F", 0))));
        gc.fillRoundRect(x, y, w, h, 22, 22);

        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(2.5);
        gc.strokeRoundRect(x, y, w, h, 22, 22);
        gc.setStroke(Color.web("#FFE9A0", 0.35));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x + 10, y + 10, w - 20, h - 20, 16, 16);

        double pulse = 0.7 + 0.3 * Math.sin(t * 3);
        double[][] c = { { x, y }, { x + w, y }, { x, y + h }, { x + w, y + h } };
        for (double[] p : c) {
            double s = 9;
            gc.setFill(Color.web("#FFE9A0", pulse));
            gc.fillPolygon(new double[] { p[0], p[0] + s, p[0], p[0] - s },
                    new double[] { p[1] - s, p[1], p[1] + s, p[1] }, 4);
            gc.setStroke(Color.web("#7A0F12"));
            gc.setLineWidth(1.5);
            gc.strokePolygon(new double[] { p[0], p[0] + s, p[0], p[0] - s },
                    new double[] { p[1] - s, p[1], p[1] + s, p[1] }, 4);
        }
    }

    private void divider(GraphicsContext gc, double cx, double y, double half) {
        gc.setStroke(new LinearGradient(cx - half, 0, cx + half, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#C9A24B", 0)), new Stop(0.5, Color.web("#FFE9A0", 0.9)),
                new Stop(1, Color.web("#C9A24B", 0))));
        gc.setLineWidth(1.5);
        gc.strokeLine(cx - half, y, cx + half, y);
        gc.setFill(MysticTheme.GOLD);
        gc.fillPolygon(new double[] { cx, cx + 6, cx, cx - 6 },
                new double[] { y - 6, y, y + 6, y }, 4);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private static double ease(double x) {
        return 1 - Math.pow(1 - x, 3);
    }
}