package mystic.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** Persistent XP, upgrades, unlocked fighters and Arcade progress. */
public class ProgressionManager {

    public enum Stat {
        HEALTH(0.08, "Health"),
        DAMAGE(0.05, "Damage"),
        SPEED(0.03, "Speed");

        public final double bonusPerLevel;
        public final String label;

        Stat(double bonusPerLevel, String label) {
            this.bonusPerLevel = bonusPerLevel;
            this.label = label;
        }
    }

    public static final String STARTER = "mage";
    /** Compatibility with CharacterSelectScreen. Roster index of Mystic Mage. */
    public static final int STARTER_CHARACTER = 0;

    /** Compatibility with CharacterSelectScreen: unlocked check by Roster index. */
    public boolean isCharacterUnlocked(int choice) {
        return isUnlocked(GameEngine.fighterId(choice));
    }
    public static final int MAX_LEVEL = 10;
    public static final int ARCADE_WIN_XP = 100;
    public static final int ARCADE_TRIALS = 10;

    private static final Path SAVE_DIR = Path.of(System.getProperty("user.home"), ".mystic-kombat");
    private static final Path SAVE_FILE = SAVE_DIR.resolve("save.properties");

    private int xp;
    private int arcadeCleared; // number of trials beaten (0..10)
    private final Set<String> unlocked = new LinkedHashSet<>();
    private final Properties levels = new Properties(); // "mage.HEALTH" -> "3"

    public ProgressionManager() {
        unlocked.add(STARTER);
        load();
    }

    // ---------- XP ----------
    public int getXp() {
        return xp;
    }

    public void addXp(int amount) {
        if (amount > 0) {
            xp += amount;
            save();
        }
    }

    // ---------- Arcade ----------
    /** Call when the player wins trial index (0-based). Returns XP gained. */
    public int awardArcadeWin(int trialIndex) {
        int gained = 0;
        // Full XP only the first time a trial is cleared; replays give 25%.
        if (trialIndex >= arcadeCleared) {
            arcadeCleared = Math.min(ARCADE_TRIALS, trialIndex + 1);
            gained = ARCADE_WIN_XP;
        } else {
            gained = ARCADE_WIN_XP / 4;
        }
        xp += gained;
        save();
        return gained;
    }

    /** Number of trials the player may enter (cleared + the next one). */
    public int getArcadeUnlocked() {
        return Math.min(ARCADE_TRIALS, arcadeCleared + 1);
    }

    public int getArcadeCleared() {
        return arcadeCleared;
    }

    // ---------- Fighters ----------
    public boolean isUnlocked(String fighterId) {
        return unlocked.contains(fighterId);
    }

    public Set<String> getUnlocked() {
        return Set.copyOf(unlocked);
    }

    /** 250 XP for the first extra fighter, then +100 each time. */
    public int getFighterPrice() {
        return 250 + 100 * (unlocked.size() - 1);
    }

    public boolean unlockFighter(String fighterId) {
        if (isUnlocked(fighterId))
            return false;
        int price = getFighterPrice();
        if (xp < price)
            return false;
        xp -= price;
        unlocked.add(fighterId);
        save();
        return true;
    }

    // ---------- Upgrades ----------
    public int getLevel(String fighterId, Stat stat) {
        return Integer.parseInt(
                levels.getProperty(fighterId + "." + stat.name(), "0"));
    }

    /** Cost of the next level: 100, 200, 300 ... */
    public int getUpgradeCost(String fighterId, Stat stat) {
        return 100 * (getLevel(fighterId, stat) + 1);
    }

    public boolean canUpgrade(String fighterId, Stat stat) {
        return isUnlocked(fighterId)
                && getLevel(fighterId, stat) < MAX_LEVEL
                && xp >= getUpgradeCost(fighterId, stat);
    }

    public boolean upgrade(String fighterId, Stat stat) {
        if (!canUpgrade(fighterId, stat))
            return false;
        xp -= getUpgradeCost(fighterId, stat);
        levels.setProperty(fighterId + "." + stat.name(),
                String.valueOf(getLevel(fighterId, stat) + 1));
        save();
        return true;
    }

    /** Multiplier to apply in combat, e.g. 1.24 for +24%. */
    public double getMultiplier(String fighterId, Stat stat) {
        return 1.0 + getLevel(fighterId, stat) * stat.bonusPerLevel;
    }

    // ---------- Save / load ----------
    public void save() {
        try {
            Files.createDirectories(SAVE_DIR);
            Properties p = new Properties();
            p.putAll(levels);
            p.setProperty("xp", String.valueOf(xp));
            p.setProperty("arcadeCleared", String.valueOf(arcadeCleared));
            p.setProperty("unlocked", String.join(",", unlocked));
            try (OutputStream out = Files.newOutputStream(SAVE_FILE)) {
                p.store(out, "Mystic Kombat save");
            }
        } catch (IOException e) {
            System.err.println("Could not save progress: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(SAVE_FILE))
            return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(SAVE_FILE)) {
            p.load(in);
        } catch (IOException e) {
            System.err.println("Could not load progress: " + e.getMessage());
            return;
        }
        xp = Integer.parseInt(p.getProperty("xp", "0"));
        arcadeCleared = Integer.parseInt(p.getProperty("arcadeCleared", "0"));
        for (String id : p.getProperty("unlocked", STARTER).split(",")) {
            if (!id.isBlank())
                unlocked.add(id.trim());
        }
        for (String key : p.stringPropertyNames()) {
            if (key.contains("."))
                levels.setProperty(key, p.getProperty(key));
        }
    }
}