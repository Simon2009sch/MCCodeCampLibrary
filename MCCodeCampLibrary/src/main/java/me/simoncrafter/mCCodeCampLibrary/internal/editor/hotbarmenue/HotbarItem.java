package me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.function.Consumer;

public class HotbarItem implements Listener, Cloneable {

    private final NamespacedKey ITEM_KEY;

    private Plugin plugin;
    private UUID uuid;
    private ItemStack item;

    private Set<Consumer<PlayerInteractEvent>> blockClickActions = new HashSet<>();
    private Set<Consumer<PlayerInteractEntityEvent>> rightClickEntityActions = new HashSet<>();
    private Set<Consumer<PrePlayerAttackEntityEvent>> leftClickEntityActions = new HashSet<>();
    private Set<Consumer<Player>> hoverActions = new HashSet<>();
    private Set<Consumer<Player>> unHoverActions = new HashSet<>();

    public HotbarItem(Plugin plugin, ItemStack item) {
        this.plugin = plugin;
        this.uuid = UUID.randomUUID();

        ITEM_KEY = new NamespacedKey(plugin, "editor_hotbaritem");

        setItem(item);
    }

    public HotbarItem(Plugin plugin, ItemStack item, Set<Consumer<PlayerInteractEvent>> blockClickActions, Set<Consumer<PlayerInteractEntityEvent>> rightClickEntityActions, Set<Consumer<PrePlayerAttackEntityEvent>> leftClickEntityActions, Set<Consumer<Player>> hoverActions, Set<Consumer<Player>> unHoverActions) {
        this.plugin = plugin;
        this.uuid = UUID.randomUUID();
        this.item = item;
        this.blockClickActions = blockClickActions;
        this.rightClickEntityActions = rightClickEntityActions;
        this.leftClickEntityActions = leftClickEntityActions;
        this.hoverActions = hoverActions;
        this.unHoverActions = unHoverActions;

        ITEM_KEY = new NamespacedKey(plugin, "editor_hotbaritem");

        setItem(item);
    }

    /**
     * @return the item bound to this HotbarItem.
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Sets the item bound to this HotbarItem, tagging it with this HotbarItem's identifier
     * so it can later be recognised by {@link #isItem(ItemStack)}.
     */
    public HotbarItem setItem(ItemStack item) {
        item.editPersistentDataContainer(pdc -> pdc.set(ITEM_KEY, PersistentDataType.STRING, uuid.toString()));
        this.item = item;

        forEachInstance((player, slot, itemToSet) -> {
            player.getInventory().setItem(slot, itemToSet);
        });

        return this;
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
        if (!isItem(heldItem)) {
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
        return item.getPersistentDataContainer().has(ITEM_KEY, PersistentDataType.STRING) && item.getPersistentDataContainer().get(ITEM_KEY, PersistentDataType.STRING).equals(uuid.toString());
    }

    @Override
    protected HotbarItem clone() {
        return new HotbarItem(plugin, item, blockClickActions, rightClickEntityActions, leftClickEntityActions, hoverActions, unHoverActions);
    }
}
