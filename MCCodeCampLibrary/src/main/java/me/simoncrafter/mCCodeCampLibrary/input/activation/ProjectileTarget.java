package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.ProjectileTargetHitEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.display.EditableDisplay;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.*;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import java.util.UUID;

/** Persistent exact-block projectile input. All operations belong on the server thread.
 * Unlike a button, right-clicks only select this object in the editor, never activate it.
 */
public class ProjectileTarget implements IBlockRegestryObject, IEditable, IEditorObjectDescriptor {
    private Plugin plugin;
    private Location location;
    private String id;
    private RegistryObjectType objectType;
    private ConfigurationNode config;
    private UUID uuid;
    private BlockMarkerRegistry registry;
    private Cooldown cooldown;
    private BlockData blockData, cooldownBlockdata;
    private boolean detectVanillaProjectiles;
    private boolean active;
    private EditableDisplay editorDisplay;

    @Override public void init(Plugin plugin, Location loc, String id, RegistryObjectType type,
                               ConfigurationNode config, UUID uuid, BlockMarkerRegistry registry) {
        destroy();
        this.plugin = plugin;
        this.location = loc.clone();
        this.id = id;
        this.objectType = type;
        this.config = config;
        this.uuid = uuid;
        this.registry = registry;
        blockData = readBlockData("blockdata");
        cooldownBlockdata = readBlockData("cooldownBlockdata");
        detectVanillaProjectiles = config.node("detectVanillaProjectiles").getBoolean(false);
        cooldown = new Cooldown(Math.max(0, config.node("cooldown").getInt(0)));
        cooldown.registerStartCallback(() -> {
            if (active && blockData != null && cooldownBlockdata != null) location.getBlock().setBlockData(cooldownBlockdata);
        });
        cooldown.registerCallback(() -> {
            if (active && blockData != null) location.getBlock().setBlockData(blockData);
        });
        editorDisplay = new EditableDisplay(location.clone().add(.5, .5, .5), plugin, Component.text(id),
                event -> new PlayerClickEditableObjectEvent(event.getPlayer(), this,
                        PlayerClickEditableObjectEvent.ClickType.RIGHT).callEvent());
        editorDisplay.setStyle(new EditableDisplay.Style(Color.fromARGB(50, 200, 50, 0), Color.YELLOW, .01f));
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Observe only accepted pre-impact custom hits; do not change projectile cancellation. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomProjectileHit(me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent event) {
        if (!active || event.isCancelled() || !matches(event.getHit().block()) || cooldown.isOnCooldown()) return;
        cooldown.start();
        new ProjectileTargetHitEvent(this, event.getProjectile(), null, event.getProjectile().getShooter(),
                event.getHit().position().toLocation(location.getWorld()), event.getHit().face()).callEvent();
    }

    /** Vanilla support is deliberately opt-in. Bukkit exposes the projectile's current position. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVanillaProjectileHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!active || !detectVanillaProjectiles || event.isCancelled() || !matches(event.getHitBlock()) || cooldown.isOnCooldown()) return;
        cooldown.start();
        Entity shooter = event.getEntity().getShooter() instanceof Entity entity ? entity : null;
        new ProjectileTargetHitEvent(this, null, event.getEntity(), shooter,
                event.getEntity().getLocation(), event.getHitBlockFace()).callEvent();
    }

    private boolean matches(Block block) {
        return block != null && location.getWorld().equals(block.getWorld())
                && location.getBlockX() == block.getX() && location.getBlockY() == block.getY() && location.getBlockZ() == block.getZ();
    }
    private BlockData readBlockData(String key) {
        String value = config.node(key).getString();
        if (value == null) return null;
        try { return Bukkit.createBlockData(value); }
        catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid " + key + " for projectile target " + id + ": " + value);
            return null;
        }
    }
    private void persist(String key, Object value) {
        try { config.node(key).set(value); }
        catch (SerializationException e) { throw new IllegalArgumentException("Cannot save projectile target " + key, e); }
        registry.saveObject(this);
    }
    public boolean isDetectVanillaProjectiles() { return detectVanillaProjectiles; }
    public void setDetectVanillaProjectiles(boolean detect) { detectVanillaProjectiles = detect; persist("detectVanillaProjectiles", detect); }
    public int getCooldown() { return cooldown.getCooldown(); }
    /** Tick duration; changes affect future hits, not an already running cooldown. */
    public void setCooldown(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("Cooldown must be nonnegative");
        cooldown.setCooldown(ticks); persist("cooldown", ticks);
    }
    public Cooldown getCooldownInstance() { return cooldown; }
    public BlockData getBlockData() { return blockData == null ? null : blockData.clone(); }
    public void setBlockData(BlockData data) { blockData = data == null ? null : data.clone(); persist("blockdata", data == null ? null : data.getAsString()); }
    public BlockData getCooldownBlockdata() { return cooldownBlockdata == null ? null : cooldownBlockdata.clone(); }
    public void setCooldownBlockdata(BlockData data) { cooldownBlockdata = data == null ? null : data.clone(); persist("cooldownBlockdata", data == null ? null : data.getAsString()); }
    @Override public String getID() { return id; }
    @Override public UUID getUUID() { return uuid; }
    @Override public Location getLocation() { return location.clone(); }
    @Override public ConfigurationNode getConfig() { return config; }
    @Override public String getTypeID() { return objectType.getTypeID(); }
    @Override public void setLocation(Location location) {
        this.location = location.clone();
        if (editorDisplay != null) editorDisplay.setLocation(location);
        registry.saveObject(this);
    }
    @Override public void showFor(Player p) { if (editorDisplay != null) editorDisplay.showFor(p); }
    @Override public void hideFor(Player p) { if (editorDisplay != null) editorDisplay.hideFor(p); }
    @Override public void select(Player p) { if (editorDisplay != null) editorDisplay.select(p); }
    @Override public void deselect(Player p) { if (editorDisplay != null) editorDisplay.deselect(p); }
    @Override public @NotNull String getDisplayKey(String key) {
        return switch (key) { case "displayname" -> "Projectile Target"; case "description" -> "A block activated by projectile impacts"; default -> key; };
    }
    /** Idempotent detach on removal, chunk unload, replacement, or plugin shutdown. */
    @Override public void destroy() {
        active = false;
        IBlockRegestryObject.super.destroy();
        if (cooldown != null) cooldown.dispose();
        if (editorDisplay != null) { editorDisplay.remove(); editorDisplay = null; }
    }
}
