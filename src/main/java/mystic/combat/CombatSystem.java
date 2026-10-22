package mystic.combat;

import mystic.common.Rect;
import mystic.fighters.Fighter;

/** Decides whether an attack connects, and applies the result. */
public class CombatSystem {

    /** Check if the attacker's active hitbox touches the defender's hurtbox. */
    public void resolve(Fighter attacker, Fighter defender) {
        Rect hitbox = attacker.getHitbox(); // null if no attack is active
        if (hitbox == null || attacker.hasHitThisAttack()) {
            return; // one hit per attack
        }
        if (hitbox.overlaps(defender.getHurtbox())) {
            attacker.markHit();
            AttackType attack = attacker.getCurrentAttack();
            double direction = attacker.isFacingRight() ? 1 : -1;
            defender.takeHit(attacker.attackDamage(), attack.hitstun, direction * attack.knockback);
        }
    }
}