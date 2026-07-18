package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.mCCodeCampLibrary.input.activation.blockActivation.PressurePlateDeactivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.RegionTriggerEnterIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.RegionTriggerLeaveIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.playerBlockActivation.PressurePlateIDEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.ItemMeta;

public class EditorListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        String itemType = MCCodeCampLib.getItemsManager().getCustomItemType(event.getItem());
        if (!(itemType != null && itemType.startsWith("editor_"))) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        switch (itemType) {
            case "editor_toggleVisibility": {
                LocationEditor.togglePlayer(event.getPlayer());

                // item state feedback
                boolean newState = LocationEditor.isVisibleForPlayer(event.getPlayer());
                ItemMeta meta = event.getItem().getItemMeta();
                meta.setEnchantmentGlintOverride(newState);
                event.getItem().setItemMeta(meta);

                //sound feedback
                float pitch = newState ? 1 : 0.5f;
                event.getPlayer().playSound(event.getPlayer(), Sound.ENTITY_ARROW_HIT_PLAYER, 1, pitch);

                // message
                Component message = newState ? Component.text("Enabled visuals!", NamedTextColor.GREEN) : Component.text("Disabled visuals!", NamedTextColor.RED);
                event.getPlayer().sendMessage(message);
                break;
            }
            case "editor_createNew": {
                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) LocationEditor.showAddDialogToPlayer(event.getPlayer());
                break;
            }
            case "editor_remove": {

                if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (event.getPlayer().isSneaking()) MCCodeCampLib.getBlockMarkerRegistry().removeMarker(event.getClickedBlock());
                    LocationEditor.showRemoveDialogToPlayer(event.getPlayer(), event.getClickedBlock());
                }
                break;
            }
        }
    }

    @EventHandler
    public void onBlockRegistryUpdate(BlockRegistryUpdateEvent event) {
        LocationEditor.updateFromBlockMarkerCache();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        LocationEditor.clearPlayerState(event.getPlayer());
    }

    @EventHandler
    public void onButtonActivation(ButtonIDEvent event) {
        event.getPlayer().sendMessage(Component.text(event.getId() + " " + event.getBlock().getType()));
    }

    @EventHandler
    public void onPressurePlateActivation(PressurePlateIDEvent event) {
        event.getPlayer().sendMessage(Component.text(event.getId() + " " + event.getBlock().getType()));
    }

    @EventHandler
    public void onPressurePlateDeactivation(PressurePlateDeactivationEvent event) {
        Bukkit.broadcast(Component.text("Pressure plate deactivated! " + event.getId()));
    }

    @EventHandler
    public void onRegionTriggerEnterActivation(RegionTriggerEnterIDEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.sendMessage(Component.text("You entered " + event.getId()));
        }
    }

    @EventHandler
    public void onRegionTriggerLeaveActivation(RegionTriggerLeaveIDEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.sendMessage(Component.text("You left " + event.getId()));
        }
    }
}
