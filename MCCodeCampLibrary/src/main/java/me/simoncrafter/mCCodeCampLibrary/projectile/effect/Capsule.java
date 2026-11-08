package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import org.bukkit.util.Vector;
import java.util.Objects;

/** Immutable world-coordinate capsule: a closed segment swept by a sphere.
 * Bounds are conservative inclusive integer block coordinates; test block centers with contains.
 */
public final class Capsule {
    private final double ax, ay, az, dx, dy, dz, lengthSquared, radiusSquared;
    private final int minX, minY, minZ, maxX, maxY, maxZ;

    public Capsule(Vector start, Vector end, double radius) {
        Objects.requireNonNull(start, "start").checkFinite();
        Objects.requireNonNull(end, "end").checkFinite();
        if (!Double.isFinite(radius) || radius < 0) throw new IllegalArgumentException("radius must be finite and nonnegative");
        ax = start.getX(); ay = start.getY(); az = start.getZ();
        dx = end.getX()-ax; dy = end.getY()-ay; dz = end.getZ()-az;
        lengthSquared = dx*dx + dy*dy + dz*dz;
        radiusSquared = radius*radius;
        if (!Double.isFinite(lengthSquared) || !Double.isFinite(radiusSquared)) throw new IllegalArgumentException("capsule is too large");
        minX = bound(Math.min(ax,end.getX())-radius); maxX = bound(Math.max(ax,end.getX())+radius);
        minY = bound(Math.min(ay,end.getY())-radius); maxY = bound(Math.max(ay,end.getY())+radius);
        minZ = bound(Math.min(az,end.getZ())-radius); maxZ = bound(Math.max(az,end.getZ())+radius);
    }
    private static int bound(double coordinate) {
        double floor = Math.floor(coordinate);
        if (floor < Integer.MIN_VALUE || floor > Integer.MAX_VALUE) throw new IllegalArgumentException("bounds exceed integer block coordinates");
        return (int)floor;
    }
    /** Includes the surface and both rounded ends; a zero-length segment is a sphere. */
    public boolean contains(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return false;
        double px=x-ax, py=y-ay, pz=z-az;
        double t=lengthSquared==0 ? 0 : Math.max(0,Math.min(1,(px*dx+py*dy+pz*dz)/lengthSquared));
        px-=t*dx; py-=t*dy; pz-=t*dz;
        return px*px+py*py+pz*pz <= radiusSquared;
    }
    public int minX() { return minX; }
    public int minY() { return minY; }
    public int minZ() { return minZ; }
    public int maxX() { return maxX; }
    public int maxY() { return maxY; }
    public int maxZ() { return maxZ; }
}
