package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

/**
 * Additional fighter archetype retained for future character-select expansion.
 * Uses the shared Fighter implementation so it remains compatible with the
 * current combat, hitbox, input, animation and round-reset systems.
 */
public class SpeedsterSonic extends Fighter {

    public SpeedsterSonic(int player, double startX) {
        super("Speedster Sonic", 95, 7, 10, player, startX, Color.DEEPSKYBLUE);
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.ARCANE_CATACLYSM;
    }
}
