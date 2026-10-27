
package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

public class SpeedsterSonic extends Fighter {

    public SpeedsterSonic(int player, double startX) {
        super(
                "Speedster Sonic",
                95,
                7,
                10,
                player,
                startX,
                Color.DEEPSKYBLUE);
    }

    @Override
    public void useUltimate() {
        System.out.println("SONIC OVERDRIVE!");
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.SONIC_OVERDRIVE;
    }

    @Override
    public int getUltimateDamage() {
        return 30;
    }
}
