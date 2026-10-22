package mystic.fighters;

import javafx.scene.paint.Color;

public class MysticMage extends Fighter {

    public MysticMage(int player, double startX) {
        super("Mystic Mage", 100, 5, 12, player, startX, Color.MEDIUMPURPLE);
    }

    @Override
    public void useUltimate() {
        System.out.println("Dimensional Blast!"); // real ultimate comes later
    }
}