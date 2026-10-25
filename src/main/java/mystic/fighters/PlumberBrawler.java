package mystic.fighters;

import javafx.scene.paint.Color;
import mystic.combat.UltimateType;

/**
 * Additional fighter archetype retained for future character-select expansion.
 * Uses the shared Fighter implementation so it remains compatible with the
 * current combat, hitbox, input, animation and round-reset systems.
 */
public class PlumberBrawler extends Fighter {

    public PlumberBrawler(int player, double startX) {
        super("Plumber Brawler", 130, 3, 17, player, startX, Color.FIREBRICK);
    }

    @Override
    public UltimateType getUltimateType() {
        return UltimateType.ECLIPSE_BREAKER;
    }
}
