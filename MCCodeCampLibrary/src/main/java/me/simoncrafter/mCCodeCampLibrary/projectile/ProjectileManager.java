package me.simoncrafter.mCCodeCampLibrary.projectile;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.*;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import java.util.*;
import java.util.logging.Level;

/** Owns live projectiles. MCCodeCampLib calls tick once per server tick and close on disable. */
public final class ProjectileManager implements AutoCloseable, Listener {
    private final Plugin plugin;
    private final Set<CustomProjectile> active = new LinkedHashSet<>();
    private boolean closed;
    private boolean ticking;
    public ProjectileManager(Plugin plugin) { this.plugin = Objects.requireNonNull(plugin); }
    public Plugin getPlugin() { return plugin; }
    public int size() { return active.size(); }
    static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Projectiles must be used on the server thread");
    }
    void add(CustomProjectile projectile) {
        requireMainThread(); if (closed) throw new IllegalStateException("Projectile manager is closed"); active.add(projectile);
    }
    void forget(CustomProjectile projectile) { active.remove(projectile); }
    /** Nested calls from projectile callbacks are ignored, never treated as another server tick. */
    public void tick() {
        requireMainThread();
        if (closed || ticking) return;
        ticking = true;
        try {
            for (CustomProjectile projectile : List.copyOf(active)) {
                if (!projectile.isActive()) continue;
                try { projectile.update(); }
                catch (RuntimeException failure) {
                    plugin.getLogger().log(Level.WARNING, "Removing failed custom projectile", failure);
                    removeSafely(projectile);
                }
            }
        } finally {
            ticking = false;
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        for (CustomProjectile projectile : List.copyOf(active)) {
            if (projectile.getLocation().getWorld().equals(event.getWorld())) removeSafely(projectile);
        }
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (CustomProjectile projectile : List.copyOf(active)) {
            var location = projectile.getLocation();
            if (location.getWorld().equals(event.getWorld()) && (location.getBlockX() >> 4) == event.getChunk().getX()
                    && (location.getBlockZ() >> 4) == event.getChunk().getZ()) removeSafely(projectile);
        }
    }
    private void removeSafely(CustomProjectile projectile) {
        try { projectile.remove(); } catch (RuntimeException failure) { plugin.getLogger().log(Level.WARNING, "Projectile cleanup failed", failure); }
    }
    @Override public void close() {
        requireMainThread(); closed = true;
        for (CustomProjectile projectile : List.copyOf(active)) {
            try { projectile.remove(); } catch (RuntimeException failure) { plugin.getLogger().log(Level.WARNING, "Projectile cleanup failed", failure); }
        }
        active.clear();
        HandlerList.unregisterAll(this);
    }
}
