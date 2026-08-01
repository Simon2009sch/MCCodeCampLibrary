package me.simoncrafter.mCCodeCampLibrary.obstical.Door;

import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.OpenableState;
import me.simoncrafter.mCCodeCampLibrary.obstical.events.OpenableObjectStateChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class SimpleDoor extends AOpenableObject {

    private BlockData openBlockData;
    private BlockData closedBlockData;

    @Override
    protected void readConfig() {
        ConfigurationNode config = getConfig();
        openBlockData = readBlockData(config, "openBlockdata");
        closedBlockData = readBlockData(config, "closedBlockdata");
    }

    @Override
    public void open(boolean skipTransition) {
        if (getState() != OpenableState.CLOSED && getState() != null) {
            return;
        }
        applyBlockData(openBlockData);
        setState(OpenableState.OPENED);
        new OpenableObjectStateChangeEvent(getID(), OpenableState.CLOSED, OpenableState.OPENED).callEvent();
    }

    @Override
    public void close(boolean skipTransition) {
        if (getState() != OpenableState.OPENED && getState() != null) {
            return;
        }
        applyBlockData(closedBlockData);
        setState(OpenableState.CLOSED);
        new OpenableObjectStateChangeEvent(getID(), OpenableState.OPENED, OpenableState.CLOSED).callEvent();
    }

    private void applyBlockData(BlockData blockData) {
        if (blockData == null) return;
        for (Vector v : getBlocks()) {
            getLocation().clone().add(v).getBlock().setBlockData(blockData);
        }
    }

    public BlockData getOpenBlockData() {
        return openBlockData;
    }

    public void setOpenBlockData(BlockData openBlockData) {
        this.openBlockData = openBlockData;
        try {
            getConfig().node("openBlockdata").set(openBlockData.getAsString());
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("Failed to serialize open block data while saving!");
        }
    }

    public BlockData getClosedBlockData() {
        return closedBlockData;
    }

    public void setClosedBlockData(BlockData closedBlockData) {
        this.closedBlockData = closedBlockData;
        try {
            getConfig().node("closedBlockdata").set(closedBlockData.getAsString());
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("Failed to serialize closed block data while saving!");
        }
    }

    /**
     * @param config The config node the door's blockdata was read from
     * @param key The path at which the block data should be read from. (Relative to config)
     * @return {@link BlockData} that was read. If the string value at the path was invalid or doesn't exist returns null
     */
    private BlockData readBlockData(ConfigurationNode config, String key) {
        if (config.hasChild(key)) {
            try {
                return Bukkit.createBlockData(config.node(key).getString(""));
            } catch (IllegalArgumentException e) {
                getPlugin().getLogger().warning("Invalid blockdata of door with ID: \"" + getID() + "\". Blockdata: \"" + config.node(key).getString("") + "\"");
            }
        }
        return null;
    }
}
