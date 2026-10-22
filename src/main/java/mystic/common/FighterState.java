package mystic.common;

/** What a fighter is doing right now. Animations are chosen from this. */
public enum FighterState {
    IDLE,
    WALK,
    JUMP,
    CROUCH,
    ATTACK_LIGHT,
    ATTACK_HEAVY,
    HITSTUN,
    ULTIMATE,
    KO
}