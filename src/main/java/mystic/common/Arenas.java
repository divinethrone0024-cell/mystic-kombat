package mystic.common;

/**
 * Arena names, descriptions and gameplay bonuses. Index matches the arena
 * index.
 */
public final class Arenas {
    private Arenas() {
    }

    public static final String[] NAMES = {
            "KAMAR-TAJ: THE SANCTUM", "MIRROR DIMENSION",
            "GOTHAM: BLOOD MOON", "MULTIVERSE BREAKDOWN"
    };

    public static final String[] DESC = {
            "Golden mandalas awaken beneath crimson sanctum arches.",
            "A city folds in on itself, shattered into endless mirrors.",
            "A blood moon burns over rain-dark rooftops and gothic stone.",
            "Realities collide in a storm of crimson, cyan and molten gold."
    };

    private static final String[] FAVORED = {
            "MysticMage", "SpeedsterSonic", "ShadowVigilante", "PlumberBrawler"
    };

    public static final String[] EFFECT = {
            "Mystic Mage: +12% damage, +25% Ki",
            "Speedster Sonic: +12% damage, +25% Ki",
            "Shadow Vigilante: +12% damage, +25% Ki",
            "Plumber Brawler +12% damage  |  ALL: +15% Ki"
    };

    public static boolean isFavored(int arena, String fighterClass) {
        int a = Math.floorMod(arena, FAVORED.length);
        return FAVORED[a].equals(fighterClass);
    }

    public static double damageMult(int arena, String fighterClass) {
        return isFavored(arena, fighterClass) ? 1.12 : 1.0;
    }

    public static double kiMult(int arena, String fighterClass) {
        double m = isFavored(arena, fighterClass) ? 1.25 : 1.0;
        if (Math.floorMod(arena, FAVORED.length) == 3)
            m = Math.max(m, 1.15);
        return m;
    }
}