package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.display.EditableDisplay;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ActivationButton implements IBlockRegestryObject, Listener, IEditable, IEditorObjectDescriptor {

    private Plugin plugin;

    //editor object descriptor
    private final Map<String, String> DESCRIPTOR_MAP = Map.ofEntries(
            Map.entry("description", "A button able to be used in code"),
            Map.entry("displayname", "Button"));

    //object related
    private Location location;
    private String ID;
    private RegistryObjectType objectType;
    private ConfigurationNode config;
    private BlockMarkerRegistry registry;
    private UUID uuid;

    //button related
    private BlockData blockData = null;
    private BlockData cooldownBlockdata = null;
    private Cooldown cooldown;

    // Shared editor visuals; visibility, selection and entity cleanup belong to the helper.
    private EditableDisplay editorDisplay;

    //configuration editing
    private Map<String, Object> configEditValues = null;


    @Override
    public void init(Plugin plugin, Location loc, String ID, RegistryObjectType objectType, ConfigurationNode config, UUID uuid, BlockMarkerRegistry registryInstance) {
        if (editorDisplay != null) {
            editorDisplay.remove();
            editorDisplay = null;
        }
        this.plugin = plugin;
        location = loc;
        this.ID = ID;
        this.objectType = objectType;
        this.config = config;
        this.uuid = uuid;
        this.registry = registryInstance;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        blockData = readBlockdata("blockdata");
        cooldownBlockdata = readBlockdata("cooldownBlockdata");
        cooldown = new Cooldown(config.node("cooldown").getInt(0));
        cooldown.registerCallback(this::cooldownElapseCallback);
        cooldown.registerStartCallback(this::cooldownStartCallback);

        editorDisplay = new EditableDisplay(location.clone().add(0.5, 0.5, 0.5), plugin, Component.text(ID), this::onRightClickDisplay);
        editorDisplay.setStyle(new EditableDisplay.Style(Color.fromARGB(50, 0, 200, 50), Color.YELLOW, 0.01f));

    }

    public BlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(BlockData blockData) {
        this.blockData = blockData;
        try {
            config.node("blockdata").set(blockData.getAsString());
        } catch (SerializationException e) {
            plugin.getLogger().warning("Failed to serialize block data while saving!");
        }
        registry.saveObject(this);
    }

    public BlockData getCooldownBlockdata() {
        return cooldownBlockdata;
    }

    public void setCooldownBlockdata(BlockData cooldownBlockdata) {
        this.cooldownBlockdata = cooldownBlockdata;
        try {
            config.node("cooldownBlockdata").set(cooldownBlockdata.getAsString());
        } catch (SerializationException e) {
            plugin.getLogger().warning("Failed to serialize cooldown block data while saving!");
        }
        registry.saveObject(this);
    }

    public int getCooldown() {
        return cooldown.getCooldown();
    }

    public void setCooldown(int cooldown) {
        this.cooldown.setCooldown(cooldown);
        try {
            config.node("cooldown").set(cooldown);
        } catch (SerializationException e) {
            plugin.getLogger().warning("Failed to serialize cooldown while saving!");
        }
        registry.saveObject(this);
    }

    public Cooldown getCooldownInstance() {
        return cooldown;
    }

    public void setCooldownInstance(Cooldown cooldown) {
        Objects.requireNonNull(cooldown, "cooldown");
        if (this.cooldown != cooldown) {
            if (this.cooldown != null) this.cooldown.dispose();
            this.cooldown = cooldown;
            cooldown.registerCallback(this::cooldownElapseCallback);
            cooldown.registerStartCallback(this::cooldownStartCallback);
        }
        setCooldown(cooldown.getCooldown());
    }

    /**
     *
     * @param object The path at which the block data should be read from. (Relative to the config object)
     * @return {@link BlockData} that was read. If the string value at the path was invalid or doesn't exist returns null
     */
    private BlockData readBlockdata(String object) {
        if (config.hasChild(object)) {
            try {
                return Bukkit.createBlockData(config.node(object).getString(""));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid blockdata of button with ID: \"" + ID + "\". Blockdata: \"" + config.node("blockdata").getString("") + "\"");
            }
        }
        return null;
    }

    private void cooldownElapseCallback() {
        if (blockData != null) {
            location.getBlock().setBlockData(blockData);
        }
    }

    private void cooldownStartCallback() {
        if (blockData != null && cooldownBlockdata != null && cooldown.getCooldown() > 0) {
            location.getBlock().setBlockData(cooldownBlockdata);
        }
    }

    @Override
    public void showFor(Player player) {
        if (editorDisplay != null) editorDisplay.showFor(player);
    }

    @Override
    public void hideFor(Player player) {
        if (editorDisplay != null) editorDisplay.hideFor(player);
    }

    @Override
    public void select(Player player) {
        if (editorDisplay != null) editorDisplay.select(player);
    }

    @Override
    public void deselect(Player player) {
        if (editorDisplay != null) editorDisplay.deselect(player);
    }

    private void onRightClickDisplay(PlayerInteractAtEntityEvent event) {
        new PlayerClickEditableObjectEvent(event.getPlayer(), this,
                PlayerClickEditableObjectEvent.ClickType.RIGHT).callEvent();
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public ConfigurationNode getConfig() {
        return config;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
        if (editorDisplay != null) editorDisplay.setLocation(location);
        registry.saveObject(this);
    }

    @Override
    public String getTypeID() {
        return objectType.getTypeID();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock().getLocation().equals(location.getBlock().getLocation())) {
            if (cooldown.isOnCooldown()) return;
            cooldown.start();
            new ButtonIDEvent(event.getPlayer(), ID, event.getClickedBlock()).callEvent();
        }
    }






    /**
     * Releases the button's world attachments: removes the display entities, then
     * unregisters this object's event handlers. Idempotent; the instance stays inert
     * and safe to reference after this call. Its cooldown is removed from the central
     * list without firing further callbacks.
     */
    @Override
    public void destroy() {
        if (cooldown != null) cooldown.dispose();
        if (editorDisplay != null) {
            editorDisplay.remove();
            editorDisplay = null;
        }
        IBlockRegestryObject.super.destroy();
    }

    @Override
    public @NotNull String getDisplayKey(String key) {
        return DESCRIPTOR_MAP.get(key);
    }
}
