package me.simoncrafter.mCCodeCampLibrary.obstical.Door;

import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.OpenableState;
import me.simoncrafter.mCCodeCampLibrary.obstical.events.OpenableObjectStateChangeEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.util.Vector;

import java.util.Set;

public class SimpleDoor extends AOpenableObject {

    private Material openMaterial;
    private Material closedMaterial;

    public SimpleDoor(String ID, Location origen, Set<Vector> blocks, Material closedMaterial, Material openMaterial) {
        super(ID, origen, blocks);
        this.closedMaterial = closedMaterial;
        this.openMaterial = openMaterial;
    }

    @Override
    public void open(boolean skipTransition) {
        if (state != OpenableState.CLOSED && state != null) {
            return;
        }
        setBlocks(openMaterial);
        state = OpenableState.OPENED;
        new OpenableObjectStateChangeEvent(ID, OpenableState.CLOSED, OpenableState.OPENED).callEvent();
    }

    @Override
    public void close(boolean skipTransition) {
        if (state != OpenableState.OPENED && state != null) {
            return;
        }
        setBlocks(closedMaterial);
        state = OpenableState.CLOSED;
        new OpenableObjectStateChangeEvent(ID, OpenableState.OPENED, OpenableState.CLOSED).callEvent();
    }

    private void setBlocks(Material material) {
        for (Vector v : blocks) {
            origen.clone().add(v).getBlock().setType(material);
        }
    }
}
