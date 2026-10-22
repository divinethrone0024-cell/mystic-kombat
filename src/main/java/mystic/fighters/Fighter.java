package mystic.fighters;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
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
    protected boolean onGround = true;
    protected boolean facingRight;
    protected FighterState state = FighterState.IDLE;

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
    public void resetForRound() {
        health = maxHealth;
        x = startX;
        y = Constants.GROUND_Y;
        vy = 0;
        onGround = true;
        state = FighterState.IDLE;
        facingRight = startX < Constants.WIDTH / 2.0;
    }

    /** Every character has its own ultimate. */
    public abstract void useUltimate();

    public void update(double dt, InputManager input, Fighter opponent) {
        boolean crouching = onGround && input.isDown(player, Action.CROUCH);

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

        // movement and gravity
        x += dir * speed * SPEED_SCALE * dt;
        vy += Constants.GRAVITY * dt;
        y += vy * dt;
        if (y >= Constants.GROUND_Y) {
            y = Constants.GROUND_Y;
            vy = 0;
            onGround = true;
        }

        // stay inside the arena
        x = Math.max(WIDTH / 2, Math.min(Constants.WIDTH - WIDTH / 2, x));

        // always face the opponent
        if (opponent != null) {
            facingRight = opponent.x > x;
        }

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

    /** Move sideways (used to push fighters apart) and stay inside the arena. */
    public void shiftX(double amount) {
        x = Math.max(WIDTH / 2, Math.min(Constants.WIDTH - WIDTH / 2, x + amount));
    }

    /** The area where this fighter can be hit. */
    public Rect getHurtbox() {
        double h = (state == FighterState.CROUCH) ? CROUCH_HEIGHT : STAND_HEIGHT;
        return new Rect(x - WIDTH / 2, y - h, WIDTH, h);
    }

    /** Placeholder drawing: a coloured rectangle. Real sprites come later. */
    public void render(GraphicsContext gc) {
        Rect hb = getHurtbox();
        gc.setFill(color);
        gc.fillRect(hb.x(), hb.y(), hb.w(), hb.h());

        // a white "eye" shows which way the fighter faces
        gc.setFill(Color.WHITE);
        double eyeX = facingRight ? hb.x() + hb.w() - 18 : hb.x() + 6;
        gc.fillRect(eyeX, hb.y() + 14, 12, 10);

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
}