
package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

public class PlumberBrawler extends Fighter {

    public PlumberBrawler(int player, double startX) {
        super(
                "Plumber Brawler",
                130,
                3,
                17,
                player,
                startX,
                Color.FIREBRICK);
    }

    @Override
    public void useUltimate() {
        System.out.println("TITAN IMPACT!");
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.TITAN_IMPACT;
    }

    @Override
    public int getUltimateDamage() {
        return 42;
    }
}
