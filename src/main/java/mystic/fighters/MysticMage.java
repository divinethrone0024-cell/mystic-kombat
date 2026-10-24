
package mystic.fighters;

import javafx.scene.paint.Color;
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
        // The visual effect is handled by UltimateEffect.
        System.out.println("ARCANE CATACLYSM!");
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.ARCANE_CATACLYSM;
    }
}
