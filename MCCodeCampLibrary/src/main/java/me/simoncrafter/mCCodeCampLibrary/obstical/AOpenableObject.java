package me.simoncrafter.mCCodeCampLibrary.obstical;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public abstract class AOpenableObject implements IOpenable {
    protected static Plugin plugin;

    protected OpenableState state;
    protected Set<Vector> blocks;
    protected Location origen;
    protected final String ID;

    public AOpenableObject(String ID, Location origen, Set<Vector> blocks) {
        this.ID = ID;
        this.origen = origen;
        this.blocks = blocks;
        this.state = OpenableState.CLOSED;
    }

    public boolean isOpen() {
        return state == OpenableState.OPENED || state == OpenableState.TRANSITION;
    }

    public OpenableState getState() {
        return state;
    }

    public Set<Vector> getBlocks() {
        return new HashSet<>(blocks);
    }

    public void setBlocks(Set<Vector> blocks) {
        this.blocks = blocks;
    }

    public void addBlock(Vector block) {
        this.blocks.add(block);
    }

    public void removeBlock(Vector block) {
        this.blocks.remove(block);
    }

    public Location getOrigen() {
        return origen;
    }

    public void setOrigen(Location origen) {
        this.origen = origen;
    }

    public static void setPlugin(Plugin plugin) {
        AOpenableObject.plugin = plugin;
    }
}
