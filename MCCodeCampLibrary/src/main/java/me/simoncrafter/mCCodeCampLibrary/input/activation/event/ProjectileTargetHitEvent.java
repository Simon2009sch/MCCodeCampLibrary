package me.simoncrafter.mCCodeCampLibrary.input.activation.event;

import me.simoncrafter.mCCodeCampLibrary.input.activation.ProjectileTarget;
import me.simoncrafter.mCCodeCampLibrary.projectile.CustomProjectile;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/** Noncancellable target notification for an accepted impact. Exactly one projectile is present.
 * Custom hit positions are precise collision positions; vanilla positions are Bukkit projectile locations.
 * Shooters may be absent (including vanilla block projectile sources).
 */
public class ProjectileTargetHitEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final ProjectileTarget target;
    private final String id;
    private final CustomProjectile customProjectile;
    private final Projectile vanillaProjectile;
    private final Entity shooter;
    private final Location hitPosition;
    private final BlockFace hitFace;
    public ProjectileTargetHitEvent(ProjectileTarget target, @Nullable CustomProjectile customProjectile,
                                   @Nullable Projectile vanillaProjectile, @Nullable Entity shooter,
                                   Location hitPosition, @Nullable BlockFace hitFace) {
        this.target = Objects.requireNonNull(target, "target");
        if ((customProjectile == null) == (vanillaProjectile == null)) throw new IllegalArgumentException("Exactly one projectile is required");
        this.id = target.getID();
        this.customProjectile = customProjectile;
        this.vanillaProjectile = vanillaProjectile;
        this.shooter = shooter;
        this.hitPosition = Objects.requireNonNull(hitPosition, "hitPosition").clone();
        this.hitFace = hitFace;
    }
    public ProjectileTarget getTarget() { return target; }
    public String getID() { return id; }
    public @Nullable CustomProjectile getCustomProjectile() { return customProjectile; }
    public @Nullable Projectile getVanillaProjectile() { return vanillaProjectile; }
    public @Nullable Entity getShooter() { return shooter; }
    public Location getHitPosition() { return hitPosition.clone(); }
    public @Nullable BlockFace getHitFace() { return hitFace; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
