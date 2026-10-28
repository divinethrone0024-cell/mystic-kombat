package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

/**
 * The Swamp Tank: Slow and heavy with a huge health pool. That tongue reaches
 * farther than you think.
 */
public class ToxicToad extends Fighter {

    private static final double[] ARENA = { 1.00, 0.95, 1.05, 1.10 };

    public ToxicToad(int player, double startX) {
        super("Toxic Toad", 130, 3, 10, player, startX, Color.CRIMSON);
    }

    // ---------------- Identity (character select / versus screens) -------

    @Override
    public String getTitle() {
        return "The Swamp Tank";
    }

    @Override
    public String getLore() {
        return "Slow and heavy with a huge health pool. That tongue reaches farther than you think.";
    }

    @Override
    public String getLightAttackName() {
        return "Tongue Flick";
    }

    @Override
    public String getHeavyAttackName() {
        return "Belly Slam";
    }

    // ---------------- Ultimate ----------------

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.POISON_BURST;
    }

    @Override
    public int getUltimateDamage() {
        return 37;
    }

    // ---------------- Balance: Ki, arena, impact ----------------

    /** Ki gained when this fighter lands damage. */
    @Override
    public double getKiDealtMultiplier() {
        return 0.8;
    }

    /** Ki gained when this fighter is hit. */
    @Override
    public double getKiTakenMultiplier() {
        return 1.6;
    }

    @Override
    public double getArenaAffinity(int arenaIndex) {
        return ARENA[Math.floorMod(arenaIndex, ARENA.length)];
    }

    @Override
    public Color getImpactColor() {
        return Color.CRIMSON;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.25;
    }

    // ---------------- Sprites ----------------

    @Override
    protected String getSpriteSetId() {
        return "toxic_toad";
    }

    @Override
    protected double getSpriteWidth() {
        return 135.0;
    }

    @Override
    protected double getSpriteHeight() {
        return 85.0;
    }

    @Override
    protected boolean isSpriteFacingLeft() {
        return true;
    }

    // ---------------- Signature move: Tongue Lash ----------------

    @Override
    public String getSpecialMoveName() {
        return "Tongue Lash";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Long-range tongue that hits and drags the opponent in close.";
    }

    @Override
    public double getSpecialCooldown() {
        return 3.0;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        startSpecialEffect(x + dir * 90, y - STAND_HEIGHT * 0.35, 230,
                !facingRight, getAttackEffectHue(), 0.35);

        double gap = Math.abs(opponent.x - x);

        if (gap <= 320) {
            strikeTarget(opponent, 18, 0.50, 0, 0);
            // Reel the opponent in front of the toad.
            opponent.shiftX(-dir * Math.max(0, gap - 90));
        }

        return true;
    }
}
