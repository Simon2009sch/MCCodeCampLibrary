package me.simoncrafter.mCCodeCampLibraryTestPlugin.testing;

import me.simoncrafter.mCCodeCampLibrary.input.item.ACustomItemHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import java.util.Objects;
import java.util.function.Consumer;

/** Registered restore tool; input routing and identity belong to the shared ItemsManager. */
public final class ProjectileRestoreItem extends ACustomItemHandler {
    private final Consumer<Player> restore;
    public ProjectileRestoreItem(Consumer<Player> restore) { this.restore = Objects.requireNonNull(restore); }
    @Override public String getItemID() { return "projectile_test_restore"; }
    @Override protected ItemStack getDefaultItemStack() {
        ItemStack item = new ItemStack(Material.CLOCK);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Restore projectile terrain backups").decoration(TextDecoration.ITALIC, false));
            meta.setMaxStackSize(1);
        });
        return item;
    }
    @Override protected ConfigurationNode getDefaultItemData() { return BasicConfigurationNode.root(); }
    @Override public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) restore.accept(event.getPlayer());
    }
    @Override public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) restore.accept(event.getPlayer());
    }
}
