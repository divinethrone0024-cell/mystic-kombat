package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.ArcaneProjectile;
import mystic.combat.UltimateType;

/**
 * The Teleporting Sage: Bends space itself, appearing behind enemies before
 * they can react.
 */
public class StarSage extends Fighter {

    private static final double[] ARENA = { 1.12, 1.00, 0.95, 1.08 };

    public StarSage(int player, double startX) {
        super("Star Sage", 95, 5, 11, player, startX, Color.GOLD);
    }

    // ---------------- Identity (character select / versus screens) -------

    @Override
    public String getTitle() {
        return "The Teleporting Sage";
    }

    @Override
    public String getLore() {
        return "Bends space itself, appearing behind enemies before they can react.";
    }

    @Override
    public String getLightAttackName() {
        return "Star Palm";
    }

    @Override
    public String getHeavyAttackName() {
        return "Heavenly Fist";
    }

    // ---------------- Ultimate ----------------

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.METEOR_RAIN;
    }

    @Override
    public int getUltimateDamage() {
        return 39;
    }

    // ---------------- Balance: Ki, arena, impact ----------------

    /** Ki gained when this fighter lands damage. */
    @Override
    public double getKiDealtMultiplier() {
        return 1.4;
    }

    /** Ki gained when this fighter is hit. */
    @Override
    public double getKiTakenMultiplier() {
        return 0.6;
    }

    @Override
    public double getArenaAffinity(int arenaIndex) {
        return ARENA[Math.floorMod(arenaIndex, ARENA.length)];
    }

    @Override
    public Color getImpactColor() {
        return Color.GOLD;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.15;
    }

    // ---------------- Sprites ----------------

    @Override
    protected String getSpriteSetId() {
        return "star_sage";
    }

    @Override
    protected double getSpriteWidth() {
        return 105.0;
    }

    @Override
    protected double getSpriteHeight() {
        return 120.0;
    }

    // ---------------- Signature move: Warp Strike ----------------

    @Override
    public String getSpecialMoveName() {
        return "Warp Strike";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Teleports behind the opponent and releases a star bolt at point-blank range.";
    }

    @Override
    public double getSpecialCooldown() {
        return 4.0;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        double side = opponent.x > x ? 1.0 : -1.0;

        // Appear on the far side of the opponent.
        shiftX(opponent.x + side * 85 - x);

        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        startSpecialEffect(x + dir * 55, y - STAND_HEIGHT * 0.55, 170,
                !facingRight, getAttackEffectHue(), 0.35);

        queueProjectile(new ArcaneProjectile(this));
        return true;
    }
}
