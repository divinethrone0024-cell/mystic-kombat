package mystic.common;

import javafx.scene.paint.Color;

/**
 * Display data for the champions book. Order matches the engine's choice index.
 */
public final class Roster {
    private Roster() {
    }

    public record Hero(String name, String title, boolean playable,
            int health, int speed, int damage,
            String[] powers, Color color) {
    }

    // Stats are on a 1-10 scale, converted from your old values:
    // HP 100/120/95/130, SPD 5/4/7/3, DMG 12/15/10/17.
    public static final Hero[] HEROES = {
            new Hero("MYSTIC MAGE", "Master of the Arcane", true, 8, 7, 7,
                    new String[] {
                            "Special: Arcane Blast",
                            "Ultimate: Mystic Nova",
                            "Wields ancient magic in battle." },
                    Color.rgb(176, 108, 255)),
            new Hero("SHADOW VIGILANTE", "The Silent Executioner", true, 9, 6, 9,
                    new String[] {
                            "Special: Shadow Strike",
                            "Ultimate: Eternal Night",
                            "A relentless close-range warrior." },
                    Color.rgb(255, 75, 85)),
            new Hero("SPEEDSTER SONIC", "The Lightning Fighter", true, 7, 10, 6,
                    new String[] {
                            "Special: Lightning Rush",
                            "Ultimate: Sonic Overdrive",
                            "Uses speed and dash attacks." },
                    Color.rgb(50, 205, 255)),
            // Set playable to true once PlumberBrawler.java exists (see below).
            new Hero("PLUMBER BRAWLER", "The Heavy Hitter", false, 10, 4, 10,
                    new String[] {
                            "Special: Power Smash",
                            "Ultimate: Titan Impact",
                            "Uses powerful, heavy strikes.",
                            "(Locked: fighter not built yet)" },
                    Color.rgb(255, 180, 65)),
    };

    /** Number of fighters the engine can actually create. */
    public static final int PLAYABLE = 3;
}