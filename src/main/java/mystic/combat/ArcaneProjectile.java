package mystic.combat;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.common.Rect;
import mystic.fighters.Fighter;

/** A travelling projectile fired by Mystic Mage. */
public final class ArcaneProjectile {

    private static final double SPEED = 620.0;
    private static final double RADIUS = 13.0;
    private static final int DAMAGE = 18;

    private final Fighter owner;
    private final double direction;

    private double x;
    private double y;
    private boolean active = true;

    public ArcaneProjectile(Fighter owner) {
        this.owner = owner;
        this.direction = owner.isFacingRight() ? 1.0 : -1.0;
        this.x = owner.getX() + direction * 45.0;
        this.y = owner.getY() - 52.0;
    }

    public void update(double dt) {
        if (!active || !Double.isFinite(dt) || dt <= 0) {
            return;
        }

        x += direction * SPEED * dt;

        if (x < -RADIUS || x > 5000) {
            active = false;
        }
    }

    public boolean hits(Fighter target) {
        if (!active || target == null || target.isKO()) {
            return false;
        }

        Rect hurtbox = target.getHurtbox();

        boolean overlaps = x + RADIUS >= hurtbox.x()
                && x - RADIUS <= hurtbox.x() + hurtbox.w()
                && y + RADIUS >= hurtbox.y()
                && y - RADIUS <= hurtbox.y() + hurtbox.h();

        return overlaps;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public Fighter getOwner() {
        return owner;
    }

    public int getDamage() {
        return DAMAGE;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void render(GraphicsContext gc) {
        if (!active || gc == null) {
            return;
        }

        gc.save();

        try {
            gc.setGlobalAlpha(0.28);
            gc.setFill(Color.VIOLET);
            gc.fillOval(x - 25, y - 25, 50, 50);

            gc.setGlobalAlpha(0.8);
            gc.setFill(Color.MEDIUMPURPLE);
            gc.fillOval(x - 16, y - 16, 32, 32);

            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.WHITE);
            gc.fillOval(x - 7, y - 7, 14, 14);
        } finally {
            gc.restore();
        }
    }
}