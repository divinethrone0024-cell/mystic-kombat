package mystic.combat;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.common.Rect;
import mystic.fighters.Fighter;

/**
 * Handles attacks, combo tracking, damage scaling,
 * Ultimate Incantations, hit-stop, screen shake,
 * and impact effects.
 */
public class CombatSystem {

    private static final int ULTIMATE_DAMAGE = 35;
    private static final double ULTIMATE_STUN = 0.65;
    private static final double ULTIMATE_KNOCKBACK = 650.0;

    private static final double COMBO_TIMEOUT = 1.25;
    private static final double DAMAGE_SCALE_PER_HIT = 0.10;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.50;

    private static final double IMPACT_DURATION = 0.22;
    private static final double LIGHT_HIT_STOP = 0.045;
    private static final double HEAVY_HIT_STOP = 0.075;
    private static final double ULTIMATE_HIT_STOP = 0.12;

    private static final int MAX_IMPACT_EFFECTS = 40;

    private final Map<Fighter, ComboData> combos = new IdentityHashMap<>();

    private final List<ImpactEffect> impactEffects = new ArrayList<>();

    private double hitStopTimer;

    // Screen-shake state.
    private double screenShakeTimer;
    private double screenShakeDuration;
    private double screenShakeMagnitude;

    private static class ComboData {
        private final Fighter target;
        private int count;
        private double timer;

        private ComboData(Fighter target) {
            this.target = target;
            this.count = 1;
            this.timer = COMBO_TIMEOUT;
        }
    }

    /**
     * Temporary visual effect displayed when a hit connects.
     */
    private static class ImpactEffect {
        private final double x;
        private final double y;
        private final boolean ultimate;
        private final Color color;
        private final double direction;

        private double timer = IMPACT_DURATION;

        private ImpactEffect(
                double x,
                double y,
                boolean ultimate,
                Color color,
                double direction) {

            this.x = x;
            this.y = y;
            this.ultimate = ultimate;
            this.color = color;
            this.direction = direction;
        }
    }

    /**
     * Update combo timers, hit-stop, screen shake,
     * and impact effects once per frame.
     */
    public void update(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) {
            return;
        }

        hitStopTimer = Math.max(0, hitStopTimer - dt);

        // Fade out screen shake over time.
        screenShakeTimer = Math.max(
                0,
                screenShakeTimer - dt);

        if (screenShakeTimer <= 0) {
            screenShakeMagnitude = 0;
            screenShakeDuration = 0;
        }

        Iterator<Map.Entry<Fighter, ComboData>> comboIterator = combos.entrySet().iterator();

        while (comboIterator.hasNext()) {
            ComboData combo = comboIterator.next().getValue();

            combo.timer -= dt;

            if (combo.timer <= 0) {
                comboIterator.remove();
            }
        }

        updateEffects(dt);
    }

    /**
     * Returns true while the combat system's hit-stop timer is active.
     */
    public boolean isHitStopActive() {
        return hitStopTimer > 0;
    }

    /**
     * Return the current combo count for a fighter.
     */
    public int getComboCount(Fighter attacker) {
        ComboData combo = combos.get(attacker);

        return combo == null ? 0 : combo.count;
    }

    /**
     * Current horizontal screen-shake offset.
     */
    public double getScreenShakeX() {
        return getScreenShakeOffset(1.0);
    }

    /**
     * Current vertical screen-shake offset.
     */
    public double getScreenShakeY() {
        return getScreenShakeOffset(2.3);
    }

    /**
     * Calculate a decaying screen-shake offset.
     */
    private double getScreenShakeOffset(double frequency) {
        if (screenShakeTimer <= 0 || screenShakeDuration <= 0) {
            return 0;
        }

        double progress = Math.max(
                0,
                Math.min(
                        1,
                        screenShakeTimer / screenShakeDuration));

        double envelope = progress * progress;

        return Math.sin(screenShakeTimer * 85 * frequency)
                * screenShakeMagnitude
                * envelope;
    }

    /**
     * Reset combo tracking at the beginning of a round.
     */
    public void resetCombos() {
        combos.clear();
    }

    /**
     * Clear temporary effects, hit-stop, and screen shake at round reset.
     */
    public void resetEffects() {
        impactEffects.clear();

        hitStopTimer = 0;

        screenShakeTimer = 0;
        screenShakeDuration = 0;
        screenShakeMagnitude = 0;
    }

    /**
     * Interrupt a fighter's combo.
     */
    private void breakCombo(Fighter fighter) {
        combos.remove(fighter);
    }

    /**
     * Register a hit and calculate its scaled damage.
     */
    private int registerHit(
            Fighter attacker,
            Fighter defender,
            int baseDamage) {

        ComboData combo = combos.get(attacker);

        if (combo == null
                || combo.target != defender
                || combo.timer <= 0) {

            combo = new ComboData(defender);
            combos.put(attacker, combo);

        } else {
            combo.count++;
            combo.timer = COMBO_TIMEOUT;
        }

        double multiplier = Math.max(
                MIN_DAMAGE_MULTIPLIER,
                1.0 - (combo.count - 1)
                        * DAMAGE_SCALE_PER_HIT);

        return Math.max(
                1,
                (int) Math.round(baseDamage * multiplier));
    }

    /**
     * Start a short hit-stop interval.
     * A stronger hit can extend the current interval.
     */
    private void triggerHitStop(double duration) {
        hitStopTimer = Math.max(
                hitStopTimer,
                duration);
    }

    /**
     * Start or strengthen screen shake.
     */
    private void triggerScreenShake(
            double duration,
            double magnitude) {

        if (magnitude >= screenShakeMagnitude
                || screenShakeTimer <= 0) {

            screenShakeDuration = duration;
            screenShakeMagnitude = magnitude;
        }

        screenShakeTimer = Math.max(
                screenShakeTimer,
                duration);
    }

    /**
     * Add a temporary impact effect at the hit location.
     */
    private void addImpactEffect(
            Fighter attacker,
            Fighter defender,
            boolean ultimate,
            AttackType attack) {

        double impactX = (attacker.getX() + defender.getX()) / 2.0;

        double impactY = defender.getY() - (ultimate ? 50 : 45);

        Color effectColor;

        if (ultimate) {
            effectColor = Color.GOLD;
        } else if (attack == AttackType.HEAVY) {
            effectColor = Color.ORANGERED;
        } else {
            effectColor = Color.CYAN;
        }

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        // Prevent the temporary effect list from growing indefinitely.
        if (impactEffects.size() >= MAX_IMPACT_EFFECTS) {
            impactEffects.remove(0);
        }

        impactEffects.add(
                new ImpactEffect(
                        impactX,
                        impactY,
                        ultimate,
                        effectColor,
                        direction));
    }

    /**
     * Resolve a normal attack when its hitbox overlaps
     * the opponent's hurtbox.
     */
    public void resolve(
            Fighter attacker,
            Fighter defender) {

        if (attacker == null
                || defender == null
                || attacker.isKO()
                || defender.isKO()) {

            return;
        }

        Rect hitbox = attacker.getHitbox();

        if (hitbox == null || attacker.hasHitThisAttack()) {
            return;
        }

        if (!hitbox.overlaps(defender.getHurtbox())) {
            return;
        }

        AttackType attack = attacker.getCurrentAttack();

        if (attack == null) {
            return;
        }

        // Mark the attack as having connected so it cannot hit repeatedly.
        attacker.markHit();

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        int damage = registerHit(
                attacker,
                defender,
                attacker.attackDamage());

        // Being hit interrupts the defender's combo.
        breakCombo(defender);

        defender.takeHit(
                damage,
                attack.hitstun,
                direction * attack.knockback);

        addImpactEffect(
                attacker,
                defender,
                false,
                attack);

        if (attack == AttackType.HEAVY) {
            triggerHitStop(HEAVY_HIT_STOP);
            triggerScreenShake(0.14, 8);
        } else {
            triggerHitStop(LIGHT_HIT_STOP);
            triggerScreenShake(0.09, 4);
        }

        // Reward the attacker with Mystic Energy.
        attacker.gainKi(damage * 0.5);
    }

    /**
     * Attempt an Ultimate Incantation.
     *
     * Returns true if the ultimate successfully activates.
     */
    public boolean activateUltimate(
            Fighter attacker,
            Fighter defender) {

        if (attacker == null || defender == null) {
            return false;
        }

        if (attacker.isKO() || defender.isKO()) {
            return false;
        }

        if (!attacker.isUltimateReady()
                || attacker.isUltimateActive()) {

            return false;
        }

        double distance = Math.abs(
                attacker.getX() - defender.getX());

        if (distance > attacker.getUltimateRange()) {
            return false;
        }

        // Consume Ki only after all activation conditions pass.
        if (!attacker.consumeKiForUltimate()) {
            return false;
        }

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        // Trigger the fighter-specific ultimate effect.
        attacker.useUltimate();

        int damage = registerHit(
                attacker,
                defender,
                ULTIMATE_DAMAGE);

        breakCombo(defender);

        defender.takeHit(
                damage,
                ULTIMATE_STUN,
                direction * ULTIMATE_KNOCKBACK);

        addImpactEffect(
                attacker,
                defender,
                true,
                null);

        triggerHitStop(ULTIMATE_HIT_STOP);
        triggerScreenShake(0.28, 16);

        return true;
    }

    /**
     * Update and expire temporary impact effects.
     */
    public void updateEffects(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) {
            return;
        }

        Iterator<ImpactEffect> iterator = impactEffects.iterator();

        while (iterator.hasNext()) {
            ImpactEffect effect = iterator.next();

            effect.timer -= dt;

            if (effect.timer <= 0) {
                iterator.remove();
            }
        }
    }

    /**
     * Render expanding impact rings and radial sparks.
     * Call after rendering the fighters.
     */
    public void renderEffects(GraphicsContext gc) {
        gc.save();

        try {
            for (ImpactEffect effect : impactEffects) {
                double progress = Math.max(
                        0,
                        Math.min(
                                1,
                                effect.timer / IMPACT_DURATION));

                double elapsed = 1.0 - progress;

                double radius = effect.ultimate
                        ? 28 * elapsed + 8
                        : 14 * elapsed + 4;

                double alpha = progress;

                gc.setGlobalAlpha(alpha);
                gc.setStroke(effect.color);
                gc.setFill(effect.color);

                gc.setLineWidth(
                        effect.ultimate ? 4 : 2);

                // Expanding impact ring.
                gc.strokeOval(
                        effect.x - radius,
                        effect.y - radius,
                        radius * 2,
                        radius * 2);

                int rays = effect.ultimate ? 14 : 8;

                for (int i = 0; i < rays; i++) {
                    double angle = (Math.PI * 2 * i / rays)
                            + (effect.direction < 0
                                    ? Math.PI / 8
                                    : 0);

                    double inner = radius * 0.65;

                    double outer = radius
                            * (effect.ultimate ? 2.0 : 1.6);

                    double startX = effect.x + Math.cos(angle) * inner;

                    double startY = effect.y + Math.sin(angle) * inner;

                    double endX = effect.x + Math.cos(angle) * outer;

                    double endY = effect.y + Math.sin(angle) * outer;

                    gc.strokeLine(
                            startX,
                            startY,
                            endX,
                            endY);

                    // Small spark at the end of each ray.
                    double sparkSize = effect.ultimate ? 4 : 2.5;

                    gc.fillOval(
                            endX - sparkSize / 2,
                            endY - sparkSize / 2,
                            sparkSize,
                            sparkSize);
                }
            }
        } finally {
            // Restore graphics settings for the HUD and next frame.
            gc.restore();
        }
    }
}