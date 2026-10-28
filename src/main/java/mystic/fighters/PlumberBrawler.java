package mystic.fighters;

import javafx.scene.paint.Color;

public class PlumberBrawler extends Fighter {

    public PlumberBrawler(int player, double startX) {
        super("Plumber Brawler", 130, 3, 17, player, startX,
                Color.rgb(255, 180, 65));
    }

    @Override
    public String getTitle() {
        return "The Heavy Hitter";
    }

    @Override
    public String getLore() {
        return "Slow, but every hit launches.";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Power Smash: a close-range launcher.";
    }

    @Override
    public String getLightAttackName() {
        return "Wrench Jab";
    }

    @Override
    public String getHeavyAttackName() {
        return "Pipe Slam";
    }

    @Override
    public int getUltimateDamage() {
        return 45;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.1;
    }

    @Override
    public Color getImpactColor() {
        return Color.rgb(255, 180, 65);
    }

    @Override
    public String getPortraitPath() {
        return "/assets/portraits/plumber_brawler.png";
    }

    // Sprite set: /assets/characters/dandaman/sheet.png (see SpriteSets.java).
    // The artwork faces right, so isSpriteFacingLeft() keeps its default (false).
    @Override
    protected String getSpriteSetId() {
        return "dandaman";
    }

    // ---------------- Signature move: Power Smash ----------------
    @Override
    public String getSpecialMoveName() {
        return "Power Smash";
    }

    @Override
    public double getSpecialCooldown() {
        return 5.0;
    }

    @Override
    protected double getSpecialEffectDuration() {
        return 0.45;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        startSpecialEffect(x + dir * 60, y - STAND_HEIGHT * 0.3, 200,
                !facingRight, getAttackEffectHue(), 0.45);

        if (Math.abs(opponent.x - x) < 150) {
            strikeTarget(opponent, 28, 0.55, dir * 520, 650); // launcher
        }
        return true;
    }
}