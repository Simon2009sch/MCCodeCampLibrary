package me.simoncrafter.mCCodeCampLibrary.internal.activation;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.RegionTriggerEnterActivationEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.RegionTriggerLeaveActivationEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RegionActivationHandler implements Listener {

    private static final String TRIGGER_PREFIX = "trigger_";

    private final Map<UUID, Set<String>> playerTriggerRegions = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // PlayerMoveEvent also fires for sub-block movement (e.g. just turning the
        // camera) — skip the WorldGuard query entirely unless the block actually changed.
        if (sameBlock(event.getFrom(), event.getTo())) return;

        Player player = event.getPlayer();
        Set<String> currentRegions = getTriggerRegions(event.getTo());
        Set<String> previousRegions = playerTriggerRegions.getOrDefault(player.getUniqueId(), Collections.emptySet());

        for (String regionId : currentRegions) {
            if (!previousRegions.contains(regionId)) {
                new RegionTriggerEnterActivationEvent(player, regionId.substring(TRIGGER_PREFIX.length())).callEvent();
            }
        }
        for (String regionId : previousRegions) {
            if (!currentRegions.contains(regionId)) {
                new RegionTriggerLeaveActivationEvent(player, regionId.substring(TRIGGER_PREFIX.length())).callEvent();
            }
        }

        if (currentRegions.isEmpty()) {
            playerTriggerRegions.remove(player.getUniqueId());
        } else {
            playerTriggerRegions.put(player.getUniqueId(), currentRegions);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerTriggerRegions.remove(event.getPlayer().getUniqueId());
    }

    private Set<String> getTriggerRegions(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        Set<ProtectedRegion> regions = query.getApplicableRegions(BukkitAdapter.adapt(location)).getRegions();

        Set<String> triggerRegions = new HashSet<>();
        for (ProtectedRegion region : regions) {
            if (region.getId().startsWith(TRIGGER_PREFIX)) {
                triggerRegions.add(region.getId());
            }
        }
        return triggerRegions;
    }

    private boolean sameBlock(Location from, Location to) {
        return from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld().equals(to.getWorld());
    }

}
