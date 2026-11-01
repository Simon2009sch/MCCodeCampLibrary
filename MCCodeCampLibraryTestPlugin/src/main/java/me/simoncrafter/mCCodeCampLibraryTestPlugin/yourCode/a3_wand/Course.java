package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a3_wand;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.simoncrafter.mCCodeCampLibrary.input.item.ACustomItemHandler;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import me.simoncrafter.mCCodeCampLibraryTestPlugin.MCCodeCampLibraryTestPlugin;
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

    private int getLastUse(ItemStack itemStack) {
        ConfigurationNode node = getItemData(itemStack);
        return node.node("lastUse").getInt();
    }




    @Override
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getHand() == EquipmentSlot.HAND) {

            if (event.getPlayer().isSneaking()) {
                event.getClickedBlock().getWorld().createExplosion(event.getClickedBlock().getLocation(), 10);
                return;
            }
            event.getClickedBlock().getWorld().createExplosion(event.getClickedBlock().getLocation(), 2);
            event.getPlayer().sendMessage(Component.text("Explosion! " + event.getClickedBlock().getLocation()));
        }
    }
}
