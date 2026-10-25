
package mystic.combat;

/**
 * The attacks every fighter can do. Each attack has three phases:
 * startup (nothing can hit yet), active (the hitbox exists), recovery (cannot
 * act).
 * All times are in seconds.
 */
public enum AttackType {
    // startup active recovery damage reach height hitstun knockback

    LIGHT(
            0.08, 0.10, 0.20,
            0.8, 70, 30, 0.25, 250),

    HEAVY(
            0.25, 0.12, 0.40,
            1.6, 90, 40, 0.45, 450),

    // Launches the opponent upward when launcher behavior is implemented.
    LAUNCHER(
            0.30, 0.14, 0.50,
            1.25, 85, 48, 0.55, 300);

    public final double startup;
    public final double active;
    public final double recovery;
    public final double damageMultiplier;
    public final double reach;
    public final double height;
    public final double hitstun;
    public final double knockback;

    AttackType(
            double startup,
            double active,
            double recovery,
            double damageMultiplier,
            double reach,
            double height,
            double hitstun,
            double knockback) {
        this.startup = startup;
        this.active = active;
        this.recovery = recovery;
        this.damageMultiplier = damageMultiplier;
        this.reach = reach;
        this.height = height;
        this.hitstun = hitstun;
        this.knockback = knockback;
    }

    public double total() {
        return startup + active + recovery;
    }
}
