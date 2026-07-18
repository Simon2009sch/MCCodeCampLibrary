package me.simoncrafter.mCCodeCampLibrary.internal;

import me.simoncrafter.CraftersDisplayLibrary.persistence.DisplayPersistence;
import me.simoncrafter.mCCodeCampLibrary.input.activation.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.List;

public class Listeners implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        MCCodeCampLib.getBlockMarkerRegistry().onChunkLoad(event.getChunk());
        List<Long> timestamps = DisplayPersistence.listIterationTimestamps(event.getChunk());
        for (long l : timestamps) {
            if (l < MCCodeCampLib.getCurrentDisplayPluginIteration()) {
                DisplayPersistence.removeIteration(event.getChunk(), l);
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        MCCodeCampLib.getBlockMarkerRegistry().onChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

    }

    @EventHandler
    public void onButtonID(ButtonIDEvent event) {
        if (event.getId().equals("test_door")) {
            MCCodeCampLib.toggleDoor();
        }
    }

}
