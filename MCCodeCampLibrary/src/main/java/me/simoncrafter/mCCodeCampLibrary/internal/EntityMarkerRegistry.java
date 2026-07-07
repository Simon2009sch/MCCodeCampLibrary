package me.simoncrafter.mCCodeCampLibrary.internal;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public class EntityMarkerRegistry {

    private final Plugin plugin;

    public EntityMarkerRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Tag an entity as course-relevant (used during course authoring). */
    public void setMarker(Entity entity, String type, String id) {
        entity.getPersistentDataContainer().set(keyFor(type), PersistentDataType.STRING, id);
    }

    /** Remove a marker (e.g. entity un-tagged by author). */
    public void removeMarker(Entity entity, String type) {
        entity.getPersistentDataContainer().remove(keyFor(type));
    }

    /** Entities carry their own marker via PDC, so no cache is needed — just read it directly. */
    public Optional<String> getMarker(Entity entity, String type) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(keyFor(type), PersistentDataType.STRING));
    }

    private NamespacedKey keyFor(String type) {
        return new NamespacedKey(plugin, "marker_" + type);
    }
}
