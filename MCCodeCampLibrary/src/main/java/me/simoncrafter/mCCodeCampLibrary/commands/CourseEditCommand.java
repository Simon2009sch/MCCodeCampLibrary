package me.simoncrafter.mCCodeCampLibrary.commands;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorItems;
import me.simoncrafter.mCCodeCampLibrary.internal.events.BlockRegistryUpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CourseEditCommand implements TabExecutor, org.bukkit.command.CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("You need to be a player to execute this!", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("getItems") && player.hasPermission("mccodecamp.command.edit.getitems")) {
            player.give(EditorItems.getToggleVisibility());
            player.give(EditorItems.getCreateNew());
            player.give(EditorItems.getRemove());
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            new BlockRegistryUpdateEvent().callEvent();
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        return List.of();
    }
}
