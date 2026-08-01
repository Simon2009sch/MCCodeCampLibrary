package me.simoncrafter.mCCodeCampLibrary.obstical;

import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AOpenableObject implements IOpenable, IBlockRegestryObject {

    public static class OpenableRegion {
        private final Vector min;
        private final Vector max;

        public OpenableRegion(Vector start, Vector end) {
            this.min = new Vector(
                    Math.min(start.getX(), end.getX()),
                    Math.min(start.getY(), end.getY()),
                    Math.min(start.getZ(), end.getZ())
            );
            this.max = new Vector(
                    Math.max(start.getX(), end.getX()),
                    Math.max(start.getY(), end.getY()),
                    Math.max(start.getZ(), end.getZ())
            );
        }

        public OpenableRegion(ConfigurationNode configNode) {
            this.min = readVector(configNode.node("min"), 0, 0, 0);
            this.max = readVector(configNode.node("max"), 0, 0, 0);
        }

        public Vector getMin() {
            return min.clone();
        }

        public Vector getMax() {
            return max.clone();
        }

        public boolean contains(Vector vector) {
            return vector.getX() >= min.getX() && vector.getX() <= max.getX()
                    && vector.getY() >= min.getY() && vector.getY() <= max.getY()
                    && vector.getZ() >= min.getZ() && vector.getZ() <= max.getZ();
        }

        public void doForEveryBlock(Consumer<Vector> consumer) {
            int minX = min.getBlockX(), minY = min.getBlockY(), minZ = min.getBlockZ();
            int maxX = max.getBlockX(), maxY = max.getBlockY(), maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        consumer.accept(new Vector(x, y, z));
                    }
                }
            }
        }

        public List<Vector> getBlockVectorList() {
            List<Vector> result = new ArrayList<>();
            doForEveryBlock(result::add);
            return result;
        }

        public int getBlockCount() {
            int sizeX = max.getBlockX() - min.getBlockX() + 1;
            int sizeY = max.getBlockY() - min.getBlockY() + 1;
            int sizeZ = max.getBlockZ() - min.getBlockZ() + 1;
            return sizeX * sizeY * sizeZ;
        }

        public ConfigurationNode getConfig() throws SerializationException {
            ConfigurationNode node = BasicConfigurationNode.root();
            writeVector(node.node("min"), min);
            writeVector(node.node("max"), max);
            return node;
        }
    }

    /**
     * Reads an x/y/z triple from a ConfigurationNode into a Vector. Shared by this
     * class and its subclasses so vector fields aren't hand-rolled node-by-node.
     */
    protected static Vector readVector(ConfigurationNode node, double defaultX, double defaultY, double defaultZ) {
        return new Vector(
                node.node("x").getDouble(defaultX),
                node.node("y").getDouble(defaultY),
                node.node("z").getDouble(defaultZ)
        );
    }

    protected static void writeVector(ConfigurationNode node, Vector vector) throws SerializationException {
        node.node("x").set(vector.getX());
        node.node("y").set(vector.getY());
        node.node("z").set(vector.getZ());
    }

    protected static ConfigurationNode writeVector(Vector vector) throws SerializationException {
        ConfigurationNode node = BasicConfigurationNode.root();
        writeVector(node, vector);
        return node;
    }

    private OpenableState state;
    private Set<OpenableRegion> blockRegions;

    private Plugin plugin;
    private Location location;
    private String ID;
    private RegistryObjectType objectType;
    private ConfigurationNode config;
    private BlockMarkerRegistry registry;
    private UUID uuid;

    @Override
    public void init(Plugin plugin, Location loc, String ID, RegistryObjectType objectType, ConfigurationNode config, UUID uuid, BlockMarkerRegistry registryInstance) {
        this.plugin = plugin;
        location = loc;
        this.ID = ID;
        this.objectType = objectType;
        this.config = config;
        this.uuid = uuid;
        this.registry = registryInstance;

        if (config.hasChild("blocks")) {
            parseBlocks();
        } else {
            blockRegions = new HashSet<>();
        }

        readConfig();
    }

    protected abstract void readConfig();

    private void parseBlocks() {
        blockRegions.clear();
        for (ConfigurationNode n : config.node("blocks").childrenList()) {
            OpenableRegion reg = new OpenableRegion(n);
            if (reg.getBlockCount() <= 3000) {
                blockRegions.add(reg);
            }
        }
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public ConfigurationNode getConfig() {
        return config;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
        registry.saveObject(this);
    }

    @Override
    public String getTypeID() {
        return objectType.typeID();
    }

    protected void setState(OpenableState state) {
        this.state = state;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isOpen() {
        return state == OpenableState.OPENED || state == OpenableState.TRANSITION;
    }

    public OpenableState getState() {
        return state;
    }

    public Set<Vector> getBlocks() {
        return getAllBlocks();
    }

    public Set<OpenableRegion> getBlockRegions() {
        return new HashSet<>(blockRegions);
    }

    public void setBlockRegions(Set<OpenableRegion> blocks) {
        this.blockRegions = blocks;
        saveBlocks();
    }

    public void setBlocks(Set<Vector> blockRegions) {
        this.blockRegions = groupBlockRegions(blockRegions);
        saveBlocks();
    }

    public void addBlock(Vector block) {
        Set<Vector> blocks = getAllBlocks();
        blocks.remove(block);
        this.blockRegions = groupBlockRegions(blocks);
        saveBlocks();
    }

    public void removeBlock(Vector block) {
        Set<Vector> blocks = getAllBlocks();
        blocks.add(block);
        this.blockRegions = groupBlockRegions(blocks);
        saveBlocks();
    }

    protected void saveBlocks() {
        config.removeChild("blocks");
        ConfigurationNode blocksNode = config.node("blocks");

        for (OpenableRegion region : blockRegions) {
            try {
                blocksNode.appendListNode().from(region.getConfig());
            } catch (SerializationException e) {
                plugin.getLogger().warning("Error while passing block area of a openable Object (e.g door like) into the config!");
            }
        }
        registry.saveObject(this);
    }

    protected Set<Vector> getAllBlocks() {
        Set<Vector> blocks = new HashSet<>();
        for (OpenableRegion region : this.blockRegions) {
            blocks.addAll(region.getBlockVectorList());
        }
        return blocks;
    }

    private Set<OpenableRegion> groupBlockRegions(Set<Vector> blocks) {
        if (blocks == null || blocks.isEmpty()) return new HashSet<>();
        Vector min = null;
        Vector max = null;
        for (Vector b : blocks) {
            if (min == null) {
                min = b.clone();
                max = b.clone();
                continue;
            }
            min = new Vector(
                    Math.min(min.getBlockX(), b.getBlockX()),
                    Math.min(min.getBlockY(), b.getBlockY()),
                    Math.min(min.getBlockZ(), b.getBlockZ())
            );
            max = new Vector(
                    Math.max(max.getBlockX(), b.getBlockX()),
                    Math.max(max.getBlockY(), b.getBlockY()),
                    Math.max(max.getBlockZ(), b.getBlockZ())
            );
        }
        int sizeX = max.getBlockX() - min.getBlockX() + 1;
        int sizeY = max.getBlockY() - min.getBlockY() + 1;
        int sizeZ = max.getBlockZ() - min.getBlockZ() + 1;

        long[] grid = new long[((sizeX * sizeY * sizeZ) + 63) >>> 6];
        for (Vector b : blocks) {
            int idx = blockIndex(b, min, sizeX, sizeY);
            grid[idx >>> 6] |= 1L << idx;
        }

        long[] visited = new long[grid.length];
        Set<OpenableRegion> regions = new HashSet<>();

        for (int z = 0; z < sizeZ; z++) {
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    int idx = flatIndex(x, y, z, sizeX, sizeY);
                    if (!isSet(grid, idx) || isSet(visited, idx)) {
                        continue;
                    }

                    // Grow along X: keep going right while the next cell is occupied and unvisited
                    int endX = x;
                    while (endX + 1 < sizeX) {
                        int nextIdx = flatIndex(endX + 1, y, z, sizeX, sizeY);
                        if (!isSet(grid, nextIdx) || isSet(visited, nextIdx)) break;
                        endX++;
                    }

                    // TODO (your turn): grow along Y the same way growX works above, but instead
                    // of testing a single cell, you must test the WHOLE x-run (x..endX) in the
                    // next y-row. Only advance endY if every cell in that row is occupied and
                    // unvisited - one failing cell in the row means the row can't be added.
                    int endY = y;

                    growY:
                    while (endY + 1 < sizeY) {
                        for (int xi = x; xi <= endX; xi++) {
                            int layerIdx = flatIndex(xi, endY + 1, z, sizeX, sizeY);
                            if (!isSet(grid, layerIdx) || isSet(visited, layerIdx)) break growY;
                        }
                        endY++;
                    }

                    // Grow along Z: keep going up a layer while every cell in the
                    // (x..endX, y..endY) rectangle on the next z-layer is occupied and unvisited
                    int endZ = z;
                    growZ:
                    while (endZ + 1 < sizeZ) {
                        for (int yi = y; yi <= endY; yi++) {
                            for (int xi = x; xi <= endX; xi++) {
                                int layerIdx = flatIndex(xi, yi, endZ + 1, sizeX, sizeY);
                                if (!isSet(grid, layerIdx) || isSet(visited, layerIdx)) break growZ;
                            }
                        }
                        endZ++;
                    }

                    // Mark every cell in the box we just found as visited so later seeds skip it
                    for (int zi = z; zi <= endZ; zi++) {
                        for (int yi = y; yi <= endY; yi++) {
                            for (int xi = x; xi <= endX; xi++) {
                                int boxIdx = flatIndex(xi, yi, zi, sizeX, sizeY);
                                visited[boxIdx >>> 6] |= 1L << boxIdx;
                            }
                        }
                    }

                    Vector start = min.clone().add(new Vector(x, y, z));
                    Vector end = min.clone().add(new Vector(endX, endY, endZ));
                    regions.add(new OpenableRegion(start, end));
                }
            }
        }

        return regions;
    }

    private int flatIndex(int x, int y, int z, int sizeX, int sizeY) {
        return (z * sizeY + y) * sizeX + x;
    }

    private int blockIndex(Vector block, Vector min, int sizeX, int sizeY) {
        int x = block.getBlockX() - min.getBlockX();
        int y = block.getBlockY() - min.getBlockY();
        int z = block.getBlockZ() - min.getBlockZ();
        return flatIndex(x, y, z, sizeX, sizeY);
    }

    private boolean isSet(long[] grid, int idx) {
        return (grid[idx >>> 6] & (1L << idx)) != 0;
    }

}
