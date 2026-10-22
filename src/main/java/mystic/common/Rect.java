package mystic.common;

/**
 * A rectangle used for hitboxes and hurtboxes. (x, y) is the top-left corner.
 */
public record Rect(double x, double y, double w, double h) {

    /** True if this rectangle overlaps the other one. */
    public boolean overlaps(Rect o) {
        return x < o.x + o.w && x + w > o.x
                && y < o.y + o.h && y + h > o.y;
    }
}