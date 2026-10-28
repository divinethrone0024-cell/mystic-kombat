package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

/**
 * The Silent Assassin: A cloaked swordsman with the highest speed and damage
 * but the lowest health.
 */
public class PhantomBlade extends Fighter {

    private static final double[] ARENA = { 0.95, 1.10, 1.12, 1.00 };

    public PhantomBlade(int player, double startX) {
        super("Phantom Blade", 85, 7, 13, player, startX, Color.LAVENDER);
    }

    // ---------------- Identity (character select / versus screens) -------

    @Override
    public String getTitle() {
        return "The Silent Assassin";
    }

    @Override
    public String getLore() {
        return "A cloaked swordsman with the highest speed and damage but the lowest health.";
    }

    @Override
    public String getLightAttackName() {
        return "Quick Slash";
    }

    @Override
    public String getHeavyAttackName() {
        return "Cloak Cutter";
    }

    // ---------------- Ultimate ----------------

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.CRIMSON_MOON_FLURRY;
    }

    @Override
    public int getUltimateDamage() {
        return 42;
    }

    // ---------------- Balance: Ki, arena, impact ----------------

    /** Ki gained when this fighter lands damage. */
    @Override
    public double getKiDealtMultiplier() {
        return 1.0;
    }

    /** Ki gained when this fighter is hit. */
    @Override
    public double getKiTakenMultiplier() {
        return 0.8;
    }

    @Override
    public double getArenaAffinity(int arenaIndex) {
        return ARENA[Math.floorMod(arenaIndex, ARENA.length)];
    }

    @Override
    public Color getImpactColor() {
        return Color.MEDIUMSLATEBLUE;
    }

    @Override
    protected double getAttackEffectHue() {
        return -0.2;
    }

    // ---------------- Sprites ----------------

    @Override
    protected String getSpriteSetId() {
        return "phantom_blade";
    }

    @Override
    protected double getSpriteWidth() {
        return 105.0;
    }

    @Override
    protected double getSpriteHeight() {
        return 105.0;
    }

    // ---------------- Signature move: Shadow Dash Slash ----------------

    @Override
    public String getSpecialMoveName() {
        return "Shadow Dash Slash";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Dashes through the opponent, slashing and ending up behind them.";
    }

    @Override
    public double getSpecialCooldown() {
        return 3.5;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        boolean close = Math.abs(opponent.x - x) <= 280;

        startSpecialEffect(x + dir * 70, y - STAND_HEIGHT * 0.5, 220,
                !facingRight, getAttackEffectHue(), 0.30);

        if (close) {
            strikeTarget(opponent, 24, 0.45, dir * 260, 0);
            // Pass through to the other side.
            shiftX(opponent.x + dir * 70 - x);
        } else {
            shiftX(dir * 220);
        }

        return true;
    }
}
