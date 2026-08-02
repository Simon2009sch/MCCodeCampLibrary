package me.simoncrafter.mCCodeCampLibrary.internal.registry;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import javax.annotation.Nullable;
import javax.swing.text.html.Option;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.logging.Logger;

public class BlockMarkerRegistry implements Listener {
    private static final char MARKER_DATA_DENOMINATOR = ';';
    private final Logger PLUGIN_LOGGER;
    private final Plugin PLUGIN;
    private final NamespacedKey NAMESPACED_KEY;

    private Map<String, RegistryObjectType> objectTypes = new HashMap<>();
    private Map<String, Map<String, IBlockRegestryObject>> registeredObjects = new HashMap<>();


    public BlockMarkerRegistry(Plugin plugin) {
        PLUGIN_LOGGER = plugin.getLogger();
        PLUGIN = plugin;
        NAMESPACED_KEY = new NamespacedKey(PLUGIN, "blockmarker_registry");
        for (World w : Bukkit.getWorlds()) {
            for (Chunk c : w.getLoadedChunks()) {
                // todo: implement init chunk scanning and loading
            }
        }

    }

    public record PDCSplitData(Location location, String type, String ID, ConfigurationNode config) {}

    private record ObjectRegistryKey(String typeKey, String idKey) {}

    private void parseChunk(Chunk chunk) {
        String jsonString = chunk.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING);
        ConfigurationNode config = loadChunkConfig(jsonString);
        if (config == null) {
            return;
        }

        List<? extends ConfigurationNode> childList = config.node("objects").childrenList();
        for (ConfigurationNode node : childList) {
            parseObjectNode(node);
        }
    }

    private @Nullable ConfigurationNode loadChunkConfig(String jsonString) {
        if (jsonString == null || jsonString.isBlank() || jsonString.isBlank()) {
            return null;
        }
        try {
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(jsonString)))
                    .build();
            return loader.load();
        } catch (ConfigurateException e) {
            PLUGIN_LOGGER.warning("Failed to parse json marker configuration of chunk. Error" + e.getMessage());
            return null;
        }
    }

    private void parseObjectNode(ConfigurationNode node) {
        if (!node.hasChild("type") || !node.hasChild("id") || !node.hasChild("location") || !node.hasChild("uuid")) {
            PLUGIN_LOGGER.warning("Missing keys in marker configuration! Skipping");
            return;
        }
        String type = node.node("type").getString("");
        String id = node.node("id").getString("");

        UUID uuid;
        try {
            uuid = UUID.fromString(node.node("uuid").getString(""));
        } catch (Exception e) {
            PLUGIN_LOGGER.warning("Malformed UUID of object: " + node.node("uuid").getString() + " ID: " + id + " Type: " + type);
            return;
        }

        Location location = readLocation(node.node("location"));
        if (location == null) {
            PLUGIN_LOGGER.warning("World was not defined in location of object");
            return;
        }

        RegistryObjectType objectType = objectTypes.get(type);
        if (objectType == null) {
            PLUGIN_LOGGER.warning("Object type is not recognized: " + type);
            return;
        }

        IBlockRegestryObject obj;
        try {
            obj = objectType.createObject().call();
        } catch (Exception e) {
            PLUGIN_LOGGER.warning("Error with creating block marker object");
            return;
        }

        obj.init(PLUGIN, location, id, objectType, node.node("config"), uuid, this);
        putObject(type, id, obj);
    }

    private @Nullable Location readLocation(ConfigurationNode locationNode) {
        double x = locationNode.node("x").getDouble();
        double y = locationNode.node("y").getDouble();
        double z = locationNode.node("z").getDouble();
        World world = Bukkit.getWorld(locationNode.node("world").getString(""));

        if (world == null) {
            return null;
        }
        return new Location(world, x, y, z);
    }

    public void saveObject(IBlockRegestryObject obj) {
        Chunk chunk = obj.getLocation().getChunk();
        ChunkObjectMatch match = findObjectWithUUID(obj.getUUID(), chunk);
        if (match == null) {
            PLUGIN_LOGGER.warning("Could not find marker entry for object " + obj.getID() + " (" + obj.getUUID() + ") in chunk PDC. Not saving");
            return;
        }
        try {
            writeObjectNode(match.node(), obj);
        } catch (SerializationException e) {
            PLUGIN_LOGGER.warning("Failed to write marker entry to chunk PDC. Error" + e.getMessage());
            return;
        }
        saveChunkConfig(chunk, match.root());
    }

    private void writeObjectNode(ConfigurationNode node, IBlockRegestryObject obj) throws SerializationException {
        node.node("type").set(obj.getTypeID());
        node.node("id").set(obj.getID());
        node.node("uuid").set(obj.getUUID().toString());
        writeLocation(node.node("location"), obj.getLocation());
        node.node("config").from(obj.getConfig());
    }

    private void writeLocation(ConfigurationNode locationNode, Location location) throws SerializationException {
        locationNode.node("x").set(location.getX());
        locationNode.node("y").set(location.getY());
        locationNode.node("z").set(location.getZ());
        locationNode.node("world").set(location.getWorld().getName());
    }

    public void saveObject(UUID uuid) {
        IBlockRegestryObject obj = findRegisteredObject(uuid);
        if (obj == null) {
            PLUGIN_LOGGER.warning("Could not find registered object with UUID " + uuid + " to save");
            return;
        }
        saveObject(obj);
    }

    public void removeObject(IBlockRegestryObject obj) {
        Chunk chunk = obj.getLocation().getChunk();
        ChunkObjectMatch match = findObjectWithUUID(obj.getUUID(), chunk);
        if (match != null) {
            try {
                match.node().set(null);
            } catch (SerializationException e) {
                PLUGIN_LOGGER.warning("Failed to remove marker entry from chunk PDC. Error" + e.getMessage());
            }
            saveChunkConfig(chunk, match.root());
        }
        removeObject(obj.getTypeID(), obj.getID());
    }

    public void removeObject(UUID uuid) {
        IBlockRegestryObject obj = findRegisteredObject(uuid);
        if (obj == null) {
            PLUGIN_LOGGER.warning("Could not find registered object with UUID " + uuid + " to remove");
            return;
        }
        removeObject(obj);
    }

    private @Nullable IBlockRegestryObject findRegisteredObject(UUID uuid) {
        for (Map<String, IBlockRegestryObject> objectsOfType : registeredObjects.values()) {
            for (IBlockRegestryObject obj : objectsOfType.values()) {
                if (obj.getUUID().equals(uuid)) {
                    return obj;
                }
            }
        }
        return null;
    }

    private record ChunkObjectMatch(ConfigurationNode root, ConfigurationNode node) {}

    private @Nullable ChunkObjectMatch findObjectWithUUID(UUID uuid, Chunk chunk) {
        String jsonString = chunk.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING);
        ConfigurationNode config = loadChunkConfig(jsonString);
        if (config == null) {
            return null;
        }
        List<? extends ConfigurationNode> childList = config.node("objects").childrenList();
        for (ConfigurationNode node : childList) {
            UUID readUUID;
            try {
                readUUID = UUID.fromString(node.node("uuid").getString(""));
            } catch (Exception e) {
                PLUGIN_LOGGER.warning("Found empty or invalid UUID: " + node.node("uuid").getString(""));
                continue;
            }
            if (readUUID.equals(uuid)) {
                return new ChunkObjectMatch(config, node);
            }
        }
        return null;
    }

    private void saveChunkConfig(Chunk chunk, ConfigurationNode config) {
        try {
            StringWriter writer = new StringWriter();
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .sink(() -> new BufferedWriter(writer))
                    .build();
            loader.save(config);
            chunk.getPersistentDataContainer().set(NAMESPACED_KEY, PersistentDataType.STRING, writer.toString());
        } catch (ConfigurateException e) {
            PLUGIN_LOGGER.warning("Failed to save marker configuration to chunk PDC. Error" + e.getMessage());
        }
    }


    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        parseChunk(event.getChunk());
    }

    public void onChunkLoad(Chunk chunk) {
        parseChunk(chunk);
    }

    public void registerObjectType(String objectTypeID, RegistryObjectType obj) {
        objectTypes.putIfAbsent(objectTypeID, obj);
    }

    /**
     * @return an unmodifiable view of the registered object type IDs.
     */
    public Set<String> getObjectTypeIDs() {
        return Collections.unmodifiableSet(objectTypes.keySet());
    }

    /**
     * @return {@code true} if an object of the given type is already registered under the given ID.
     */
    public boolean hasObject(String type, String id) {
        Map<String, IBlockRegestryObject> objectsOfType = registeredObjects.get(type);
        return objectsOfType != null && objectsOfType.containsKey(id);
    }

    /**
     * Creates a brand-new marker object of the given (previously {@link #registerObjectType registered})
     * type at the given location, registers it in memory, and persists it to the chunk's PDC.
     *
     * @return the created object, or {@code null} if {@code typeID} is not a registered type or the
     * object failed to construct.
     */
    public @Nullable IBlockRegestryObject createObject(String typeID, String id, Location location) {
        RegistryObjectType objectType = objectTypes.get(typeID);
        if (objectType == null) {
            PLUGIN_LOGGER.warning("Cannot create marker: unrecognized type \"" + typeID + "\"");
            return null;
        }

        IBlockRegestryObject obj;
        try {
            obj = objectType.createObject().call();
        } catch (Exception e) {
            PLUGIN_LOGGER.warning("Error creating block marker object of type \"" + typeID + "\". Error: " + e.getMessage());
            return null;
        }

        obj.init(PLUGIN, location, id, objectType, BasicConfigurationNode.root(), UUID.randomUUID(), this);
        putObject(typeID, id, obj);
        appendObjectNode(obj);
        return obj;
    }

    private void appendObjectNode(IBlockRegestryObject obj) {
        Chunk chunk = obj.getLocation().getChunk();
        ConfigurationNode config = loadOrCreateChunkConfig(chunk);
        try {
            writeObjectNode(config.node("objects").appendListNode(), obj);
        } catch (SerializationException e) {
            PLUGIN_LOGGER.warning("Failed to write new marker entry to chunk PDC. Error " + e.getMessage());
            return;
        }
        saveChunkConfig(chunk, config);
    }

    private ConfigurationNode loadOrCreateChunkConfig(Chunk chunk) {
        String jsonString = chunk.getPersistentDataContainer().get(NAMESPACED_KEY, PersistentDataType.STRING);
        if (jsonString == null) {
            return BasicConfigurationNode.root();
        }
        ConfigurationNode config = loadChunkConfig(jsonString);
        return config != null ? config : BasicConfigurationNode.root();
    }

    private void putObject(String type, String id, IBlockRegestryObject obj) {
        registeredObjects.computeIfAbsent(type, k -> new HashMap<>()).put(id, obj);
    }

    private void removeObject(String type, String id) {
        Map<String, IBlockRegestryObject> objectsOfType = registeredObjects.get(type);
        if (objectsOfType != null) {
            HandlerList.unregisterAll(objectsOfType.get(id));
            objectsOfType.remove(id);
        }
    }
}
