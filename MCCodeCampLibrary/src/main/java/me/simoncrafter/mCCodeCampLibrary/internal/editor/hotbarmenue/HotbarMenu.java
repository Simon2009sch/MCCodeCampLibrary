package me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue;

import io.papermc.paper.event.player.PlayerPickBlockEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

public class HotbarMenu implements Listener {

    private static Set<HotbarMenu> instances = new HashSet<>();

    private final NamespacedKey PLAYER_TAG;
    private final NamespacedKey PLAYER_HOTBAR_SAVE_TAG;

    private Plugin plugin;

    private UUID uuid;
    private List<Player> players = new ArrayList<>();
    private HotbarItem[] items = new HotbarItem[9];
    private Set<Consumer<InventoryScrollEvent>> scrollActions = new HashSet<>();
    private Set<UUID> suppressedHeldSlotChanges = new HashSet<>();

    /**
     * The direction a player scrolled their hotbar selection in.
     */
    public enum ScrollDirection {
        UP,
        DOWN
    }

    /**
     * Describes a hotbar selection change caused by scrolling (or the equivalent number-key press).
     */
    public record InventoryScrollEvent(Player player, ScrollDirection direction, int previousSlot, int newSlot) {
    }

    public HotbarMenu(Plugin plugin) {
        this.plugin = plugin;
        PLAYER_TAG = new NamespacedKey(plugin, "player_in_hotbarmenue");
        PLAYER_HOTBAR_SAVE_TAG = new NamespacedKey(plugin, "player_hotbar_save_tag");
        uuid = UUID.randomUUID();
    }

    public void setItemAt(int index, HotbarItem item) {
        items[index] = item.clone();
    }

    public void show(Player player) {
        if (!player.getPersistentDataContainer().has(PLAYER_TAG)) {
            saveHotbar(player);
            player.getPersistentDataContainer().set(PLAYER_TAG, PersistentDataType.STRING, uuid.toString());
        }
        for (HotbarMenu m : instances) {
            m.onMenuChange(player);
        }
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) {
                player.getInventory().setItem(i, new ItemStack(Material.AIR));
                continue;
            };
            Bukkit.getPluginManager().registerEvents(items[i], plugin);
            player.getInventory().setItem(i, items[i].getItem());
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        players.add(player);
        instances.add(this);
    }

    /**
     * Gets called every time the menu instance of a player changes.
     */
    protected void onMenuChange(Player player) {
        players.remove(player);
        unregisterIfEmpty();
    }

    public void exit(Player player) {
        if (!players.contains(player)) return;
        if (player.getPersistentDataContainer().has(PLAYER_TAG)) {
            reloadHotbar(player);
            player.getPersistentDataContainer().remove(PLAYER_TAG);
        }
        players.remove(player);
        unregisterIfEmpty();
    }

    public static void onDisablePlugin() {
        for (HotbarMenu m : instances) {
            for (Player player : m.players) {
                m.exit(player);
            }
        }
    }

    private void unregisterIfEmpty() {
        if (players.isEmpty()) {
            instances.remove(this);
            for (HotbarItem item : items) {
                if (item == null) continue;
                HandlerList.unregisterAll(item);
            }
            HandlerList.unregisterAll(this);
        }
    }

    private void saveHotbar(Player player) {
        PlayerInventory inventory = player.getInventory();
        StringBuilder dataString = new StringBuilder();

        for (int i = 0; i < 9; i++) {
            if (i > 0) {
                dataString.append(';');
            }

            ItemStack item = inventory.getItem(i);
            if (item != null) {
                dataString.append(Base64.getEncoder().encodeToString(item.serializeAsBytes()));
            }
        }

        player.getPersistentDataContainer().set(PLAYER_HOTBAR_SAVE_TAG, PersistentDataType.STRING, dataString.toString());
    }

    private void reloadHotbar(Player player) {
        String dataString = player.getPersistentDataContainer().get(PLAYER_HOTBAR_SAVE_TAG, PersistentDataType.STRING);
        if (dataString == null) {
            return;
        }

        String[] slots = dataString.split(";", -1);
        PlayerInventory inventory = player.getInventory();

        for (int i = 0; i < slots.length; i++) {
            ItemStack item = slots[i].isEmpty() ? null : ItemStack.deserializeBytes(Base64.getDecoder().decode(slots[i]));
            inventory.setItem(i, item);
        }
    }

    /**
     * Adds an action to run when a player in this menu scrolls their hotbar selection.
     */
    public HotbarMenu addScrollAction(Consumer<InventoryScrollEvent> action) {
        scrollActions.add(action);
        return this;
    }

    /**
     * @return an unmodifiable view of the scroll actions.
     */
    public Set<Consumer<InventoryScrollEvent>> getScrollActions() {
        return Collections.unmodifiableSet(scrollActions);
    }

    /**
     * Removes all scroll actions.
     */
    public HotbarMenu clearScrollActions() {
        scrollActions.clear();
        return this;
    }

    /**
     * Sets the player's held hotbar slot without triggering this menu's own
     * {@link #onPlayerItemHeld(PlayerItemHeldEvent)} handling for the resulting event
     * (setHeldItemSlot fires {@link PlayerItemHeldEvent} synchronously, which would
     * otherwise be misread as a second, player-initiated scroll).
     */
    public void setHeldItemSlotQuietly(Player player, int slot) {
        suppressedHeldSlotChanges.add(player.getUniqueId());
        player.getInventory().setHeldItemSlot(slot);
    }

    /**
     * @return Returns a hotbar item, that when clicked returns the player to this
     */
    public HotbarItem createLoopbackItem(ItemStack item) {
        HotbarItem hItem = new HotbarItem(plugin, item);
        hItem.addBlockClickAction(e -> {
            this.show(e.getPlayer());
        });
        return hItem;
    }

    /**
     * @return Returns a hotbar item that exits the entire menu for the player
     */
    public HotbarItem createExitItem(ItemStack item) {
        HotbarItem hItem = new HotbarItem(plugin, item);
        hItem.addBlockClickAction(e -> {
            this.exit(e.getPlayer());
        });
        return hItem;
    }



    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (event.getInventory().getHolder() instanceof Player player && players.contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (players.contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof Player player && players.contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!players.contains(player) || suppressedHeldSlotChanges.contains(player.getUniqueId())) {
            suppressedHeldSlotChanges.remove(player.getUniqueId());
            return;
        }
        if (items[event.getNewSlot()] != null) {
            items[event.getNewSlot()].onHover(event.getPlayer());
        }
        if (items[event.getPreviousSlot()] != null) {
            items[event.getPreviousSlot()].onUnHover(event.getPlayer());
        }


        int previousSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();

        ScrollDirection direction;
        if (previousSlot == 8 && newSlot == 0) {
            direction = ScrollDirection.UP;
        } else if (previousSlot == 0 && newSlot == 8) {
            direction = ScrollDirection.DOWN;
        } else if (newSlot > previousSlot) {
            direction = ScrollDirection.UP;
        } else {
            direction = ScrollDirection.DOWN;
        }

        InventoryScrollEvent scrollEvent = new InventoryScrollEvent(player, direction, previousSlot, newSlot);
        scrollActions.forEach(a -> a.accept(scrollEvent));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        exit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        exit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerPickBlock(PlayerPickBlockEvent event) {
        if (players.contains(event.getPlayer())) event.setCancelled(true);
    }
}
