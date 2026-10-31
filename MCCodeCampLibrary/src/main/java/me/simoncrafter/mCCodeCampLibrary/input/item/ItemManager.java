package me.simoncrafter.mCCodeCampLibrary.input.item;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class ItemManager implements Listener {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey("me.simoncrafter.mccodecamplibrary", "customItems_item_key");
    private final Plugin PLUGIN;
    private final Map<NamespacedKey, ACustomItem> itemHandlers = new HashMap<>();

    private static boolean deleteInvalidItems = false;

    public ItemManager(Plugin PLUGIN) {
        this.PLUGIN = PLUGIN;
    }

    public void registerCustomItem(ACustomItem item) {
        if (itemHandlers.containsKey(item.getNamespaceKey())) {
            return;
        }
        itemHandlers.put(item.getNamespaceKey(), item);
    }

    public ACustomItem getCustomItemFromItemStack(ItemStack itemStack) {
        ConfigurationNode node = readItemConfig(itemStack);
        if (node == null) return null;
        String[] splitNamespace = node.node("namespace").getString("").split(":");
        if (splitNamespace.length != 2) { // remove invalid namespace items
            removeInvalidItem(itemStack);
            return null;
        }
        NamespacedKey namespacedKey = new NamespacedKey(splitNamespace[0], splitNamespace[1]);
        if (!itemHandlers.containsKey(namespacedKey)) { // check if item type exists
            removeInvalidItem(itemStack);
            return null;
        }
        return itemHandlers.get(namespacedKey);
    }

    private void checkInventory(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ACustomItem item = getCustomItemFromItemStack(inv.getItem(i));
        }
    }

    private ConfigurationNode readItemConfig(ItemStack itemStack) {
        PersistentDataContainerView pdc = itemStack.getPersistentDataContainer();
        if (!pdc.has(ITEM_KEY)) {
            return null;
        }
        return convertToConfigNode(pdc.get(ITEM_KEY, PersistentDataType.STRING));
    }


    /**
     * Removes the item from the inventory if the config value is set to delete invalid items
     * @param itemStack The item in question
     */
    private void removeInvalidItem(ItemStack itemStack) {
        if (!itemStack.getPersistentDataContainer().has(ITEM_KEY)) return;
        if (deleteInvalidItems) {
            itemStack.setAmount(0);
        }
    }

    private @Nullable ConfigurationNode convertToConfigNode(String jsonString) {
        if (jsonString == null || jsonString.isBlank() || jsonString.isBlank()) {
            return null;
        }
        try {
            GsonConfigurationLoader loader = GsonConfigurationLoader.builder()
                    .source(() -> new BufferedReader(new StringReader(jsonString)))
                    .build();
            return loader.load();
        } catch (ConfigurateException e) {
            PLUGIN.getLogger().warning("Failed to parse json marker configuration of custom item. Error" + e.getMessage());
            return null;
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        
    }

    @EventHandler
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        
    }
    
}
