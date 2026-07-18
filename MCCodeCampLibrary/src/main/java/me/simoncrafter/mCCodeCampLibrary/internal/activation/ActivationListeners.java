package me.simoncrafter.mCCodeCampLibrary.internal.activation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.blockActivation.PressurePlateDeactivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation.EntityKillIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation.EntityLeftClickIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation.EntityRightClickIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.playerBlockActivation.PressurePlateIDEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

public class ActivationListeners implements Listener {

    private static final String NPC_MARKER_TYPE = "npc";

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;


        if (event.getHand() != EquipmentSlot.HAND && event.getAction() != Action.PHYSICAL) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK ) {
            event.setCancelled(activateButton(block, event.getPlayer()));
        } else if (event.getAction() == Action.PHYSICAL) {
            activatePressurePlate(block, event.getPlayer());
        }
    }

    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event) {
        if (event.getOldCurrent() > 0 && event.getNewCurrent() == 0) {
            deactivatePressurePlate(event.getBlock());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // PlayerInteractAtEntityEvent extends this and fires alongside it for the same
        // click (e.g. armor stands) — only handle the base event to avoid double firing.
        if (event.getClass() != PlayerInteractEntityEvent.class) return;
        activateEntityRightClick(event.getRightClicked(), event.getPlayer());
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        activateEntityLeftClick(event.getEntity(), event.getDamager());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity target = event.getEntity();
        EntityDamageEvent lastDamage = target.getLastDamageCause();
        if (!(lastDamage instanceof EntityDamageByEntityEvent damageByEntity)) return;
        activateEntityKill(target, damageByEntity.getDamager());
    }

    private boolean activateButton(Block block, Player player) {
        Optional<String> returnedMarker = MCCodeCampLib.getBlockMarkerRegistry().getMarker(block, "button");
        if (returnedMarker.isEmpty()) {
            return false;
        }

        ButtonIDEvent callEvent = new ButtonIDEvent(player, returnedMarker.get(), block);

        if (Tag.BUTTONS.isTagged(block.getType())) {
            Powerable button = (Powerable) block.getBlockData();
            if (!button.isPowered()) callEvent.callEvent();
            return false;
        }
        callEvent.callEvent();
        return true;
    }

    private void activatePressurePlate(Block block, Player player) {
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) return;

        Optional<String> returnedMarker = MCCodeCampLib.getBlockMarkerRegistry().getMarker(block, "plate");
        if (returnedMarker.isEmpty()) return;

        Powerable plate = (Powerable) block.getBlockData();
        if (plate.isPowered()) return;

        new PressurePlateIDEvent(player, returnedMarker.get(), block).callEvent();
    }

    private void deactivatePressurePlate(Block block) {
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) return;

        Optional<String> returnedMarker = MCCodeCampLib.getBlockMarkerRegistry().getMarker(block, "plate");
        if (returnedMarker.isEmpty()) return;

        new PressurePlateDeactivationEvent(returnedMarker.get(), block).callEvent();
    }

    private void activateEntityRightClick(Entity target, Entity entity) {
        MCCodeCampLib.getEntityMarkerRegistry().getMarker(target, NPC_MARKER_TYPE)
                .ifPresent(id -> new EntityRightClickIDEvent(entity, id, target).callEvent());
    }

    private void activateEntityLeftClick(Entity target, Entity entity) {
        MCCodeCampLib.getEntityMarkerRegistry().getMarker(target, NPC_MARKER_TYPE)
                .ifPresent(id -> new EntityLeftClickIDEvent(entity, id, target).callEvent());
    }

    private void activateEntityKill(Entity target, Entity entity) {
        MCCodeCampLib.getEntityMarkerRegistry().getMarker(target, NPC_MARKER_TYPE)
                .ifPresent(id -> new EntityKillIDEvent(entity, id, target).callEvent());
    }

}
