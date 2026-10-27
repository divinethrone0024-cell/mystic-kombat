
package mystic.combat;

import javafx.scene.paint.Color;

public enum UltimateType {

    // Original Mystic Mage ultimate.
    ARCANE_CATACLYSM(
            "ARCANE CATACLYSM",
            Color.MEDIUMPURPLE,
            Color.VIOLET,
            Color.GOLD),

    // Original Shadow Vigilante ultimate.
    ECLIPSE_BREAKER(
            "ECLIPSE BREAKER",
            Color.CRIMSON,
            Color.DARKRED,
            Color.ORANGERED),

    // New Speedster Sonic ultimate.
    SONIC_OVERDRIVE(
            "SONIC OVERDRIVE",
            Color.DEEPSKYBLUE,
            Color.DODGERBLUE,
            Color.LIGHTCYAN),

    // New Plumber Brawler ultimate.
    TITAN_IMPACT(
            "TITAN IMPACT",
            Color.FIREBRICK,
            Color.DARKRED,
            Color.ORANGE);

    private final String displayName;
    private final Color primaryColor;
    private final Color secondaryColor;
    private final Color accentColor;

    UltimateType(
            String displayName,
            Color primaryColor,
            Color secondaryColor,
            Color accentColor) {
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }
}
