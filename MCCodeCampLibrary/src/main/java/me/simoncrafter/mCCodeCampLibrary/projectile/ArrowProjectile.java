package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.Particle;
/** Arrow-style gravity and drag, with a critical particle trail. */
public class ArrowProjectile extends DamageProjectile {
    public ArrowProjectile() { this(2); }
    public ArrowProjectile(double damage) { super(ProjectileMovement.ARROW_LIKE, Particle.CRIT, damage); }
}
