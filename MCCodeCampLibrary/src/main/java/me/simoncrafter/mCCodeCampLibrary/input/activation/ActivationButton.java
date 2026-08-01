package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.UUID;

public class ActivationButton implements IBlockRegestryObject, Listener {

    private Plugin plugin;
    private Location location;
    private String ID;
    private RegistryObjectType objectType;
    private ConfigurationNode config;
    private BlockMarkerRegistry registry;
    private UUID uuid;

    private BlockData blockData = null;
    private BlockData cooldownBlockdata = null;
    private Cooldown cooldown;

    @Override
    public void init(Plugin plugin, Location loc, String ID, RegistryObjectType objectType, ConfigurationNode config, UUID uuid, BlockMarkerRegistry registryInstance) {
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
        if (config.hasChild("cooldown")) {
            int cooldownTime = config.node("cooldown").getInt();
            cooldown = new Cooldown(plugin, cooldownTime);
            cooldown.registerCallback(this::cooldownElapseCallback);
            cooldown.registerStartCallback(this::cooldownStartCallback);
        } else {
            cooldown = new Cooldown(plugin, 0);
        }
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
            config.node("cooldownBlockdata").set(blockData.getAsString());
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
            config.node("blockdata").set(cooldown);
        } catch (SerializationException e) {
            plugin.getLogger().warning("Failed to serialize cooldown while saving!");
        }
        registry.saveObject(this);
    }

    public Cooldown getCooldownInstance() {
        return cooldown;
    }

    public void setCooldownInstance(Cooldown cooldown) {
        setCooldown(cooldown.getCooldown());
        this.cooldown = cooldown;
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
        registry.saveObject(this);
    }

    @Override
    public String getTypeID() {
        return objectType.typeID();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock().getLocation().equals(location.getBlock().getLocation())) {
            if (cooldown.isOnCooldown()) return;
            cooldown.start();
            new ButtonIDEvent(event.getPlayer(), ID, event.getClickedBlock());
        }
    }
}
