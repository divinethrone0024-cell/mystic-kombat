package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.ArcaneProjectile;
import mystic.combat.UltimateType;

public class MysticMage extends Fighter {

    public MysticMage(int player, double startX) {
        super(
                "Mystic Mage",
                100,
                5,
                12,
                player,
                startX,
                Color.MEDIUMPURPLE);
    }

    @Override
    public void useUltimate() {
        System.out.println("ARCANE CATACLYSM!");
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.ARCANE_CATACLYSM;
    }

    @Override
    public int getUltimateDamage() {
        return 38;
    }

    @Override
    public String getLightAttackName() {
        return "Arcane Jab";
    }

    @Override
    public String getHeavyAttackName() {
        return "Rune Breaker";
    }

    /** Turns the red slash sheet purple for the mage. */
    @Override
    protected double getAttackEffectHue() {
        return -0.5;
    }

    // ---------------- Signature move: Arcane Bolt ----------------

    @Override
    public String getSpecialMoveName() {
        return "Arcane Bolt";
    }

    @Override
    public double getSpecialCooldown() {
        return 3.5;
    }

    @Override
    protected double getSpecialEffectDuration() {
        return 0.35;
    }

    /**
     * Fires a travelling arcane orb across the arena. CombatSystem moves it
     * and decides what happens when it reaches the opponent: a dodge passes
     * through it, a perfect guard destroys it, a block reduces its damage.
     */
    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;

        double dir = facingRight ? 1.0 : -1.0;

        // Cast burst at the mage's hand.
        startSpecialEffect(
                x + dir * 60,
                y - STAND_HEIGHT * 0.55,
                170,
                !facingRight,
                getAttackEffectHue(),
                0.35);

        queueProjectile(new ArcaneProjectile(this));
        return true;
    }
}