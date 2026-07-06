package me.simoncrafter.mCCodeCampLibrary.internal;

import me.simoncrafter.mCCodeCampLibrary.input.button.ButtonHandler;
import me.simoncrafter.mCCodeCampLibrary.utility.InitHelper;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class Listeners implements Listener {

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        InitHelper.getBlockMarkerRegistry().onChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        InitHelper.getBlockMarkerRegistry().onChunkUnload(event.getChunk());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || !block.getType().name().endsWith("_BUTTON")) return;

        InitHelper.getBlockMarkerRegistry()
                .getMarker(block, ButtonHandler.MARKER_TYPE)
                .ifPresent(id -> InitHelper.getButtonHandler().press(id, event.getPlayer()));
    }

}
