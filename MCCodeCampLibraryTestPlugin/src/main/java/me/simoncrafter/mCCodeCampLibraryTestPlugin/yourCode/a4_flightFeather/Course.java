package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a4_flightFeather;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.simoncrafter.mCCodeCampLibrary.input.item.ACustomItemHandler;
import me.simoncrafter.mCCodeCampLibrary.input.item.ItemsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Student course: right-click to launch; start sneaking in the air to leap where you look. */
public class Course extends ACustomItemHandler implements AutoCloseable {
    // Bukkit velocity is blocks/TICK, not blocks/second (20 ticks = one second).
    private static final double SPEED = 1.0 / 20.0;
    private final Consumer<Player> wind;
    private final Predicate<ItemStack> isFeather;
    private boolean closed;

    public Course(Consumer<Player> wind) {
        this(wind, item -> {
            ConfigurationNode data = ItemsManager.readItemConfig(item);
            return data != null && "flightFeather".equals(data.node("id").raw());
        });
    }

    Course(Consumer<Player> wind, Predicate<ItemStack> isFeather) {
        this.wind = Objects.requireNonNull(wind);
        this.isFeather = Objects.requireNonNull(isFeather);
    }

    @Override public String getItemID() { return "flightFeather"; }

    @Override protected ItemStack getDefaultItemStack() {
        ItemStack item = new ItemStack(Material.FEATHER);
        item.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Flight Feather"));
        return item;
    }

    @Override protected ConfigurationNode getDefaultItemData() {
        return CommentedConfigurationNode.root();
    }

    @Override public void onInteract(PlayerInteractEvent event) {
        if (closed || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        event.setCancelled(true);
        propel(event.getPlayer(), new Vector(0, SPEED, 0));
    }

    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (closed || event.isCancelled() || !event.isSneaking() || player.isOnGround()
                || player.isInWater() || player.isSwimming() || player.isInsideVehicle()
                || !isFeather.test(player.getInventory().getItemInMainHand())) return;
        Vector direction = player.getLocation().getDirection().clone();
        if (!Double.isFinite(direction.lengthSquared()) || direction.lengthSquared() == 0) return;
        propel(player, direction.normalize().multiply(SPEED));
    }

    private void propel(Player player, Vector velocity) {
        player.setVelocity(velocity);
        wind.accept(player);
    }

    @Override public void close() { closed = true; }
}
