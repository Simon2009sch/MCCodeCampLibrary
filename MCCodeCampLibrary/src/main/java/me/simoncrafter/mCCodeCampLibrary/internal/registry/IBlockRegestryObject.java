package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.UUID;

public interface IBlockRegestryObject extends Listener {

    /**
     * This method gets called when reading from a chunk;
     * @param plugin The plugin this instance belongs to
     * @param loc The location this object is located at
     * @param ID The ID of the object
     * @param objectType The Type ID of the object
     * @param config The config that gets read in json form (see {@link ConfigurationNode})
     * @param registryInstance The instance of the registry. Used for making a save callback on edits to the object unsing {@link BlockMarkerRegistry#saveObject(IBlockRegestryObject)}
     * @param uuid The uuid of the object. This should not change
     */
    void init(Plugin plugin, Location loc, String ID, RegistryObjectType objectType, ConfigurationNode config, UUID uuid, BlockMarkerRegistry registryInstance);
    String getID();
    Location getLocation();
    ConfigurationNode getConfig();
    void setLocation(Location location);
    String getTypeID();
    UUID getUUID();

    /**
     * Releases all world/event/task attachments held by this object (unregisters this
     * object's Bukkit event handlers, cancels pending tasks, removes spawned display
     * entities). After {@code destroy()} the instance is inert: it no longer reacts to
     * events and no longer touches the world, so external code may keep referencing it
     * safely until it is garbage collected.
     * <p>
     * This is invoked by {@link BlockMarkerRegistry} when the owning chunk unloads and
     * when the object is removed. Implementations should be idempotent and must not
     * modify the chunk's persistent data (that is the registry's source of truth).
     */
    default void destroy() {
        HandlerList.unregisterAll(this);
    }
}
