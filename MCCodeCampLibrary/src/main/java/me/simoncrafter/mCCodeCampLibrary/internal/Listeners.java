package me.simoncrafter.mCCodeCampLibrary.internal;

import com.destroystokyo.paper.MaterialTags;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityBlockActivation.ButtonActivationEventEntity;
import me.simoncrafter.mCCodeCampLibrary.utility.InitHelper;
import org.bukkit.Material;
import org.bukkit.MinecraftExperimental;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Optional;

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
    public void onBlockBreak(BlockBreakEvent event) {

    }

}
