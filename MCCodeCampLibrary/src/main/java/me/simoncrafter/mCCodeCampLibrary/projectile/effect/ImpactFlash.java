package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import me.simoncrafter.CraftersDisplayLibrary.display.cube.CubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.panel.ColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.effect.viewtinter.ViewTinter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

/** Main-thread, owner-ticked impact overlay. No listeners or additional animation tasks. */
public final class ImpactFlash implements AutoCloseable {
    private static final int DURATION = 5;
    private final Iterable<? extends Player> players;
    private final Tints tints;
    private final Map<UUID, Owned> owned = new HashMap<>();
    private long clock;
    private boolean closed;

    /** The owner must call {@link #tick()} once per server tick and close on shutdown. */
    public ImpactFlash(Plugin plugin) {
        this(() -> new ArrayList<Player>(Bukkit.getOnlinePlayers()).iterator(), new LibraryTints(Objects.requireNonNull(plugin)));
    }

    ImpactFlash(Iterable<? extends Player> players, Tints tints) {
        this.players = Objects.requireNonNull(players);
        this.tints = Objects.requireNonNull(tints);
    }

    /** Flash online players in the same world, at most 100 blocks from the impact (sphere). */
    public void flash(Location center) {
        Objects.requireNonNull(center, "center");
        if (closed || center.getWorld() == null) return;
        for (Player player : players) {
            if (!player.isOnline() || !center.getWorld().equals(player.getWorld())
                    || player.getLocation().distanceSquared(center) > 10000) continue;
            Owned previous = owned.remove(player.getUniqueId());
            if (previous != null) release(previous);
            // CDL reuses active cubes. Remove the old registry entry first so we never adopt
            // another feature's cube as our exact-owned display.
            if (tints.current(player) != null) tints.untint(player);
            CubeColorDisplay cube = tints.tint(player);
            if (cube == null) continue;
            Owned entry = new Owned(player, player.getWorld(), cube, clock + DURATION);
            owned.put(player.getUniqueId(), entry);
            try {
                tints.configure(cube, player);
            } catch (RuntimeException failure) {
                owned.remove(player.getUniqueId());
                release(entry);
                throw failure;
            }
        }
    }

    /** Advance fade and clean up expired, disconnected, world-changed or displaced overlays. */
    public void tick() {
        if (closed) return;
        clock++;
        Iterator<Owned> iterator = owned.values().iterator();
        while (iterator.hasNext()) {
            Owned entry = iterator.next();
            boolean current = tints.current(entry.player) == entry.cube;
            if (!current || !entry.player.isOnline() || !entry.world.equals(entry.player.getWorld())
                    || clock >= entry.deadline) {
                iterator.remove();
                if (current && clock >= entry.deadline) {
                    try { tints.color(entry.cube, Color.fromARGB(0, 255, 255, 255)); }
                    finally { release(entry); }
                } else release(entry);
            } else {
                int alpha = Math.round(255f * (entry.deadline - clock) / DURATION);
                tints.color(entry.cube, Color.fromARGB(alpha, 255, 255, 255));
            }
        }
    }

    private void release(Owned entry) {
        try {
            if (tints.current(entry.player) == entry.cube) tints.untint(entry.player);
        } catch (RuntimeException ignored) {
            // Registry cleanup must never prevent exact-owned native entity removal.
        } finally {
            tints.remove(entry.cube);
        }
    }

    /** Idempotently remove only this instance's owned cubes, never a replacement registry tint. */
    @Override public void close() {
        if (closed) return;
        closed = true;
        List<Owned> entries = new ArrayList<>(owned.values());
        owned.clear();
        for (Owned entry : entries) release(entry);
    }

    private record Owned(Player player, World world, CubeColorDisplay cube, long deadline) { }

    interface Tints {
        CubeColorDisplay tint(Player player);
        CubeColorDisplay current(Player player);
        void configure(CubeColorDisplay cube, Player player);
        void color(CubeColorDisplay cube, Color color);
        void untint(Player player);
        void remove(CubeColorDisplay cube);
    }

    private static final class LibraryTints implements Tints {
        private final Plugin plugin;
        // Keep native references before CDL can clear face fields during automatic registry expiry.
        private final Map<CubeColorDisplay, List<Entity>> entities = new IdentityHashMap<>();
        LibraryTints(Plugin plugin) { this.plugin = plugin; }
        public CubeColorDisplay tint(Player player) {
            // Own tick alpha, not CDL's globally scheduled animation clock.
            return ViewTinter.tintPlayer(player, Color.WHITE, null, DURATION);
        }
        public CubeColorDisplay current(Player player) { return ViewTinter.getTintDisplay(player); }
        public void configure(CubeColorDisplay cube, Player player) {
            List<Entity> nativeEntities = new ArrayList<>();
            entities.put(cube, nativeEntities);
            for (ColorDisplay face : Arrays.asList(cube.getTop(), cube.getBottom(), cube.getLeft(),
                    cube.getRight(), cube.getFront(), cube.getBack())) {
                if (face != null && face.getEntity() != null) nativeEntities.add(face.getEntity());
            }
            for (Entity entity : nativeEntities) entity.setPersistent(false);
            // CDL v1.2.5a has these calls commented out in ViewTinter.
            cube.hideByDefault(true, true);
            cube.showForPlayer(player, true);
        }
        public void color(CubeColorDisplay cube, Color color) { cube.setColor(color); }
        public void untint(Player player) { ViewTinter.untintPlayer(player); }
        public void remove(CubeColorDisplay cube) {
            List<Entity> nativeEntities = entities.remove(cube);
            try { cube.remove(); }
            catch (RuntimeException failure) { plugin.getLogger().log(Level.WARNING, "Impact tint wrapper cleanup failed", failure); }
            finally {
                if (nativeEntities != null) for (Entity entity : nativeEntities) {
                    try { entity.remove(); }
                    catch (RuntimeException failure) { plugin.getLogger().log(Level.WARNING, "Impact tint entity cleanup failed", failure); }
                }
            }
        }
    }
}
