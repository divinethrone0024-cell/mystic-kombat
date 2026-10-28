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
            Color.ORANGE),

    // Flame Karateka.
    DRAGON_FLAME_BARRAGE(
            "DRAGON FLAME BARRAGE",
            Color.ORANGERED,
            Color.DARKORANGE,
            Color.GOLD),

    // Cyber Hunter.
    PLASMA_OVERDRIVE(
            "PLASMA OVERDRIVE",
            Color.DODGERBLUE,
            Color.DEEPSKYBLUE,
            Color.WHITE),

    // Street Striker.
    HUNDRED_KICK_STORM(
            "HUNDRED KICK STORM",
            Color.LIMEGREEN,
            Color.DARKOLIVEGREEN,
            Color.YELLOW),

    // Star Sage.
    METEOR_RAIN(
            "METEOR RAIN",
            Color.GOLD,
            Color.ORANGE,
            Color.LIGHTYELLOW),

    // Phantom Blade.
    CRIMSON_MOON_FLURRY(
            "CRIMSON MOON FLURRY",
            Color.MEDIUMSLATEBLUE,
            Color.DARKSLATEBLUE,
            Color.LAVENDER),

    // Toxic Toad.
    POISON_BURST(
            "POISON BURST",
            Color.YELLOWGREEN,
            Color.DARKGREEN,
            Color.GREENYELLOW);

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
