package me.simoncrafter.mCCodeCampLibrary.internal.activation;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityBlockActivation.ButtonActivationEventEntity;
import me.simoncrafter.mCCodeCampLibrary.utility.InitHelper;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class ActivationListeners implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        event.setCancelled(activateButton(block, event.getPlayer()));
    }

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        Block block = event.getBlock();
        activateButton(block, null);
    }

    private boolean activateButton(Block block, Entity entity) {
        Optional<String> returnedMarker = InitHelper.getBlockMarkerRegistry().getMarker(block, "button");
        if (returnedMarker.isEmpty()) {
            return false;
        }

        ButtonActivationEventEntity callEvent = new ButtonActivationEventEntity(entity, returnedMarker.get(), block);

        if (Tag.BUTTONS.isTagged(block.getType())) {
            Powerable button = (Powerable) block;
            if (!button.isPowered()) callEvent.callEvent();
            return false;
        }
        callEvent.callEvent();
        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        RegionContainer container = WorldGuard.getInstance().getPlatform()
                .getRegionContainer();
        RegionQuery query = container.createQuery();
        query.getApplicableRegions(BukkitAdapter.adapt(event.getTo()));

    }

}
