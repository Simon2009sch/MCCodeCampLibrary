package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.def.AbstractQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.EditorTerminateEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AEditor implements Listener {

    private final UUID EDITOR_UUID = UUID.randomUUID();
    private final Plugin plugin;

    private Set<Player> players = new HashSet<>();
    private Map<UUID, IEditable> editableObjects = new HashMap<>();

    protected HotbarMenu hotbarMenu = null;
    protected AbstractQuestion<?> question = null;

    public AEditor(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setEditableObjects(Collection<IEditable> editableObjects) {
        // to lazy to learn collection
        Map<UUID, IEditable> newMap = new HashMap<>();
        for (IEditable e : editableObjects) {
            newMap.put(e.getUUID(), e);
        }
        setEditableObjects(newMap);
    }

    public void setEditableObjects(Map<UUID, IEditable> editableObjects) {
        this.editableObjects = editableObjects;
        editableObjects.forEach((u, e) -> {
            registerCallbacksWithIEditable(e);
        });
    }

    public void addEditableObject(IEditable editable) {
        editableObjects.put(editable.getUUID(), editable);
        registerCallbacksWithIEditable(editable);
    }

    protected void join(Player player) {
        players.add(player);
        editableObjects.forEach((u, e) -> e.showFor(player));

        // on first player
        if (players.size() == 1) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        if (hotbarMenu != null) hotbarMenu.show(player);
        if (question != null) question.show(player, EDITOR_UUID.toString());
    }

    protected void leave(Player player) {
        players.remove(player);
        editableObjects.forEach((u, e) -> e.hideFor(player));

        // on last player
        if (players.isEmpty()) {
            HandlerList.unregisterAll(this);
        }

        if (hotbarMenu != null) hotbarMenu.exit(player);
    }

    public UUID getUUID() {
        return EDITOR_UUID;
    }

    public Map<UUID, IEditable> getEditableObjects() {
        return new HashMap<>(editableObjects);
    }

    private void registerCallbacksWithIEditable(IEditable e) {
        e.registerLeftClickCallback(EDITOR_UUID, this::onLeftClick);
        e.registerRightClickCallback(EDITOR_UUID, this::onRightClick);
    }

    private void unregisterCallbacksWithIEditable(IEditable e) {
        e.unregisterLeftClickCallback(EDITOR_UUID);
        e.unregisterRightClickCallback(EDITOR_UUID);
    }

    protected abstract void onLeftClick(Player player, UUID clicked);

    protected abstract void onRightClick(Player player, UUID clicked);

    public void terminate() {
        new EditorTerminateEvent(this);
    }
}
