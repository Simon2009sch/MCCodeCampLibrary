package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.Particle;
/** Instantaneous, range-limited visible ray. */
public class RaycastProjectile extends DamageProjectile {
    public RaycastProjectile() { this(2); }
    public RaycastProjectile(double damage) { super(ProjectileMovement.RAYCAST, Particle.END_ROD, damage); }
}
