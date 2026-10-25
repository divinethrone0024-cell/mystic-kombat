
package mystic.fighters;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import mystic.combat.AttackType;
import mystic.combat.UltimateType;
import mystic.common.Action;
import mystic.common.Constants;
import mystic.common.FighterState;
import mystic.common.Rect;
import mystic.engine.InputManager;

/** Shared behaviour of every fighter. */
public abstract class Fighter {

    public static final double WIDTH = 64;
    public static final double STAND_HEIGHT = 96;
    public static final double CROUCH_HEIGHT = 56;

    private static final double SPEED_SCALE = 60;
    private static final double JUMP_SPEED = 900;
    private static final double KNOCKBACK_DECEL = 1000;
    private static final double MAX_KI = 100.0;

    private static final int ULTIMATE_DAMAGE = 35;
    private static final double ULTIMATE_RANGE = 220.0;
    private static final double ULTIMATE_EFFECT_DURATION = 0.65;

    private static final double INPUT_BUFFER_SECONDS = 0.20;
    private static final double HIT_FLASH_DURATION = 0.10;

    // Launcher settings.
    private static final double LAUNCH_VELOCITY = 900.0;

    // Perfect Guard / Parry settings.
    private static final double PARRY_WINDOW_SECONDS = 0.16;
    private static final double PARRY_FLASH_DURATION = 0.30;
    private static final double PARRY_STAGGER_SECONDS = 0.45;
    private static final double PARRY_KI_REWARD = 12.0;

    // Dash settings.
    private static final double DASH_DURATION = 0.16;
    private static final double DASH_SPEED = 1050.0;
    private static final double DASH_COOLDOWN = 0.55;

    // Dodge settings.
    private static final double DODGE_DURATION = 0.24;
    private static final double DODGE_COOLDOWN = 0.85;
    private static final double DODGE_SPEED = 780.0;

    protected final String name;
    protected final int maxHealth;
    protected int health;
    protected final int speed;
    protected final int damage;
    protected final int player;
    protected final Color color;

    private final double startX;

    protected double x;
    protected double y;
    protected double vy;
    protected double knockbackVx;
    protected double hitstunTimer;

    private boolean launched = false;

    protected boolean onGround = true;
    protected boolean facingRight;
    protected FighterState state = FighterState.IDLE;

    private AttackType currentAttack;
    private double attackTimer;
    private boolean hasHit;

    // Buffered input for chaining attacks.
    private AttackType bufferedAttack;
    private double bufferedAttackTimer;

    // Mystic Energy / Ultimate Incantation.
    private double ki = 0.0;
    private boolean ultimateActive = false;
    private double ultimateEffectTimer = 0.0;

    // Hit feedback.
    private double hitFlashTimer = 0.0;

    // Blocking state.
    private boolean blocking = false;

    // Perfect Guard / Parry state.
    private double parryWindowTimer = 0.0;
    private double parryFlashTimer = 0.0;

    // Dash state.
    private double dashTimer = 0.0;
    private double dashCooldownTimer = 0.0;
    private double dashDirection = 0.0;

    // Dodge state.
    private double dodgeTimer = 0.0;
    private double dodgeCooldownTimer = 0.0;
    private double dodgeDirection = 0.0;

    protected Fighter(
            String name,
            int maxHealth,
            int speed,
            int damage,
            int player,
            double startX,
            Color color) {

        this.name = name;
        this.maxHealth = maxHealth;
        this.speed = speed;
        this.damage = damage;
        this.player = player;
        this.startX = startX;
        this.color = color;

        resetForRound();
    }

    public UltimateType getUltimateType() {
        return UltimateType.ARCANE_CATACLYSM;
    }

    /** Reset the fighter and Ki meter for a new round. */
    public final void resetForRound() {
        health = maxHealth;

        x = startX;
        y = Constants.GROUND_Y;

        vy = 0;
        knockbackVx = 0;
        hitstunTimer = 0;
        launched = false;

        onGround = true;

        currentAttack = null;
        attackTimer = 0;
        hasHit = false;

        bufferedAttack = null;
        bufferedAttackTimer = 0;

        ki = 0;
        ultimateActive = false;
        ultimateEffectTimer = 0;

        hitFlashTimer = 0;
        blocking = false;

        parryWindowTimer = 0;
        parryFlashTimer = 0;

        dashTimer = 0;
        dashCooldownTimer = 0;
        dashDirection = 0;

        dodgeTimer = 0;
        dodgeCooldownTimer = 0;
        dodgeDirection = 0;

        state = FighterState.IDLE;
        facingRight = startX < Constants.WIDTH / 2.0;
    }

    /** Optional character-specific ultimate hook. */
    public void useUltimate() {
        // Character-specific behaviour can be implemented in subclasses.
    }

    public void update(
            double dt,
            InputManager input,
            Fighter opponent) {

        if (!Double.isFinite(dt) || dt <= 0 || input == null) {
            return;
        }

        updateUltimateEffect(dt);
        updateHitFlash(dt);
        updateMovementAbilityTimers(dt);

        parryFlashTimer = Math.max(0, parryFlashTimer - dt);
        parryWindowTimer = Math.max(0, parryWindowTimer - dt);

        if (state == FighterState.KO) {
            blocking = false;
            parryWindowTimer = 0;
            dashTimer = 0;
            dodgeTimer = 0;
            physics(dt, 0);
            return;
        }

        // Hit-stun prevents movement abilities, blocking and attacks.
        if (hitstunTimer > 0) {
            blocking = false;
            parryWindowTimer = 0;
            dashTimer = 0;
            dodgeTimer = 0;

            hitstunTimer -= dt;

            state = FighterState.HITSTUN;
            physics(dt, 0);

            if (hitstunTimer <= 0) {
                hitstunTimer = 0;

                if (onGround) {
                    launched = false;
                    state = FighterState.IDLE;
                } else {
                    state = FighterState.JUMP;
                }
            }

            return;
        }

        // Update facing before choosing a default dodge direction.
        if (opponent != null) {
            facingRight = opponent.x > x;
        }

        /*
         * Continue an active dash or dodge before processing other
         * actions. Dash is not invulnerable; dodge is.
         */
        if (dashTimer > 0) {
            blocking = false;
            parryWindowTimer = 0;
            state = FighterState.DASHING;
            physics(dt, dashDirection * DASH_SPEED);
            return;
        }

        if (dodgeTimer > 0) {
            blocking = false;
            parryWindowTimer = 0;
            state = FighterState.DODGING;
            physics(dt, dodgeDirection * DODGE_SPEED);
            return;
        }

        boolean crouching = onGround
                && input.isDown(player, Action.CROUCH);

        // Blocking is grounded and cancels movement and attacks.
        blocking = onGround
                && input.isDown(player, Action.BLOCK)
                && currentAttack == null
                && hitstunTimer <= 0;

        // A fresh block press opens a short parry window.
        if (blocking
                && input.wasPressed(player, Action.BLOCK)) {
            parryWindowTimer = PARRY_WINDOW_SECONDS;
        }

        if (!blocking) {
            parryWindowTimer = 0;
        }

        if (blocking) {
            bufferedAttack = null;
            bufferedAttackTimer = 0;

            if (opponent != null) {
                facingRight = opponent.x > x;
            }

            state = FighterState.BLOCKING;
            physics(dt, 0);
            return;
        }

        /*
         * Dash and dodge require fresh key presses.
         * They cannot interrupt an existing attack.
         */
        if (currentAttack == null && !crouching) {
            double requestedDirection = 0;

            if (input.isDown(player, Action.LEFT)) {
                requestedDirection -= 1;
            }

            if (input.isDown(player, Action.RIGHT)) {
                requestedDirection += 1;
            }

            if (input.wasPressed(player, Action.DODGE)) {
                if (startDodge(requestedDirection)) {
                    physics(dt, dodgeDirection * DODGE_SPEED);
                    return;
                }
            }

            if (input.wasPressed(player, Action.DASH)) {
                if (startDash(requestedDirection)) {
                    physics(dt, dashDirection * DASH_SPEED);
                    return;
                }
            }
        }

        // Continue the current attack and accept buffered inputs.
        if (currentAttack != null) {

            if (!crouching) {
                if (input.wasPressed(player, Action.LAUNCHER)) {
                    bufferedAttack = AttackType.LAUNCHER;
                    bufferedAttackTimer = INPUT_BUFFER_SECONDS;

                } else if (input.wasPressed(player, Action.HEAVY)) {
                    bufferedAttack = AttackType.HEAVY;
                    bufferedAttackTimer = INPUT_BUFFER_SECONDS;

                } else if (input.wasPressed(player, Action.LIGHT)) {
                    bufferedAttack = AttackType.LIGHT;
                    bufferedAttackTimer = INPUT_BUFFER_SECONDS;

                } else if (bufferedAttack != null) {
                    bufferedAttackTimer -= dt;

                    if (bufferedAttackTimer <= 0) {
                        bufferedAttack = null;
                        bufferedAttackTimer = 0;
                    }
                }

            } else {
                bufferedAttack = null;
                bufferedAttackTimer = 0;
            }

            attackTimer += dt;
            physics(dt, 0);

            if (attackTimer >= currentAttack.total()) {
                currentAttack = null;

                if (bufferedAttack != null
                        && bufferedAttackTimer > 0) {

                    AttackType nextAttack = bufferedAttack;

                    bufferedAttack = null;
                    bufferedAttackTimer = 0;

                    startAttack(nextAttack);

                } else {
                    state = onGround
                            ? FighterState.IDLE
                            : FighterState.JUMP;
                }
            }

            return;
        }

        // Attacks are allowed on the ground and in the air,
        // but not while crouching.
        if (!crouching) {
            if (input.wasPressed(player, Action.LAUNCHER)) {
                startAttack(AttackType.LAUNCHER);
                physics(dt, 0);
                return;
            }

            if (input.wasPressed(player, Action.HEAVY)) {
                startAttack(AttackType.HEAVY);
                physics(dt, 0);
                return;
            }

            if (input.wasPressed(player, Action.LIGHT)) {
                startAttack(AttackType.LIGHT);
                physics(dt, 0);
                return;
            }
        }

        // Horizontal movement.
        double dir = 0;

        if (!crouching) {
            if (input.isDown(player, Action.LEFT)) {
                dir -= 1;
            }

            if (input.isDown(player, Action.RIGHT)) {
                dir += 1;
            }
        }

        // Jump only on a fresh key press.
        if (onGround
                && !crouching
                && input.wasPressed(player, Action.JUMP)) {

            vy = -JUMP_SPEED;
            onGround = false;
        }

        physics(dt, dir * speed * SPEED_SCALE);

        // Update animation state.
        if (!onGround) {
            state = FighterState.JUMP;

        } else if (crouching) {
            state = FighterState.CROUCH;

        } else if (dir != 0) {
            state = FighterState.WALK;

        } else {
            state = FighterState.IDLE;
        }
    }

    /** Update dash/dodge durations and cooldowns. */
    private void updateMovementAbilityTimers(double dt) {
        dashCooldownTimer = Math.max(0, dashCooldownTimer - dt);
        dodgeCooldownTimer = Math.max(0, dodgeCooldownTimer - dt);

        dashTimer = Math.max(0, dashTimer - dt);
        dodgeTimer = Math.max(0, dodgeTimer - dt);
    }

    /** Start a short dash in the requested horizontal direction. */
    private boolean startDash(double direction) {
        if (dashCooldownTimer > 0
                || dashTimer > 0
                || dodgeTimer > 0
                || !onGround
                || currentAttack != null
                || blocking
                || state == FighterState.KO
                || hitstunTimer > 0) {
            return false;
        }

        dashDirection = direction == 0
                ? (facingRight ? 1.0 : -1.0)
                : Math.signum(direction);

        dashTimer = DASH_DURATION;
        dashCooldownTimer = DASH_COOLDOWN;

        bufferedAttack = null;
        bufferedAttackTimer = 0;

        state = FighterState.DASHING;
        return true;
    }

    /** Start a brief evasive dodge. */
    private boolean startDodge(double direction) {
        if (dodgeCooldownTimer > 0
                || dodgeTimer > 0
                || dashTimer > 0
                || !onGround
                || currentAttack != null
                || blocking
                || state == FighterState.KO
                || hitstunTimer > 0) {
            return false;
        }

        /*
         * A directional input chooses the dodge direction.
         * With no directional input, dodge away from the opponent.
         */
        dodgeDirection = direction == 0
                ? (facingRight ? -1.0 : 1.0)
                : Math.signum(direction);

        dodgeTimer = DODGE_DURATION;
        dodgeCooldownTimer = DODGE_COOLDOWN;

        bufferedAttack = null;
        bufferedAttackTimer = 0;

        state = FighterState.DODGING;
        return true;
    }

    /** True while a dash is active. A dash does not grant invulnerability. */
    public boolean isDashing() {
        return dashTimer > 0;
    }

    /** True while dodge invulnerability is active. */
    public boolean isDodging() {
        return dodgeTimer > 0;
    }

    /** CombatSystem uses this to determine whether an incoming attack is evaded. */
    public boolean canEvadeAttack() {
        return isDodging() && state != FighterState.KO;
    }

    public double getDashCooldownRemaining() {
        return dashCooldownTimer;
    }

    public double getDodgeCooldownRemaining() {
        return dodgeCooldownTimer;
    }

    private void startAttack(AttackType attack) {
        blocking = false;
        parryWindowTimer = 0;

        currentAttack = attack;
        attackTimer = 0;
        hasHit = false;

        if (attack == AttackType.HEAVY
                || attack == AttackType.LAUNCHER) {
            state = FighterState.ATTACK_HEAVY;
        } else {
            state = FighterState.ATTACK_LIGHT;
        }
    }

    private void physics(double dt, double moveX) {
        x += (moveX + knockbackVx) * dt;

        // Gradually reduce knockback velocity.
        double slow = KNOCKBACK_DECEL * dt;

        if (Math.abs(knockbackVx) <= slow) {
            knockbackVx = 0;
        } else {
            knockbackVx -= Math.signum(knockbackVx) * slow;
        }

        // Gravity and vertical movement.
        vy += Constants.GRAVITY * dt;
        y += vy * dt;

        // Land on the ground.
        if (y >= Constants.GROUND_Y) {
            y = Constants.GROUND_Y;
            vy = 0;
            onGround = true;
            launched = false;
        } else {
            onGround = false;
        }

        // Keep the fighter inside the arena.
        x = Math.max(
                WIDTH / 2,
                Math.min(Constants.WIDTH - WIDTH / 2, x));
    }

    /** True while the short perfect-guard window is active. */
    public boolean isPerfectGuardActive() {
        return blocking && parryWindowTimer > 0;
    }

    /** Consume a successful parry opportunity. */
    public boolean consumePerfectGuard() {
        if (!isPerfectGuardActive()) {
            return false;
        }

        parryWindowTimer = 0;
        parryFlashTimer = PARRY_FLASH_DURATION;
        gainKi(PARRY_KI_REWARD);

        return true;
    }

    /** Briefly stagger the attacker after a successful parry. */
    public void staggerFromParry() {
        if (state == FighterState.KO) {
            return;
        }

        blocking = false;
        parryWindowTimer = 0;

        currentAttack = null;
        attackTimer = 0;
        hasHit = false;

        bufferedAttack = null;
        bufferedAttackTimer = 0;

        dashTimer = 0;
        dodgeTimer = 0;

        knockbackVx = 0;
        hitstunTimer = PARRY_STAGGER_SECONDS;
        state = FighterState.HITSTUN;
    }

    /** Called when the fighter receives a successful normal hit. */
    public void takeHit(
            int amount,
            double stunSeconds,
            double knockback) {

        if (state == FighterState.KO) {
            return;
        }

        int actualDamage = Math.min(
                health,
                Math.max(0, amount));

        health -= actualDamage;

        // Taking damage also generates Mystic Energy.
        gainKi(actualDamage * 0.7);

        hitFlashTimer = HIT_FLASH_DURATION;

        blocking = false;
        parryWindowTimer = 0;

        dashTimer = 0;
        dodgeTimer = 0;

        currentAttack = null;
        attackTimer = 0;
        hasHit = false;

        bufferedAttack = null;
        bufferedAttackTimer = 0;

        knockbackVx = Double.isFinite(knockback)
                ? knockback
                : 0;

        launched = false;

        if (health <= 0) {
            health = 0;
            state = FighterState.KO;
            hitstunTimer = 0;

        } else {
            state = FighterState.HITSTUN;

            hitstunTimer = Double.isFinite(stunSeconds)
                    ? Math.max(0, stunSeconds)
                    : 0;
        }
    }

    /**
     * Called when a launcher successfully hits.
     * Applies normal hit effects and launches the target upward.
     */
    public void takeLauncherHit(
            int amount,
            double stunSeconds,
            double knockback,
            double launchVelocity) {

        takeHit(amount, stunSeconds, knockback);

        if (state != FighterState.KO) {
            vy = -Math.abs(
                    Double.isFinite(launchVelocity)
                            ? launchVelocity
                            : LAUNCH_VELOCITY);

            onGround = false;
            launched = true;
        }
    }

    /**
     * Applies a follow-up hit to a fighter who is already airborne
     * from a launcher. Preserves the existing vertical momentum.
     */
    public void takeAirComboHit(
            int amount,
            double stunSeconds,
            double knockback) {

        takeHit(amount, stunSeconds, knockback);

        if (state != FighterState.KO) {
            launched = true;
            onGround = false;
        }
    }

    /** True when this fighter is currently in a launcher juggle. */
    public boolean isLaunched() {
        return launched;
    }

    /**
     * Called when an attack is successfully blocked.
     * Applies reduced damage without normal hitstun or knockback.
     */
    public void takeBlockedHit(int amount) {
        if (state == FighterState.KO) {
            return;
        }

        int actualDamage = Math.min(
                health,
                Math.max(0, amount));

        health -= actualDamage;

        gainKi(actualDamage * 0.7);

        hitstunTimer = 0;
        knockbackVx = 0;
        launched = false;

        dashTimer = 0;
        dodgeTimer = 0;

        currentAttack = null;
        attackTimer = 0;
        hasHit = false;

        bufferedAttack = null;
        bufferedAttackTimer = 0;

        if (health <= 0) {
            health = 0;
            blocking = false;
            parryWindowTimer = 0;
            state = FighterState.KO;
        } else {
            blocking = true;
            state = FighterState.BLOCKING;
        }
    }

    private void updateHitFlash(double dt) {
        if (hitFlashTimer > 0) {
            hitFlashTimer = Math.max(0, hitFlashTimer - dt);
        }
    }

    /** Add Ki, capped at 100. */
    public void gainKi(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return;
        }

        ki = Math.min(MAX_KI, ki + amount);
    }

    public double getKi() {
        return ki;
    }

    public double getMaxKi() {
        return MAX_KI;
    }

    public boolean isUltimateReady() {
        return ki >= MAX_KI;
    }

    /** Consume the full Ki bar and start the visual effect. */
    public boolean consumeKiForUltimate() {
        if (!isUltimateReady()
                || ultimateActive
                || isKO()) {
            return false;
        }

        ki = 0;
        ultimateActive = true;
        ultimateEffectTimer = ULTIMATE_EFFECT_DURATION;

        return true;
    }

    public boolean isUltimateActive() {
        return ultimateActive;
    }

    public void updateUltimateEffect(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) {
            return;
        }

        if (ultimateEffectTimer > 0) {
            ultimateEffectTimer -= dt;

            if (ultimateEffectTimer <= 0) {
                ultimateEffectTimer = 0;
                ultimateActive = false;
            }
        }
    }

    public int getUltimateDamage() {
        return ULTIMATE_DAMAGE;
    }

    public double getUltimateRange() {
        return ULTIMATE_RANGE;
    }

    public void shiftX(double amount) {
        if (!Double.isFinite(amount)) {
            return;
        }

        x = Math.max(
                WIDTH / 2,
                Math.min(
                        Constants.WIDTH - WIDTH / 2,
                        x + amount));
    }

    public Rect getHurtbox() {
        if (state == FighterState.KO) {
            return new Rect(x - 48, y - 28, 96, 28);
        }

        double h = state == FighterState.CROUCH
                ? CROUCH_HEIGHT
                : STAND_HEIGHT;

        return new Rect(
                x - WIDTH / 2,
                y - h,
                WIDTH,
                h);
    }

    public Rect getHitbox() {
        if (currentAttack == null) {
            return null;
        }

        boolean active = attackTimer >= currentAttack.startup
                && attackTimer < currentAttack.startup + currentAttack.active;

        if (!active) {
            return null;
        }

        double left = facingRight
                ? x + WIDTH / 2
                : x - WIDTH / 2 - currentAttack.reach;

        // Follow the fighter vertically during jumps and air attacks.
        double hitboxY = y - STAND_HEIGHT + 20;

        return new Rect(
                left,
                hitboxY,
                currentAttack.reach,
                currentAttack.height);
    }

    public boolean hasHitThisAttack() {
        return hasHit;
    }

    public void markHit() {
        hasHit = true;
    }

    public AttackType getCurrentAttack() {
        return currentAttack;
    }

    public int attackDamage() {
        if (currentAttack == null) {
            return 0;
        }

        return (int) Math.round(
                damage * currentAttack.damageMultiplier);
    }

    /** Draw the fighter and temporary visual effects. */
    public void render(GraphicsContext gc, boolean debug) {
        Rect hb = getHurtbox();

        // Dash and dodge afterimages.
        if (isDashing() || isDodging()) {
            gc.save();

            try {
                gc.setGlobalAlpha(0.30);
                gc.setFill(
                        isDodging()
                                ? Color.DEEPSKYBLUE
                                : Color.AQUA);

                double direction = isDodging()
                        ? dodgeDirection
                        : dashDirection;

                for (int i = 1; i <= 3; i++) {
                    double trailX = x - direction * i * 13;

                    gc.fillRect(
                            trailX - WIDTH / 2,
                            hb.y(),
                            WIDTH,
                            hb.h());
                }
            } finally {
                gc.restore();
            }
        }

        Color body = color;

        if (state == FighterState.KO) {
            body = color.darker().darker();

        } else if (parryFlashTimer > 0) {
            body = Color.WHITE;

        } else if (hitFlashTimer > 0 && !blocking) {
            body = Color.WHITE;

        } else if (state == FighterState.HITSTUN) {
            body = color.brighter();

        } else if (blocking) {
            body = color.deriveColor(0, 0.75, 0.85, 1);

        } else if (isDodging()) {
            body = color.deriveColor(0, 0.65, 1.25, 0.85);

        } else if (isDashing()) {
            body = color.brighter();
        }

        if (ultimateActive) {
            gc.setStroke(Color.GOLD);
            gc.setLineWidth(5);

            gc.strokeOval(
                    x - 65,
                    y - 105,
                    130,
                    115);

            gc.setLineWidth(1);
        }

        gc.setFill(body);
        gc.fillRect(hb.x(), hb.y(), hb.w(), hb.h());

        // Eyes.
        gc.setFill(Color.WHITE);

        double eyeX = facingRight
                ? hb.x() + hb.w() - 18
                : hb.x() + 6;

        gc.fillRect(
                eyeX,
                hb.y() + 14,
                12,
                10);

        // Dash / dodge status labels.
        if (isDashing() || isDodging()) {
            gc.setFill(isDodging() ? Color.DEEPSKYBLUE : Color.AQUA);
            gc.setFont(Font.font("Arial", 12));
            gc.setTextAlign(TextAlignment.CENTER);

            gc.fillText(
                    isDodging() ? "DODGE" : "DASH",
                    x,
                    y - STAND_HEIGHT - 8);
        }

        // Blue guard shield.
        if (blocking) {
            gc.setStroke(
                    isPerfectGuardActive()
                            ? Color.WHITE
                            : Color.DEEPSKYBLUE);

            gc.setLineWidth(
                    isPerfectGuardActive() ? 7 : 4);

            double shieldX = facingRight
                    ? x + WIDTH / 2 - 2
                    : x - WIDTH / 2 - 20;

            gc.strokeArc(
                    shieldX,
                    y - STAND_HEIGHT + 8,
                    40,
                    72,
                    facingRight ? -90 : 90,
                    180,
                    javafx.scene.shape.ArcType.OPEN);

            gc.setLineWidth(1);

            gc.setFill(
                    isPerfectGuardActive()
                            ? Color.WHITE
                            : Color.DEEPSKYBLUE);

            gc.setFont(Font.font("Arial", 12));
            gc.setTextAlign(TextAlignment.CENTER);

            gc.fillText(
                    isPerfectGuardActive() ? "PARRY" : "GUARD",
                    x,
                    y - STAND_HEIGHT - 8);
        }

        // Successful parry flash.
        if (parryFlashTimer > 0) {
            double progress = parryFlashTimer / PARRY_FLASH_DURATION;
            double radius = 42 + (1 - progress) * 42;

            gc.setStroke(Color.WHITE.deriveColor(
                    0, 1, 1, Math.max(0, progress)));

            gc.setLineWidth(4);

            gc.strokeOval(
                    x - radius,
                    y - STAND_HEIGHT / 2 - radius,
                    radius * 2,
                    radius * 2);

            gc.setStroke(Color.DEEPSKYBLUE.deriveColor(
                    0, 1, 1, Math.max(0, progress)));

            gc.setLineWidth(2);

            gc.strokeOval(
                    x - radius * 0.72,
                    y - STAND_HEIGHT / 2 - radius * 0.72,
                    radius * 1.44,
                    radius * 1.44);

            gc.setLineWidth(1);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 18));
            gc.setTextAlign(TextAlignment.CENTER);

            gc.fillText(
                    "PERFECT GUARD!",
                    x,
                    y - STAND_HEIGHT - 24);
        }

        Rect hit = getHitbox();

        if (hit != null) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(
                    hit.x(),
                    hit.y(),
                    hit.w(),
                    hit.h());
        }

        if (debug) {
            gc.setStroke(Color.LIMEGREEN);
            gc.strokeRect(
                    hb.x(),
                    hb.y(),
                    hb.w(),
                    hb.h());
        }

        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 14));

        gc.fillText(
                name + " (" + state + ")",
                x,
                hb.y() - 8);
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public FighterState getState() {
        return state;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean isKO() {
        return state == FighterState.KO;
    }
}