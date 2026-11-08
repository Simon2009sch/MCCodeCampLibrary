package me.simoncrafter.mCCodeCampLibraryTestPlugin.testing;

import me.simoncrafter.mCCodeCampLibrary.input.activation.ProjectileTarget;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.ProjectileTargetHitEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.projectile.*;
import me.simoncrafter.mCCodeCampLibrary.input.item.ItemsManager;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;

/** Plugin-owned projectile test kit. Does not create or place world markers. */
public class ProjectileTestSupport implements Listener, AutoCloseable {
    public static final String ITEM_BUTTON_ID = "projectile_test_items";
    public static final String TARGET_ID = "projectile_test_target";
    private final ItemsManager itemsManager;
    private final ProjectileRestoreItem restoreItem;
    private final Map<ProjectileMovement, ProjectileTestWand> wands = new EnumMap<>(ProjectileMovement.class);
    private final Set<UUID> testers = new HashSet<>();
    private boolean closed;

    public ProjectileTestSupport(Plugin plugin) {
        this(plugin, Objects.requireNonNull(MCCodeCampLib.getItemsManager(), "Initialize MCCodeCampLib first"));
    }
    ProjectileTestSupport(Plugin plugin, ItemsManager itemsManager) {
        this(plugin, itemsManager, null);
    }
    public ProjectileTestSupport(Plugin plugin, ItemsManager itemsManager, me.simoncrafter.mCCodeCampLibrary.projectile.effect.ProjectileEffects effects) {
        this.itemsManager = Objects.requireNonNull(itemsManager);
        restoreItem = new ProjectileRestoreItem(player -> { if (effects != null) effects.restore(player); });
        itemsManager.registerCustomItem(restoreItem);
        for (ProjectileMovement mode : ProjectileMovement.values()) {
            ProjectileTestWand wand = new ProjectileTestWand(plugin, mode, player -> testers.add(player.getUniqueId()),
                    (movement, player) -> effects == null ? createProjectile(movement) : effects.createProjectile(movement, player));
            wands.put(mode, wand);
            itemsManager.registerCustomItem(wand);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        var registry = MCCodeCampLib.getBlockMarkerRegistry();
        if (registry != null) registry.getRegisteredObjects().forEach(this::configureTarget);
    }

    @EventHandler public void onButton(ButtonIDEvent event) {
        if (closed || !ITEM_BUTTON_ID.equals(event.getID())) return;
        testers.add(event.getPlayer().getUniqueId());
        giveTestItems(event.getPlayer());
    }
    protected void giveTestItems(Player player) {
        ItemStack[] items = {
                itemsManager.createNewItem(wands.get(ProjectileMovement.ARROW_LIKE)),
                itemsManager.createNewItem(wands.get(ProjectileMovement.SNOWBALL_LIKE)),
                itemsManager.createNewItem(wands.get(ProjectileMovement.FIREBALL_LIKE)),
                itemsManager.createNewItem(wands.get(ProjectileMovement.RAYCAST)),
                itemsManager.createNewItem(restoreItem),
                new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 16), new ItemStack(Material.SNOWBALL, 16)
        };
        var leftovers = player.getInventory().addItem(Arrays.stream(items).filter(Objects::nonNull).toArray(ItemStack[]::new));
        player.sendMessage(Component.text("Projectile test kit: right-click with a wand; fireball/ray terrain effects require backup protection in testworld. Clock restores backups.", NamedTextColor.GREEN));
        if (!leftovers.isEmpty()) player.sendMessage(Component.text("Inventory full: some test items could not be added. Make space and click again.", NamedTextColor.YELLOW));
    }
    static CustomProjectile createProjectile(ProjectileMovement mode) {
        return switch (mode) {
            case ARROW_LIKE -> new ArrowProjectile(0);
            case SNOWBALL_LIKE -> new SnowballProjectile();
            case FIREBALL_LIKE -> new FireballProjectile(0);
            case RAYCAST -> new RaycastProjectile(0);
        };
    }
    @EventHandler public void onTarget(ProjectileTargetHitEvent event) {
        if (closed || !TARGET_ID.equals(event.getID())) return;
        Component message = formatDebug(event);
        if (event.getShooter() instanceof Player player) player.sendMessage(message);
        else for (UUID tester : Set.copyOf(testers)) {
            Player player = Bukkit.getPlayer(tester);
            if (player != null) player.sendMessage(message);
        }
    }
    static Component formatDebug(ProjectileTargetHitEvent event) {
        CustomProjectile custom = event.getCustomProjectile();
        Location location = event.getHitPosition();
        String type = custom != null ? custom.getClass().getSimpleName() : "Vanilla " + event.getVanillaProjectile().getType();
        String shooter = event.getShooter() instanceof Player player ? player.getName()
                : event.getShooter() == null ? "none / block source" : event.getShooter().getType().toString();
        Component message = Component.text("━━ Projectile target hit ━━", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)
                .append(row("Target", event.getID())).append(row("Projectile", type)).append(row("Shooter", shooter))
                .append(row("World", location.getWorld() == null ? "unknown" : location.getWorld().getName()))
                .append(row("Hit", coordinates(location.getX(), location.getY(), location.getZ())))
                .append(row("Block face", event.getHitFace() == null ? "unknown (not supplied)" : event.getHitFace().name()));
        if (custom != null) {
            Vector velocity = custom.getVelocity();
            message = message.append(row("Age", custom.getAge() + " ticks"))
                    .append(row("Travelled", String.format(Locale.ROOT, "%.3f blocks", custom.getDistanceTravelled())))
                    .append(row("Velocity", coordinates(velocity.getX(), velocity.getY(), velocity.getZ()) + " blocks/tick"));
        }
        return message;
    }
    private static String coordinates(double x, double y, double z) { return String.format(Locale.ROOT, "%.3f, %.3f, %.3f", x, y, z); }
    private static Component row(String name, String value) {
        return Component.newline().append(Component.text(name + ": ", NamedTextColor.AQUA))
                .append(Component.text(value, NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false);
    }
    @EventHandler public void onMarker(BlockRegistryUpdateEvent event) {
        if (!closed && (event.getUpdateType() == BlockRegistryUpdateEvent.UpdateType.CREATE
                || event.getUpdateType() == BlockRegistryUpdateEvent.UpdateType.LOAD)) configureTarget(event.getRegistryObject());
    }
    private void configureTarget(IBlockRegestryObject object) {
        // Only this explicit fixture opts into vanilla impacts; other targets keep their chosen setting.
        if (TARGET_ID.equals(object.getID()) && object instanceof ProjectileTarget target && !target.isDetectVanillaProjectiles())
            target.setDetectVanillaProjectiles(true);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { testers.remove(event.getPlayer().getUniqueId()); }
    @Override public void close() {
        if (closed) return;
        closed = true; HandlerList.unregisterAll(this); testers.clear();
        wands.values().forEach(itemsManager::unregisterCustomItem);
        wands.clear();
        itemsManager.unregisterCustomItem(restoreItem);
    }
}
