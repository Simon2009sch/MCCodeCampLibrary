package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.mCCodeCampLibrary.utility.ItemsManager;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EditorItems {
    public static ItemStack getToggleVisibility() {
        ItemStack toggleVisibility = new ItemStack(Material.ENDER_EYE);
        ItemMeta toggleVisibilityMeta = toggleVisibility.getItemMeta();
        toggleVisibilityMeta.customName(Component.text("Toggle visibility", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        toggleVisibility.setItemMeta(toggleVisibilityMeta);
        toggleVisibility = MCCodeCampLib.getItemsManager().createNewItem(toggleVisibility, "editor_toggleVisibility");

        return toggleVisibility;
    }

    public static ItemStack getCreateNew() {
        ItemStack createNew = new ItemStack(Material.SUNFLOWER);
        ItemMeta toggleVisibilityMeta = createNew.getItemMeta();
        toggleVisibilityMeta.customName(Component.text("Add new", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

        createNew.setItemMeta(toggleVisibilityMeta);
        createNew = MCCodeCampLib.getItemsManager().createNewItem(createNew, "editor_createNew");

        return createNew;
    }

    public static ItemStack getRemove() {
        ItemStack remove = new ItemStack(Material.TNT);
        ItemMeta toggleVisibilityMeta = remove.getItemMeta();
        toggleVisibilityMeta.customName(Component.text("Add new", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

        remove.setItemMeta(toggleVisibilityMeta);
        remove = MCCodeCampLib.getItemsManager().createNewItem(remove, "editor_remove");

        return remove;
    }
}
