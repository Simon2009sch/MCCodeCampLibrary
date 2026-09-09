package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Editor implements Listener {

    private final UUID EDITOR_UUID = UUID.randomUUID();
    private final Plugin plugin;

    private Map<Player, IEditable> playerSelection = new HashMap<>();
    private Map<UUID, IEditable> editableObjects = new HashMap<>();

    private HotbarMenu hotbarMenu;

    public Editor(Plugin plugin) {
        this.plugin = plugin;
    }

    public void join(Player player) {
        playerSelection.put(player, null);
        editableObjects.forEach((u, e) -> e.showFor(player));

        // on first player
        if (playerSelection.size() == 1) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }
    }

    public void leave(Player player) {
        playerSelection.remove(player);
        editableObjects.forEach((u, e) -> e.hideFor(player));

        // on last player
        if (playerSelection.isEmpty()) {
            HandlerList.unregisterAll(this);
        }
    }

    public UUID getUUID() {
        return EDITOR_UUID;
    }
}
