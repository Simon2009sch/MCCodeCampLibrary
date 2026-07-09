package me.simoncrafter.mCCodeCampLibrary.utility;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Tags {@link ItemStack}s as custom plugin items and reads that tag back.
 * <p>
 * Custom items are marked by storing a single string of the form
 * {@code <type><ITEM_DATA_SEPARATOR><uuid>} under the {@link #PLUGIN_ITEM} key
 * in the item's persistent data container.
 */
public class ItemsManager {

    private final Plugin plugin;
    private final NamespacedKey PLUGIN_ITEM;
    private static final String ITEM_DATA_SEPARATOR = ";";

    public ItemsManager(Plugin plugin) {
        this.plugin = plugin;
        PLUGIN_ITEM = new NamespacedKey(plugin, "pluginItem");
    }

    /**
     * Clones the given item and tags it as a custom plugin item of the given type
     * and UUID.
     *
     * @param itemStack  the item to clone and tag
     * @param ID         the custom item type identifier
     * @param uuidString the UUID to tag the item with
     * @return the tagged clone; the original {@code itemStack} is left untouched
     */
    public ItemStack createNewItem(ItemStack itemStack, String ID, String uuidString, List<String> data) {
        ItemStack clone = itemStack.clone();

        String dataString = ID + ITEM_DATA_SEPARATOR + uuidString;
        if (!data.isEmpty()) {
            dataString = dataString + ITEM_DATA_SEPARATOR;
        }
        for (String s : data) {
            dataString = dataString + ITEM_DATA_SEPARATOR + s;
        }


        String finalDataString = dataString;
        clone.editPersistentDataContainer(pdc -> {
            pdc.set(PLUGIN_ITEM, PersistentDataType.STRING, finalDataString);
        });
        return clone;
    }

    /**
     * Clones the given item and tags it as a custom plugin item of the given type,
     * without assigning it a real UUID.
     *
     * @param itemStack the item to clone and tag
     * @param ID        the custom item type identifier
     * @return the tagged clone; the original {@code itemStack} is left untouched
     * @see #createNewItem(ItemStack, String, String, List<String>)
     */
    public ItemStack createNewItem(ItemStack itemStack, String ID) {
        return createNewItem(itemStack, ID, "_", new ArrayList<>());
    }

    /**
     * Clones the given item and tags it as a custom plugin item of the given type,
     * without assigning it a real UUID.
     *
     * @param itemStack the item to clone and tag
     * @param ID        the custom item type identifier
     * @param data      list of custom data to pass with the item
     * @return the tagged clone; the original {@code itemStack} is left untouched
     * @see #createNewItem(ItemStack, String, String, List<String>)
     */
    public ItemStack createNewItem(ItemStack itemStack, String ID, List<String> data) {
        return createNewItem(itemStack, ID, "_", data);
    }

    /**
     * @param itemStack the item to inspect
     * @return the custom item type, or {@code null} if the item isn't a tagged custom item
     */
    public @Nullable String getCustomItemType(ItemStack itemStack) {
        String[] data = getValidatedItemInfo(itemStack);
        return data == null ? null : data[0];
    }

    /**
     * @param itemStack the item to inspect
     * @return the custom item's UUID, or {@code null} if the item isn't a tagged custom item
     */
    public @Nullable String getCustomItemUUID(ItemStack itemStack) {
        String[] data = getValidatedItemInfo(itemStack);
        return data == null ? null : data[1];
    }

    /**
     * Validates that the item is a plugin custom item and, if so, parses its tag data.
     *
     * @param itemStack the item to inspect
     * @return a two-element array {@code [type, uuid]}, or {@code null} if the item
     * isn't a tagged custom item
     */
    public @Nullable String[] getValidatedItemInfo(ItemStack itemStack) {
        if (!isPluginCustomItem(itemStack)) {
            return null;
        }
        return getCustomItemInfo(itemStack.getPersistentDataContainer());
    }

    /**
     * Reads and parses the {@link #PLUGIN_ITEM} tag from a persistent data container.
     *
     * @param container the container to read from
     * @return a two-element array {@code [type, uuid]}, or {@code null} if the tag is absent
     */
    private @Nullable String[] getCustomItemInfo(PersistentDataContainerView container) {
        String[] out;
        try {
            out = container.get(PLUGIN_ITEM, PersistentDataType.STRING).split(ITEM_DATA_SEPARATOR);
        } catch (NullPointerException e) {
            out = null;
        }
        return out;
    }

    /**
     * @param itemStack the item to check
     * @return {@code true} if the item carries this plugin's custom item tag
     */
    public boolean isPluginCustomItem(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        return itemStack.getPersistentDataContainer().has(PLUGIN_ITEM);
    }
}
