package me.simoncrafter.mCCodeCampLibrary.projectile;

import me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Extensible, entity-free projectile. All operations belong on the server thread.
 * Override onTick, move, hit, onHit and onRemove independently; no Bukkit listener is
 * needed for ticking. Each instance can be shot once. Use a new instance for each shot.
 */
public class CustomProjectile implements AutoCloseable {
    private static final double EPSILON = 1e-7;
    private ProjectileManager manager;
    private Location location;
    private Vector velocity = new Vector();
    private Entity shooter;
    private Movement movement;
    private double distance, maxDistance = 128;
    private int age, maxTicks = 200;
    private boolean active, used;
    private double raySize = .15;
    private final Set<Listener> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
    // A cancelled target is ignored for the remainder of this flight, not retriggered every tick.
    private final Set<Block> ignoredBlocks = new HashSet<>();
    private final Set<Entity> ignoredEntities = Collections.newSetFromMap(new IdentityHashMap<>());
    private Predicate<Entity> entityFilter = entity -> true;
    private Consumer<CustomProjectile> tickAction = projectile -> {};
    private Consumer<ProjectileHit> hitAction = hit -> {};

    public CustomProjectile() { this(ProjectileMovement.FIREBALL_LIKE); }
    public CustomProjectile(Movement movement) { this.movement = Objects.requireNonNull(movement); }

    /** Shoots from an entity's eyes in its look direction, with speed in blocks per tick. */
    public final CustomProjectile shoot(LivingEntity shooter, double speed) {
        Objects.requireNonNull(shooter, "shooter");
        if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("speed must be positive and finite");
        return shoot(shooter.getEyeLocation(), shooter.getEyeLocation().getDirection().multiply(speed), shooter);
    }
    public final CustomProjectile shoot(Location from, Vector velocity, @Nullable Entity shooter) {
        return shoot(MCCodeCampLib.getProjectileManager(), from, velocity, shooter);
    }
    /** Explicit manager overload for integrations and tests. The manager owns cleanup. */
    public final CustomProjectile shoot(ProjectileManager manager, Location from, Vector velocity, @Nullable Entity shooter) {
        ProjectileManager.requireMainThread();
        if (used) throw new IllegalStateException("A projectile can only be shot once");
        Objects.requireNonNull(manager, "Initialize MCCodeCampLib before shooting");
        Objects.requireNonNull(from, "from"); Objects.requireNonNull(from.getWorld(), "world"); from.checkFinite();
        setVelocity(velocity);
        this.manager = manager; this.location = from.clone(); this.shooter = shooter;
        manager.add(this); used = true; active = true;
        try { onShoot(); } catch (RuntimeException failure) { remove(); throw failure; }
        return this;
    }

    public final boolean isActive() { return active; }
    public final @Nullable Entity getShooter() { return shooter; }
    public final Location getLocation() {
        if (location == null) throw new IllegalStateException("Projectile has not been shot");
        return location.clone();
    }
    public final Vector getVelocity() { return velocity.clone(); }
    public final void setVelocity(Vector velocity) {
        ProjectileManager.requireMainThread();
        Objects.requireNonNull(velocity).checkFinite();
        if (!Double.isFinite(velocity.lengthSquared())) throw new IllegalArgumentException("Velocity is too large");
        this.velocity = velocity.clone();
    }
    public final int getAge() { return age; }
    public final double getDistanceTravelled() { return distance; }
    public final double getRemainingDistance() { return Math.max(0, maxDistance - distance); }
    public final Movement getMovement() { return movement; }
    public final CustomProjectile setMovement(Movement movement) { ProjectileManager.requireMainThread(); this.movement = Objects.requireNonNull(movement); return this; }
    public final CustomProjectile setMovement(String preset) { return setMovement(ProjectileMovement.named(preset)); }
    public final CustomProjectile setMaxTicks(int ticks) {
        ProjectileManager.requireMainThread(); if (ticks <= 0) throw new IllegalArgumentException("ticks must be positive"); maxTicks = ticks; return this;
    }
    public final CustomProjectile setMaxDistance(double blocks) {
        ProjectileManager.requireMainThread();
        if (!Double.isFinite(blocks) || blocks <= 0 || blocks > 4096) throw new IllegalArgumentException("range must be in (0, 4096]");
        maxDistance = blocks; return this;
    }
    public final CustomProjectile setRaySize(double blocks) {
        ProjectileManager.requireMainThread();
        if (!Double.isFinite(blocks) || blocks < 0 || blocks > 4) throw new IllegalArgumentException("ray size must be in [0, 4]");
        raySize = blocks; return this;
    }
    public final CustomProjectile setEntityFilter(Predicate<Entity> filter) { ProjectileManager.requireMainThread(); entityFilter = Objects.requireNonNull(filter); return this; }
    public final CustomProjectile whenTick(Consumer<CustomProjectile> action) { ProjectileManager.requireMainThread(); tickAction = Objects.requireNonNull(action); return this; }
    /** Runs after a noncancelled event, before the subclass onHit hook. */
    public final CustomProjectile whenHit(Consumer<ProjectileHit> action) { ProjectileManager.requireMainThread(); hitAction = Objects.requireNonNull(action); return this; }

    protected void onShoot() {}
    protected void onTick() {}
    /** Override to supply a custom movement; return a displacement, not an absolute location. */
    protected Vector move() { return movement.move(this); }
    /**
     * Override collision detection if needed. Null means no hit. A ProjectileHit may
     * omit its face, but must contain a block OR an entity, and a point on the segment.
     */
    protected @Nullable ProjectileHit hit(Location from, Vector step) {
        double length = step.length();
        if (length <= EPSILON) return null;
        Vector direction = step.clone().normalize();
        World world = from.getWorld();
        RayTraceResult blockHit = world.rayTraceBlocks(from, direction, length, FluidCollisionMode.NEVER, true,
                block -> !ignoredBlocks.contains(block));
        RayTraceResult entityHit = world.rayTraceEntities(from, direction, length, raySize,
                entity -> entity != shooter && entity.isValid() && !entity.isDead()
                        && !(entity instanceof Display) && !(entity instanceof Interaction)
                        && (!(entity instanceof Player player) || player.getGameMode() != GameMode.SPECTATOR)
                        && !ignoredEntities.contains(entity) && entityFilter.test(entity));
        if (entityHit != null && (blockHit == null || entityHit.getHitPosition().distanceSquared(from.toVector())
                < blockHit.getHitPosition().distanceSquared(from.toVector()))) {
            return new ProjectileHit(null, entityHit.getHitEntity(), entityHit.getHitPosition(), entityHit.getHitBlockFace());
        }
        return blockHit == null ? null : new ProjectileHit(blockHit.getHitBlock(), null, blockHit.getHitPosition(), blockHit.getHitBlockFace());
    }
    /** Default behavior stops. Override for damage, bouncing, effects or piercing. */
    protected void onHit(ProjectileHit hit) { remove(); }
    /** Called for every actually travelled segment; useful for continuous particle trails/rays. */
    protected void onTravel(Location from, Location to) {}
    protected void onRemove() {}

    /** Register listeners through here so removal, expiry and plugin shutdown unregister them. */
    protected final void listen(Listener listener) {
        ProjectileManager.requireMainThread();
        if (!active) throw new IllegalStateException("Register listeners in onShoot, not the constructor");
        if (listeners.add(Objects.requireNonNull(listener))) Bukkit.getPluginManager().registerEvents(listener, manager.getPlugin());
    }
    public final void remove() {
        ProjectileManager.requireMainThread();
        if (!active) return;
        active = false;
        manager.forget(this);
        for (Listener listener : listeners) HandlerList.unregisterAll(listener);
        listeners.clear();
        // Also release subclass handlers if the subclass registered itself outside listen().
        if (this instanceof Listener listener) HandlerList.unregisterAll(listener);
        ignoredBlocks.clear(); ignoredEntities.clear();
        onRemove();
    }
    @Override public final void close() { remove(); }

    final void update() {
        if (!active) return;
        World world = location.getWorld();
        if (!world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                || location.getY() < world.getMinHeight() || location.getY() >= world.getMaxHeight()) { remove(); return; }
        if (age >= maxTicks || getRemainingDistance() <= EPSILON) { remove(); return; }
        age++;
        onTick(); if (!active) return;
        tickAction.accept(this); if (!active) return;
        Vector moved = move();
        if (!active) return;
        Vector step = Objects.requireNonNull(moved, "move returned null").clone(); step.checkFinite();
        double length = step.length();
        if (!Double.isFinite(length)) throw new IllegalArgumentException("Movement is too large");
        if (length > getRemainingDistance()) { step.multiply(getRemainingDistance()/length); length = step.length(); }
        double safe = loadedDistance(location, step);
        boolean unloaded = safe < length;
        if (unloaded) { step.multiply(length == 0 ? 0 : safe/length); length = safe; }
        Location end = location.clone().add(step);
        int impacts = 0;
        while (active && length > EPSILON) {
            // Hit/travel listeners can lower the range during this sweep.
            double available = getRemainingDistance();
            if (available <= EPSILON) break;
            if (length > available) {
                end = location.clone().add(end.toVector().subtract(location.toVector()).normalize().multiply(available));
                length = available;
            }
            ProjectileHit result = hit(location.clone(), end.toVector().subtract(location.toVector()));
            if (!active) break;
            if (result == null) { travelTo(end); break; }
            Vector point = result.position(), from = location.toVector();
            Vector delta = point.clone().subtract(from), remaining = end.toVector().subtract(from);
            double along = delta.dot(remaining) / remaining.lengthSquared();
            if (along < -EPSILON || along > 1+EPSILON || delta.clone().subtract(remaining.multiply(along)).lengthSquared() > 1e-8)
                throw new IllegalArgumentException("hit returned a position outside its movement segment");
            travelTo(point.toLocation(world));
            if (!active) break;
            ProjectileHitEvent event = new ProjectileHitEvent(this,result);
            event.callEvent();
            if (!active) break;
            if (!event.isCancelled()) {
                hitAction.accept(result); if (active) onHit(result);
                break; // A retained/bouncing projectile resumes with its new velocity next tick.
            }
            if (result.block() != null) ignoredBlocks.add(result.block());
            else ignoredEntities.add(result.entity());
            // Custom hit implementations must honor ignored targets or make progress themselves.
            if (++impacts >= 64) { remove(); break; }
            length = location.distance(end);
        }
        if (active && (unloaded || age >= maxTicks || getRemainingDistance() <= EPSILON || movement == ProjectileMovement.RAYCAST)) remove();
    }
    private void travelTo(Location target) {
        Location previous = location.clone(); distance += previous.distance(target); location = target.clone();
        onTravel(previous, target.clone());
    }
    /** Chunk-grid DDA clips the centerline to loaded chunks; Paper owns shape-neighbor lookups. */
    private static double loadedDistance(Location start, Vector step) {
        double length = step.length(); if (length <= EPSILON) return length;
        Vector direction = step.clone().normalize(); World world = start.getWorld();
        int x = start.getBlockX() >> 4, z = start.getBlockZ() >> 4;
        double dx = direction.getX(), dz = direction.getZ();
        double tx = dx == 0 ? Double.POSITIVE_INFINITY : ((dx > 0 ? (x+1)*16.0 : x*16.0)-start.getX())/dx;
        double tz = dz == 0 ? Double.POSITIVE_INFINITY : ((dz > 0 ? (z+1)*16.0 : z*16.0)-start.getZ())/dz;
        while (Math.min(tx,tz) <= length) {
            double next = Math.min(tx,tz);
            if (tx <= next) { x += dx > 0 ? 1 : -1; tx += 16/Math.abs(dx); }
            if (tz <= next) { z += dz > 0 ? 1 : -1; tz += 16/Math.abs(dz); }
            if (!world.isChunkLoaded(x,z)) return Math.max(0,next-EPSILON);
        }
        double dy = direction.getY();
        if (dy > 0) length = Math.min(length,Math.max(0,(world.getMaxHeight()-start.getY())/dy-EPSILON));
        else if (dy < 0) length = Math.min(length,Math.max(0,(world.getMinHeight()-start.getY())/dy-EPSILON));
        return length;
    }
}
