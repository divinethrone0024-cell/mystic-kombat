package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.ArcaneProjectile;
import mystic.combat.UltimateType;

/**
 * The Long-Range Zoner: An armoured hunter who keeps his distance and punishes
 * with plasma fire.
 */
public class CyberHunter extends Fighter {

    private static final double[] ARENA = { 1.00, 0.95, 0.95, 1.12 };

    public CyberHunter(int player, double startX) {
        super("Cyber Hunter", 90, 5, 10, player, startX, Color.DODGERBLUE);
    }

    // ---------------- Identity (character select / versus screens) -------

    @Override
    public String getTitle() {
        return "The Long-Range Zoner";
    }

    @Override
    public String getLore() {
        return "An armoured hunter who keeps his distance and punishes with plasma fire.";
    }

    @Override
    public String getLightAttackName() {
        return "Buster Punch";
    }

    @Override
    public String getHeavyAttackName() {
        return "Armor Slam";
    }

    // ---------------- Ultimate ----------------

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.PLASMA_OVERDRIVE;
    }

    @Override
    public int getUltimateDamage() {
        return 40;
    }

    // ---------------- Balance: Ki, arena, impact ----------------

    /** Ki gained when this fighter lands damage. */
    @Override
    public double getKiDealtMultiplier() {
        return 0.9;
    }

    /** Ki gained when this fighter is hit. */
    @Override
    public double getKiTakenMultiplier() {
        return 1.35;
    }

    @Override
    public double getArenaAffinity(int arenaIndex) {
        return ARENA[Math.floorMod(arenaIndex, ARENA.length)];
    }

    @Override
    public Color getImpactColor() {
        return Color.DEEPSKYBLUE;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.55;
    }

    // ---------------- Sprites ----------------

    @Override
    protected String getSpriteSetId() {
        return "cyber_hunter";
    }

    @Override
    protected double getSpriteWidth() {
        return 105.0;
    }

    @Override
    protected double getSpriteHeight() {
        return 120.0;
    }

    // ---------------- Signature move: Charge Shot ----------------

    @Override
    public String getSpecialMoveName() {
        return "Charge Shot";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Fires a fast plasma bolt. A dodge passes through it; a perfect guard destroys it.";
    }

    @Override
    public double getSpecialCooldown() {
        return 2.5;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        startSpecialEffect(x + dir * 58, y - STAND_HEIGHT * 0.55, 150,
                !facingRight, getAttackEffectHue(), 0.30);

        queueProjectile(new ArcaneProjectile(this));
        return true;
    }
}
