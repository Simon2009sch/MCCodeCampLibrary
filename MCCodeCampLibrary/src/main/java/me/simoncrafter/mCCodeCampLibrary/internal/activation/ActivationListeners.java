package me.simoncrafter.mCCodeCampLibrary.internal.activation;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.simoncrafter.mCCodeCampLibrary.input.activation.blockActivation.PressurePlateDeactivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation.EntityKillActivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation.EntityLeftClickActivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation.EntityRightClickActivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityBlockActivation.ButtonActivationEventEntity;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityBlockActivation.PressurePlateActivationEventEntity;
import me.simoncrafter.mCCodeCampLibrary.utility.InitHelper;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

public class ActivationListeners implements Listener {

    private static final String NPC_MARKER_TYPE = "npc";

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(activateButton(block, event.getPlayer()));
        } else if (event.getAction() == Action.PHYSICAL) {
            activatePressurePlate(block, event.getPlayer());
        }
    }

    @EventHandler
    public void onEntityInteract(EntityInteractEvent event) {
        activatePressurePlate(event.getBlock(), event.getEntity());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        if (block == null) return;
        activateButton(block, event.getEntity());
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        RegionContainer container = WorldGuard.getInstance().getPlatform()
                .getRegionContainer();
        RegionQuery query = container.createQuery();
        query.getApplicableRegions(BukkitAdapter.adapt(event.getTo()));

    }

    private boolean activateButton(Block block, Entity entity) {
        Optional<String> returnedMarker = InitHelper.getBlockMarkerRegistry().getMarker(block, "button");
        if (returnedMarker.isEmpty()) {
            return false;
        }

        ButtonActivationEventEntity callEvent = new ButtonActivationEventEntity(entity, returnedMarker.get(), block);

        if (Tag.BUTTONS.isTagged(block.getType())) {
            Powerable button = (Powerable) block.getBlockData();
            if (!button.isPowered()) callEvent.callEvent();
            return false;
        }
        callEvent.callEvent();
        return true;
    }

    private void activatePressurePlate(Block block, Entity entity) {
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) return;

        Optional<String> returnedMarker = InitHelper.getBlockMarkerRegistry().getMarker(block, "pressureplate");
        if (returnedMarker.isEmpty()) return;

        Powerable plate = (Powerable) block.getBlockData();
        if (plate.isPowered()) return;

        new PressurePlateActivationEventEntity(entity, returnedMarker.get(), block).callEvent();
    }

    private void deactivatePressurePlate(Block block) {
        if (!Tag.PRESSURE_PLATES.isTagged(block.getType())) return;

        Optional<String> returnedMarker = InitHelper.getBlockMarkerRegistry().getMarker(block, "pressureplate");
        if (returnedMarker.isEmpty()) return;

        new PressurePlateDeactivationEvent(returnedMarker.get(), block).callEvent();
    }

    private void activateEntityRightClick(Entity target, Entity entity) {
        InitHelper.getEntityMarkerRegistry().getMarker(target, NPC_MARKER_TYPE)
                .ifPresent(id -> new EntityRightClickActivationEvent(entity, id, target).callEvent());
    }

    private void activateEntityLeftClick(Entity target, Entity entity) {
        InitHelper.getEntityMarkerRegistry().getMarker(target, NPC_MARKER_TYPE)
                .ifPresent(id -> new EntityLeftClickActivationEvent(entity, id, target).callEvent());
    }

    private void activateEntityKill(Entity target, Entity entity) {
        InitHelper.getEntityMarkerRegistry().getMarker(target, NPC_MARKER_TYPE)
                .ifPresent(id -> new EntityKillActivationEvent(entity, id, target).callEvent());
    }

}
