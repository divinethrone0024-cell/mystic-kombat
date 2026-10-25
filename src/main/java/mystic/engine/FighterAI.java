package mystic.engine;

import java.util.Random;

import mystic.fighters.Fighter;

/**
 * Decision-making helper for a CPU fighter.
 *
 * This class deliberately returns decisions instead of writing to Fighter's
 * private/protected fields. The combat loop must translate decisions into
 * game actions before this AI controls a fighter during a match.
 */
public final class FighterAI {

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }

    public enum Decision {
        APPROACH, RETREAT, LIGHT_ATTACK, HEAVY_ATTACK, LAUNCHER, ULTIMATE, WAIT
    }

    private final Difficulty difficulty;
    private final Random random = new Random();
    private double decisionTimer;

    public FighterAI(Difficulty difficulty) {
        this.difficulty = difficulty == null ? Difficulty.MEDIUM : difficulty;
    }

    /** Returns the next AI decision when its decision timer expires. */
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
            case EASY -> 0.45;
            case MEDIUM -> 0.25;
            case HARD -> 0.10;
        };

        return makeDecision(bot, player);
    }

    private Decision makeDecision(Fighter bot, Fighter player) {
        double distance = Math.abs(bot.getX() - player.getX());

        if (bot.isUltimateReady()
                && random.nextDouble() < (difficulty == Difficulty.HARD ? 0.90 : 0.40)) {
            return Decision.ULTIMATE;
        }

        if (distance < 100) {
            double roll = random.nextDouble();
            if (roll < 0.45)
                return Decision.LIGHT_ATTACK;
            if (roll < 0.75)
                return Decision.HEAVY_ATTACK;
            if (roll < 0.90)
                return Decision.LAUNCHER;
            return Decision.RETREAT;
        }

        if (distance < 230 && difficulty != Difficulty.EASY
                && random.nextDouble() < 0.35) {
            return Decision.HEAVY_ATTACK;
        }

        return Decision.APPROACH;
    }
}
