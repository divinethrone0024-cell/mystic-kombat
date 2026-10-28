package mystic.fighters;

import javafx.scene.paint.Color;

public class ShadowVigilante extends Fighter {

    public ShadowVigilante(int player, double startX) {
        super("Shadow Vigilante", 120, 4, 15, player, startX,
                Color.rgb(255, 75, 85));
    }

    @Override
    public String getTitle() {
        return "The Silent Executioner";
    }

    @Override
    public String getLore() {
        return "Teleports behind the enemy and slashes.";
    }

    @Override
    public String getSpecialMoveDescription() {
        return "Shadow Strike: warp behind the target and cut.";
    }

    @Override
    public String getLightAttackName() {
        return "Night Cut";
    }

    @Override
    public String getHeavyAttackName() {
        return "Umbra Slash";
    }

    @Override
    public int getUltimateDamage() {
        return 42;
    }

    @Override
    protected double getAttackEffectHue() {
        return -0.05;
    }

    @Override
    public Color getImpactColor() {
        return Color.rgb(255, 75, 85);
    }

    @Override
    public String getPortraitPath() {
        return "/assets/portraits/shadow_vigilante.png";
    }

    // Sprite set: /assets/characters/warrior/sheet.png (see SpriteSets.java).
    // The artwork faces right, so isSpriteFacingLeft() keeps its default (false).
    @Override
    protected String getSpriteSetId() {
        return "warrior";
    }

    // ---------------- Signature move: Shadow Strike ----------------
    @Override
    public String getSpecialMoveName() {
        return "Shadow Strike";
    }

    @Override
    public double getSpecialCooldown() {
        return 4.5;
    }

    @Override
    protected double getSpecialEffectDuration() {
        return 0.35;
    }

    @Override
    protected boolean performSpecialMove(Fighter opponent) {
        if (Math.abs(opponent.x - x) > 320) {
            return false; // too far, no cooldown spent
        }

        double side = opponent.x > x ? 1.0 : -1.0;

        // Land just behind the opponent, then turn back toward them.
        shiftX((opponent.x - x) + side * 70);
        facingRight = opponent.x > x;
        double dir = facingRight ? 1.0 : -1.0;

        startSpecialEffect(x + dir * 50, y - STAND_HEIGHT * 0.5, 190,
                !facingRight, getAttackEffectHue(), 0.35);

        strikeTarget(opponent, 20, 0.4, dir * 320, 0);
        return true;
    }
}