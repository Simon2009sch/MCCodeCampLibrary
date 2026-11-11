package me.simoncrafter.mCCodeCampLibrary.internal.editor.area;

import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Mutable, unsaved multi-cuboid draft using absolute inclusive world block coordinates. */
public final class CuboidAreaSelection {
    private final List<OpenableRegion> areas;
    private Vector firstCorner;

    /** Copies initial areas; changes never affect the source collection. */
    public CuboidAreaSelection(Collection<OpenableRegion> initial) {
        areas = new ArrayList<>(initial);
    }

    /** Selects a block corner; returns true when the second corner appends an area. */
    public boolean selectCorner(Vector corner) {
        Vector block = new Vector(corner.getBlockX(), corner.getBlockY(), corner.getBlockZ());
        if (firstCorner == null) {
            firstCorner = block;
            return false;
        }
        areas.add(new OpenableRegion(firstCorner, block));
        firstCorner = null;
        return true;
    }

    /** Discards an unfinished two-corner selection. */
    public void cancelCorner() { firstCorner = null; }
    /** Returns a detached ordered snapshot of the draft. */
    public List<OpenableRegion> getAreas() { return new ArrayList<>(areas); }
    /** Removes just the indexed area. */
    public void removeArea(int index) { areas.remove(index); }
    /** Clears all areas and the unfinished selection. */
    public void clear() { areas.clear(); cancelCorner(); }
    /** Converts between world selection and legacy marker-relative door bounds without mutating either. */
    public static List<OpenableRegion> translate(Collection<OpenableRegion> areas,Vector offset) {
        return areas.stream().map(area->new OpenableRegion(area.getMin().add(offset),area.getMax().add(offset))).toList();
    }
    /** Center of the full block-inclusive box, not the block-center extrema. */
    public static Vector center(OpenableRegion area) {
        return area.getMin().add(area.getMax()).add(new Vector(1, 1, 1)).multiply(.5);
    }
    /** Full block-inclusive dimensions for rendering. */
    public static Vector size(OpenableRegion area) {
        return area.getMax().subtract(area.getMin()).add(new Vector(1, 1, 1));
    }
}
