package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.def.AbstractQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.EditorTerminateEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.*;

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
    }

    public void addEditableObject(IEditable editable) {
        IEditable previous = editableObjects.put(editable.getUUID(), editable);
        if (previous == editable) return;
        if (previous != null) players.forEach(previous::hideFor);
        players.forEach(editable::showFor);
    }

    public boolean removeEditableObject(UUID uuid) {
        IEditable removed = editableObjects.remove(uuid);
        if (removed == null) return false;
        players.forEach(removed::hideFor);
        return true;
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


    protected abstract void onPlayerClickObjectEvent(PlayerClickEditableObjectEvent event);


    @EventHandler
    public void onEditorTerminate(EditorTerminateEvent event) {
        if (event.getEditor() != this) return;
    }

    @EventHandler
    public void onPlayerClickEditableObject(PlayerClickEditableObjectEvent event) {
        if (!editableObjects.containsValue(event.getClicked())) {
            return;
        }
        onPlayerClickObjectEvent(event);
    }

    @EventHandler
    public void onBlockRegistryUpdate(BlockRegistryUpdateEvent event) {
        if (!(event.getRegistryObject() instanceof IEditable editable)) return;
        if (event.getUpdateType() == BlockRegistryUpdateEvent.UpdateType.CREATE
                || event.getUpdateType() == BlockRegistryUpdateEvent.UpdateType.LOAD) {
            addEditableObject(editable);
        }
    }

}
