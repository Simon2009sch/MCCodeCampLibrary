package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditData;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditPlayerData;
import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.CraftersDisplayLibrary.display.cube.CubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.cube.CubeColorInformation;
import me.simoncrafter.CraftersDisplayLibrary.display.panel.TextDisplay;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.editables.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.utility.Cooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActivationButton implements IBlockRegestryObject, Listener, IEditable {

    private Plugin plugin;

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

    //editable related
    private CubeColorDisplay displayObject = null;
    private TextDisplay displayLabel = null;

    //configuration editing
    private Map<String, Object> configEditValues = null;

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
            cooldown = new Cooldown(cooldownTime);
            cooldown.registerCallback(this::cooldownElapseCallback);
            cooldown.registerStartCallback(this::cooldownStartCallback);
        } else {
            cooldown = new Cooldown(0);
        }

        displayObject = CubeColorDisplay.create(loc.clone(), new Vector3f(1.01f, 1.01f, 1.01f), new Vector3f(), new Quaternionf(), new CubeColorInformation(Color.fromARGB(100, 150, 255, 0)));
        displayLabel = TextDisplay.create(loc.clone().add(0.5f, 0.5f, 0.5f), new Vector3f(1, 1, 1), new Vector3f(), new Quaternionf());
        displayLabel.setText(
                Component.empty()
                        .append(Component.text("Button", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .appendNewline()
                        .append(Component.empty().decoration(TextDecoration.BOLD, false))
                        .append(Component.text("ID: ", NamedTextColor.WHITE, TextDecoration.BOLD))
                        .append(Component.text(ID))
        );
        displayObject.addChild(displayLabel);
        displayObject.setSeeThrough(true);
        displayLabel.setSeeThrough(true);
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
            new ButtonIDEvent(event.getPlayer(), ID, event.getClickedBlock()).callEvent();
        }
    }

    @Override
    public void onPlayerEnterEditor(Player player) {

    }

    @Override
    public void onPlayerLeaveEditor(Player player) {

    }

    @Override
    public Set<Interaction> getActivationEntityList() {
        return Set.of();
    }

    @Override
    public boolean isPlayerInEditingRage(Player player) {
        return true;
    }

    @Override
    public void onPlayerSelect(Player player) {
        String syncKey = "edit_button_" + uuid;

        ConfigEditQuestion editQuestion = getConfigEditQuestion()
        .onReload(p -> {
            // Reload: re-send the question with the (same) current config
            ConfigEditQuestion reloaded = getConfigEditQuestion();
            reloaded.show(p, syncKey);
        });
        editQuestion.show(player, syncKey);

    }

    @Override
    public void onPlayerDeselect(Player player) {

    }

    @Override
    public PositionObject getDisplay() {
        return displayObject;
    }

    @Override
    public void spawnDisplay() {
        displayLabel.spawnDisplay();
        displayObject.spawnDisplay();
    }

    @Override
    public void despawnDisplay() {
        displayObject.remove();
        displayLabel.remove();
    }

    /**
     * Releases the button's world attachments: removes the display entities, then
     * unregisters this object's event handlers. Idempotent; the instance stays inert
     * and safe to reference after this call. The cooldown is timestamp-based and needs
     * no cancellation.
     */
    @Override
    public void destroy() {
        if (displayObject != null) {
            displayObject.remove();
            displayObject = null;
        }
        if (displayLabel != null) {
            displayLabel.remove();
            displayLabel = null;
        }
        IBlockRegestryObject.super.destroy();
    }


    private ConfigEditQuestion getConfigEditQuestion() {
        if (configEditValues == null) {
            configEditValues = configToMap(getConfig());
        }

        ConfigEditData data = ConfigEditData.create(configEditValues);
        ConfigEditQuestion question = ConfigEditQuestion.create(new ConfigEditPlayerData(), data)
                .showSaveChangesButton(true)
                .saveChangesAction(() -> {
                    // Apply the edited values back into the object's config node
                    try {
                        config.set(configEditValues);
                    } catch (SerializationException e) {
                        plugin.getLogger().warning("Failed to serialize edited config for button \"" + ID + "\"");
                    }
                    registry.saveObject(this);
                });

        question.setRootSection(data.getNewRootSection());
        return question;
    }

}
