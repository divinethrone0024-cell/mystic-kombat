
package mystic.combat;

import javafx.scene.paint.Color;

public enum UltimateType {

    ARCANE_CATACLYSM(
            "ARCANE CATACLYSM",
            Color.MEDIUMPURPLE,
            Color.VIOLET,
            Color.GOLD),

    ECLIPSE_BREAKER(
            "ECLIPSE BREAKER",
            Color.CRIMSON,
            Color.DARKRED,
            Color.ORANGERED);

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
