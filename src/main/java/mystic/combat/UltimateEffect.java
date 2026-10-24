
package mystic.combat;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import mystic.fighters.Fighter;

public class UltimateEffect {

    private static final double DURATION = 1.25;
    private static final int PARTICLE_COUNT = 32;

    private final UltimateType type;
    private final double x;
    private final double y;
    private final double direction;

    private final List<Particle> particles = new ArrayList<>();

    private double elapsed;

    private static class Particle {
        double angle;
        double speed;
        double size;
        double distance;

        Particle(double angle, double speed, double size) {
            this.angle = angle;
            this.speed = speed;
            this.size = size;
        }
    }

    public UltimateEffect(Fighter fighter, UltimateType type) {
        this.type = type;
        this.x = fighter.getX();
        this.y = fighter.getY() - 58;
        this.direction = fighter.isFacingRight() ? 1 : -1;

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = Math.PI * 2 * i / PARTICLE_COUNT;

            particles.add(new Particle(
                    angle,
                    65 + (i % 5) * 28,
                    2 + (i % 4)));
        }
    }

    public void update(double dt) {
        if (!Double.isFinite(dt) || dt <= 0) {
            return;
        }

        elapsed = Math.min(DURATION, elapsed + dt);

        for (Particle particle : particles) {
            particle.distance += particle.speed * dt;
        }
    }

    public boolean isFinished() {
        return elapsed >= DURATION;
    }

    public void render(GraphicsContext gc) {
        if (isFinished()) {
            return;
        }

        gc.save();

        try {
            double progress = elapsed / DURATION;
            double fade = 1.0 - progress;

            double radius = 20 + progress * 125;
            double rotation = elapsed * 4 * direction;

            gc.setGlobalAlpha(fade);

            // Outer energy ring.
            gc.setStroke(type.getPrimaryColor());
            gc.setLineWidth(7 * fade + 1);

            gc.strokeOval(
                    x - radius,
                    y - radius,
                    radius * 2,
                    radius * 2);

            // Inner ring.
            gc.setStroke(type.getAccentColor());
            gc.setLineWidth(3);

            gc.strokeOval(
                    x - radius * 0.65,
                    y - radius * 0.65,
                    radius * 1.3,
                    radius * 1.3);

            // Radial energy rays.
            int rays = 16;

            for (int i = 0; i < rays; i++) {
                double angle = Math.PI * 2 * i / rays + rotation;

                double inner = radius * 0.35;
                double outer = radius * (0.85 + (i % 3) * 0.12);

                double x1 = x + Math.cos(angle) * inner;
                double y1 = y + Math.sin(angle) * inner;

                double x2 = x + Math.cos(angle) * outer;
                double y2 = y + Math.sin(angle) * outer;

                gc.setStroke(i % 2 == 0
                        ? type.getPrimaryColor()
                        : type.getAccentColor());

                gc.setLineWidth(i % 3 == 0 ? 4 : 2);

                gc.strokeLine(x1, y1, x2, y2);
            }

            // Energy core.
            double coreRadius = 14 + Math.sin(elapsed * 22) * 5;

            gc.setFill(type.getSecondaryColor());
            gc.fillOval(
                    x - coreRadius,
                    y - coreRadius,
                    coreRadius * 2,
                    coreRadius * 2);

            gc.setFill(type.getAccentColor());
            gc.fillOval(
                    x - coreRadius * 0.45,
                    y - coreRadius * 0.45,
                    coreRadius * 0.9,
                    coreRadius * 0.9);

            // Flying particles.
            for (Particle particle : particles) {
                double px = x + Math.cos(particle.angle)
                        * particle.distance;

                double py = y + Math.sin(particle.angle)
                        * particle.distance;

                gc.setFill(particle.size > 4
                        ? type.getAccentColor()
                        : type.getPrimaryColor());

                gc.fillOval(
                        px - particle.size / 2,
                        py - particle.size / 2,
                        particle.size,
                        particle.size);
            }

            // Ultimate title.
            gc.setGlobalAlpha(Math.min(1, fade * 1.5));
            gc.setFill(type.getAccentColor());

            gc.fillText(
                    type.getDisplayName(),
                    x,
                    y - radius - 18);

        } finally {
            gc.restore();
        }
    }
}
