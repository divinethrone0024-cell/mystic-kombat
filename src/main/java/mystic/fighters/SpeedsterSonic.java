package mystic.fighters;

import javafx.scene.paint.Color;

public class SpeedsterSonic extends Fighter {

    public SpeedsterSonic(int player, double startX) {
        super("Speedster Sonic", 95, 7, 10, player, startX,
                Color.rgb(50, 205, 255));
    }

    @Override
    public String getTitle() {
        return "The Lightning Fighter";
    }

    @Override
    public String getLore() {
        return "Dashes straight through the enemy.";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Lightning Rush: dash through the target.";
    }

    @Override
    public String getLightAttackName() {
        return "Spin Jab";
    }

    @Override
    public String getHeavyAttackName() {
        return "Sonic Kick";
    }

    @Override
    public int getUltimateDamage() {
        return 36;
    }

    @Override
    protected double getAttackEffectHue() {
        return 0.55;
    }

    @Override
    public Color getImpactColor() {
        return Color.rgb(50, 205, 255);
    }

    @Override
    public String getPortraitPath() {
        return "/assets/portraits/speedster_sonic.png";
    }

    // Sprite set: /assets/characters/speedster/sheet.png (see SpriteSets.java).
    // The artwork faces right, so isSpriteFacingLeft() keeps its default (false).
    @Override
    protected String getSpriteSetId() {
        return "speedster";
    }

    // ---------------- Signature move: Lightning Rush ----------------
    @Override
    public String getSpecialMoveName() {
        return "Lightning Rush";
    }

    @Override
    public double getSpecialCooldown() {
        return 3.0;
    }

    @Override
    protected double getSpecialEffectDuration() {
        return 0.30;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;
        double dist = Math.abs(opponent.x - x);

        startSpecialEffect(x + dir * 70, y - STAND_HEIGHT * 0.5, 190,
                !facingRight, getAttackEffectHue(), 0.30);

        if (dist < 350) {
            strikeTarget(opponent, 16, 0.3, dir * 400, 0);
            shiftX(dir * (dist + 80)); // rush through to the other side
        } else {
            shiftX(dir * 250); // whiff: still dashes forward
        }
        return true;
    }
}