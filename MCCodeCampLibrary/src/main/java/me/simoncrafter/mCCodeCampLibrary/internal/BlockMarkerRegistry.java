package me.simoncrafter.mCCodeCampLibrary.internal;

import me.simoncrafter.mCCodeCampLibrary.internal.events.BlockRegistryUpdateEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class BlockMarkerRegistry {



    private final Plugin plugin;
    private final Map<Location, BlockMarkerEntry> cache = new HashMap<>();

    public BlockMarkerRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Tag a block as course-relevant (used during course authoring). */
    public void setMarker(Block block, String type, String id) {
        NamespacedKey key = keyFor(block, type);
        block.getChunk().getPersistentDataContainer()
                .set(key, PersistentDataType.STRING, id);
        cache.put(block.getLocation(), new BlockMarkerEntry(type, id));
        sendUpdateEvent();
    }

    /** Remove a marker (e.g. block broken, or author un-tags it). */
    public void removeMarker(Block block) {
        BlockMarkerEntry entry = cache.remove(block.getLocation());
        if (entry == null) return;
        block.getChunk().getPersistentDataContainer().remove(keyFor(block, entry.type()));
        sendUpdateEvent();
    }

    /** O(1) runtime lookup — no PDC/file access during gameplay. Empty if the marker is missing or a different type. */
    public Optional<String> getMarker(Block block, String type) {
        BlockMarkerEntry entry = cache.get(block.getLocation());
        if (entry == null || !entry.type().equals(type)) {
            return Optional.empty();
        }
        return Optional.of(entry.id());
    }

    public boolean hasMarker(Block block) {
        return cache.containsKey(block.getLocation());
    }

    /** Returns a copy of the cached items (location -> id), across all marker types. */
    public Map<Location, BlockMarkerEntry> getCache() {
        return new HashMap<>(cache);
    }

    /** Call from a ChunkLoadEvent listener. */
    public void onChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        boolean updated = false;
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals(plugin.namespace())) continue;

            // marker_x_y_z_type — limit 5 so a multi-word type keeps its underscore(s)
            String[] parts = key.getKey().split("_", 5);
            if (parts.length != 5 || !parts[0].equals("marker")) continue;

            int x, y, z;
            try {
                x = chunk.getX() * 16 + Integer.parseInt(parts[1]);
                y = Integer.parseInt(parts[2]);
                z = chunk.getZ() * 16 + Integer.parseInt(parts[3]);
            } catch (NumberFormatException e) {
                continue;
            }
            String type = parts[4];
            String id = pdc.get(key, PersistentDataType.STRING);
            if (id == null) continue;

            Location location = new Location(chunk.getWorld(), x, y, z);
            cache.put(location, new BlockMarkerEntry(type, id));
            updated = true;
        }
        if (updated) {
            sendUpdateEvent();
        }
    }

    /** Call from a ChunkUnloadEvent listener, to bound memory use. */
    public void onChunkUnload(Chunk chunk) {
        AtomicBoolean update = new AtomicBoolean(false);  // atomic boolen was sudgested by intelij as a fix
        cache.entrySet().removeIf(entry -> {
            if (entry.getKey().getChunk().equals(chunk)) {
                update.set(true);
                return true;
            }
            return false;
        });
        if (update.get()) {
            sendUpdateEvent();
        }
    }

    private NamespacedKey keyFor(Block block, String type) {
        int localX = block.getX() & 15;
        int localZ = block.getZ() & 15;
        return new NamespacedKey(plugin, "marker_" + localX + "_" + block.getY() + "_" + localZ + "_" + type);
    }

    private void sendUpdateEvent() {
        new BlockRegistryUpdateEvent().callEvent();
    }
}
