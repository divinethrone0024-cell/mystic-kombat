
package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

public class ShadowVigilante extends Fighter {

    public ShadowVigilante(int player, double startX) {
        super(
                "Shadow Vigilante",
                120,
                4,
                15,
                player,
                startX,
                Color.ORANGERED);
    }

    @Override
    public void useUltimate() {
        // The visual effect is handled by UltimateEffect.
        System.out.println("ECLIPSE BREAKER!");
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.ECLIPSE_BREAKER;
    }
}
