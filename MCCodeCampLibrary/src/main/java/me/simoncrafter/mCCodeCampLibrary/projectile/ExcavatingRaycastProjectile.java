package me.simoncrafter.mCCodeCampLibrary.projectile;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import java.util.Objects;
import java.util.function.BiConsumer;

/** Collision-independent 64-block particle ray. The integration owns any radius-4
 * capsule snapshot/terrain edits; this class never reads or deletes blocks.
 */
public class ExcavatingRaycastProjectile extends ParticleProjectile {
    public static final double RADIUS = 4;
    public static final double MAX_DISTANCE = 64;
    private final BiConsumer<Location, Location> excavate;
    private boolean callbackDelivered;

    /** Called once with defensive locations for the actually travelled ray (possibly clipped
     * by loaded chunks/world bounds). Snapshot terrain before changing it in this callback.
     */
    public ExcavatingRaycastProjectile(BiConsumer<Location, Location> excavate) {
        super(ProjectileMovement.RAYCAST, Particle.END_ROD);
        this.excavate = Objects.requireNonNull(excavate, "excavate");
        setMaxDistance(MAX_DISTANCE);
    }
    @Override protected ProjectileHit hit(Location from, Vector step) { return null; }

    @Override protected void onTravel(Location from, Location to) {
        super.onTravel(from, to);
        if (!callbackDelivered) {
            callbackDelivered = true;
            excavate.accept(from.clone(), to.clone());
        }
    }
}
