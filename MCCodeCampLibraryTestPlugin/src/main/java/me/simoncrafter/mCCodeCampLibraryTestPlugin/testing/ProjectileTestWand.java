package me.simoncrafter.mCCodeCampLibraryTestPlugin.testing;

import me.simoncrafter.mCCodeCampLibrary.input.item.ACustomItemHandler;
import me.simoncrafter.mCCodeCampLibrary.projectile.ProjectileMovement;
import me.simoncrafter.mCCodeCampLibrary.utility.ItemCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/** One registered custom-item handler per test wand; input dispatch belongs to ItemsManager. */
public class ProjectileTestWand extends ACustomItemHandler {
    private final ProjectileMovement mode;
    private final ItemCooldown cooldown;
    private final Consumer<Player> onShot;
    private final java.util.function.BiFunction<ProjectileMovement, Player, me.simoncrafter.mCCodeCampLibrary.projectile.CustomProjectile> factory;

    public ProjectileTestWand(Plugin plugin, ProjectileMovement mode, Consumer<Player> onShot) {
        this(plugin, mode, onShot, (movement, player) -> ProjectileTestSupport.createProjectile(movement));
    }
    public ProjectileTestWand(Plugin plugin, ProjectileMovement mode, Consumer<Player> onShot,
            java.util.function.BiFunction<ProjectileMovement, Player, me.simoncrafter.mCCodeCampLibrary.projectile.CustomProjectile> factory) {
        this.factory = Objects.requireNonNull(factory);
        this.mode = Objects.requireNonNull(mode);
        this.onShot = Objects.requireNonNull(onShot);
        cooldown = new ItemCooldown(new NamespacedKey(plugin, "projectile_test_cooldown"));
    }
    @Override public String getItemID() { return "projectile_test_" + mode.name().toLowerCase(Locale.ROOT); }
    @Override protected ItemStack getDefaultItemStack() {
        Material material = switch (mode) {
            case ARROW_LIKE -> Material.STICK;
            case SNOWBALL_LIKE -> Material.BREEZE_ROD;
            case FIREBALL_LIKE -> Material.BLAZE_ROD;
            case RAYCAST -> Material.AMETHYST_SHARD;
        };
        NamedTextColor color = switch (mode) {
            case ARROW_LIKE -> NamedTextColor.GREEN;
            case SNOWBALL_LIKE -> NamedTextColor.WHITE;
            case FIREBALL_LIKE -> NamedTextColor.GOLD;
            case RAYCAST -> NamedTextColor.LIGHT_PURPLE;
        };
        String name = switch (mode) {
            case ARROW_LIKE -> "Arrow"; case SNOWBALL_LIKE -> "Snowball";
            case FIREBALL_LIKE -> "Fireball"; case RAYCAST -> "Raycast";
        };
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(Component.text(name + " test wand", color).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Right-click to shoot: " + mode.name().toLowerCase(Locale.ROOT), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("10 tick per-item cooldown | 64 block range", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.setMaxStackSize(1);
        });
        return item;
    }
    @Override protected ConfigurationNode getDefaultItemData() {
        ConfigurationNode data = BasicConfigurationNode.root();
        try {
            data.node("cooldown").set(10);
            data.node("range").set(64);
            data.node("speed").set(defaultSpeed());
        } catch (SerializationException failure) {
            throw new IllegalStateException("Cannot create test wand defaults", failure);
        }
        return data;
    }
    @Override public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK))
            shoot(event.getPlayer(), event.getItem());
    }
    @Override public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) shoot(event.getPlayer(), event.getPlayer().getInventory().getItem(event.getHand()));
    }
    private double defaultSpeed() { return switch (mode) { case ARROW_LIKE -> 2; case SNOWBALL_LIKE, RAYCAST -> 1; case FIREBALL_LIKE -> 1.2; }; }
    private void shoot(Player player, ItemStack item) {
        if (item == null) return;
        ConfigurationNode data = getItemData(item);
        long ticks = Math.max(0, data.node("cooldown").getLong(10));
        double speed = data.node("speed").getDouble(defaultSpeed());
        double range = data.node("range").getDouble(64);
        if (!Double.isFinite(speed) || speed <= 0) speed = defaultSpeed();
        if (!Double.isFinite(range) || range <= 0 || range > 4096) range = 64;
        if (!cooldown.tryUse(item, ticks)) return;
        factory.apply(mode, player).setMaxDistance(range).shoot(player, speed);
        onShot.accept(player);
    }
}
