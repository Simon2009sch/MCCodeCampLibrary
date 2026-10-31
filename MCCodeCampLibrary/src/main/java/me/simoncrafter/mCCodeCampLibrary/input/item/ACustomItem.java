package me.simoncrafter.mCCodeCampLibrary.input.item;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.ConfigurationNode;

public abstract class ACustomItem {

    private final NamespacedKey itemID;
    private ConfigurationNode itemConfig = null;

    public ACustomItem(NamespacedKey itemID) {
        this.itemID = itemID;
    }

    public final String getItemID() {
        return itemID.getKey();
    }

    public final String getItemNamespace() {
        return itemID.getNamespace();
    }

    public final NamespacedKey getNamespaceKey() {
        return itemID;
    }

    protected final void setItem(ItemStack item) {

    }

    protected final void setDisplayItem(Material material) {

    }

    protected final ConfigurationNode getItemData() {
        return itemConfig.node("itemData");
    }

    final void _onItemUpdate() {

    }

    protected void onItemUpdate() {

    }

    final ConfigurationNode _serializeItem() {
        return null;
    }

    protected ConfigurationNode serializeItem() {
        return null;
    }

    public void onPlayerInteract(PlayerInteractEvent event) {

    }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

    }


}
