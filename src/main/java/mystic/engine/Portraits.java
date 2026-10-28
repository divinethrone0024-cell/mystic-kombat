package mystic.engine;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import mystic.common.Roster;

/**
 * Loads and caches /assets/portraits/<id>.png for every Roster hero.
 * id = hero name lower-cased with spaces turned into underscores
 * (e.g. MYSTIC MAGE -> mystic_mage.png). Returns null when a file is missing.
 */
public final class Portraits {
    private static final Image[] CACHE = new Image[Roster.HEROES.length];
    private static final boolean[] TRIED = new boolean[Roster.HEROES.length];

    private Portraits() {
    }

    public static Image get(int idx) {
        idx = Math.floorMod(idx, Roster.HEROES.length);
        if (!TRIED[idx]) {
            TRIED[idx] = true;
            String base = Roster.HEROES[idx].name().toLowerCase().trim();
            String[] ids = { base.replace(' ', '_'), base.replace(" ", ""), base.replace(' ', '-') };
            for (String id : ids) {
                var url = Portraits.class.getResource("/assets/portraits/" + id + ".png");
                if (url != null) {
                    Image img = new Image(url.toExternalForm());
                    if (!img.isError()) {
                        CACHE[idx] = img;
                        break;
                    }
                }
            }
        }
        return CACHE[idx];
    }

    /**
     * Draws the portrait fitted into a box centred on (cx, cy). Returns false if
     * missing.
     */
    public static boolean draw(GraphicsContext gc, int idx, double cx, double cy,
            double boxW, double boxH, boolean flipX) {
        Image img = get(idx);
        if (img == null)
            return false;
        double s = Math.min(boxW / img.getWidth(), boxH / img.getHeight());
        double iw = img.getWidth() * s, ih = img.getHeight() * s;
        gc.save();
        gc.setImageSmoothing(false);
        if (flipX) {
            gc.translate(cx, 0);
            gc.scale(-1, 1);
            gc.translate(-cx, 0);
        }
        gc.drawImage(img, cx - iw / 2, cy - ih / 2, iw, ih);
        gc.restore();
        return true;
    }
}