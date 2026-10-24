
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
 * Ultimate Incantations, blocking, perfect guards,
 * dodge evasion, hit-stop, screen shake, and impact effects.
 */
public class CombatSystem {

    private static final int ULTIMATE_DAMAGE = 35;
    private static final double ULTIMATE_STUN = 0.65;
    private static final double ULTIMATE_KNOCKBACK = 650.0;

    private static final double COMBO_TIMEOUT = 1.25;
    private static final double DAMAGE_SCALE_PER_HIT = 0.10;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.50;

    private static final double IMPACT_DURATION = 0.32;
    private static final double LIGHT_HIT_STOP = 0.045;
    private static final double HEAVY_HIT_STOP = 0.075;
    private static final double ULTIMATE_HIT_STOP = 0.12;
    private static final double PARRY_HIT_STOP = 0.11;

    private static final int MAX_IMPACT_EFFECTS = 40;

    // Blocking balance.
    private static final double NORMAL_BLOCK_DAMAGE_MULTIPLIER = 0.20;
    private static final double ULTIMATE_BLOCK_DAMAGE_MULTIPLIER = 0.50;

    // Impact effect colors.
    private static final Color LIGHT_COLOR = Color.CYAN;
    private static final Color HEAVY_COLOR = Color.ORANGERED;
    private static final Color BLOCK_COLOR = Color.DEEPSKYBLUE;
    private static final Color ULTIMATE_COLOR = Color.GOLD;
    private static final Color PARRY_COLOR = Color.AQUA;
    private static final Color DODGE_COLOR = Color.DEEPSKYBLUE;
    private static final Color FLASH_COLOR = Color.WHITE;

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
     * Temporary visual effect displayed when an attack connects,
     * is parried, or is successfully evaded.
     */
    private static class ImpactEffect {
        private final double x;
        private final double y;
        private final boolean ultimate;
        private final boolean heavy;
        private final boolean blocked;
        private final boolean parry;
        private final Color color;
        private final double direction;

        private double timer = IMPACT_DURATION;

        private ImpactEffect(
                double x,
                double y,
                boolean ultimate,
                boolean heavy,
                boolean blocked,
                boolean parry,
                Color color,
                double direction) {

            this.x = x;
            this.y = y;
            this.ultimate = ultimate;
            this.heavy = heavy;
            this.blocked = blocked;
            this.parry = parry;
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
        screenShakeTimer = Math.max(0, screenShakeTimer - dt);

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

    /** Returns true while hit-stop is active. */
    public boolean isHitStopActive() {
        return hitStopTimer > 0;
    }

    /** Return the current combo count for a fighter. */
    public int getComboCount(Fighter attacker) {
        ComboData combo = combos.get(attacker);
        return combo == null ? 0 : combo.count;
    }

    /** Current horizontal screen-shake offset. */
    public double getScreenShakeX() {
        return getScreenShakeOffset(1.0);
    }

    /** Current vertical screen-shake offset. */
    public double getScreenShakeY() {
        return getScreenShakeOffset(2.3);
    }

    /** Calculate a decaying screen-shake offset. */
    private double getScreenShakeOffset(double frequency) {
        if (screenShakeTimer <= 0 || screenShakeDuration <= 0) {
            return 0;
        }

        double progress = Math.max(
                0,
                Math.min(1, screenShakeTimer / screenShakeDuration));

        double envelope = progress * progress;

        return Math.sin(screenShakeTimer * 85 * frequency)
                * screenShakeMagnitude
                * envelope;
    }

    /** Reset combo tracking at the beginning of a round. */
    public void resetCombos() {
        combos.clear();
    }

    /** Clear temporary effects, hit-stop, and screen shake. */
    public void resetEffects() {
        impactEffects.clear();
        hitStopTimer = 0;
        screenShakeTimer = 0;
        screenShakeDuration = 0;
        screenShakeMagnitude = 0;
    }

    /** Interrupt a fighter's combo. */
    private void breakCombo(Fighter fighter) {
        combos.remove(fighter);
    }

    /** Register a hit and calculate its scaled damage. */
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
                1.0 - (combo.count - 1) * DAMAGE_SCALE_PER_HIT);

        return Math.max(
                1,
                (int) Math.round(baseDamage * multiplier));
    }

    /** Start a short hit-stop interval. */
    private void triggerHitStop(double duration) {
        hitStopTimer = Math.max(hitStopTimer, duration);
    }

    /** Start or strengthen screen shake. */
    private void triggerScreenShake(
            double duration,
            double magnitude) {

        if (magnitude >= screenShakeMagnitude
                || screenShakeTimer <= 0) {

            screenShakeDuration = duration;
            screenShakeMagnitude = magnitude;
        }

        screenShakeTimer = Math.max(screenShakeTimer, duration);
    }

    /**
     * Add a styled impact effect at the hit location.
     */
    private void addImpactEffect(
            Fighter attacker,
            Fighter defender,
            boolean ultimate,
            AttackType attack,
            boolean blocked,
            boolean parry) {

        double impactX = (attacker.getX() + defender.getX()) / 2.0;
        double impactY = defender.getY() - (ultimate ? 50 : 45);

        boolean heavy = !ultimate && attack == AttackType.HEAVY;

        Color effectColor;

        if (parry) {
            effectColor = PARRY_COLOR;
        } else if (blocked) {
            effectColor = BLOCK_COLOR;
        } else if (ultimate) {
            effectColor = ULTIMATE_COLOR;
        } else if (heavy) {
            effectColor = HEAVY_COLOR;
        } else {
            effectColor = LIGHT_COLOR;
        }

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        if (impactEffects.size() >= MAX_IMPACT_EFFECTS) {
            impactEffects.remove(0);
        }

        impactEffects.add(new ImpactEffect(
                impactX,
                impactY,
                ultimate,
                heavy,
                blocked,
                parry,
                effectColor,
                direction));
    }

    /**
     * Add a visual cue when a fighter successfully dodges.
     * Dodge evasion does not cause damage or break combos.
     */
    private void addDodgeEffect(Fighter attacker, Fighter defender) {
        double impactX = defender.getX();
        double impactY = defender.getY() - 48;

        if (impactEffects.size() >= MAX_IMPACT_EFFECTS) {
            impactEffects.remove(0);
        }

        impactEffects.add(new ImpactEffect(
                impactX,
                impactY,
                false,
                false,
                false,
                false,
                DODGE_COLOR,
                attacker.isFacingRight() ? 1.0 : -1.0));
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

        // Prevent this attack from hitting repeatedly.
        attacker.markHit();

        /*
         * Dodge evasion:
         * The attack misses if the defender is currently invulnerable.
         * The attack remains consumed, even if the hitbox overlaps again.
         */
        if (defender.canEvadeAttack()) {
            addDodgeEffect(attacker, defender);
            return;
        }

        /*
         * Check for a perfect guard FIRST.
         * A successful parry takes priority over normal blocking.
         */
        boolean parried = defender.consumePerfectGuard();

        if (parried) {
            // No damage to the defender.
            // Interrupt and briefly stagger the attacker.
            attacker.staggerFromParry();

            // The attacker loses their current combo.
            breakCombo(attacker);

            // The defender's combo is not broken by a successful parry.
            addImpactEffect(
                    attacker,
                    defender,
                    false,
                    attack,
                    false,
                    true);

            triggerHitStop(PARRY_HIT_STOP);
            triggerScreenShake(0.16, 9);

            // A parried attack does not award attack-hit Ki.
            return;
        }

        boolean guarded = defender.isBlocking();

        int damage;

        if (guarded) {
            damage = Math.max(
                    1,
                    (int) Math.ceil(
                            attacker.attackDamage()
                                    * NORMAL_BLOCK_DAMAGE_MULTIPLIER));
        } else {
            damage = registerHit(
                    attacker,
                    defender,
                    attacker.attackDamage());

            breakCombo(defender);
        }

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        if (guarded) {
            defender.takeBlockedHit(damage);
        } else {
            defender.takeHit(
                    damage,
                    attack.hitstun,
                    direction * attack.knockback);
        }

        addImpactEffect(
                attacker,
                defender,
                false,
                attack,
                guarded,
                false);

        if (guarded) {
            triggerHitStop(LIGHT_HIT_STOP * 0.5);
            triggerScreenShake(0.05, 2);

        } else if (attack == AttackType.HEAVY) {
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
     * Returns true if the ultimate successfully activates,
     * even if the defender parries or dodges it.
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

        /*
         * Dodge evasion:
         * The ultimate activates and consumes Ki, but deals no damage
         * when the defender is inside their dodge invulnerability window.
         */
        if (defender.canEvadeAttack()) {
            addDodgeEffect(attacker, defender);
            return true;
        }

        /*
         * A perfect guard can parry an ultimate too.
         * The ultimate still consumes the attacker's Ki.
         */
        boolean parried = defender.consumePerfectGuard();

        if (parried) {
            attacker.staggerFromParry();
            breakCombo(attacker);

            addImpactEffect(
                    attacker,
                    defender,
                    true,
                    null,
                    false,
                    true);

            triggerHitStop(PARRY_HIT_STOP);
            triggerScreenShake(0.20, 12);

            return true;
        }

        boolean guarded = defender.isBlocking();

        int damage;

        if (guarded) {
            damage = Math.max(
                    1,
                    (int) Math.ceil(
                            ULTIMATE_DAMAGE
                                    * ULTIMATE_BLOCK_DAMAGE_MULTIPLIER));
        } else {
            damage = registerHit(
                    attacker,
                    defender,
                    ULTIMATE_DAMAGE);

            breakCombo(defender);
        }

        if (guarded) {
            defender.takeBlockedHit(damage);
        } else {
            defender.takeHit(
                    damage,
                    ULTIMATE_STUN,
                    direction * ULTIMATE_KNOCKBACK);
        }

        addImpactEffect(
                attacker,
                defender,
                true,
                null,
                guarded,
                false);

        if (guarded) {
            triggerHitStop(ULTIMATE_HIT_STOP * 0.65);
            triggerScreenShake(0.14, 8);

        } else {
            triggerHitStop(ULTIMATE_HIT_STOP);
            triggerScreenShake(0.28, 16);
        }

        return true;
    }

    /** Update and expire temporary impact effects. */
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
     * Render enhanced impact rings, flashes and sparks.
     * Call after rendering the fighters.
     */
    public void renderEffects(GraphicsContext gc) {
        if (gc == null) {
            return;
        }

        gc.save();

        try {
            for (ImpactEffect effect : impactEffects) {
                double progress = Math.max(
                        0,
                        Math.min(1, effect.timer / IMPACT_DURATION));

                double elapsed = 1.0 - progress;

                double flashAlpha = Math.min(
                        1.0,
                        progress * (effect.ultimate ? 1.8 : 2.5));

                double baseRadius;

                if (effect.parry) {
                    baseRadius = 38;
                } else if (effect.ultimate) {
                    baseRadius = 42;
                } else if (effect.blocked) {
                    baseRadius = 20;
                } else if (effect.heavy) {
                    baseRadius = 30;
                } else {
                    baseRadius = 17;
                }

                double radius = baseRadius * elapsed + 3;

                gc.setGlobalAlpha(Math.min(1.0, progress * 1.5));

                // Main expanding ring.
                gc.setStroke(effect.color);

                gc.setLineWidth(
                        effect.parry ? 5.0
                                : effect.ultimate ? 4.5
                                        : effect.heavy ? 3.5
                                                : effect.blocked ? 3.0
                                                        : 2.0);

                gc.strokeOval(
                        effect.x - radius,
                        effect.y - radius,
                        radius * 2,
                        radius * 2);

                // Secondary ring for ultimates and parries.
                if (effect.ultimate || effect.parry) {
                    double secondRadius = radius * 0.62;

                    gc.setGlobalAlpha(progress * 0.9);
                    gc.setStroke(
                            effect.parry ? Color.WHITE : ULTIMATE_COLOR);

                    gc.setLineWidth(effect.parry ? 3.0 : 2.0);

                    gc.strokeOval(
                            effect.x - secondRadius,
                            effect.y - secondRadius,
                            secondRadius * 2,
                            secondRadius * 2);
                }

                // Bright center flash.
                gc.setGlobalAlpha(
                        flashAlpha * (effect.parry ? 1.0 : 0.75));

                gc.setFill(FLASH_COLOR);

                double flashSize = effect.parry ? 24
                        : effect.ultimate ? 18
                                : effect.heavy ? 12
                                        : effect.blocked ? 8
                                                : 7;

                gc.fillOval(
                        effect.x - flashSize / 2,
                        effect.y - flashSize / 2,
                        flashSize,
                        flashSize);

                // Radial sparks.
                int rays;

                if (effect.parry) {
                    rays = 20;
                } else if (effect.ultimate) {
                    rays = 18;
                } else if (effect.heavy) {
                    rays = 12;
                } else if (effect.blocked) {
                    rays = 10;
                } else {
                    rays = 8;
                }

                for (int i = 0; i < rays; i++) {
                    double angle = (Math.PI * 2 * i / rays)
                            + (effect.direction < 0
                                    ? Math.PI / 8
                                    : 0);

                    double lengthVariation = (i % 2 == 0) ? 1.0 : 0.72;

                    double inner = radius * 0.45;

                    double outer = radius
                            * (effect.parry ? 2.2
                                    : effect.ultimate ? 2.15
                                            : effect.heavy ? 1.9
                                                    : effect.blocked ? 1.65
                                                            : 1.55)
                            * lengthVariation;

                    double startX = effect.x + Math.cos(angle) * inner;
                    double startY = effect.y + Math.sin(angle) * inner;

                    double endX = effect.x + Math.cos(angle) * outer;
                    double endY = effect.y + Math.sin(angle) * outer;

                    gc.setGlobalAlpha(progress);
                    gc.setStroke(effect.color);

                    gc.setLineWidth(
                            effect.parry ? 3.0
                                    : effect.ultimate ? 2.8
                                            : effect.heavy ? 2.4
                                                    : effect.blocked ? 2.2
                                                            : 1.6);

                    gc.strokeLine(startX, startY, endX, endY);

                    double sparkSize = effect.parry ? 5
                            : effect.ultimate ? 5
                                    : effect.heavy ? 4
                                            : effect.blocked ? 3.5
                                                    : 3;

                    gc.setFill(
                            effect.parry ? Color.WHITE
                                    : effect.blocked ? BLOCK_COLOR
                                            : effect.ultimate
                                                    ? Color.WHITE
                                                    : effect.color);

                    gc.fillOval(
                            endX - sparkSize / 2,
                            endY - sparkSize / 2,
                            sparkSize,
                            sparkSize);
                }

                // Orbiting particles for ultimate impacts.
                if (effect.ultimate) {
                    int particles = 8;

                    for (int i = 0; i < particles; i++) {
                        double angle = (Math.PI * 2 * i / particles)
                                + elapsed * 5 * effect.direction;

                        double particleRadius = radius * (1.2 + elapsed * 0.8);

                        double particleX = effect.x + Math.cos(angle) * particleRadius;

                        double particleY = effect.y + Math.sin(angle) * particleRadius;

                        double particleSize = 3.5 * progress;

                        gc.setGlobalAlpha(progress * 0.9);

                        gc.setFill(
                                i % 2 == 0
                                        ? ULTIMATE_COLOR
                                        : Color.WHITE);

                        gc.fillOval(
                                particleX - particleSize / 2,
                                particleY - particleSize / 2,
                                particleSize,
                                particleSize);
                    }
                }

                // Extra rotating white sparks for perfect parries.
                if (effect.parry) {
                    int particles = 6;

                    for (int i = 0; i < particles; i++) {
                        double angle = (Math.PI * 2 * i / particles)
                                - elapsed * 7 * effect.direction;

                        double particleRadius = radius * (1.0 + elapsed);

                        double particleX = effect.x + Math.cos(angle) * particleRadius;

                        double particleY = effect.y + Math.sin(angle) * particleRadius;

                        double particleSize = 4.5 * progress;

                        gc.setGlobalAlpha(progress);
                        gc.setFill(Color.WHITE);

                        gc.fillOval(
                                particleX - particleSize / 2,
                                particleY - particleSize / 2,
                                particleSize,
                                particleSize);
                    }
                }
            }
        } finally {
            // Restore graphics settings for the HUD and next frame.
            gc.restore();
        }
    }
}
