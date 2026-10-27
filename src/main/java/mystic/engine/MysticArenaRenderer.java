package mystic.engine;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Multi-arena renderer for Mystic Kombat.
 *
 * The renderer draws a far layer, architectural mid-layer, near framing layer,
 * a strict gameplay ground line, and arena-specific animated atmosphere. It is
 * procedural, so the arenas work without external background image files.
 */
public final class MysticArenaRenderer {
    private static final int PARTICLES = 100;
    private final double[] px = new double[PARTICLES];
    private final double[] py = new double[PARTICLES];
    private final double[] speed = new double[PARTICLES];
    private final double[] drift = new double[PARTICLES];
    private final Random random = new Random(4271);

    private double time;
    private double rainTime;

    public MysticArenaRenderer() {
        for (int i = 0; i < PARTICLES; i++) {
            px[i] = random.nextDouble();
            py[i] = random.nextDouble();
            speed[i] = 0.08 + random.nextDouble() * 0.34;
            drift[i] = (random.nextDouble() - 0.5) * 0.14;
        }
    }

    /** Backwards-compatible default draw: Sanctum Sanctorum. */
    public void draw(GraphicsContext gc, double w, double h) {
        draw(gc, w, h, 0, h * 0.833333, 0);
    }

    /**
     * @param arenaIndex selected arena: 0 Sanctum, 1 Mirror, 2 Gotham, 3 Void
     * @param groundY    exact baseline used by the fighter physics
     * @param cameraX    camera/parallax offset derived from the fighters' midpoint
     */
    public void draw(GraphicsContext gc, double w, double h,
            int arenaIndex, double groundY, double cameraX) {
        if (w <= 0 || h <= 0)
            return;
        time += 1.0 / 60.0;
        int arena = Math.floorMod(arenaIndex, 4);
        double floor = Math.max(0, Math.min(h - 1, groundY));

        gc.save();
        try {
            gc.setGlobalAlpha(1);
            drawFarLayer(gc, w, h, arena, cameraX);
            drawArchitecture(gc, w, h, arena, cameraX);
            drawNearLayer(gc, w, h, arena, cameraX);
            drawGameplayFloor(gc, w, h, floor, arena);
            drawAtmosphere(gc, w, h, floor, arena);
            // Shared Mystic Kombat identity: a restrained crimson wash with molten-gold
            // light.
            gc.setFill(new RadialGradient(0, 0, w * (0.48 + 0.025 * Math.sin(time * 0.7)),
                    h * 0.43, Math.max(w, h) * 0.62, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 82, 42, 0.075)),
                    new Stop(0.72, Color.rgb(191, 22, 29, 0.045)),
                    new Stop(1, Color.rgb(0, 0, 0, 0))));
            gc.fillRect(0, 0, w, h);
            drawLightingAndVignette(gc, w, h, arena);
        } finally {
            gc.restore();
        }
    }

    private void drawFarLayer(GraphicsContext gc, double w, double h, int arena, double cameraX) {
        Color top = switch (arena) {
            case 0 -> Color.web("#100309");
            case 1 -> Color.web("#13050A");
            case 2 -> Color.web("#090308");
            default -> Color.web("#080207");
        };
        Color bottom = switch (arena) {
            case 0 -> Color.web("#68151A");
            case 1 -> Color.web("#4A1117");
            case 2 -> Color.web("#381019");
            default -> Color.web("#52101A");
        };
        gc.setFill(new LinearGradient(0, 0, 0, h * 0.82, false, CycleMethod.NO_CYCLE,
                new Stop(0, top), new Stop(1, bottom)));
        gc.fillRect(0, 0, w, h);

        if (arena == 0) {
            // Sanctum: moonlit stained glass and a glowing mystic sigil.
            double cx = w * 0.5 - cameraX * 0.035;
            double cy = h * 0.28;
            double r = Math.min(w * 0.20, h * 0.31);
            gc.setFill(new RadialGradient(0, 0, cx, cy, r * 1.65, false, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 70, 45, 0.32)),
                    new Stop(1, Color.rgb(255, 155, 61, 0))));
            gc.fillOval(cx - r * 1.65, cy - r * 1.65, r * 3.3, r * 3.3);
            gc.setStroke(Color.web("#FFD16A", 0.78));
            gc.setLineWidth(4);
            gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
            for (int i = 0; i < 12; i++) {
                double a = Math.PI * 2 * i / 12 + time * 0.08;
                gc.strokeLine(cx, cy, cx + Math.cos(a) * r, cy + Math.sin(a) * r);
            }
            gc.setStroke(Color.web("#D29B42", 0.62));
            gc.setLineWidth(2);
            for (int i = 0; i < 5; i++) {
                double x = w * (0.17 + i * 0.165) - cameraX * 0.025;
                gc.strokeLine(x, h * 0.05, x, h * 0.59);
            }
        } else if (arena == 1) {
            // Mirror dimension: fractured reflective planes.
            for (int i = 0; i < 10; i++) {
                double x = wrap(i * w * 0.13 - cameraX * 0.04, -w * 0.1, w * 1.1);
                double y = h * (0.10 + (i % 4) * 0.105);
                gc.setFill(Color.rgb(255, 65, 48, 0.07 + (i % 3) * 0.025));
                gc.fillPolygon(new double[] { x, x + w * 0.09, x + w * 0.14, x + w * 0.04 },
                        new double[] { y, y - h * 0.04, y + h * 0.19, y + h * 0.15 }, 4);
                gc.setStroke(Color.web("#FFD17A", 0.28));
                gc.strokeLine(x, y, x + w * 0.14, y + h * 0.19);
            }
        } else if (arena == 2) {
            // Gotham: distant skyline, window lights and rain-dark clouds.
            gc.setFill(Color.web("#10101F", 0.9));
            for (int i = 0; i < 24; i++) {
                double bw = w * (0.025 + (i % 4) * 0.009);
                double bh = h * (0.10 + ((i * 37) % 19) * 0.018);
                double x = wrap(i * w / 22.0 - cameraX * 0.04, -bw, w + bw);
                double y = h * 0.67 - bh;
                gc.fillRect(x, y, bw, bh);
                gc.setFill(Color.rgb(255, 190, 89, 0.22 + (i % 3) * 0.09));
                for (int wy = 0; wy < 5; wy++)
                    if ((i + wy) % 3 != 0)
                        gc.fillRect(x + bw * 0.25, y + 10 + wy * 13, bw * 0.16, 5);
                gc.setFill(Color.web("#10101F", 0.9));
            }
            gc.setStroke(Color.web("#B33BFF", 0.4));
            gc.setLineWidth(2);
            gc.strokeLine(0, h * 0.31, w, h * 0.31);
        } else {
            // Multiversal void: nebula clouds and dimensional rift.
            glow(gc, w, h, w * 0.32 - cameraX * 0.025, h * 0.34, Math.max(w, h) * 0.42,
                    Color.rgb(150, 60, 255, 0.42), Color.rgb(150, 60, 255, 0));
            glow(gc, w, h, w * 0.72 - cameraX * 0.02, h * 0.40, Math.max(w, h) * 0.35,
                    Color.rgb(35, 225, 218, 0.30), Color.rgb(35, 225, 218, 0));
            gc.setStroke(Color.web("#69F3FF", 0.35));
            gc.setLineWidth(2);
            gc.beginPath();
            for (int i = 0; i <= 16; i++) {
                double y = h * 0.04 + h * 0.68 * i / 16.0;
                double x = w * 0.5 + Math.sin(time * 1.8 + i * 1.6) * w * 0.025;
                if (i == 0)
                    gc.moveTo(x, y);
                else
                    gc.lineTo(x, y);
            }
            gc.stroke();
        }

        // Star/dust field belongs to the slowest parallax plane.
        for (int i = 0; i < 90; i++) {
            double x = wrap((i * 137.0) % w - cameraX * 0.018, -2, w + 2);
            double y = ((i * 71.0) % (h * 0.67));
            double tw = 1.0 + 1.4 * Math.abs(Math.sin(time * 1.7 + i));
            Color c = arena == 2 ? Color.rgb(255, 80, 55, 0.30) : Color.rgb(255, 229, 177, 0.55);
            gc.setFill(c);
            gc.fillOval(x, y, tw, tw);
        }
    }

    private void drawArchitecture(GraphicsContext gc, double w, double h, int arena, double cameraX) {
        double parallax = 0.075;
        if (arena == 0) {
            // Towering gothic arches.
            gc.setStroke(Color.web("#261B35", 0.96));
            gc.setLineWidth(Math.max(12, w * 0.018));
            for (int i = -1; i < 7; i++) {
                double x = wrap(i * w * 0.19 - cameraX * parallax, -w * 0.2, w * 1.2);
                gc.strokeLine(x, h * 0.02, x, h * 0.72);
                gc.strokeArc(x - w * 0.095, h * 0.02, w * 0.19, h * 0.32, 0, 180, javafx.scene.shape.ArcType.OPEN);
                gc.setStroke(Color.web("#C99A56", 0.25));
                gc.setLineWidth(2);
                gc.strokeLine(x - w * 0.075, h * 0.16, x - w * 0.075, h * 0.63);
                gc.setStroke(Color.web("#261B35", 0.96));
                gc.setLineWidth(Math.max(12, w * 0.018));
            }
            gc.setFill(Color.web("#0A0817", 0.42));
            gc.fillRect(0, h * 0.62, w, h * 0.11);
        } else if (arena == 1) {
            gc.setStroke(Color.web("#8ADFFF", 0.24));
            gc.setLineWidth(3);
            for (int i = 0; i < 12; i++) {
                double x = wrap(i * w * 0.12 - cameraX * parallax, -w * 0.1, w * 1.1);
                gc.strokePolygon(new double[] { x, x + w * 0.06, x + w * 0.11, x + w * 0.05 },
                        new double[] { h * 0.18, h * 0.10, h * 0.48, h * 0.58 }, 4);
            }
        } else if (arena == 2) {
            // Rooftop parapet, water tanks and gargoyle silhouettes.
            gc.setFill(Color.web("#0C0B16"));
            gc.fillRect(0, h * 0.57, w, h * 0.16);
            gc.setFill(Color.web("#171321"));
            for (int i = 0; i < 9; i++) {
                double x = wrap(i * w * 0.15 - cameraX * parallax, -w * 0.2, w * 1.2);
                gc.fillRect(x, h * 0.43, w * 0.07, h * 0.24);
                gc.fillPolygon(new double[] { x, x + w * 0.035, x + w * 0.07 },
                        new double[] { h * 0.43, h * 0.36, h * 0.43 }, 3);
            }
            // Neon signage flickers subtly.
            double flicker = 0.65 + 0.3 * Math.abs(Math.sin(time * 11));
            gc.setStroke(Color.rgb(255, 45, 183, flicker));
            gc.setLineWidth(3);
            gc.strokeRoundRect(w * 0.13 - cameraX * 0.015, h * 0.25, w * 0.16, h * 0.09, 10, 10);
            gc.setStroke(Color.rgb(54, 220, 255, 0.7));
            gc.strokeRoundRect(w * 0.72 - cameraX * 0.015, h * 0.34, w * 0.13, h * 0.08, 10, 10);
        } else {
            // Floating shattered platforms drift at a medium parallax speed.
            for (int i = 0; i < 9; i++) {
                double x = wrap(i * w * 0.14 - cameraX * parallax, -w * 0.2, w * 1.2);
                double y = h * (0.24 + (i % 4) * 0.09) + Math.sin(time * 0.65 + i) * 7;
                gc.setFill(Color.web(i % 2 == 0 ? "#30214F" : "#153C4B", 0.9));
                gc.fillPolygon(new double[] { x, x + w * 0.08, x + w * 0.11, x + w * 0.03 },
                        new double[] { y, y - 8, y + 10, y + 17 }, 4);
                gc.setStroke(Color.web(i % 2 == 0 ? "#BE8CFF" : "#5CF5EE", 0.65));
                gc.setLineWidth(2);
                gc.strokeLine(x, y, x + w * 0.08, y - 8);
            }
        }
    }

    private void drawNearLayer(GraphicsContext gc, double w, double h, int arena, double cameraX) {
        // Framing columns / rooftop edges are closest to the virtual camera.
        double shift = cameraX * 0.15;
        gc.setFill(Color.web("#05040B", 0.68));
        gc.fillPolygon(new double[] { -10, w * 0.055 - shift, w * 0.08 - shift, 0 },
                new double[] { 0, 0, h * 0.72, h * 0.82 }, 4);
        gc.fillPolygon(new double[] { w, w * 0.945 - shift, w * 0.92 - shift, w },
                new double[] { 0, 0, h * 0.72, h * 0.82 }, 4);
        if (arena == 0) {
            gc.setStroke(Color.web("#B8894B", 0.45));
            gc.setLineWidth(4);
            gc.strokeLine(w * 0.055 - shift, 0, w * 0.08 - shift, h * 0.72);
            gc.strokeLine(w * 0.945 - shift, 0, w * 0.92 - shift, h * 0.72);
        } else if (arena == 2) {
            // Rain streaks are drawn in atmosphere; near layer adds dark roof ledge.
            gc.setFill(Color.web("#05050A", 0.85));
            gc.fillRect(0, h * 0.69, w, h * 0.025);
        } else if (arena == 3) {
            gc.setStroke(Color.web("#B88CFF", 0.22));
            gc.setLineWidth(2);
            for (int i = 0; i < 6; i++) {
                double x = wrap(i * w * 0.2 - cameraX * 0.15, -w * 0.2, w * 1.2);
                gc.strokeLine(x, h * 0.02, x + w * 0.06, h * 0.16);
            }
        }
    }

    private void drawGameplayFloor(GraphicsContext gc, double w, double h, double floor, int arena) {
        Color floorTop = switch (arena) {
            case 0 -> Color.web("#21172B");
            case 1 -> Color.web("#152536");
            case 2 -> Color.web("#17131E");
            default -> Color.web("#171127");
        };
        Color floorBottom = switch (arena) {
            case 0 -> Color.web("#090711");
            case 1 -> Color.web("#050B14");
            case 2 -> Color.web("#07070D");
            default -> Color.web("#05020C");
        };
        gc.setFill(new LinearGradient(0, floor, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, floorTop), new Stop(1, floorBottom)));
        gc.fillRect(0, floor, w, h - floor);

        gc.setStroke(Color.rgb(212, 175, 55, 0.18));
        gc.setLineWidth(1);
        for (int i = 0; i <= 16; i++) {
            double x = w * i / 16.0;
            gc.strokeLine(w * 0.5, floor, x, h);
        }
        for (int i = 1; i <= 6; i++) {
            double p = i / 6.0;
            double y = floor + (h - floor) * p * p;
            gc.strokeLine(0, y, w, y);
        }

        // This is the exact physics baseline: fighters' feet must meet this line.
        gc.setStroke(arena == 2 ? Color.web("#5EE8FF", 0.75) : Color.web("#D4AF37", 0.85));
        gc.setLineWidth(2.5);
        gc.strokeLine(0, floor, w, floor);
        gc.setStroke(Color.rgb(255, 255, 255, 0.12));
        gc.setLineWidth(1);
        gc.strokeLine(0, floor + 3, w, floor + 3);
    }

    private void drawAtmosphere(GraphicsContext gc, double w, double h, double floor, int arena) {
        if (arena == 2) {
            // Dense diagonal rain, with slight movement each frame.
            gc.setStroke(Color.rgb(127, 174, 222, 0.35));
            gc.setLineWidth(1);
            rainTime += 0.02;
            for (int i = 0; i < 180; i++) {
                double x = wrap((i * 83.0 + rainTime * 210) % (w + 100), -50, w + 50);
                double y = wrap((i * 47.0 + rainTime * 470) % h, -12, h + 12);
                gc.strokeLine(x, y, x - 7, y + 18);
            }
            // Occasional neon puddle glow on the floor.
            double pulse = 0.35 + 0.25 * Math.abs(Math.sin(time * 2.8));
            gc.setFill(Color.rgb(48, 190, 255, pulse * 0.16));
            gc.fillOval(w * 0.18, floor + 8, w * 0.18, (h - floor) * 0.22);
            gc.setFill(Color.rgb(231, 42, 176, pulse * 0.12));
            gc.fillOval(w * 0.63, floor + 10, w * 0.20, (h - floor) * 0.2);
        } else {
            for (int i = 0; i < PARTICLES; i++) {
                py[i] -= speed[i] / 60.0;
                px[i] += drift[i] / 60.0;
                if (py[i] < 0) {
                    py[i] = 1;
                    px[i] = random.nextDouble();
                }
                if (px[i] < 0)
                    px[i] = 1;
                if (px[i] > 1)
                    px[i] = 0;
                double x = px[i] * w, y = py[i] * h * 0.92;
                if (y > floor)
                    continue;
                if (arena == 1) {
                    gc.setFill(Color.rgb(116, 225, 255, 0.45));
                    gc.fillOval(x, y, 2, 2);
                } else if (arena == 3) {
                    gc.setFill(i % 3 == 0 ? Color.rgb(87, 242, 235, 0.75) : Color.rgb(183, 126, 255, 0.65));
                    gc.fillOval(x, y, 2 + (i % 3), 2 + (i % 3));
                } else {
                    gc.setFill(i % 4 == 0 ? Color.rgb(255, 212, 122, 0.8) : Color.rgb(255, 124, 43, 0.6));
                    gc.fillOval(x, y, 2 + (i % 3), 2 + (i % 3));
                }
            }
        }

        if (arena == 0) {
            // Pulsing magical floor sigil behind the fighters.
            double cx = w * 0.5, cy = floor + 22, r = w * 0.23;
            double pulse = 0.75 + 0.25 * Math.sin(time * 2.1);
            gc.save();
            gc.translate(cx, cy);
            gc.scale(1, 0.22);
            gc.setStroke(Color.rgb(255, 173, 71, 0.32 + 0.25 * pulse));
            gc.setLineWidth(3);
            gc.strokeOval(-r, -r, r * 2, r * 2);
            gc.setStroke(Color.rgb(255, 216, 139, 0.24));
            gc.strokeOval(-r * 0.72, -r * 0.72, r * 1.44, r * 1.44);
            gc.rotate(time * 9);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                gc.strokeLine(Math.cos(a) * r * 0.72, Math.sin(a) * r * 0.72,
                        Math.cos(a) * r * 0.96, Math.sin(a) * r * 0.96);
            }
            gc.restore();
        } else if (arena == 3) {
            // Rift pulses above the floor.
            double alpha = 0.15 + 0.15 * Math.sin(time * 3.0);
            gc.setFill(Color.rgb(117, 68, 255, alpha));
            gc.fillOval(w * 0.34, floor - 24, w * 0.32, 50);
        }
    }

    private void drawLightingAndVignette(GraphicsContext gc, double w, double h, int arena) {
        Color tint = switch (arena) {
            case 0 -> Color.rgb(255, 126, 43, 0.035);
            case 1 -> Color.rgb(74, 185, 255, 0.04);
            case 2 -> Color.rgb(38, 87, 170, 0.055);
            default -> Color.rgb(121, 52, 255, 0.05);
        };
        gc.setFill(tint);
        gc.fillRect(0, 0, w, h);
        double pulse = 0.04 + 0.02 * Math.sin(time * 1.15);
        gc.setFill(new RadialGradient(0, 0, w / 2, h * 0.45, Math.max(w, h) * 0.78, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(0, 0, 0, 0)),
                new Stop(1, Color.rgb(0, 0, 0, 0.56 + pulse))));
        gc.fillRect(0, 0, w, h);
    }

    private static void glow(GraphicsContext gc, double w, double h, double cx, double cy, double radius,
            Color inner, Color outer) {
        gc.setFill(new RadialGradient(0, 0, cx, cy, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, inner), new Stop(1, outer)));
        gc.fillRect(0, 0, w, h);
    }

    private static double wrap(double value, double min, double max) {
        double range = max - min;
        if (range <= 0)
            return min;
        return min + ((value - min) % range + range) % range;
    }
}
