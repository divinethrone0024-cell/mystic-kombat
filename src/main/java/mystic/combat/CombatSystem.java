
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
 * Handles normal attacks, combo tracking, damage scaling,
 * Ultimate Incantations, and combat impact effects.
 */
public class CombatSystem {

    private static final int ULTIMATE_DAMAGE = 35;
    private static final double ULTIMATE_STUN = 0.65;
    private static final double ULTIMATE_KNOCKBACK = 650.0;

    private static final double COMBO_TIMEOUT = 1.25;
    private static final double DAMAGE_SCALE_PER_HIT = 0.10;
    private static final double MIN_DAMAGE_MULTIPLIER = 0.50;

    private static final double IMPACT_DURATION = 0.22;

    private final Map<Fighter, ComboData> combos = new IdentityHashMap<>();

    private final List<ImpactEffect> impactEffects = new ArrayList<>();

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
        private double timer = IMPACT_DURATION;

        private ImpactEffect(
                double x,
                double y,
                boolean ultimate) {
            this.x = x;
            this.y = y;
            this.ultimate = ultimate;
        }
    }

    /**
     * Update combo timers and impact effects once per frame.
     */
    public void update(double dt) {
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
     * Return the current combo count for a fighter.
     */
    public int getComboCount(Fighter attacker) {
        ComboData combo = combos.get(attacker);
        return combo == null ? 0 : combo.count;
    }

    /**
     * Reset combo tracking at the beginning of a round.
     */
    public void resetCombos() {
        combos.clear();
    }

    /**
     * Clear all temporary impact effects.
     */
    public void resetEffects() {
        impactEffects.clear();
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
     * Add a temporary impact effect at the hit location.
     */
    private void addImpactEffect(
            Fighter attacker,
            Fighter defender,
            boolean ultimate) {

        double impactX = (attacker.getX() + defender.getX()) / 2.0;

        double impactY = defender.getY()
                - (ultimate ? 50 : 45);

        impactEffects.add(
                new ImpactEffect(impactX, impactY, ultimate));
    }

    /**
     * Resolve a normal attack when its hitbox overlaps
     * the opponent's hurtbox.
     */
    public void resolve(Fighter attacker, Fighter defender) {
        if (attacker == null || defender == null
                || attacker.isKO() || defender.isKO()) {
            return;
        }

        Rect hitbox = attacker.getHitbox();

        if (hitbox == null || attacker.hasHitThisAttack()) {
            return;
        }

        if (!hitbox.overlaps(defender.getHurtbox())) {
            return;
        }

        attacker.markHit();

        AttackType attack = attacker.getCurrentAttack();

        if (attack == null) {
            return;
        }

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        int baseDamage = attacker.attackDamage();

        int damage = registerHit(
                attacker,
                defender,
                baseDamage);

        // Being hit interrupts the defender's combo.
        breakCombo(defender);

        defender.takeHit(
                damage,
                attack.hitstun,
                direction * attack.knockback);

        // Show an impact ring and sparks.
        addImpactEffect(attacker, defender, false);

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

        // Consume Ki only after activation conditions pass.
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

        // The defender's combo is interrupted.
        breakCombo(defender);

        defender.takeHit(
                damage,
                ULTIMATE_STUN,
                direction * ULTIMATE_KNOCKBACK);

        // A larger golden effect marks the ultimate impact.
        addImpactEffect(attacker, defender, true);

        return true;
    }

    /**
     * Update and expire temporary impact effects.
     */
    public void updateEffects(double dt) {
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
     * Render impact rings and radial sparks.
     *
     * Call this after rendering the fighters so that
     * the effects appear in front of them.
     */
    public void renderEffects(GraphicsContext gc) {
        for (ImpactEffect effect : impactEffects) {
            double progress = Math.max(
                    0,
                    Math.min(1,
                            effect.timer / IMPACT_DURATION));

            double radius = effect.ultimate
                    ? 24 * (1 - progress) + 8
                    : 12 * (1 - progress) + 4;

            Color effectColor = effect.ultimate
                    ? Color.GOLD
                    : Color.ORANGE;

            gc.setGlobalAlpha(progress);
            gc.setStroke(effectColor);
            gc.setLineWidth(effect.ultimate ? 4 : 2);

            // Expanding impact ring.
            gc.strokeOval(
                    effect.x - radius,
                    effect.y - radius,
                    radius * 2,
                    radius * 2);

            // Radial sparks.
            int rays = effect.ultimate ? 12 : 8;

            for (int i = 0; i < rays; i++) {
                double angle = Math.PI * 2 * i / rays;

                double inner = radius * 0.7;

                double outer = radius
                        * (effect.ultimate ? 1.8 : 1.5);

                gc.strokeLine(
                        effect.x + Math.cos(angle) * inner,
                        effect.y + Math.sin(angle) * inner,
                        effect.x + Math.cos(angle) * outer,
                        effect.y + Math.sin(angle) * outer);
            }
        }

        // Do not leak drawing state into the rest of the HUD.
        gc.setGlobalAlpha(1.0);
        gc.setLineWidth(1);
    }
}
