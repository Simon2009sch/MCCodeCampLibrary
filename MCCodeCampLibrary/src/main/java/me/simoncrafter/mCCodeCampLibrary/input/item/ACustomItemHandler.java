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

    private final void setItemData(ItemStack itemStack, ConfigurationNode itemData) {
        CommentedConfigurationNode finalData = CommentedConfigurationNode.root();
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

    public abstract void onInteract(PlayerInteractEvent event);
}
