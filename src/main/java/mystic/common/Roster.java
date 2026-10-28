package mystic.common;

import javafx.scene.paint.Color;

/**
 * Display data for the champions book. Order matches the engine's choice index.
 *
 * Indexes 0-3 are the original roster, 4-9 are the new fighters. Stats are
 * on a 1-10 scale and match the real values in each Fighter class.
 */
public final class Roster {
        private Roster() {
        }

        public record Hero(String name, String title, boolean playable,
                        int health, int speed, int damage,
                        String[] powers, Color color) {
        }

        public static final Hero[] HEROES = {
                        // 0
                        new Hero("MYSTIC MAGE", "Master of the Arcane", true, 8, 7, 7,
                                        new String[] {
                                                        "Special: Arcane Bolt",
                                                        "Ultimate: Arcane Cataclysm",
                                                        "Wields ancient magic in battle." },
                                        Color.rgb(176, 108, 255)),
                        // 1
                        new Hero("SHADOW VIGILANTE", "The Silent Executioner", true, 9, 6, 9,
                                        new String[] {
                                                        "Special: Shadow Strike",
                                                        "Ultimate: Eternal Night",
                                                        "Teleports behind the enemy and slashes." },
                                        Color.rgb(255, 75, 85)),
                        // 2
                        new Hero("SPEEDSTER SONIC", "The Lightning Fighter", true, 7, 10, 6,
                                        new String[] {
                                                        "Special: Lightning Rush",
                                                        "Ultimate: Sonic Overdrive",
                                                        "Dashes straight through the enemy." },
                                        Color.rgb(50, 205, 255)),
                        // 3
                        new Hero("PLUMBER BRAWLER", "The Heavy Hitter", true, 10, 4, 10,
                                        new String[] {
                                                        "Special: Power Smash",
                                                        "Ultimate: Titan Impact",
                                                        "Slow, but every hit launches." },
                                        Color.rgb(255, 180, 65)),

                        // ---- New fighters (indexes 4-9) ----
                        new Hero("FLAME KARATEKA", "The Rushdown Master", true, 8, 8, 7,
                                        new String[] {
                                                        "Special: Blazing Uppercut",
                                                        "Ultimate: Dragon Flame Barrage",
                                                        "Rushdown: builds Ki fast when attacking.",
                                                        "Strongest in the Blood Moon." },
                                        Color.rgb(255, 110, 40)),
                        new Hero("CYBER HUNTER", "The Long-Range Zoner", true, 7, 7, 6,
                                        new String[] {
                                                        "Special: Charge Shot",
                                                        "Ultimate: Plasma Overdrive",
                                                        "Zoner: charges Ki when hit.",
                                                        "Strongest in the Multiverse Breakdown." },
                                        Color.rgb(40, 130, 255)),
                        new Hero("STREET STRIKER", "The Kick Specialist", true, 7, 8, 7,
                                        new String[] {
                                                        "Special: Sky Dive Kick",
                                                        "Ultimate: Hundred Kick Storm",
                                                        "Mix-up: closes gaps with aerial kicks.",
                                                        "Strongest in the Sanctum." },
                                        Color.rgb(120, 190, 90)),
                        new Hero("STAR SAGE", "The Teleporting Sage", true, 7, 7, 7,
                                        new String[] {
                                                        "Special: Warp Strike",
                                                        "Ultimate: Meteor Rain",
                                                        "Trickster: teleports behind enemies.",
                                                        "Strongest in the Sanctum." },
                                        Color.rgb(255, 210, 60)),
                        new Hero("PHANTOM BLADE", "The Silent Assassin", true, 6, 10, 8,
                                        new String[] {
                                                        "Special: Shadow Dash Slash",
                                                        "Ultimate: Crimson Moon Flurry",
                                                        "Assassin: fastest, but fragile.",
                                                        "Strongest in the Blood Moon." },
                                        Color.rgb(160, 140, 255)),
                        new Hero("TOXIC TOAD", "The Swamp Tank", true, 10, 4, 6,
                                        new String[] {
                                                        "Special: Tongue Lash",
                                                        "Ultimate: Poison Burst",
                                                        "Tank: charges Ki fastest when hit.",
                                                        "Strongest in the Multiverse Breakdown." },
                                        Color.rgb(230, 40, 60)),
        };

        /** Every hero is playable now; kept for older code that references it. */
        public static final int PLAYABLE = HEROES.length;

        public static boolean isPlayable(int index) {
                return index >= 0 && index < HEROES.length && HEROES[index].playable();
        }
}