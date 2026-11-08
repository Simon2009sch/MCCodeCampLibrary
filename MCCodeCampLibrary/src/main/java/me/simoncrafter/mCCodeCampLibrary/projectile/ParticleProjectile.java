package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import java.util.Objects;

/** Visible particle-only projectile. Trail particles must not require extra data. */
public class ParticleProjectile extends CustomProjectile {
    private final Particle particle;
    public ParticleProjectile() { this(ProjectileMovement.FIREBALL_LIKE, Particle.END_ROD); }
    public ParticleProjectile(Movement movement, Particle particle) {
        super(movement); this.particle = Objects.requireNonNull(particle);
        if (particle.getDataType() != Void.class) throw new IllegalArgumentException("Use a data-free trail particle or override onTravel");
    }
    @Override protected void onTravel(Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        // Bounded even for long instantaneous rays; include the impact endpoint.
        int samples = Math.max(1, Math.min(512, (int)Math.ceil(delta.length()/.25)));
        for (int i=0; i<=samples; i++) {
            Location point = from.clone().add(delta.clone().multiply((double)i/samples));
            point.getWorld().spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
}
