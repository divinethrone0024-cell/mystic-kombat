package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

/**
 * The Kick Specialist: Lightning-fast legs. Closes any gap with an aerial kick
 * and chains combos.
 */
public class StreetStriker extends Fighter {

    private static final double[] ARENA = { 1.08, 1.00, 0.95, 1.00 };

    public StreetStriker(int player, double startX) {
        super("Street Striker", 95, 6, 11, player, startX, Color.DARKOLIVEGREEN);
    }

    // ---------------- Identity (character select / versus screens) -------

    @Override
    public String getTitle() {
        return "The Kick Specialist";
    }

    @Override
    public String getLore() {
        return "Lightning-fast legs. Closes any gap with an aerial kick and chains combos.";
    }

    @Override
    public String getLightAttackName() {
        return "Snap Kick";
    }

    @Override
    public String getHeavyAttackName() {
        return "Axe Kick";
    }

    // ---------------- Ultimate ----------------

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.HUNDRED_KICK_STORM;
    }

    @Override
    public int getUltimateDamage() {
        return 34;
    }

    // ---------------- Balance: Ki, arena, impact ----------------

    /** Ki gained when this fighter lands damage. */
    @Override
    public double getKiDealtMultiplier() {
        return 1.1;
    }

    /** Ki gained when this fighter is hit. */
    @Override
    public double getKiTakenMultiplier() {
        return 0.95;
    }

    @Override
    public double getArenaAffinity(int arenaIndex) {
        return ARENA[Math.floorMod(arenaIndex, ARENA.length)];
    }

    @Override
    public Color getImpactColor() {
        return Color.LIMEGREEN;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.1;
    }

    // ---------------- Sprites ----------------

    @Override
    protected String getSpriteSetId() {
        return "street_striker";
    }

    @Override
    protected double getSpriteWidth() {
        return 105.0;
    }

    @Override
    protected double getSpriteHeight() {
        return 120.0;
    }

    // ---------------- Signature move: Sky Dive Kick ----------------

    @Override
    public String getSpecialMoveName() {
        return "Sky Dive Kick";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Leaps across the arena and lands a diving kick for a big hit.";
    }

    @Override
    public double getSpecialCooldown() {
        return 3.2;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        double gap = Math.abs(opponent.x - x);

        // Close the distance, stopping just short of the opponent.
        shiftX(dir * Math.max(0, Math.min(gap - 70, 260)));

        startSpecialEffect(x + dir * 50, y - STAND_HEIGHT * 0.5, 180,
                !facingRight, getAttackEffectHue(), 0.35);

        if (Math.abs(opponent.x - x) <= 120) {
            strikeTarget(opponent, 20, 0.45, dir * 300, 0);
        }

        vy = -450;
        onGround = false;
        return true;
    }
}
