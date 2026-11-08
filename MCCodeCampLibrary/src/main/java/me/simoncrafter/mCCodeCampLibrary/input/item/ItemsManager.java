package me.simoncrafter.mCCodeCampLibrary.input.item;

import io.papermc.paper.persistence.PersistentDataContainerView;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class ItemsManager implements Listener, AutoCloseable {

    private Plugin PLUGIN;
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("me.simoncrafter.mccodecamplibrary", "custom_items_item_key");
    private Map<String, ACustomItemHandler> itemHandlers = new HashMap<>();
    private record EntityClickKey(java.util.UUID player, java.util.UUID target,
                                  org.bukkit.inventory.EquipmentSlot hand) { }
    private record EntityClick(boolean precise, ACustomItemHandler handler, boolean cancelled) { }
    private final Map<EntityClickKey, EntityClick> entityClicks = new HashMap<>();
    private int entityClickTick = Integer.MIN_VALUE;
    private boolean closed;

    /** Removes by ID. Prefer the identity overload for lifecycle cleanup. */
    public boolean unregisterCustomItem(String id) {
        ACustomItemHandler removed = itemHandlers.remove(id);
        if (removed == null) return false;
        entityClicks.values().removeIf(click -> click.handler() == removed);
        return true;
    }

    /** A stale owner cannot unregister another handler subsequently registered under the same ID. */
    public boolean unregisterCustomItem(ACustomItemHandler item) {
        if (item == null || itemHandlers.get(item.getItemID()) != item) return false;
        return unregisterCustomItem(item.getItemID());
    }

    /** Permanently closes this manager; owners must call this on disable/replacement. */
    @Override public void close() {
        if (closed) return;
        closed = true;
        org.bukkit.event.HandlerList.unregisterAll(this);
        itemHandlers.clear();
        entityClicks.clear();
    }

    public ItemsManager(Plugin PLUGIN) {
        this.PLUGIN = PLUGIN;
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    public void registerCustomItem(ACustomItemHandler item) {
        if (closed) throw new IllegalStateException("ItemsManager is closed");
        if (itemHandlers.containsKey(item.getItemID())) {
            return;
        }
        itemHandlers.put(item.getItemID(), item);
    }

    public static ConfigurationNode readItemConfig(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();
        if (!pdc.has(ITEM_KEY, PersistentDataType.STRING)) {
            return null;
        }
        return convertToConfigNode(pdc.get(ITEM_KEY, PersistentDataType.STRING));
    }

    public static @Nullable ConfigurationNode convertToConfigNode(String jsonString) {
        if (jsonString == null || jsonString.isBlank()) {
            return null;
        }
        try {
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(jsonString)))
                    .build();
            ConfigurationNode node = loader.load();
            return node.isMap() ? node : null;
        } catch (ConfigurateException | IllegalArgumentException e) {
            // Item tags are untrusted persisted data; malformed tags behave like ordinary items.
            return null;
        }
    }
    public static String serializeJson(ConfigurationNode config) {
        try {
            StringWriter writer = new StringWriter();
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .sink(() -> new BufferedWriter(writer))
                    .build();
            loader.save(config);
            return writer.toString();
        } catch (ConfigurateException e) {
            MCCodeCampLib.getPluginLogger().warning("Failed to save marker configuration to chunk PDC. Error" + e.getMessage());
        }
        return "";
    }

    private ACustomItemHandler getItemHandler(ItemStack itemStack) {
        ConfigurationNode node = readItemConfig(itemStack);
        if (node == null) {
            return null;
        }
        Object rawId = node.node("id").raw();
        if (!(rawId instanceof String id) || id.isEmpty()) {
            return null;
        }
        return itemHandlers.get(id);
    }


    public ItemStack createNewItem(String itemID) {
        ACustomItemHandler handler = itemHandlers.get(itemID);
        if (handler == null) return null;
        ItemStack item;
        try {
            item = handler.createNewItem();
        } catch (SerializationException e) {
            MCCodeCampLib.getPluginLogger().warning("Failed to create new item. Error" + e.getMessage());
            item = null;
        }
        return item;
    }

    public ItemStack createNewItem(ACustomItemHandler itemHandler) {
        return createNewItem(itemHandler.getItemID());
    }

    @EventHandler
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        routeEntityInteract(event);
    }

    // The precise subclass has its own HandlerList (not inherited listener dispatch).
    @EventHandler
    public void onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        routeEntityInteract(event);
    }

    private void routeEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        ACustomItemHandler handler = getItemHandler(event.getPlayer().getInventory().getItem(event.getHand()));
        if (handler == null) return;
        int tick = Bukkit.getCurrentTick();
        if (tick != entityClickTick) {
            entityClicks.clear();
            entityClickTick = tick;
        }
        var key = new EntityClickKey(event.getPlayer().getUniqueId(), event.getRightClicked().getUniqueId(), event.getHand());
        boolean precise = event instanceof org.bukkit.event.player.PlayerInteractAtEntityEvent;
        EntityClick previous = entityClicks.get(key);
        // Paper exposes no packet/gesture ID: pair opposite packet forms within one tick.
        if (previous != null && previous.precise() != precise && previous.handler() == handler) {
            entityClicks.remove(key);
            event.setCancelled(previous.cancelled());
            return;
        }
        event.setCancelled(true);
        handler.onEntityInteract(event);
        if (!closed && itemHandlers.get(handler.getItemID()) == handler) {
            entityClicks.put(key, new EntityClick(precise, handler, event.isCancelled()));
        }
    }

    @EventHandler
    public void onPlayerAttackEntity(io.papermc.paper.event.player.PrePlayerAttackEntityEvent event) {
        ACustomItemHandler handler = getItemHandler(event.getPlayer().getInventory().getItemInMainHand());
        if (handler == null) return;
        event.setCancelled(true);
        handler.onEntityAttack(event);
    }

    @EventHandler
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        if (getItemHandler(event.getPlayer().getInventory().getItemInMainHand()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.PHYSICAL) return;
        ItemStack item = event.getItem();
        ACustomItemHandler handler = getItemHandler(item);
        if (handler == null) {
            return;
        }
        event.setCancelled(true);
        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK, RIGHT_CLICK_BLOCK -> handler.onBlockInteract(event);
            case LEFT_CLICK_AIR, RIGHT_CLICK_AIR -> handler.onInteract(event);
            default -> { }
        }
    }
    
}
