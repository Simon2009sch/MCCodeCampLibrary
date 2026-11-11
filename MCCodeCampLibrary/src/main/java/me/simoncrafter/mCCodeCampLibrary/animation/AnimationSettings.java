package me.simoncrafter.mCCodeCampLibrary.animation;

import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import org.bukkit.util.Vector;
import org.joml.Vector3f;
import org.spongepowered.configurate.ConfigurationNode;
import java.util.ArrayList;
import java.util.List;

/** Validated JSON settings shared by both marker types. Coordinates are absolute world coordinates. */
public record AnimationSettings(List<OpenableRegion> regions, Vector3f pivot, OpenableRegion storage,
        double nearDistance, double storageDistance, double activeDistance, float curveHeight, boolean includeSpectators) {
    public static AnimationSettings read(ConfigurationNode source, Vector3f defaultPivot) {
        ConfigurationNode n = source.copy();
        List<OpenableRegion> regions = new ArrayList<>();
        long total=0;
        for (ConfigurationNode r : n.node("blocks").childrenList()) {
            OpenableRegion region = region(r);
            Vector min=region.getMin(), max=region.getMax();
            total += (long)(max.getBlockX()-min.getBlockX()+1)*(max.getBlockY()-min.getBlockY()+1)*(max.getBlockZ()-min.getBlockZ()+1);
            if (total>3000) throw new IllegalArgumentException("Selected areas exceed 3000 blocks");
            regions.add(region);
        }
        Vector3f pivot = n.node("pivot").virtual() ? new Vector3f(defaultPivot) : AnimationTrack.vector(n.node("pivot"), 0);
        OpenableRegion storage = n.node("storage").virtual() ? null : region(n.node("storage"));
        double near=n.node("nearDistance").getDouble(3), far=n.node("storageDistance").getDouble(10), active=n.node("activeDistance").getDouble(50);
        float curve=n.node("curveHeight").getFloat(4);
        if (!Double.isFinite(near) || !Double.isFinite(far) || !Double.isFinite(active) || near<0 || near>=far || far>=active || active>256)
            throw new IllegalArgumentException("Require 0 <= nearDistance < storageDistance < activeDistance <= 256");
        if (!Float.isFinite(curve) || Math.abs(curve)>100) throw new IllegalArgumentException("curveHeight must be between -100 and 100");
        return new AnimationSettings(List.copyOf(regions), pivot, storage, near, far, active, curve, n.node("includeSpectators").getBoolean(false));
    }

    private static OpenableRegion region(ConfigurationNode n) {
        Vector3f a=AnimationTrack.vector(n.node("min"), 0), b=AnimationTrack.vector(n.node("max"), 0);
        for (float f : new float[]{a.x,a.y,a.z,b.x,b.y,b.z})
            if (f != Math.floor(f)) throw new IllegalArgumentException("Area corners must be integer block coordinates");
        return new OpenableRegion(new Vector(a.x,a.y,a.z), new Vector(b.x,b.y,b.z));
    }
}
