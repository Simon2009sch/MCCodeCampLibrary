package me.simoncrafter.mCCodeCampLibrary.input.item;

import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public abstract class ACustomItemHandler {

    public abstract String getItemID();
    protected abstract ItemStack getDefaultItemStack();
    protected abstract ConfigurationNode getDefaultItemData();

    public int getCurrentItemDataVersion() {
        return 1;
    }

    protected static int getItemDataVersion(ItemStack itemStack) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node("dataVersion").getInt();
    }

    protected static int getDataInt(ItemStack itemStack, String key) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node(key).getInt();
    }

    protected static String getDataString(ItemStack itemStack, String key) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node(key).getString();
    }

    protected static float getDataFloat(ItemStack itemStack, String key) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node(key).getFloat();
    }

    protected static double getDataDouble(ItemStack itemStack, String key) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node(key).getDouble();
    }

    protected static boolean getDataBoolean(ItemStack itemStack, String key) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node(key).getBoolean();
    }

    public final static ConfigurationNode getItemData(ItemStack item) {
        ConfigurationNode itemData = ItemsManager.readItemConfig(item);
        return itemData.node("itemData");
    }

    /** Replaces only the payload, preserving ID, version and future root metadata. */
    protected final void setItemData(ItemStack itemStack, ConfigurationNode itemData) {
        ConfigurationNode finalData = ItemsManager.readItemConfig(itemStack);
        if (finalData == null) throw new IllegalArgumentException("Item has no valid custom-item metadata");
        try {
            finalData.node("itemData").set(itemData);
        } catch (SerializationException e) {
            MCCodeCampLib.getPluginLogger().warning("Failed to set item data: " + e.getMessage());
        }

        itemStack.editPersistentDataContainer(c -> c.set(ItemsManager.ITEM_KEY, PersistentDataType.STRING, ItemsManager.serializeJson(finalData)));
    }

    public final ItemStack createNewItem() throws SerializationException {
        ItemStack itemStack = getDefaultItemStack();
        ConfigurationNode itemData = getDefaultItemData();
        CommentedConfigurationNode finalData = CommentedConfigurationNode.root();
        finalData.node("itemData").set(itemData);
        finalData.node("id").set(getItemID());
        finalData.node("dataVersion").set(getCurrentItemDataVersion());
        itemStack.editPersistentDataContainer(c -> c.set(ItemsManager.ITEM_KEY, PersistentDataType.STRING, ItemsManager.serializeJson(finalData)));
        return itemStack;
    };

    /** Air-click hook; also receives block clicks through the default block hook. */
    public void onInteract(PlayerInteractEvent event) { }

    /** Override to specialize block clicks; the default preserves legacy handlers exactly once. */
    public void onBlockInteract(PlayerInteractEvent event) { onInteract(event); }

    /** Right-click entity hook. May receive a PlayerInteractAtEntityEvent for precise hits. */
    public void onEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) { }

    /** Left-click entities before damage, including invulnerable/unattackable targets.
     * Paper does not permit uncancelling when willAttack() is false. */
    public void onEntityAttack(io.papermc.paper.event.player.PrePlayerAttackEntityEvent event) { }
}
