
package mystic.engine;

import java.util.Random;

import mystic.fighters.Fighter;

public final class FighterAI {

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum Decision {
        APPROACH,
        RETREAT,
        LIGHT_ATTACK,
        HEAVY_ATTACK,
        LAUNCHER,
        BLOCK,
        DODGE,
        DASH,
        CROUCH,
        ULTIMATE,
        WAIT
    }

    private final Difficulty difficulty;
    private final Random random = new Random();
    private double decisionTimer;

    public FighterAI(Difficulty difficulty) {
        this.difficulty = difficulty == null
                ? Difficulty.MEDIUM
                : difficulty;
    }

    public Decision update(double delta, Fighter bot, Fighter player) {
        if (bot == null || player == null || bot.isKO()
                || !Double.isFinite(delta) || delta < 0) {
            return Decision.WAIT;
        }

        decisionTimer -= delta;
        if (decisionTimer > 0) {
            return Decision.WAIT;
        }

        decisionTimer = switch (difficulty) {
            case EASY -> 0.48;
            case MEDIUM -> 0.26;
            case HARD -> 0.12;
        };

        return makeDecision(bot, player);
    }

    private Decision makeDecision(Fighter bot, Fighter player) {
        double distance = Math.abs(bot.getX() - player.getX());
        double roll = random.nextDouble();

        // Easy AI occasionally makes mistakes and rarely defends.
        if (difficulty == Difficulty.EASY) {
            if (bot.isUltimateReady() && roll < 0.20) {
                return Decision.ULTIMATE;
            }

            if (distance > 115) {
                return Decision.APPROACH;
            }

            if (roll < 0.52)
                return Decision.LIGHT_ATTACK;
            if (roll < 0.72)
                return Decision.HEAVY_ATTACK;
            if (roll < 0.82)
                return Decision.RETREAT;
            if (roll < 0.90)
                return Decision.BLOCK;
            return Decision.WAIT;
        }

        // Medium and Hard use ultimates more reliably.
        double ultimateChance = difficulty == Difficulty.HARD ? 0.85 : 0.55;

        if (bot.isUltimateReady() && roll < ultimateChance) {
            return Decision.ULTIMATE;
        }

        // Close-range combat.
        if (distance < 105) {
            if (difficulty == Difficulty.HARD && roll < 0.16) {
                return Decision.DODGE;
            }
            if (roll < 0.34)
                return Decision.LIGHT_ATTACK;
            if (roll < 0.57)
                return Decision.HEAVY_ATTACK;
            if (roll < 0.72)
                return Decision.LAUNCHER;
            if (roll < 0.84)
                return Decision.BLOCK;
            return Decision.RETREAT;
        }

        // Mid-range combat.
        if (distance < 230) {
            if (difficulty == Difficulty.HARD && roll < 0.20) {
                return Decision.DASH;
            }
            if (roll < 0.40)
                return Decision.APPROACH;
            if (roll < 0.62)
                return Decision.HEAVY_ATTACK;
            if (roll < 0.76)
                return Decision.BLOCK;
            if (roll < 0.87)
                return Decision.DODGE;
            return Decision.APPROACH;
        }

        // Long-range combat.
        if (difficulty == Difficulty.HARD && roll < 0.35) {
            return Decision.DASH;
        }

        return Decision.APPROACH;
    }
}
