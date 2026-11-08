package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.util.Vector;
import java.util.Locale;

/** Approximate vanilla-style physics, not native Minecraft entities. Speed is in blocks/tick. */
public enum ProjectileMovement implements Movement {
    ARROW_LIKE(.99, .05), SNOWBALL_LIKE(.99, .03), FIREBALL_LIKE(1, 0), RAYCAST(1, 0);
    private final double drag, gravity;
    ProjectileMovement(double drag, double gravity) { this.drag = drag; this.gravity = gravity; }
    @Override public Vector move(CustomProjectile projectile) {
        Vector step = projectile.getVelocity();
        if (this == RAYCAST) return step.lengthSquared() == 0 ? step : step.normalize().multiply(projectile.getRemainingDistance());
        projectile.setVelocity(step.clone().multiply(drag).subtract(new Vector(0, gravity, 0)));
        return step;
    }
    /** Accepts names such as "arrow_like". */
    public static ProjectileMovement named(String name) { return valueOf(name.toUpperCase(Locale.ROOT)); }
}
