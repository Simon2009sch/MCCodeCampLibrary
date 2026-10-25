package me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu;


import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import me.simoncrafter.CraftersDisplayLibrary.effect.highlighter.BlockHighlighter;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

public class HotbarItem implements Listener, Cloneable {

    public static final NamespacedKey ITEM_KEY = new NamespacedKey("me.simoncrafter.mccodecamplibrary", "editor_hotbaritem");
    public static final NamespacedKey ITEM_DEBOUNCE_TIMESTAMP = new NamespacedKey("me.simoncrafter.mccodecamplibrary", "editor_item_debounce_timestamp");
    private static final int ITEM_DEBOUNCE_TIME = 1;

    private Plugin plugin;
    private UUID uuid;
    private String referenceID = ""; // for identifying the item when its used elsewhere without reference to the Hotbar item instance
    private ItemStack item;
    private Material masqueradedMaterial;
    private NamespacedKey originalItemModel;

    private Set<Consumer<PlayerInteractEvent>> blockClickActions = new HashSet<>();
    private Set<Consumer<PlayerInteractEntityEvent>> rightClickEntityActions = new HashSet<>();
    private Set<Consumer<PrePlayerAttackEntityEvent>> leftClickEntityActions = new HashSet<>();
    private Set<Consumer<Player>> hoverActions = new HashSet<>();
    private Set<Consumer<Player>> unHoverActions = new HashSet<>();

    private int numberOfCurrentUsages = 0;
    private Set<Player> debouncePlayerList = new HashSet<>(); // the internal alternative to the global debounce so an action that happens in the hotbarmenu and doesn't change the menu

    public HotbarItem(Plugin plugin, ItemStack item) {
        this.plugin = plugin;
        this.uuid = UUID.randomUUID();

        setItem(item);
    }

    private HotbarItem(Plugin plugin, ItemStack item, Set<Consumer<PlayerInteractEvent>> blockClickActions, Set<Consumer<PlayerInteractEntityEvent>> rightClickEntityActions, Set<Consumer<PrePlayerAttackEntityEvent>> leftClickEntityActions, Set<Consumer<Player>> hoverActions, Set<Consumer<Player>> unHoverActions, Material masqueradedMaterial, NamespacedKey originalItemModel, String referenceID) {
        this.plugin = plugin;
        this.uuid = UUID.randomUUID();

        this.blockClickActions = blockClickActions;
        this.rightClickEntityActions = rightClickEntityActions;
        this.leftClickEntityActions = leftClickEntityActions;
        this.hoverActions = hoverActions;
        this.unHoverActions = unHoverActions;
        this.masqueradedMaterial = masqueradedMaterial;
        this.originalItemModel = originalItemModel;
        this.referenceID = referenceID;

        setItem(item);
    }

    /**
     * Should be called when the hotbar item is put into a players hotbar. This method adds one to the internal usages counter. If it reaches 0 item will perform cleanup. For removing from this counter see {@link #onItemNoLongerInUse()}<br>
     * Also applies a small debounce time onto the player so if the menu change was triggered by a ingame event like clicking a block it doesn't trigger the next item immediately.
     */
    public void onItemPutInHotbar(Player player) {
        if (item.getType() == Material.AIR) return;
        player.getPersistentDataContainer().set(ITEM_DEBOUNCE_TIMESTAMP, PersistentDataType.INTEGER, Bukkit.getCurrentTick());
        Bukkit.getPluginManager().registerEvents(this, plugin);
        numberOfCurrentUsages++;
    }

    /**
     * Gets the reference ID of the item. It is appended at the end of the uuid string in PDC with the {@link #ITEM_KEY}.<br>
     * You can check if an item has this reference id by retrieving the PDC value with the {@link #ITEM_KEY} and checking if it {@link String#endsWith(String)} your desired key
     * @return The reference ID of this item
     */
    public String getReferenceID() {
        return referenceID;
    }

    /**
     * Gets the reference ID of the item. It is appended at the end of the uuid string in PDC with the {@link #ITEM_KEY}.<br>
     * You can check if an item has this reference id by retrieving the PDC value with the {@link #ITEM_KEY} and checking if it {@link String#endsWith(String)} your desired key
     * @param referenceID The id that should be applied to this item
     */
    public void setReferenceID(String referenceID) {
        this.referenceID = referenceID;
        setItem(item);
    }

    /**
     * Removes one from the internal counter of usages. When it reaches 0 item will unregister all listeners and perform a cleanup. See {@link #onItemPutInHotbar(Player)} for adding to this counter and also setting a debounce timeout for the player
     */
    public void onItemNoLongerInUse() {
        numberOfCurrentUsages--;
        if (numberOfCurrentUsages <= 0) {
            HandlerList.unregisterAll(this);
        }
    }

    /**
     * Called once the plugin loads. In normal circumstances no players would be online, but in testing or on a reload the timestamps need to be cleared.
     * @param plugin The plugin instance
     */
    public static void onPluginLoad(Plugin plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getPersistentDataContainer().remove(ITEM_DEBOUNCE_TIMESTAMP);
        }
    }


    /**
     * @return the item bound to this HotbarItem.
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Stores a clone of the supplied item, tagging only that owned copy with this HotbarItem's
     * identifier so it can later be recognised by {@link #isItem(ItemStack)}.
     */
    public HotbarItem setItem(ItemStack item) {
        this.item = Objects.requireNonNull(item, "item").clone();
        // set the items uuid in the item key PDC and if reference id is set, appned it, else leave empty
        this.item.editPersistentDataContainer(pdc -> pdc.set(ITEM_KEY, PersistentDataType.STRING, uuid.toString() + (!referenceID.isEmpty() ? "_" + referenceID : "")));
        masqueradedMaterial = null;
        originalItemModel = null;
        refreshItemInInventories();
        return this;
    }

    private void refreshItemInInventories() {
        forEachInstance((player, slot, current) -> {
            player.getInventory().setItem(slot, this.item.clone());
        });
    }

    /**
     * Runs the given action on every ItemStack tagged as this HotbarItem, in any online player's
     * inventory, then writes the (possibly mutated) result back into its slot.
     */
    private void forEachInstance(TriConsumer<Player, Integer, ItemStack> action) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    PlayerInventory inventory = player.getInventory();
                    ItemStack[] contents = inventory.getContents();

                    for (int slot = 0; slot < contents.length; slot++) {
                        ItemStack current = contents[slot];
                        if (!isItem(current)) {
                            continue;
                        }
                        int finalSlot = slot;
                        new BukkitRunnable(){
                            @Override
                            public void run() {
                                action.accept(player, finalSlot, current.clone());
                            }
                        }.runTask(plugin);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);

    }

    /**
     * Registers an action in the given action set.
     */
    private <T> HotbarItem addAction(Set<T> actions, T action) {
        actions.add(action);
        return this;
    }

    /**
     * @return an unmodifiable view of the given action set.
     */
    private <T> Set<T> getActions(Set<T> actions) {
        return Collections.unmodifiableSet(actions);
    }

    /**
     * Clears the given action set.
     */
    private HotbarItem clearActions(Set<?> actions) {
        actions.clear();
        return this;
    }

    /**
     * Adds an action to run when the player left- or right-clicks air or a block (not an entity)
     * with this item. Check {@link PlayerInteractEvent#getAction()} and
     * {@link PlayerInteractEvent#getClickedBlock()} to distinguish the cases.
     */
    public HotbarItem addBlockClickAction(Consumer<PlayerInteractEvent> action) {
        return addAction(blockClickActions, action);
    }

    /**
     * @return an unmodifiable view of the block-click actions.
     */
    public Set<Consumer<PlayerInteractEvent>> getBlockClickActions() {
        return getActions(blockClickActions);
    }

    /**
     * Removes all block-click actions.
     */
    public HotbarItem clearBlockClickActions() {
        return clearActions(blockClickActions);
    }

    /**
     * Adds an action to run when the player right-clicks an entity with this item.
     */
    public HotbarItem addRightClickEntityAction(Consumer<PlayerInteractEntityEvent> action) {
        return addAction(rightClickEntityActions, action);
    }

    /**
     * @return an unmodifiable view of the right-click-entity actions.
     */
    public Set<Consumer<PlayerInteractEntityEvent>> getRightClickEntityActions() {
        return getActions(rightClickEntityActions);
    }

    /**
     * Removes all right-click-entity actions.
     */
    public HotbarItem clearRightClickEntityActions() {
        return clearActions(rightClickEntityActions);
    }

    /**
     * Adds an action to run when the player left-clicks (attacks) an entity with this item.
     */
    public HotbarItem addLeftClickEntityAction(Consumer<PrePlayerAttackEntityEvent> action) {
        return addAction(leftClickEntityActions, action);
    }

    /**
     * @return an unmodifiable view of the left-click-entity actions.
     */
    public Set<Consumer<PrePlayerAttackEntityEvent>> getLeftClickEntityActions() {
        return getActions(leftClickEntityActions);
    }

    /**
     * Removes all left-click-entity actions.
     */
    public HotbarItem clearLeftClickEntityActions() {
        return clearActions(leftClickEntityActions);
    }

    /**
     * Adds an action to run when the player starts hovering over this item.
     */
    public HotbarItem addHoverAction(Consumer<Player> action) {
        return addAction(hoverActions, action);
    }

    /**
     * @return an unmodifiable view of the hover actions.
     */
    public Set<Consumer<Player>> getHoverActions() {
        return getActions(hoverActions);
    }

    /**
     * Removes all hover actions.
     */
    public HotbarItem clearHoverActions() {
        return clearActions(hoverActions);
    }

    /**
     * Adds an action to run when the player stops hovering over this item.
     */
    public HotbarItem addUnHoverAction(Consumer<Player> action) {
        return addAction(unHoverActions, action);
    }

    /**
     * @return an unmodifiable view of the un-hover actions.
     */
    public Set<Consumer<Player>> getUnHoverActions() {
        return getActions(unHoverActions);
    }

    /**
     * Removes all un-hover actions.
     */
    public HotbarItem clearUnHoverActions() {
        return clearActions(unHoverActions);
    }

    void onHover(Player player) {
        hoverActions.forEach(a -> a.accept(player));
    }

    void onUnHover(Player player) {
        unHoverActions.forEach(a -> a.accept(player));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PlayerInventory playerInv = player.getInventory();


        ItemStack heldItem = playerInv.getItem(playerInv.getHeldItemSlot());
        if (!isItem(heldItem) || player.getPersistentDataContainer().getOrDefault(ITEM_DEBOUNCE_TIMESTAMP, PersistentDataType.INTEGER, 0) + ITEM_DEBOUNCE_TIME >= Bukkit.getCurrentTick()) {
            return;
        }
        event.setCancelled(true);

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() == Action.PHYSICAL) {
            return;
        }
        blockClickActions.forEach(a -> a.accept(event));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        PlayerInventory playerInv = player.getInventory();

        ItemStack heldItem = playerInv.getItem(playerInv.getHeldItemSlot());
        if (!isItem(heldItem)) {
            return;
        }
        event.setCancelled(true);

        rightClickEntityActions.forEach(a -> a.accept(event));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrePlayerAttackEntity(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        PlayerInventory playerInv = player.getInventory();


        ItemStack heldItem = playerInv.getItem(playerInv.getHeldItemSlot());
        if (!isItem(heldItem)) {
            return;
        }
        event.setCancelled(true);

        leftClickEntityActions.forEach(a -> a.accept(event));
    }

    private boolean isItem(ItemStack item) {
        if (item == null) return false;
        return item.getPersistentDataContainer().has(ITEM_KEY, PersistentDataType.STRING) && item.getPersistentDataContainer().getOrDefault(ITEM_KEY, PersistentDataType.STRING, "").startsWith(uuid.toString());
    }

    @Override
    protected HotbarItem clone() {
        return new HotbarItem(plugin, item, blockClickActions, rightClickEntityActions, leftClickEntityActions, hoverActions, unHoverActions, masqueradedMaterial, originalItemModel, referenceID);
    }

    /**
     * Changes the items properties so the clients don't display the item being used (e.g. when it's a block that it gets placed for a tick on the client). <br>
     * Reach modes prevent targeting the corresponding blocks/entities. Masquerade only changes the base item and model.<br>
     * Switching modes removes this feature's previous changes without clearing unrelated metadata or modifiers.
     */
    public void setNoInteractMode(NoInteractMode mode) {
        Objects.requireNonNull(mode, "mode");
        ItemStack updatedItem = item;
        if (masqueradedMaterial != null) {
            updatedItem.setType(masqueradedMaterial);
        }
        ItemMeta meta = updatedItem.getItemMeta();
        if (masqueradedMaterial != null) {
            meta.setItemModel(originalItemModel);
            masqueradedMaterial = null;
            originalItemModel = null;
        }

        // Remove only this feature's modifiers, including when switching between modes.
        AttributeModifier blockReach = noReachModifier("no_block_reach");
        AttributeModifier entityReach = noReachModifier("no_entity_reach");
        meta.removeAttributeModifier(Attribute.BLOCK_INTERACTION_RANGE, blockReach);
        meta.removeAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, entityReach);
        switch (mode) {
            case Off -> { }
            case NoReach -> {
                meta.addAttributeModifier(Attribute.BLOCK_INTERACTION_RANGE, blockReach);
                meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, entityReach);
            }
            case NoBlockReach -> meta.addAttributeModifier(Attribute.BLOCK_INTERACTION_RANGE, blockReach);
            case NoEntityReach -> meta.addAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE, entityReach);
            case Masquerade -> {
                masqueradedMaterial = updatedItem.getType();
                originalItemModel = meta.getItemModel();
                meta.setItemModel(originalItemModel != null ? originalItemModel : masqueradedMaterial.getKey());
                updatedItem.setType(Material.STICK);
            }
        }
        updatedItem.setItemMeta(meta);
        refreshItemInInventories();
    }

    private AttributeModifier noReachModifier(String key) {
        return new AttributeModifier(new NamespacedKey(plugin, key), -1,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.HAND);
    }

    /**
     * {@link NoInteractMode#Off} -> Restores the original item type/model and removes this feature's reach modifiers.<br>
     * {@link NoInteractMode#NoReach} -> Uses {@link NoInteractMode#NoEntityReach} and {@link NoInteractMode#NoBlockReach}<br>
     * {@link NoInteractMode#NoBlockReach} -> Turns the block reach attribute of the item to 0<br>
     * {@link NoInteractMode#NoEntityReach} -> Turns the entity reach attribute of the item to 0<br>
     * {@link NoInteractMode#Masquerade} -> Changes the item type to a {@link Material#STICK} and sets the display item attribute to the current item type
     */
    public static enum NoInteractMode {
        Off,
        NoReach,
        NoBlockReach,
        NoEntityReach,
        Masquerade
    }
}
