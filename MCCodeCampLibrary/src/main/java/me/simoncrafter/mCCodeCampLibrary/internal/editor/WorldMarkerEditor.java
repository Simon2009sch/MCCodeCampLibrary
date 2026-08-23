package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.DataComponentValue;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WorldMarkerEditor extends AEditor {

    private List<CreatableObject> creatableObjects = new ArrayList<>();
    private HotbarMenu addHotbarMenue;

    public record CreatableObject(ItemStack displayItem, Consumer<PlayerInteractEvent> createAction) {}

    public WorldMarkerEditor(Plugin plugin, List<Player> players, List<CreatableObject> creatableObjects) {
        super(plugin, players);
        this.creatableObjects = creatableObjects;

        addHotbarMenue = buildAddHotbarMenu();
        ItemStack addButton = new ItemStack(Material.SUNFLOWER);
        addButton.setData(DataComponentTypes.CUSTOM_NAME, Component.text("New Object"));
        mainHotbarMenu.setItemAt(0, new HotbarItem(plugin, addButton).addBlockClickAction(e -> addHotbarMenue.show(e.getPlayer())));
    }

    public void addCreatableObject(CreatableObject obj) {
        creatableObjects.add(obj);
    }

    public void removeCreatableObject(CreatableObject obj) {
        creatableObjects.remove(obj);
    }

    public List<CreatableObject> getCreatableObjects() {
        return new ArrayList<>(creatableObjects);
    }

    private HotbarMenu buildAddHotbarMenu() {
        List<HotbarMenu> hotbarMenus = new ArrayList<>();

        ItemStack nextPageItem = new ItemStack(Material.BEDROCK);
        ItemMeta nextPageItemMeta = nextPageItem.getItemMeta();
        nextPageItemMeta.displayName(Component.text("Next Page"));
        nextPageItemMeta.lore(List.of(Component.text("Select (Hover) this item in the hotbar to go to the next page")));
        nextPageItem.setItemMeta(nextPageItemMeta);

        ItemStack previousPageItem = new ItemStack(Material.BEDROCK);
        ItemMeta previousPageItemMeta = nextPageItem.getItemMeta();
        previousPageItemMeta.displayName(Component.text("Previous Page"));
        previousPageItemMeta.lore(List.of(Component.text("Select (Hover) this item in the hotbar to go to the previous page")));
        previousPageItem.setItemMeta(previousPageItemMeta);

        for (int i = 0; i < (float) creatableObjects.size() /7; i++) {
            HotbarMenu menu = new HotbarMenu(plugin);


            for (int j = 1; j < Math.min(8, creatableObjects.size()+1); j++) {
                plugin.getLogger().info((i * 6)+j-1 + "");
                HotbarItem item = new HotbarItem(plugin, creatableObjects.get((i * 6)+j-1).displayItem).addBlockClickAction(creatableObjects.get((i * 6)+j-1).createAction);
                menu.setItemAt(j, item);
            }


            if (i == 0) {
                ItemStack exitItem = new ItemStack(Material.BARRIER);
                ItemMeta itemMeta = exitItem.getItemMeta();
                itemMeta.displayName(Component.text("Exit Add dialog", NamedTextColor.RED));
                exitItem.setItemMeta(itemMeta);
                menu.setItemAt(0, mainHotbarMenu.createLoopbackItem(exitItem));
            }

            // add page navigation items
            if (i > 0) {
                int finalI = i;
                menu.setItemAt(0, new HotbarItem(plugin, previousPageItem).addHoverAction(p -> {
                    hotbarMenus.get(finalI -1).show(p);
                    p.sendActionBar(Component.text("Page " + (finalI - 1)));
                    menu.setHeldItemSlotQuietly(p, 7);
                }));
                hotbarMenus.get(i-1).setItemAt(8, new HotbarItem(plugin, nextPageItem).addHoverAction(p -> {
                    menu.show(p);
                    p.sendActionBar(Component.text("Page " + (finalI + 1)));
                    menu.setHeldItemSlotQuietly(p, 1);
                }));
            }

            hotbarMenus.add(menu);
        }
        if (hotbarMenus.isEmpty()) {
            return null;
        }
        return hotbarMenus.getFirst();
    }

}
