package me.simoncrafter.mCCodeCampLibrary.internal;

import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class Listeners implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        MCCodeCampLib.getBlockMarkerRegistry().onChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        MCCodeCampLib.getBlockMarkerRegistry().onChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

    }

}
