package me.simoncrafter.mCCodeCampLibrary.projectile;

import me.simoncrafter.CraftersDisplayLibrary.display.panel.BlockDisplayObject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.bukkit.util.Vector;

/** Snowball-style toss with a centered animated snow-block visual and native impact burst. */
public class SnowballProjectile extends ParticleProjectile {
    /** Native burst marker; integrations can restrict burst entity damage without per-ball listeners. */
    public static final org.bukkit.NamespacedKey BURST_KEY = new org.bukkit.NamespacedKey("mccodecamp", "snowball_burst");
    private BlockDisplayObject snowDisplay;
    private boolean burstEmitted;

    @Override protected void onHit(ProjectileHit hit) {
        if (!isActive() || burstEmitted) return;
        burstEmitted = true;
        Location impact = hit.position().toLocation(getLocation().getWorld());
        try {
            impact.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, impact, 40, .2, .2, .2, .1);
            // Fibonacci sphere: equal-area latitude bands and golden-angle spacing, not a ring/cone.
            double goldenAngle = Math.PI * (3 - Math.sqrt(5));
            for (int i = 0; i < 100; i++) {
                double y = 1 - 2 * (i + .5) / 100;
                double radial = Math.sqrt(1 - y * y);
                Vector direction = new Vector(Math.cos(i * goldenAngle) * radial, y, Math.sin(i * goldenAngle) * radial);
                impact.getWorld().spawn(impact, org.bukkit.entity.Snowball.class, ball -> {
                    ball.setPersistent(false);
                    ball.getPersistentDataContainer().set(BURST_KEY, org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
                    if (getShooter() instanceof org.bukkit.projectiles.ProjectileSource source) ball.setShooter(source);
                    ball.setVelocity(direction);
                });
            }
        } finally {
            // Vanilla owns every native ball's remaining collision/despawn lifecycle.
            super.onHit(hit);
        }
    }

    public SnowballProjectile() { super(ProjectileMovement.SNOWBALL_LIKE, Particle.SNOWFLAKE); }

    @Override protected void onShoot() {
        super.onShoot();
        // Keep entity yaw/pitch neutral: the local quaternion already faces the world-space velocity.
        Location anchor = getLocation();
        anchor.setYaw(0);
        anchor.setPitch(0);
        // CDL centers the block model on its origin; this is a world anchor, not a block corner.
        snowDisplay = BlockDisplayObject.create(anchor, new Vector3f(1), new Vector3f(), facing(getVelocity()), Material.SNOW_BLOCK.createBlockData());
        snowDisplay.spawnDisplay().setPersistent(false);
    }

    @Override protected void onTravel(Location from, Location to) {
        if (snowDisplay != null) {
            Vector travelled = to.toVector().subtract(from.toVector());
            if (travelled.lengthSquared() > 1e-12) {
                // Stage rotation without a separate animation task; moveAbsolute pushes both
                // rotation and translation together through one-tick client interpolation.
                snowDisplay.LRotateAbsoluteNoUpdate(facing(travelled));
            }
            // moveAbsolute takes LOCAL translation relative to the fixed spawn anchor.
            snowDisplay.moveAbsolute(to.toVector().subtract(snowDisplay.getLocation().toVector()).toVector3f(), 1);
        }
        super.onTravel(from, to);
    }

    private static Quaternionf facing(Vector direction) {
        if (direction.lengthSquared() < 1e-12) return new Quaternionf();
        return new Quaternionf().rotationTo(new Vector3f(0, 0, 1), direction.toVector3f().normalize());
    }

    @Override protected void onRemove() {
        if (snowDisplay != null) {
            snowDisplay.remove();
            snowDisplay = null;
        }
        super.onRemove();
    }
}
