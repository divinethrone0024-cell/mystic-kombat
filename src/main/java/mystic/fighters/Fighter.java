package mystic.fighters;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import mystic.combat.AttackType;
import mystic.common.Action;
import mystic.common.Constants;
import mystic.common.FighterState;
import mystic.common.Rect;
import mystic.engine.InputManager;

/** Shared behaviour of every fighter. Each character extends this class. */
public abstract class Fighter {
    public static final double WIDTH = 64;
    public static final double STAND_HEIGHT = 96;
    public static final double CROUCH_HEIGHT = 56;

    private static final double SPEED_SCALE = 60; // speed stat 5 -> 300 pixels per second
    private static final double JUMP_SPEED = 900; // pixels per second, upwards
    private static final double KNOCKBACK_DECEL = 1000; // how fast a knockback push fades

    protected final String name;
    protected final int maxHealth;
    protected int health;
    protected final int speed;
    protected final int damage;
    protected final int player; // 1 or 2: decides which keys control this fighter
    protected final Color color;
    private final double startX;

    protected double x; // horizontal centre
    protected double y; // y of the feet
    protected double vy; // vertical speed
    protected double knockbackVx; // push from the last hit
    protected double hitstunTimer; // seconds left in hit-stun
    protected boolean onGround = true;
    protected boolean facingRight;
    protected FighterState state = FighterState.IDLE;

    private AttackType currentAttack; // null when not attacking
    private double attackTimer; // seconds since the attack started
    private boolean hasHit; // has this attack already hit someone?

    protected Fighter(String name, int maxHealth, int speed, int damage,
            int player, double startX, Color color) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.speed = speed;
        this.damage = damage;
        this.player = player;
        this.startX = startX;
        this.color = color;
        resetForRound();
    }

    /** Put the fighter back at the start of a round. */
    public final void resetForRound() {
        health = maxHealth;
        x = startX;
        y = Constants.GROUND_Y;
        vy = 0;
        knockbackVx = 0;
        hitstunTimer = 0;
        onGround = true;
        currentAttack = null;
        attackTimer = 0;
        hasHit = false;
        state = FighterState.IDLE;
        facingRight = startX < Constants.WIDTH / 2.0;
    }

    /** Every character has its own ultimate. */
    public abstract void useUltimate();

    public void update(double dt, InputManager input, Fighter opponent) {
        // knocked out: only fall and slide
        if (state == FighterState.KO) {
            physics(dt, 0);
            return;
        }

        // in hit-stun: cannot act
        if (hitstunTimer > 0) {
            hitstunTimer -= dt;
            state = FighterState.HITSTUN;
            physics(dt, 0);
            if (hitstunTimer <= 0) {
                hitstunTimer = 0;
                state = FighterState.IDLE;
            }
            return;
        }

        // in the middle of an attack: cannot move
        if (currentAttack != null) {
            attackTimer += dt;
            physics(dt, 0);
            if (attackTimer >= currentAttack.total()) {
                currentAttack = null;
                state = FighterState.IDLE;
            }
            return;
        }

        // free to act
        if (opponent != null) {
            facingRight = opponent.x > x;
        }
        boolean crouching = onGround && input.isDown(player, Action.CROUCH);

        if (onGround && !crouching) {
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

        double dir = 0;
        if (!crouching) {
            if (input.isDown(player, Action.LEFT))
                dir -= 1;
            if (input.isDown(player, Action.RIGHT))
                dir += 1;
        }
        if (onGround && !crouching && input.isDown(player, Action.JUMP)) {
            vy = -JUMP_SPEED;
            onGround = false;
        }

        physics(dt, dir * speed * SPEED_SCALE);

        // choose the state (animations will use this later)
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

    private void startAttack(AttackType attack) {
        currentAttack = attack;
        attackTimer = 0;
        hasHit = false;
        state = (attack == AttackType.HEAVY) ? FighterState.ATTACK_HEAVY : FighterState.ATTACK_LIGHT;
    }

    /**
     * Movement, knockback, gravity and arena edges. moveX is in pixels per second.
     */
    private void physics(double dt, double moveX) {
        x += (moveX + knockbackVx) * dt;

        // the knockback push slowly fades to zero
        double slow = KNOCKBACK_DECEL * dt;
        if (Math.abs(knockbackVx) <= slow) {
            knockbackVx = 0;
        } else {
            knockbackVx -= Math.signum(knockbackVx) * slow;
        }

        vy += Constants.GRAVITY * dt;
        y += vy * dt;
        if (y >= Constants.GROUND_Y) {
            y = Constants.GROUND_Y;
            vy = 0;
            onGround = true;
        }

        x = Math.max(WIDTH / 2, Math.min(Constants.WIDTH - WIDTH / 2, x));
    }

    /** Called by the combat system when this fighter is hit. */
    public void takeHit(int amount, double stunSeconds, double knockback) {
        if (state == FighterState.KO) {
            return;
        }
        health = Math.max(0, health - amount);
        currentAttack = null; // being hit cancels your own attack
        attackTimer = 0;
        knockbackVx = knockback;
        if (health == 0) {
            state = FighterState.KO;
            hitstunTimer = 0;
        } else {
            state = FighterState.HITSTUN;
            hitstunTimer = stunSeconds;
        }
    }

    /** Move sideways (used to push fighters apart) and stay inside the arena. */
    public void shiftX(double amount) {
        x = Math.max(WIDTH / 2, Math.min(Constants.WIDTH - WIDTH / 2, x + amount));
    }

    /** The area where this fighter can be hit. */
    public Rect getHurtbox() {
        if (state == FighterState.KO) {
            return new Rect(x - 48, y - 28, 96, 28); // lying down
        }
        double h = (state == FighterState.CROUCH) ? CROUCH_HEIGHT : STAND_HEIGHT;
        return new Rect(x - WIDTH / 2, y - h, WIDTH, h);
    }

    /**
     * The area where this fighter's attack can hit. Null unless the attack is in
     * its active phase.
     */
    public Rect getHitbox() {
        if (currentAttack == null) {
            return null;
        }
        boolean active = attackTimer >= currentAttack.startup
                && attackTimer < currentAttack.startup + currentAttack.active;
        if (!active) {
            return null;
        }
        double left = facingRight ? x + WIDTH / 2 : x - WIDTH / 2 - currentAttack.reach;
        return new Rect(left, y - STAND_HEIGHT + 20, currentAttack.reach, currentAttack.height);
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

    /** Damage of the current attack = damage stat x the attack's multiplier. */
    public int attackDamage() {
        return (int) Math.round(damage * currentAttack.damageMultiplier);
    }

    /** Placeholder drawing: a coloured rectangle. Real sprites come later. */
    public void render(GraphicsContext gc, boolean debug) {
        Rect hb = getHurtbox();
        Color body = color;
        if (state == FighterState.KO) {
            body = color.darker().darker();
        } else if (state == FighterState.HITSTUN) {
            body = color.brighter();
        }
        gc.setFill(body);
        gc.fillRect(hb.x(), hb.y(), hb.w(), hb.h());

        // a white "eye" shows which way the fighter faces
        gc.setFill(Color.WHITE);
        double eyeX = facingRight ? hb.x() + hb.w() - 18 : hb.x() + 6;
        gc.fillRect(eyeX, hb.y() + 14, 12, 10);

        // the active attack shows as a yellow box
        Rect hit = getHitbox();
        if (hit != null) {
            gc.setFill(Color.YELLOW);
            gc.fillRect(hit.x(), hit.y(), hit.w(), hit.h());
        }

        if (debug) {
            gc.setStroke(Color.LIMEGREEN);
            gc.strokeRect(hb.x(), hb.y(), hb.w(), hb.h());
        }

        gc.setFill(Color.WHITE);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText(name + " (" + state + ")", x, hb.y() - 8);
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

    public boolean isKO() {
        return state == FighterState.KO;
    }
}