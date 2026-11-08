package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

/** Particle projectile that damages living targets, then stops. Never changes blocks. */
public class DamageProjectile extends ParticleProjectile {
    private final double damage;
    public DamageProjectile(Movement movement, Particle particle, double damage) {
        super(movement, particle);
        if (!Double.isFinite(damage) || damage < 0) throw new IllegalArgumentException("damage must be finite and nonnegative");
        this.damage = damage;
    }
    public double getDamage() { return damage; }
    @Override protected void onHit(ProjectileHit hit) {
        if (damage > 0 && hit.entity() instanceof LivingEntity target) {
            if (getShooter() == null) target.damage(damage);
            else target.damage(damage, getShooter());
        }
        super.onHit(hit);
    }
}
