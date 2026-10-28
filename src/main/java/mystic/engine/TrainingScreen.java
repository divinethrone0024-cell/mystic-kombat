package mystic.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import mystic.combat.AttackType;
import mystic.combat.CombatSystem;
import mystic.combat.UltimateEffect;
import mystic.common.Action;
import mystic.common.Constants;
import mystic.common.FighterState;
import mystic.common.GameState;
import mystic.common.MysticTheme;
import mystic.common.Rect;
import mystic.fighters.Fighter;

public class TrainingScreen implements Screen {

    private enum Dummy {
        STAND("STAND"), BLOCK("BLOCK"), JUMP("JUMP"), ATTACK("ATTACK"),
        SPECIAL("SPECIAL"), CPU("CPU"), BLOCK_AFTER_HIT("BLOCK AFTER HIT"),
        RANDOM("RANDOM"), MIRROR("MIRROR"), REPLAY("REPLAY");

        final String label;

        Dummy(String l) {
            label = l;
        }
    }

    private static final String[] MENU = {
            "Show hitboxes", "Infinite health", "Infinite Ki",
            "Dummy behaviour", "CPU level", "Record moves",
            "Reset positions", "Exit training"
    };

    private static final String[] MODE_NAMES = { "PRACTICE MODE", "MIRROR MODE" };
    private static final String[] MODE_LINE_1 = {
            "Free practice against a dummy.",
            "The dummy copies your moves live."
    };
    private static final String[] MODE_LINE_2 = {
            "Choose its behaviour with F1-F8 or TAB.",
            "Mirrored left/right - test spacing and defence."
    };

    private static final double MAX_RECORD = 10.0;

    private static class Entry {
        final String label;
        double age;

        Entry(String l) {
            label = l;
        }
    }

    private final GameEngine engine;
    private final MysticArenaRenderer arena = new MysticArenaRenderer();
    private final Random rnd = new Random();

    private CombatSystem combat;
    private Fighter p1, p2;
    private UltimateEffect ultimateEffect;

    private boolean showBoxes = false;
    private boolean infHealth = true;
    private boolean infKi = true;
    private Dummy dummy = Dummy.STAND;

    // Practice / Mirror choice shown when training opens.
    private boolean choosingMode;
    private int modeSel;

    private boolean menuOpen;
    private int menuSel;

    private double t, dummyTimer, idleTimer;
    private int lastHit, totalDamage, maxCombo;
    private AttackType lastAttack;
    private final List<Entry> history = new ArrayList<>();

    // CPU dummy
    private FighterAI ai;
    private FighterAI.Difficulty cpuLevel = FighterAI.Difficulty.MEDIUM;
    private FighterAI.Decision aiDecision = FighterAI.Decision.APPROACH;

    // Random dummy
    private double randomTimer;
    private int randomMode;

    // Record / replay
    private boolean recording;
    private double recClock, replayClock;
    private int replayIdx;
    private boolean recordedP1Left = true;
    private final List<Double> recTimes = new ArrayList<>();
    private final List<EnumSet<Action>> recSets = new ArrayList<>();

    // Frame advantage
    private double attackClock;
    private String advText = "-";
    private boolean advPositive;

    // Messages
    private String msg = "";
    private double msgT;

    public TrainingScreen(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void onEnter() {
        combat = new CombatSystem();
        combat.setArenaIndex(engine.getArena());
        p1 = engine.createFighter(1, Constants.WIDTH * 0.30);
        p2 = engine.createFighter(2, Constants.WIDTH * 0.70);
        ultimateEffect = null;
        menuOpen = false;
        menuSel = 0;
        choosingMode = true;
        modeSel = 0;
        t = 0;
        idleTimer = 0;
        lastHit = 0;
        totalDamage = 0;
        maxCombo = 0;
        lastAttack = null;
        history.clear();
        recording = false;
        recTimes.clear();
        recSets.clear();
        advText = "-";
        attackClock = 0;
        msg = "";
        msgT = 0;
        setDummy(Dummy.STAND);
    }

    private void setDummy(Dummy d) {
        dummy = d;
        dummyTimer = 0;
        randomTimer = 0;
        randomMode = 0;
        replayClock = 0;
        replayIdx = 0;
        ai = null;
        aiDecision = FighterAI.Decision.APPROACH;
        InputManager im = engine.getInputManager();
        if (im != null)
            im.clearVirtual(2);
        if (d == Dummy.REPLAY && recTimes.isEmpty()) {
            flash("Nothing recorded yet - press F6 and perform moves");
            dummy = Dummy.STAND;
        }
        if (dummy == Dummy.MIRROR) {
            flash("MIRROR MODE - the dummy copies your moves live");
        }
    }

    private void flash(String s) {
        msg = s;
        msgT = 2.5;
    }

    private void clearDummyInput() {
        InputManager im = engine.getInputManager();
        if (im != null)
            im.clearVirtual(2);
    }

    private void resetPositions() {
        p1.resetForRound();
        p2.resetForRound();
        combat.resetCombos();
        combat.resetEffects();
        ultimateEffect = null;
        lastHit = 0;
        totalDamage = 0;
        maxCombo = 0;
        attackClock = 0;
    }

    private void cycleCpu() {
        FighterAI.Difficulty[] v = FighterAI.Difficulty.values();
        cpuLevel = v[(cpuLevel.ordinal() + 1) % v.length];
        ai = null;
    }

    private void toggleRecord() {
        if (recording) {
            recording = false;
            if (recTimes.size() > 5) {
                flash(String.format("Saved %.1fs - dummy now replays it", recClock));
                setDummy(Dummy.REPLAY);
            } else {
                recTimes.clear();
                recSets.clear();
                flash("Recording too short - discarded");
            }
        } else {
            recording = true;
            recTimes.clear();
            recSets.clear();
            recClock = 0;
            recordedP1Left = p1.getX() < p2.getX();
            setDummy(Dummy.STAND);
            resetPositions();
            flash("RECORDING - perform your moves (F6 to stop)");
        }
    }

    // ------------------------------------------------------------ update

    @Override
    public void update(double dt, InputManager in) {
        if (p1 == null || p2 == null)
            return;
        double d = Math.max(0, Math.min(dt, 0.05));
        t += d;
        if (msgT > 0)
            msgT -= d;

        // Practice / Mirror choice shown first.
        if (choosingMode) {
            updateModeChoice(in);
            return;
        }

        if (menuOpen) {
            updateMenu(in);
            return;
        }

        if (in.wasPressed(KeyCode.ESCAPE)) {
            clearDummyInput();
            engine.changeState(GameState.MODE_SELECT);
            return;
        }
        if (in.wasPressed(KeyCode.TAB)) {
            menuOpen = true;
            return;
        }
        if (in.wasPressed(KeyCode.T))
            showBoxes = !showBoxes;
        if (in.wasPressed(KeyCode.R))
            resetPositions();
        if (in.wasPressed(KeyCode.F1))
            setDummy(Dummy.STAND);
        if (in.wasPressed(KeyCode.F2))
            setDummy(Dummy.BLOCK);
        if (in.wasPressed(KeyCode.F3))
            setDummy(Dummy.JUMP);
        if (in.wasPressed(KeyCode.F4))
            setDummy(Dummy.ATTACK);
        if (in.wasPressed(KeyCode.F5))
            setDummy(Dummy.CPU);
        if (in.wasPressed(KeyCode.F6))
            toggleRecord();
        if (in.wasPressed(KeyCode.F7))
            setDummy(Dummy.REPLAY);
        if (in.wasPressed(KeyCode.F8)) {
            cycleCpu();
            flash("CPU level: " + cpuLevel.name());
        }
        if (in.wasPressed(KeyCode.F9))
            setDummy(Dummy.MIRROR);

        for (Entry e : history)
            e.age += d;
        history.removeIf(e -> e.age > 6);

        combat.update(d);
        if (ultimateEffect != null) {
            ultimateEffect.update(d);
            if (ultimateEffect.isFinished())
                ultimateEffect = null;
        }

        recordInputs(in);
        captureRecording(in, d);

        if (in.wasPressed(1, Action.ULTIMATE)) {
            if (combat.activateUltimate(p1, p2)) {
                ultimateEffect = new UltimateEffect(p1, p1.getUltimateType());
            }
        }

        if (in.wasPressed(1, Action.SPECIAL)) {
            combat.activateSpecial(p1, p2);
        }

        applyDummy(d, in);

        if (in.wasPressed(2, Action.ULTIMATE)) {
            if (combat.activateUltimate(p2, p1)) {
                ultimateEffect = new UltimateEffect(p2, p2.getUltimateType());
            }
        }

        if (in.wasPressed(2, Action.SPECIAL)) {
            combat.activateSpecial(p2, p1);
        }

        p1.update(d, in, p2);
        p2.update(d, in, p1);

        // Track how long P1's current attack has been running.
        AttackType cur = p1.getCurrentAttack();
        if (cur == null) {
            attackClock = 0;
        } else {
            attackClock += d;
            if (attackClock > cur.total())
                attackClock = 0;
        }

        int before = p2.getHealth();
        combat.resolve(p1, p2);
        combat.resolve(p2, p1);
        combat.updateProjectiles(d, p1, p2);
        int lost = before - p2.getHealth();
        if (lost > 0) {
            lastHit = lost;
            totalDamage += lost;
            idleTimer = 0;
            computeAdvantage();
        } else {
            idleTimer += d;
        }
        maxCombo = Math.max(maxCombo, combat.getComboCount(p1));

        if (p1.getCurrentAttack() != null)
            lastAttack = p1.getCurrentAttack();

        boolean refillDummy = infHealth || p2.isKO() || idleTimer > 2.0;
        // In mirror mode the dummy also needs Ki so it can copy ultimates.
        p2.trainingRestore(refillDummy, infKi && dummy == Dummy.MIRROR);
        p1.trainingRestore(infHealth || p1.isKO(), infKi);
    }

    /** Practice / Mirror choice shown when training opens. */
    private void updateModeChoice(InputManager in) {
        if (in.wasPressed(KeyCode.ESCAPE)) {
            clearDummyInput();
            engine.changeState(GameState.MODE_SELECT);
            return;
        }

        if (in.wasPressed(KeyCode.W) || in.wasPressed(KeyCode.UP))
            modeSel = Math.floorMod(modeSel - 1, MODE_NAMES.length);
        if (in.wasPressed(KeyCode.S) || in.wasPressed(KeyCode.DOWN))
            modeSel = Math.floorMod(modeSel + 1, MODE_NAMES.length);

        if (in.wasPressed(KeyCode.DIGIT1))
            modeSel = 0;
        if (in.wasPressed(KeyCode.DIGIT2))
            modeSel = 1;

        boolean confirm = in.wasPressed(KeyCode.ENTER)
                || in.wasPressed(KeyCode.SPACE)
                || in.wasPressed(KeyCode.DIGIT1)
                || in.wasPressed(KeyCode.DIGIT2);

        if (confirm) {
            choosingMode = false;
            if (modeSel == 1) {
                setDummy(Dummy.MIRROR);
            } else {
                setDummy(Dummy.STAND);
            }
        }
    }

    private void computeAdvantage() {
        AttackType a = p1.getCurrentAttack();
        if (a == null) {
            advText = "-";
            return;
        }
        if (p2.getState() == FighterState.BLOCKING) {
            advText = "GUARDED";
            advPositive = false;
            return;
        }
        double remaining = Math.max(0, a.total() - attackClock);
        double stun = a.hitstun;
        int frames = (int) Math.round((stun - remaining) * 60.0);
        advPositive = frames >= 0;
        advText = (frames >= 0 ? "+" : "") + frames + " f";
    }

    private void recordInputs(InputManager in) {
        for (Action a : Action.values()) {
            if (in.wasPressed(1, a)) {
                KeyCode k = in.getControl(1, a);
                history.add(0, new Entry(a.name() + (k != null ? "  [" + k.getName() + "]" : "")));
            }
        }
        while (history.size() > 12)
            history.remove(history.size() - 1);
    }

    private void captureRecording(InputManager in, double d) {
        if (!recording)
            return;
        recClock += d;
        EnumSet<Action> held = EnumSet.noneOf(Action.class);
        for (Action a : Action.values()) {
            if (in.isDown(1, a))
                held.add(a);
        }
        recTimes.add(recClock);
        recSets.add(held);
        if (recClock >= MAX_RECORD)
            toggleRecord();
    }

    private void applyDummy(double dt, InputManager in) {
        // Mirror mode keeps its virtual state between frames so that
        // fresh presses (attacks, jumps) are only registered once.
        if (dummy != Dummy.MIRROR)
            in.clearVirtual(2);
        dummyTimer += dt;

        switch (dummy) {
            case BLOCK -> in.setVirtualDown(2, Action.BLOCK, true);
            case JUMP -> {
                if (dummyTimer % 1.5 < 0.08)
                    in.setVirtualDown(2, Action.JUMP, true);
            }
            case ATTACK -> {
                if (dummyTimer % 1.2 < 0.08)
                    in.setVirtualDown(2, Action.LIGHT, true);
            }
            case SPECIAL -> {
                if (dummyTimer % 2.5 < 0.08)
                    in.setVirtualDown(2, Action.SPECIAL, true);
            }
            case BLOCK_AFTER_HIT -> {
                if (totalDamage > 0 && idleTimer < 3.0) {
                    in.setVirtualDown(2, Action.BLOCK, true);
                }
            }
            case RANDOM -> applyRandom(dt, in);
            case CPU -> applyCpu(dt, in);
            case MIRROR -> applyMirror(in);
            case REPLAY -> applyReplay(dt, in);
            default -> {
            }
        }
    }

    /**
     * Live mirror: the dummy presses whatever Player 1 is pressing, with
     * left and right swapped so both fighters move symmetrically.
     */
    private void applyMirror(InputManager in) {
        for (Action a : Action.values()) {
            Action out = a == Action.LEFT ? Action.RIGHT
                    : a == Action.RIGHT ? Action.LEFT
                            : a;

            in.setVirtualDown(2, out, in.isDown(1, a));
        }
    }

    private void applyRandom(double dt, InputManager in) {
        randomTimer -= dt;
        if (randomTimer <= 0) {
            randomMode = rnd.nextInt(4); // 0 idle, 1 block, 2 dodge, 3 jump
            randomTimer = 0.5 + rnd.nextDouble() * 0.6;
            if (randomMode == 2 || randomMode == 3)
                randomTimer = 0.08;
        }
        switch (randomMode) {
            case 1 -> in.setVirtualDown(2, Action.BLOCK, true);
            case 2 -> in.setVirtualDown(2, Action.DODGE, true);
            case 3 -> in.setVirtualDown(2, Action.JUMP, true);
            default -> {
            }
        }
    }

    private void applyCpu(double dt, InputManager in) {
        if (ai == null)
            ai = new FighterAI(cpuLevel);

        FighterAI.Decision next = ai.update(dt, p2, p1);
        if (next != FighterAI.Decision.WAIT)
            aiDecision = next;

        boolean botIsLeft = p2.getX() < p1.getX();
        switch (aiDecision) {
            case APPROACH -> in.setVirtualDown(2, botIsLeft ? Action.RIGHT : Action.LEFT, true);
            case RETREAT -> in.setVirtualDown(2, botIsLeft ? Action.LEFT : Action.RIGHT, true);
            case LIGHT_ATTACK -> in.setVirtualDown(2, Action.LIGHT, true);
            case HEAVY_ATTACK -> in.setVirtualDown(2, Action.HEAVY, true);
            case LAUNCHER -> in.setVirtualDown(2, Action.LAUNCHER, true);
            case BLOCK -> in.setVirtualDown(2, Action.BLOCK, true);
            case DODGE -> in.setVirtualDown(2, Action.DODGE, true);
            case DASH -> {
                in.setVirtualDown(2, botIsLeft ? Action.RIGHT : Action.LEFT, true);
                in.setVirtualDown(2, Action.DASH, true);
            }
            case CROUCH -> in.setVirtualDown(2, Action.CROUCH, true);
            case ULTIMATE -> in.setVirtualDown(2, Action.ULTIMATE, true);
            default -> {
            }
        }
    }

    private void applyReplay(double dt, InputManager in) {
        if (recTimes.isEmpty())
            return;
        double total = recTimes.get(recTimes.size() - 1);
        replayClock += dt;
        if (replayClock > total) {
            replayClock = 0;
            replayIdx = 0;
        }
        while (replayIdx + 1 < recTimes.size() && recTimes.get(replayIdx + 1) <= replayClock) {
            replayIdx++;
        }
        boolean swap = recordedP1Left == (p2.getX() > p1.getX());
        for (Action a : recSets.get(replayIdx)) {
            Action out = a;
            if (swap && a == Action.LEFT)
                out = Action.RIGHT;
            else if (swap && a == Action.RIGHT)
                out = Action.LEFT;
            in.setVirtualDown(2, out, true);
        }
    }

    private void updateMenu(InputManager in) {
        if (in.wasPressed(KeyCode.TAB) || in.wasPressed(KeyCode.ESCAPE)) {
            menuOpen = false;
            return;
        }
        if (in.wasPressed(KeyCode.W))
            menuSel = Math.floorMod(menuSel - 1, MENU.length);
        if (in.wasPressed(KeyCode.S))
            menuSel = Math.floorMod(menuSel + 1, MENU.length);

        if (menuSel == 3) {
            Dummy[] v = Dummy.values();
            if (in.wasPressed(KeyCode.A))
                setDummy(v[Math.floorMod(dummy.ordinal() - 1, v.length)]);
            if (in.wasPressed(KeyCode.D))
                setDummy(v[Math.floorMod(dummy.ordinal() + 1, v.length)]);
        }
        if (menuSel == 4 && (in.wasPressed(KeyCode.A) || in.wasPressed(KeyCode.D))) {
            cycleCpu();
        }

        if (in.wasPressed(KeyCode.ENTER) || in.wasPressed(KeyCode.SPACE)) {
            switch (menuSel) {
                case 0 -> showBoxes = !showBoxes;
                case 1 -> infHealth = !infHealth;
                case 2 -> infKi = !infKi;
                case 3 -> {
                    Dummy[] v = Dummy.values();
                    setDummy(v[(dummy.ordinal() + 1) % v.length]);
                }
                case 4 -> cycleCpu();
                case 5 -> {
                    menuOpen = false;
                    toggleRecord();
                }
                case 6 -> {
                    resetPositions();
                    menuOpen = false;
                }
                default -> {
                    clearDummyInput();
                    engine.changeState(GameState.MODE_SELECT);
                }
            }
        }
    }

    // ------------------------------------------------------------ render

    @Override
    public void render(GraphicsContext gc) {
        if (p1 == null || p2 == null)
            return;
        double w = Constants.WIDTH, h = Constants.HEIGHT;

        arena.draw(gc, w, h, engine.getArena(), Constants.GROUND_Y, 0);
        drawGrid(gc, w);

        gc.save();
        try {
            gc.translate(combat.getScreenShakeX(), combat.getScreenShakeY());
            p1.render(gc, false);
            p2.render(gc, false);
            combat.renderEffects(gc);
            if (showBoxes)
                drawBoxes(gc);
        } finally {
            gc.restore();
        }

        if (choosingMode) {
            drawModeChoice(gc, w, h);
            return;
        }

        if (ultimateEffect != null)
            ultimateEffect.render(gc);

        drawHud(gc, w, h);
        if (menuOpen)
            drawMenu(gc, w, h);
    }

    private void drawGrid(GraphicsContext gc, double w) {
        double gy = Constants.GROUND_Y;
        gc.save();
        gc.setStroke(Color.web("#FFE9A0", 0.35));
        gc.setLineWidth(1);
        gc.strokeLine(0, gy, w, gy);
        gc.setFont(Font.font("Serif", 11));
        gc.setFill(Color.web("#FFE9A0", 0.55));
        gc.setTextAlign(TextAlignment.CENTER);
        int step = 0;
        for (double x = 0; x <= w; x += Fighter.WIDTH) {
            gc.strokeLine(x, gy, x, gy + (step % 5 == 0 ? 14 : 7));
            if (step % 5 == 0)
                gc.fillText(String.valueOf(step), x, gy + 28);
            step++;
        }
        gc.restore();
    }

    private void drawBoxes(GraphicsContext gc) {
        gc.save();
        gc.setLineWidth(2);
        for (Fighter f : new Fighter[] { p1, p2 }) {
            Rect hb = f.getHurtbox();
            gc.setStroke(Color.LIMEGREEN);
            gc.strokeRect(hb.x(), hb.y(), hb.w(), hb.h());
            Rect hit = f.getHitbox();
            if (hit != null) {
                gc.setFill(Color.web("#FF2E2E", 0.35));
                gc.fillRect(hit.x(), hit.y(), hit.w(), hit.h());
                gc.setStroke(Color.RED);
                gc.strokeRect(hit.x(), hit.y(), hit.w(), hit.h());
            }
        }
        gc.restore();
    }

    private String dummyLabel() {
        return dummy == Dummy.CPU ? "CPU (" + cpuLevel.name() + ")" : dummy.label;
    }

    private String modeLabel() {
        return dummy == Dummy.MIRROR ? "MIRROR MODE" : "PRACTICE MODE";
    }

    private void drawHud(GraphicsContext gc, double w, double h) {
        gc.save();
        MysticTheme.drawGlowText(gc, "MIRROR REALM", w / 2, 44,
                MysticTheme.title(34), MysticTheme.GOLD, 1);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        gc.setFill(MysticTheme.GOLD_D);
        gc.fillText(modeLabel() + "  -  Dummy: " + dummyLabel(), w / 2, 68);

        // Recording / replay banner
        if (recording) {
            double pulse = 0.5 + 0.5 * Math.sin(t * 8);
            gc.setFill(Color.web("#FF3030", 0.6 + 0.4 * pulse));
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 22));
            gc.fillText(String.format("REC  %.1fs / %.0fs", recClock, MAX_RECORD), w / 2, 96);
        } else if (dummy == Dummy.REPLAY) {
            gc.setFill(MysticTheme.EYE);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
            gc.fillText("REPLAY LOOP", w / 2, 96);
        } else if (dummy == Dummy.MIRROR) {
            gc.setFill(MysticTheme.EYE);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
            gc.fillText("MIRRORING YOUR MOVES", w / 2, 96);
        }

        bar(gc, 30, 90, 360, 18, p1.getHealth() / (double) p1.getMaxHealth(),
                Color.web("#32DC7D"), p1.getName() + "  " + p1.getHealth());
        bar(gc, 30, 124, 260, 12, p1.getKi() / p1.getMaxKi(),
                p1.isUltimateReady() ? Color.GOLD : Color.web("#41AAFF"), "KI  " + (int) p1.getKi() + "%");
        bar(gc, w - 390, 90, 360, 18, p2.getHealth() / (double) p2.getMaxHealth(),
                Color.web("#FF4B55"), "DUMMY  " + p2.getName() + "  " + p2.getHealth());
        bar(gc, w - 290, 124, 260, 12, p2.getKi() / p2.getMaxKi(),
                p2.isUltimateReady() ? Color.GOLD : Color.web("#41AAFF"), "DUMMY KI  " + (int) p2.getKi() + "%");

        // Damage + advantage
        panel(gc, 30, 160, 270, 140);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(MysticTheme.title(22));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("DAMAGE", 44, 188);
        gc.setFont(Font.font("Serif", 17));
        gc.setFill(Color.web("#FFE9A0"));
        gc.fillText("Last hit:   " + lastHit, 44, 212);
        gc.fillText("Combo:      " + combat.getComboCount(p1) + " hits  (best " + maxCombo + ")", 44, 234);
        gc.fillText("Total:       " + totalDamage, 44, 256);
        gc.setFill(advPositive ? Color.web("#5CFF9A") : Color.web("#FF7A7A"));
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 18));
        gc.fillText("Advantage:  " + advText, 44, 284);

        // Frame data
        panel(gc, 30, h - 160, 300, 124);
        gc.setFont(MysticTheme.title(22));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("FRAME DATA", 44, h - 134);
        gc.setFont(Font.font("Serif", 16));
        gc.setFill(Color.web("#FFE9A0"));
        if (lastAttack == null) {
            gc.fillText("Attack to see move data.", 44, h - 108);
        } else {
            double s = lastAttack.startup, a = lastAttack.active, tot = lastAttack.total();
            gc.fillText(lastAttack.name(), 44, h - 108);
            gc.fillText(String.format("Startup %.2fs   Active %.2fs", s, a), 44, h - 86);
            gc.fillText(String.format("Recovery %.2fs   Total %.2fs", Math.max(0, tot - s - a), tot), 44, h - 64);
            gc.fillText(String.format("Reach %.0f   Hitstun %.2fs",
                    (double) lastAttack.reach, (double) lastAttack.hitstun), 44, h - 42);
        }

        // Input history
        panel(gc, w - 270, 160, 240, 290);
        gc.setFont(MysticTheme.title(22));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText("INPUTS", w - 256, 188);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 16));
        for (int i = 0; i < history.size(); i++) {
            Entry e = history.get(i);
            gc.setFill(Color.web("#FFE9A0", Math.max(0.15, 1 - e.age / 6.0)));
            gc.fillText(e.label, w - 256, 214 + i * 19);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        if (msgT > 0) {
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 20));
            gc.setFill(Color.web("#FFD36B"));
            gc.fillText(msg, w / 2, h - 50);
        }
        gc.setFont(Font.font("Serif", 14));
        gc.setFill(Color.web("#C9A24B", 0.95));
        gc.fillText(
                "F1 Stand  F2 Block  F3 Jump  F4 Attack  F5 CPU  F6 Record  F7 Replay  F8 CPU level  F9 Mirror   |   T hitboxes  R reset  TAB panel  ESC exit",
                w / 2, h - 16);
        gc.restore();
    }

    private void bar(GraphicsContext gc, double x, double y, double w, double hh,
            double ratio, Color c, String label) {
        ratio = Math.max(0, Math.min(1, ratio));
        gc.setFill(Color.web("#120405", 0.9));
        gc.fillRoundRect(x, y, w, hh, 8, 8);
        gc.setFill(c);
        gc.fillRoundRect(x, y, w * ratio, hh, 8, 8);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, hh, 8, 8);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(Font.font("Serif", FontWeight.BOLD, 13));
        gc.setFill(MysticTheme.GOLD);
        gc.fillText(label, x, y - 5);
    }

    private void panel(GraphicsContext gc, double x, double y, double w, double hh) {
        gc.setFill(Color.web("#120405", 0.82));
        gc.fillRoundRect(x, y, w, hh, 14, 14);
        gc.setStroke(MysticTheme.GOLD_D);
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, hh, 14, 14);
    }

    /** Practice / Mirror choice overlay shown when training opens. */
    private void drawModeChoice(GraphicsContext gc, double w, double h) {
        gc.save();

        try {
            gc.setFill(Color.color(0, 0, 0, 0.6));
            gc.fillRect(0, 0, w, h);

            double pw = 640, ph = 440, px = (w - pw) / 2, py = (h - ph) / 2;
            gc.setFill(Color.web("#F4DE96"));
            gc.fillRoundRect(px, py, pw, ph, 14, 14);
            gc.setStroke(Color.web("#8A6A1E"));
            gc.setLineWidth(3);
            gc.strokeRoundRect(px, py, pw, ph, 14, 14);

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(MysticTheme.title(40));
            gc.setFill(Color.web("#3A0B0D"));
            gc.fillText("TRAINING", w / 2, py + 58);

            for (int i = 0; i < MODE_NAMES.length; i++) {
                double y = py + 150 + i * 120;
                boolean sel = i == modeSel;

                if (sel) {
                    gc.setFill(Color.web("#7A0F12", 0.85));
                    gc.fillRoundRect(px + 30, y - 42, pw - 60, 100, 12, 12);
                }

                gc.setTextAlign(TextAlignment.LEFT);

                gc.setFont(Font.font("Serif", FontWeight.BOLD, 28));
                gc.setFill(sel ? MysticTheme.GOLD : Color.web("#3A0B0D"));
                gc.fillText((i + 1) + "  " + MODE_NAMES[i], px + 56, y);

                gc.setFont(Font.font("Serif", 17));
                gc.setFill(sel ? Color.web("#FFE9A0") : Color.web("#5A2A1E"));
                gc.fillText(MODE_LINE_1[i], px + 56, y + 26);
                gc.fillText(MODE_LINE_2[i], px + 56, y + 48);
            }

            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Serif", 14));
            gc.setFill(Color.web("#3A0B0D"));
            gc.fillText("W/S or 1/2 choose  -  ENTER start  -  ESC back", w / 2, py + ph - 16);
        } finally {
            gc.restore();
        }
    }

    private void drawMenu(GraphicsContext gc, double w, double h) {
        gc.setFill(Color.color(0, 0, 0, 0.6));
        gc.fillRect(0, 0, w, h);

        double pw = 560, ph = 520, px = (w - pw) / 2, py = (h - ph) / 2;
        gc.setFill(Color.web("#F4DE96"));
        gc.fillRoundRect(px, py, pw, ph, 14, 14);
        gc.setStroke(Color.web("#8A6A1E"));
        gc.setLineWidth(3);
        gc.strokeRoundRect(px, py, pw, ph, 14, 14);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(MysticTheme.title(40));
        gc.setFill(Color.web("#3A0B0D"));
        gc.fillText("TRAINING", w / 2, py + 58);

        String[] vals = {
                showBoxes ? "ON" : "OFF", infHealth ? "ON" : "OFF", infKi ? "ON" : "OFF",
                "<  " + dummy.label + "  >", "<  " + cpuLevel.name() + "  >",
                recording ? "STOP" : (recTimes.isEmpty() ? "START" : "RE-RECORD"), "", ""
        };
        for (int i = 0; i < MENU.length; i++) {
            double y = py + 118 + i * 46;
            boolean sel = i == menuSel;
            if (sel) {
                gc.setFill(Color.web("#7A0F12", 0.85));
                gc.fillRoundRect(px + 24, y - 28, pw - 48, 38, 10, 10);
            }
            gc.setTextAlign(TextAlignment.LEFT);
            gc.setFont(Font.font("Serif", FontWeight.BOLD, 21));
            gc.setFill(sel ? MysticTheme.GOLD : Color.web("#3A0B0D"));
            gc.fillText(MENU[i], px + 42, y);
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText(vals[i], px + pw - 42, y);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Serif", 14));
        gc.setFill(Color.web("#3A0B0D"));
        gc.fillText("W/S choose  -  ENTER toggle  -  A/D change  -  TAB close", w / 2, py + ph - 16);
    }
}