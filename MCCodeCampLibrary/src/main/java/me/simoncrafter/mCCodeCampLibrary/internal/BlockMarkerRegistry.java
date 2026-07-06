package me.simoncrafter.mCCodeCampLibrary.internal;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class BlockMarkerRegistry {

    /**
     * type is a fixed, library-controlled token (e.g. "button") and goes into the
     * NamespacedKey itself. id is whatever a course author chose and is only ever
     * stored as the PDC value, never as part of the key — NamespacedKey keys are
     * restricted to lowercase/digits/./-/_, and ids aren't guaranteed to fit that.
     */
    private record MarkerEntry(String type, String id) {}

    private final Plugin plugin;
    private final Map<Location, MarkerEntry> cache = new HashMap<>();
    private final Map<String, Consumer<String>> loadHandlers = new HashMap<>();
    private final Map<String, Consumer<String>> unloadHandlers = new HashMap<>();

    public BlockMarkerRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Notified with the marker's id whenever a marker of this type loads in with its chunk. */
    public void registerLoadHandler(String type, Consumer<String> handler) {
        loadHandlers.put(type, handler);
    }

    /** Notified with the marker's id whenever a marker of this type unloads with its chunk. */
    public void registerUnloadHandler(String type, Consumer<String> handler) {
        unloadHandlers.put(type, handler);
    }

    public void unregisterHandlers(String type) {
        loadHandlers.remove(type);
        unloadHandlers.remove(type);
    }

    /** Tag a block as course-relevant (used during course authoring). */
    public void setMarker(Block block, String type, String id) {
        NamespacedKey key = keyFor(block, type);
        block.getChunk().getPersistentDataContainer()
                .set(key, PersistentDataType.STRING, id);
        cache.put(block.getLocation(), new MarkerEntry(type, id));
    }

    /** Remove a marker (e.g. block broken, or author un-tags it). */
    public void removeMarker(Block block) {
        MarkerEntry entry = cache.remove(block.getLocation());
        if (entry == null) return;
        block.getChunk().getPersistentDataContainer().remove(keyFor(block, entry.type()));
    }

    /** O(1) runtime lookup — no PDC/file access during gameplay. Empty if the marker is missing or a different type. */
    public Optional<String> getMarker(Block block, String type) {
        MarkerEntry entry = cache.get(block.getLocation());
        if (entry == null || !entry.type().equals(type)) {
            return Optional.empty();
        }
        return Optional.of(entry.id());
    }

    /** Returns a copy of the cached items (location -> id), across all marker types. */
    public Map<Location, String> getCache() {
        Map<Location, String> result = new HashMap<>();
        for (Map.Entry<Location, MarkerEntry> entry : cache.entrySet()) {
            result.put(entry.getKey(), entry.getValue().id());
        }
        return result;
    }

    /** Call from a ChunkLoadEvent listener. */
    public void onChunkLoad(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        for (NamespacedKey key : pdc.getKeys()) {
            if (!key.getNamespace().equals(plugin.getName().toLowerCase())) continue;

            // marker_x_y_z_type — limit 5 so a multi-word type keeps its underscore(s)
            String[] parts = key.getKey().split("_", 5);
            if (parts.length != 5 || !parts[0].equals("marker")) continue;

            int x = chunk.getX() * 16 + Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = chunk.getZ() * 16 + Integer.parseInt(parts[3]);
            String type = parts[4];
            String id = pdc.get(key, PersistentDataType.STRING);
            if (id == null) continue;

            Location location = new Location(chunk.getWorld(), x, y, z);
            cache.put(location, new MarkerEntry(type, id));

            Consumer<String> handler = loadHandlers.get(type);
            if (handler != null) handler.accept(id);
        }
    }

    /** Call from a ChunkUnloadEvent listener, to bound memory use. */
    public void onChunkUnload(Chunk chunk) {
        cache.entrySet().removeIf(entry -> {
            if (!entry.getKey().getChunk().equals(chunk)) return false;

            Consumer<String> handler = unloadHandlers.get(entry.getValue().type());
            if (handler != null) handler.accept(entry.getValue().id());
            return true;
        });
    }

    private NamespacedKey keyFor(Block block, String type) {
        int localX = block.getX() & 15;
        int localZ = block.getZ() & 15;
        return new NamespacedKey(plugin, "marker_" + localX + "_" + block.getY() + "_" + localZ + "_" + type);
    }
}
