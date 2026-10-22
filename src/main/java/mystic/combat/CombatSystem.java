
package mystic.combat;

import mystic.common.Rect;
import mystic.fighters.Fighter;

/** Resolves attacks, damage, knockback and Ki gain. */
public class CombatSystem {

    public void resolve(Fighter attacker, Fighter defender) {
        Rect hitbox = attacker.getHitbox();

        if (hitbox == null || attacker.hasHitThisAttack()) {
            return;
        }

        if (hitbox.overlaps(defender.getHurtbox())) {
            attacker.markHit();

            AttackType attack = attacker.getCurrentAttack();
            int damage = attacker.attackDamage();

            double direction = attacker.isFacingRight() ? 1 : -1;

            defender.takeHit(
                    damage,
                    attack.hitstun,
                    direction * attack.knockback);

            // D7: gain Ki for dealing damage.
            attacker.gainKi(damage * 0.5);
        }
    }
}
