
package mystic.combat;

import mystic.common.Rect;
import mystic.fighters.Fighter;

/** Handles normal attacks and Ultimate Incantations. */
public class CombatSystem {

    private static final int ULTIMATE_DAMAGE = 35;
    private static final double ULTIMATE_STUN = 0.65;
    private static final double ULTIMATE_KNOCKBACK = 650.0;

    /** Resolve a normal attack. */
    public void resolve(Fighter attacker, Fighter defender) {
        if (attacker == null || defender == null
                || attacker.isKO() || defender.isKO()) {
            return;
        }

        Rect hitbox = attacker.getHitbox();

        if (hitbox == null || attacker.hasHitThisAttack()) {
            return;
        }

        if (hitbox.overlaps(defender.getHurtbox())) {
            attacker.markHit();

            AttackType attack = attacker.getCurrentAttack();
            if (attack == null) {
                return;
            }

            double direction = attacker.isFacingRight() ? 1.0 : -1.0;

            int damage = attacker.attackDamage();
            defender.takeHit(
                    damage,
                    attack.hitstun,
                    direction * attack.knockback);

            // Reward the attacker with Mystic Energy for landing a hit.
            attacker.gainKi(damage * 0.5);
        }
    }

    /**
     * Attempt an Ultimate Incantation.
     *
     * Returns true if the ultimate successfully activates.
     */
    public boolean activateUltimate(Fighter attacker, Fighter defender) {
        if (attacker == null || defender == null) {
            return false;
        }

        if (attacker.isKO() || defender.isKO()) {
            return false;
        }

        if (!attacker.isUltimateReady() || attacker.isUltimateActive()) {
            return false;
        }

        double distance = Math.abs(attacker.getX() - defender.getX());

        if (distance > attacker.getUltimateRange()) {
            return false;
        }

        // Consume Ki only after all activation conditions pass.
        if (!attacker.consumeKiForUltimate()) {
            return false;
        }

        double direction = attacker.isFacingRight() ? 1.0 : -1.0;

        // Trigger the fighter-specific ultimate action/effect.
        attacker.useUltimate();

        // Apply ultimate damage and knockback.
        defender.takeHit(
                ULTIMATE_DAMAGE,
                ULTIMATE_STUN,
                direction * ULTIMATE_KNOCKBACK);

        return true;
    }
}
