
package mystic.combat;

public class MatchManager {

    public static final double ROUND_DURATION = 60.0;
    public static final int ROUNDS_TO_WIN = 2;

    private double timeRemaining;
    private int player1Wins;
    private int player2Wins;
    private int roundNumber;
    private boolean roundOver;
    private boolean matchOver;
    private int roundWinner; // 0 = draw, 1 = P1, 2 = P2

    public MatchManager() {
        resetMatch();
    }

    public void resetMatch() {
        player1Wins = 0;
        player2Wins = 0;
        roundNumber = 1;
        matchOver = false;
        startRound();
    }

    public void startRound() {
        timeRemaining = ROUND_DURATION;
        roundOver = false;
        roundWinner = 0;
    }

    public void update(double dt) {
        if (roundOver || matchOver) {
            return;
        }

        timeRemaining = Math.max(0, timeRemaining - dt);

        if (timeRemaining <= 0) {
            // Time-out: the fighter with more health wins.
            // Call finishRound(int) with the winner
            // determined by CombatScreen.
        }
    }

    public void finishRound(int winner) {
        if (roundOver || matchOver) {
            return;
        }

        roundOver = true;
        roundWinner = winner;

        if (winner == 1) {
            player1Wins++;
        } else if (winner == 2) {
            player2Wins++;
        }

        if (player1Wins >= ROUNDS_TO_WIN
                || player2Wins >= ROUNDS_TO_WIN) {
            matchOver = true;
        }
    }

    public void nextRound() {
        if (!roundOver || matchOver) {
            return;
        }

        roundNumber++;
        startRound();
    }

    public double getTimeRemaining() {
        return timeRemaining;
    }

    public int getPlayer1Wins() {
        return player1Wins;
    }

    public int getPlayer2Wins() {
        return player2Wins;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getRoundWinner() {
        return roundWinner;
    }

    public boolean isRoundOver() {
        return roundOver;
    }

    public boolean isMatchOver() {
        return matchOver;
    }

    public String getMatchWinner() {
        if (!matchOver) {
            return "";
        }

        if (player1Wins > player2Wins) {
            return "PLAYER 1 WINS!";
        }

        if (player2Wins > player1Wins) {
            return "PLAYER 2 WINS!";
        }

        return "MATCH DRAW!";
    }
}
