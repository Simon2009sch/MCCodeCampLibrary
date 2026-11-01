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

public class ItemsManager implements Listener {

    private Plugin PLUGIN;
    public static final NamespacedKey ITEM_KEY = new NamespacedKey("me.simoncrafter.mccodecamplibrary", "custom_items_item_key");
    private Map<String, ACustomItemHandler> itemHandlers = new HashMap<>();

    public ItemsManager(Plugin PLUGIN) {
        this.PLUGIN = PLUGIN;
        Bukkit.getPluginManager().registerEvents(this, PLUGIN);
    }

    public void registerCustomItem(ACustomItemHandler item) {
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
        if (!pdc.has(ITEM_KEY)) {
            return null;
        }
        return convertToConfigNode(pdc.get(ITEM_KEY, PersistentDataType.STRING));
    }

    public static @Nullable ConfigurationNode convertToConfigNode(String jsonString) {
        if (jsonString == null || jsonString.isBlank() || jsonString.isBlank()) {
            return null;
        }
        try {
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(jsonString)))
                    .build();
            return loader.load();
        } catch (ConfigurateException e) {
            MCCodeCampLib.getPluginLogger().warning("Failed to parse json marker configuration of custom item. Error" + e.getMessage());
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
        String id = node.node("id").getString("");
        if (id.isEmpty()) {
            return null;
        }
        return itemHandlers.get(id);
    }


    public ItemStack createNewItem(String itemID) {
        ItemStack item;
        try {
            item = itemHandlers.get(itemID).createNewItem();
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        ACustomItemHandler handler = getItemHandler(item);
        if (handler == null) {
            return;
        }
        event.setCancelled(true);
        handler.onInteract(event);
    }
    
}
