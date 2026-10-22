
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

/** Shared behaviour of every fighter. */
public abstract class Fighter {
    public static final double WIDTH = 64;
    public static final double STAND_HEIGHT = 96;
    public static final double CROUCH_HEIGHT = 56;

    private static final double SPEED_SCALE = 60;
    private static final double JUMP_SPEED = 900;
    private static final double KNOCKBACK_DECEL = 1000;
    private static final double MAX_KI = 100.0;

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
    protected boolean onGround = true;
    protected boolean facingRight;
    protected FighterState state = FighterState.IDLE;

    private AttackType currentAttack;
    private double attackTimer;
    private boolean hasHit;

    // D7: Mystic Energy (Ki).
    private double ki = 0.0;

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

    /** Reset the fighter for a new round. */
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
        ki = 0.0;
        state = FighterState.IDLE;
        facingRight = startX < Constants.WIDTH / 2.0;
    }

    /** Each character implements its own ultimate attack. */
    public abstract void useUltimate();

    public void update(double dt, InputManager input, Fighter opponent) {
        if (state == FighterState.KO) {
            physics(dt, 0);
            return;
        }

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

        if (currentAttack != null) {
            attackTimer += dt;
            physics(dt, 0);

            if (attackTimer >= currentAttack.total()) {
                currentAttack = null;
                state = FighterState.IDLE;
            }
            return;
        }

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
            if (input.isDown(player, Action.LEFT)) {
                dir -= 1;
            }
            if (input.isDown(player, Action.RIGHT)) {
                dir += 1;
            }
        }

        if (onGround && !crouching
                && input.isDown(player, Action.JUMP)) {
            vy = -JUMP_SPEED;
            onGround = false;
        }

        physics(dt, dir * speed * SPEED_SCALE);

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

        state = attack == AttackType.HEAVY
                ? FighterState.ATTACK_HEAVY
                : FighterState.ATTACK_LIGHT;
    }

    private void physics(double dt, double moveX) {
        x += (moveX + knockbackVx) * dt;

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

        x = Math.max(
                WIDTH / 2,
                Math.min(Constants.WIDTH - WIDTH / 2, x));
    }

    /** Called when this fighter receives a successful hit. */
    public void takeHit(int amount, double stunSeconds, double knockback) {
        if (state == FighterState.KO) {
            return;
        }

        int actualDamage = Math.min(health, Math.max(0, amount));
        health -= actualDamage;

        // D7: gain Ki when receiving damage.
        gainKi(actualDamage * 0.7);

        currentAttack = null;
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

    /** Add Ki, capped at 100. */
    public void gainKi(double amount) {
        ki = Math.min(MAX_KI, ki + Math.max(0, amount));
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

    /** Spend a full Ki bar when an ultimate is activated. */
    public boolean consumeKiForUltimate() {
        if (!isUltimateReady()) {
            return false;
        }

        ki = 0;
        return true;
    }

    public void shiftX(double amount) {
        x = Math.max(
                WIDTH / 2,
                Math.min(Constants.WIDTH - WIDTH / 2, x + amount));
    }

    public Rect getHurtbox() {
        if (state == FighterState.KO) {
            return new Rect(x - 48, y - 28, 96, 28);
        }

        double h = state == FighterState.CROUCH
                ? CROUCH_HEIGHT
                : STAND_HEIGHT;

        return new Rect(x - WIDTH / 2, y - h, WIDTH, h);
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

        return new Rect(
                left,
                y - STAND_HEIGHT + 20,
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
        return (int) Math.round(
                damage * currentAttack.damageMultiplier);
    }

    /** Temporary rectangle-based fighter rendering. */
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

        gc.setFill(Color.WHITE);

        double eyeX = facingRight
                ? hb.x() + hb.w() - 18
                : hb.x() + 6;

        gc.fillRect(eyeX, hb.y() + 14, 12, 10);

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
