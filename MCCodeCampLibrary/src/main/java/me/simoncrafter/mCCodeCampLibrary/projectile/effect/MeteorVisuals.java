package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import me.simoncrafter.CraftersDisplayLibrary.display.panel.BlockDisplayObject;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.world.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.BiConsumer;

/** Visual-only, centrally ticked surface waves. Never loads terrain or changes blocks.
 * Call tick once per server tick and close when the owning service shuts down.
 */
public final class MeteorVisuals implements AutoCloseable, Listener {
    private final Plugin plugin;
    private final List<Wave> waves = new ArrayList<>();
    private boolean closed, ticking;
    private static final int RAYS = 32, RINGS = 3, STAGGER = 6;
    private static final int TRAVEL_TICKS = 150;
    private static final double RANGE = 150, BURN_RANGE = 50;
    private static final Material[] MATERIALS = {Material.MAGMA_BLOCK, Material.YELLOW_STAINED_GLASS,
            Material.ORANGE_STAINED_GLASS, Material.WHITE_STAINED_GLASS,
            Material.GRAY_STAINED_GLASS, Material.BLACK_STAINED_GLASS};

    public MeteorVisuals(Plugin plugin) {
        mainThread(); this.plugin = Objects.requireNonNull(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    /** Neutral stationary entity anchor, chosen independently of the rendered world position. */
    public static Location nearestAnchor(Location position) {
        mainThread(); Objects.requireNonNull(position.getWorld(), "world");
        Location anchor = position.clone(); double best = Double.POSITIVE_INFINITY;
        for (Player player : position.getWorld().getPlayers()) {
            Location candidate = player.getLocation();
            double distance = candidate.distanceSquared(position);
            if (distance < best) { best = distance; anchor = candidate.clone(); }
        }
        anchor.setYaw(0); anchor.setPitch(0); return anchor;
    }
    /** Starts 96 staggered surface displays; destructive only enables the supplied burn callback. */
    public void startImpact(Location center, boolean destructive, BiConsumer<Location, Double> burnAt) {
        mainThread(); if (closed) throw new IllegalStateException("Meteor visuals closed");
        Objects.requireNonNull(center); Objects.requireNonNull(center.getWorld()); center.checkFinite();
        Wave wave = new Wave(center.clone(), destructive, Objects.requireNonNull(burnAt));
        waves.add(wave);
    }
    public boolean isActive() { mainThread(); return !waves.isEmpty(); }
    public void tick() {
        mainThread(); if (closed || ticking) return;
        ticking = true;
        try {
            for (Wave wave : List.copyOf(waves)) {
                if (!waves.contains(wave)) continue;
                try { if (!wave.tick()) remove(wave); }
                catch (RuntimeException failure) { remove(wave); throw failure; }
            }
        } finally { ticking = false; }
    }
    private void remove(Wave wave) { waves.remove(wave); wave.close(); }
    @Override public void close() {
        mainThread(); if (closed) return; closed = true;
        for (Wave wave : waves) wave.close();
        waves.clear(); HandlerList.unregisterAll(this);
    }
    @EventHandler public void onDisable(PluginDisableEvent event) { if (event.getPlugin() == plugin) close(); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnload(WorldUnloadEvent event) {
        mainThread(); for (Wave wave : List.copyOf(waves)) if (wave.center.getWorld() == event.getWorld()) remove(wave);
    }
    @EventHandler public void onChunkUnload(ChunkUnloadEvent event) {
        mainThread();
        for (Wave wave : List.copyOf(waves)) {
            if (wave.center.getWorld() != event.getWorld()) continue;
            Location a = wave.anchor;
            if ((a.getBlockX() >> 4) == event.getChunk().getX() && (a.getBlockZ() >> 4) == event.getChunk().getZ()) remove(wave);
        }
    }
    /** Scan only an already-loaded column. Water counts as surface; trees and foliage do not. */
    private static double surfaceY(World world, int x, int z) {
        int min = world.getMinHeight();
        for (int y = world.getHighestBlockYAt(x, z); y >= min; y--) {
            org.bukkit.block.Block block = world.getBlockAt(x, y, z);
            Material material = block.getType();
            String name = material.name();
            if (material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR || name.endsWith("_LEAVES") || name.endsWith("_LOG")
                    || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE")
                    || name.endsWith("_ROOTS") || name.endsWith("MUSHROOM_BLOCK")
                    || material == Material.BAMBOO || material == Material.AZALEA
                    || material == Material.FLOWERING_AZALEA) continue;
            // Non-solid plants (flowers, grasses, vines, etc.) must not lift the cloud.
            if (material == Material.WATER || material == Material.LAVA || !block.isPassable()) return y + 1;
        }
        return min;
    }
    private static void mainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Meteor visuals require the server thread");
    }
    private final class Wave implements AutoCloseable {
        final Location center, anchor;
        final boolean destructive;
        final BiConsumer<Location, Double> burn;
        final BlockDisplayObject[] displays = new BlockDisplayObject[RAYS * RINGS];
        int age;
        boolean removed;
        Wave(Location center, boolean destructive, BiConsumer<Location, Double> burn) {
            this.center = center; this.anchor = nearestAnchor(center); this.destructive = destructive; this.burn = burn;
        }
        boolean tick() {
            World world = center.getWorld();
            if (removed || !world.isChunkLoaded(anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4)) return false;
            for (int ring = 0; ring < RINGS; ring++) {
                int elapsed = age - ring * STAGGER;
                if (elapsed < 0) continue;
                double progress = Math.min(1, (double) elapsed / TRAVEL_TICKS);
                double distance = RANGE * (2 * progress - progress * progress);
                // Integrate the same declining velocity for slow spin, never orbit the ray.
                double spin = .04 * TRAVEL_TICKS * (progress - progress * progress / 2);
                float size = (float)(2 + 8 * Math.min(1, distance / BURN_RANGE));
                int stage = distance < 15 ? 0 : distance < 35 ? 1 : distance < 65 ? 2 : distance < 95 ? 3 : distance < 125 ? 4 : 5;
                for (int ray = 0; ray < RAYS; ray++) {
                    int index = ring * RAYS + ray;
                    BlockDisplayObject display = displays[index];
                    if (elapsed > TRAVEL_TICKS) {
                        if (display != null) { display.remove(); displays[index] = null; }
                        continue;
                    }
                    double angle = 2 * Math.PI * (ray + ring / 3.0) / RAYS;
                    Location position = center.clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
                    // Highest-block lookups are only legal for already-loaded chunks.
                    if (world.isChunkLoaded(position.getBlockX() >> 4, position.getBlockZ() >> 4))
                        position.setY(surfaceY(world, position.getBlockX(), position.getBlockZ()));
                    if (display == null) {
                        display = BlockDisplayObject.create(anchor.clone(), new Vector3f(size),
                                position.toVector().subtract(anchor.toVector()).toVector3f(), MATERIALS[stage].createBlockData());
                        displays[index] = display;
                        display.spawnDisplay().setPersistent(false);
                    }
                    if (!display.getEntity().isValid()) return false;
                    display.setBlock(MATERIALS[stage].createBlockData());
                    display.scaleAbsoluteNoUpdate(new Vector3f(size));
                    display.LRotateAbsoluteNoUpdate(new Quaternionf().rotateY((float)(angle + spin)));
                    display.moveAbsolute(position.toVector().subtract(anchor.toVector()).toVector3f(), 1);
                    if (destructive && distance <= BURN_RANGE) {
                        // CDL's local origin is the block CENTER (its entity transform includes corner correction).
                        Vector3f translation = display.getLocalTransform().getTranslation();
                        Location actual = anchor.clone().add(translation.x, translation.y, translation.z);
                        burn.accept(actual, (double)size / 2);
                        if (removed) return false;
                    }
                }
            }
            return age++ <= TRAVEL_TICKS + (RINGS - 1) * STAGGER;
        }
        @Override public void close() {
            if (removed) return; removed = true;
            for (int i = 0; i < displays.length; i++) if (displays[i] != null) {
                displays[i].remove(); displays[i] = null;
            }
        }
    }
}
