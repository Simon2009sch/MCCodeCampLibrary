package me.simoncrafter.mCCodeCampLibrary.projectile.event;

import me.simoncrafter.mCCodeCampLibrary.projectile.CustomProjectile;
import me.simoncrafter.mCCodeCampLibrary.projectile.ProjectileHit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

/** Before onHit: cancellation suppresses the projectile's behavior and lets it pass this target. */
public class ProjectileHitEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CustomProjectile projectile;
    private final ProjectileHit hit;
    private boolean cancelled;
    public ProjectileHitEvent(CustomProjectile projectile, ProjectileHit hit) {
        this.projectile = Objects.requireNonNull(projectile); this.hit = Objects.requireNonNull(hit);
    }
    public CustomProjectile getProjectile() { return projectile; }
    public ProjectileHit getHit() { return hit; }
    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
