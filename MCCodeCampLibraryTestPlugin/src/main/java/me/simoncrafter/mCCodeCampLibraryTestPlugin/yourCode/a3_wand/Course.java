package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a3_wand;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.simoncrafter.mCCodeCampLibrary.input.item.ACustomItemHandler;
import me.simoncrafter.mCCodeCampLibrary.utility.ItemCooldown;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class Course extends ACustomItemHandler {

    @Override
    public String getItemID() {
        return "customWand";
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        ItemStack itemStack = new ItemStack(Material.STICK);
        itemStack.setData(DataComponentTypes.CUSTOM_NAME, Component.text("WAND!"));
        return itemStack;
    }

    @Override
    protected ConfigurationNode getDefaultItemData() {
        ConfigurationNode node = CommentedConfigurationNode.root();
        try {
            node.node("cooldown").set(5);
        } catch (SerializationException ignored) {

        }
        return node;
    }

    private final ItemCooldown itemCooldown = new ItemCooldown(
            new NamespacedKey(JavaPlugin.getProvidingPlugin(Course.class), "wand_cooldown"));




    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getHand() == EquipmentSlot.HAND) {
            if (event.getItem() == null || !itemCooldown.tryUse(event.getItem(),
                    getItemData(event.getItem()).node("cooldown").getLong())) {
                return;
            }

            if (event.getPlayer().isSneaking()) {
                event.getClickedBlock().getWorld().createExplosion(event.getClickedBlock().getLocation(), 10);
                return;
            }
            event.getClickedBlock().getWorld().createExplosion(event.getClickedBlock().getLocation(), 2);
            event.getPlayer().sendMessage(Component.text("Explosion! " + event.getClickedBlock().getLocation()));
        }
    }
}
