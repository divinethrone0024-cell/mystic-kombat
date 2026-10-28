package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

/**
 * The Rushdown Master: A disciplined martial artist whose burning kicks punish
 * anyone who jumps.
 */
public class FlameKarateka extends Fighter {

    private static final double[] ARENA = { 0.95, 1.05, 1.12, 1.00 };

    public FlameKarateka(int player, double startX) {
        super("Flame Karateka", 100, 6, 12, player, startX, Color.ORANGERED);
    }

    // ---------------- Identity (character select / versus screens) -------

    @Override
    public String getTitle() {
        return "The Rushdown Master";
    }

    @Override
    public String getLore() {
        return "A disciplined martial artist whose burning kicks punish anyone who jumps.";
    }

    @Override
    public String getLightAttackName() {
        return "Quick Jab";
    }

    @Override
    public String getHeavyAttackName() {
        return "Roundhouse";
    }

    // ---------------- Ultimate ----------------

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.DRAGON_FLAME_BARRAGE;
    }

    @Override
    public int getUltimateDamage() {
        return 36;
    }

    // ---------------- Balance: Ki, arena, impact ----------------

    /** Ki gained when this fighter lands damage. */
    @Override
    public double getKiDealtMultiplier() {
        return 1.25;
    }

    /** Ki gained when this fighter is hit. */
    @Override
    public double getKiTakenMultiplier() {
        return 0.9;
    }

    @Override
    public double getArenaAffinity(int arenaIndex) {
        return ARENA[Math.floorMod(arenaIndex, ARENA.length)];
    }

    @Override
    public Color getImpactColor() {
        return Color.ORANGE;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.05;
    }

    // ---------------- Sprites ----------------

    @Override
    protected String getSpriteSetId() {
        return "flame_karateka";
    }

    @Override
    protected double getSpriteWidth() {
        return 105.0;
    }

    @Override
    protected double getSpriteHeight() {
        return 120.0;
    }

    // ---------------- Signature move: Blazing Uppercut ----------------

    @Override
    public String getSpecialMoveName() {
        return "Blazing Uppercut";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Rising flame strike that launches the opponent into a juggle.";
    }

    @Override
    public double getSpecialCooldown() {
        return 3.0;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        startSpecialEffect(x + dir * 50, y - STAND_HEIGHT * 0.6, 190,
                !facingRight, getAttackEffectHue(), 0.40);

        if (Math.abs(opponent.x - x) <= 130) {
            strikeTarget(opponent, 22, 0.50, dir * 250, 800);
        }

        vy = -750;
        onGround = false;
        return true;
    }
}
