package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.common.Constants;
import mystic.common.GameState;
import mystic.common.MysticTheme;

public class SettingsScreen implements Screen {

    private final GameEngine engine;
    private final MysticTheme.Embers embers = new MysticTheme.Embers(70, Constants.WIDTH, Constants.HEIGHT);

    private double t;

    private double musicVolume = 0.70;
    private double sfxVolume = 0.80;

    private boolean musicMuted = false;
    private boolean sfxMuted = false;
    private boolean showingControls = false;

    private int selectedOption = 0;

    private final String[] options = {
            "MUSIC VOLUME",
            "SOUND EFFECTS",
            "MUTE MUSIC",
            "MUTE SOUND FX",
            "KEYBOARD CONTROLS",
            "BACK"
    };

    public SettingsScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        t = 0;
        selectedOption = 0;
        showingControls = false;
        syncAudioSettings();
    }

    private void syncAudioSettings() {
        SoundManager.setMusicVolume(musicVolume);
        SoundManager.setMusicEnabled(!musicMuted);
        SoundManager.setVolume(sfxVolume);
        SoundManager.setEnabled(!sfxMuted);
    }

    @Override
    public void update(double dt, InputManager input) {
        t += dt;
        embers.update(dt, Constants.WIDTH, Constants.HEIGHT);

        if (showingControls) {
            if (input.wasPressed(KeyCode.ESCAPE)
                    || input.wasPressed(KeyCode.ENTER)
                    || input.wasPressed(KeyCode.BACK_SPACE)) {
                showingControls = false;
            }
            return;
        }

        if (input.wasPressed(KeyCode.ESCAPE)) {
            engine.changeState(GameState.MENU);
            return;
        }

        if (input.wasPressed(KeyCode.UP) || input.wasPressed(KeyCode.W)) {
            selectedOption = Math.floorMod(selectedOption - 1, options.length);
        }
        if (input.wasPressed(KeyCode.DOWN) || input.wasPressed(KeyCode.S)) {
            selectedOption = Math.floorMod(selectedOption + 1, options.length);
        }
        if (input.wasPressed(KeyCode.LEFT) || input.wasPressed(KeyCode.A)) {
            adjustOption(-0.05);
        }
        if (input.wasPressed(KeyCode.RIGHT) || input.wasPressed(KeyCode.D)) {
            adjustOption(0.05);
        }
        if (input.wasPressed(KeyCode.ENTER) || input.wasPressed(KeyCode.SPACE)) {
            activateOption();
        }

        syncAudioSettings();
    }

    private void adjustOption(double amount) {
        if (selectedOption == 0) {
            musicVolume = clamp(musicVolume + amount);
        } else if (selectedOption == 1) {
            sfxVolume = clamp(sfxVolume + amount);
        }
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private void activateOption() {
        switch (selectedOption) {
            case 2 -> musicMuted = !musicMuted;
            case 3 -> sfxMuted = !sfxMuted;
            case 4 -> showingControls = true;
            case 5 -> engine.changeState(GameState.MENU);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GraphicsContext gc) {
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        MysticTheme.drawBackground(gc, w, h);
        MysticTheme.drawMandala(gc, w * 0.78, h * 0.52, 330, t * 8, 0.45);
        MysticTheme.drawShards(gc, w, h, t);
        embers.draw(gc);

        gc.save();
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setEffect(new DropShadow(26, MysticTheme.SPARK));
        gc.setFont(MysticTheme.title(72));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("SETTINGS", 70, 120);
        gc.restore();

        double y0 = 215, gap = 66;
        for (int i = 0; i < options.length; i++) {
            double local = clamp((t - 0.15 - i * 0.08) / 0.4);
            double ease = 1 - Math.pow(1 - local, 3);
            drawRow(gc, i, 90 - (1 - ease) * 320, y0 + i * gap, i == selectedOption, ease);
        }

        gc.save();
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", 16));
        gc.setFill(Color.web("#C9A24B", 0.8));
        gc.fillText("W/S or Up/Down to move  -  Left/Right to adjust  -  Enter to toggle  -  Esc to go back",
                70, h - 30);
        gc.restore();

        if (showingControls)
            drawControls(gc, w, h);
    }

    private void drawRow(GraphicsContext gc, int i, double x, double y, boolean sel, double alpha) {
        double rowWidth = 720;

        gc.save();
        gc.setGlobalAlpha(alpha);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(MysticTheme.title(38));

        if (sel) {
            double pulse = 0.5 + 0.5 * Math.sin(t * 6);
            gc.setEffect(new DropShadow(18 + 10 * pulse, MysticTheme.SPARK));
            gc.setFill(MysticTheme.GOLD);
            gc.fillText(">", x - 38, y);
            gc.setStroke(MysticTheme.GOLD_D);
            gc.setLineWidth(2);
            gc.strokeLine(x, y + 10, x + rowWidth, y + 10);
        } else {
            gc.setFill(MysticTheme.GOLD_D);
        }
        gc.fillText(options[i], x, y);
        gc.restore();

        // value column (drawn without the glow so it stays readable)
        gc.save();
        gc.setGlobalAlpha(alpha);
        double vx = x + 450;
        switch (i) {
            case 0 -> drawBar(gc, vx, y, musicVolume, musicMuted, sel);
            case 1 -> drawBar(gc, vx, y, sfxVolume, sfxMuted, sel);
            case 2 -> drawValue(gc, vx, y, musicMuted ? "MUTED" : "ON", musicMuted, sel);
            case 3 -> drawValue(gc, vx, y, sfxMuted ? "MUTED" : "ON", sfxMuted, sel);
            case 4 -> drawValue(gc, vx, y, "VIEW", false, sel);
            default -> {
            }
        }
        gc.restore();
    }

    private void drawBar(GraphicsContext gc, double x, double y, double value, boolean muted, boolean sel) {
        double bw = 190, bh = 14, by = y - 16;

        gc.setFill(Color.web("#2A0809", 0.9));
        gc.fillRoundRect(x, by, bw, bh, 8, 8);

        gc.setFill(muted ? Color.web("#C9A24B", 0.35) : (sel ? MysticTheme.SPARK : MysticTheme.GOLD_D));
        gc.fillRoundRect(x, by, Math.max(bh, bw * value), bh, 8, 8);

        gc.setStroke(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, by, bw, bh, 8, 8);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", 20));
        gc.setFill(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
        gc.fillText((int) Math.round(value * 100) + "%", x + bw + 14, y - 3);
    }

    private void drawValue(GraphicsContext gc, double x, double y, String text, boolean warn, boolean sel) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", 24));
        if (warn)
            gc.setFill(MysticTheme.RED);
        else
            gc.setFill(sel ? MysticTheme.GOLD : MysticTheme.GOLD_D);
        gc.fillText(text, x, y - 3);
    }

    private void drawControls(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.color(0, 0, 0, 0.78));
        gc.fillRect(0, 0, w, h);

        double pw = 960, ph = 600, px = (w - pw) / 2, py = (h - ph) / 2;
        gc.setFill(Color.web("#2A0809", 0.96));
        gc.fillRoundRect(px, py, pw, ph, 20, 20);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(3);
        gc.strokeRoundRect(px, py, pw, ph, 20, 20);

        MysticTheme.drawGlowText(gc, "KEYBOARD CONTROLS", w / 2, py + 58,
                MysticTheme.title(46), MysticTheme.GOLD, 1);

        double c1 = px + pw * 0.20, c2 = px + pw * 0.50, c3 = px + pw * 0.80;

        gc.save();
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(28));
        gc.setFill(MysticTheme.SPARK);
        gc.fillText("PLAYER 1", c1, py + 110);
        gc.fillText("PLAYER 2", c3, py + 110);

        String[] actions = {
                "Move Left / Right", "Jump", "Crouch", "Light Attack", "Heavy Attack",
                "Block", "Dash", "Dodge", "Launcher", "Special", "Ultimate Incantation"
        };
        String[] p1 = { "A / D", "W", "S", "J", "K", "F", "L", "I", "H", "O", "U" };
        String[] p2 = { "LEFT / RIGHT", "UP", "DOWN", "NUMPAD 1", "NUMPAD 2",
                "NUMPAD 4", "NUMPAD 3", "NUMPAD 6", "NUMPAD 5", "NUMPAD 7", "NUMPAD 0" };

        double y = py + 155, gap = 38;
        gc.setFont(Font.font("Serif", 20));
        for (int i = 0; i < actions.length; i++) {
            gc.setFill(MysticTheme.GOLD_D);
            gc.fillText(actions[i], c2, y + i * gap);
            gc.setFill(MysticTheme.GOLD);
            gc.fillText(p1[i], c1, y + i * gap);
            gc.fillText(p2[i], c3, y + i * gap);
        }

        gc.setFont(Font.font("Serif", 18));
        gc.setFill(Color.web("#C9A24B", 0.85));
        gc.fillText("Press ESC to return", w / 2, py + ph - 24);
        gc.restore();
    }

    public double getMusicVolume() {
        return musicVolume;
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public boolean isSfxMuted() {
        return sfxMuted;
    }
}