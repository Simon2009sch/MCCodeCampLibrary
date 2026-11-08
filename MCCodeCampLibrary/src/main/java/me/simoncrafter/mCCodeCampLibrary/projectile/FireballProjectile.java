package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.Particle;
/** Straight flame bolt. No explosion, fire placement or block griefing. */
public class FireballProjectile extends DamageProjectile {
    public FireballProjectile() { this(4); }
    public FireballProjectile(double damage) { super(ProjectileMovement.FIREBALL_LIKE, Particle.FLAME, damage); }
}
