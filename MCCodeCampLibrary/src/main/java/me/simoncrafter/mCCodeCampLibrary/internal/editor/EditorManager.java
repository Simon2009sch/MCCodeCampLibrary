package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.EditorTerminateEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerRequestEditorNavigationEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Owns registered editor instances and the editor session of each online player.
 * Editors are addressed by a stable string ID; callers do not need to retain an
 * editor instance after registration.
 */
public class EditorManager implements Listener {

    private final Map<UUID, EditorSession> sessions = new HashMap<>();
    private final Map<String, AEditor> editorsById = new HashMap<>();
    private final Map<AEditor, String> idsByEditor = new IdentityHashMap<>();
    private final Map<AEditor, UUID> editorOwners = new IdentityHashMap<>();

    public EditorManager(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Registers a general-purpose editor. The ID must be unique for this manager.
     */
    public boolean registerEditor(String id, AEditor editor) {
        return registerEditor(id, editor, null);
    }

    /**
     * Registers an editor which is owned by one editable object.
     * The manager terminates this editor when that object is deleted or unloaded.
     */
    public boolean registerEditor(String id, AEditor editor, @Nullable IEditable owner) {
        if (id == null || id.isBlank() || editor == null
                || editorsById.containsKey(id) || idsByEditor.containsKey(editor)) {
            return false;
        }
        editorsById.put(id, editor);
        idsByEditor.put(editor, id);
        if (owner != null) editorOwners.put(editor, owner.getUUID());
        return true;
    }

    @Nullable
    public AEditor getEditor(String id) {
        return id == null ? null : editorsById.get(id);
    }

    @Nullable
    public String getEditorId(AEditor editor) {
        return editor == null ? null : idsByEditor.get(editor);
    }

    public boolean put(Player player, String editorId) {
        AEditor editor = getEditor(editorId);
        if (editor == null) return false;
        sessionFor(player).put(editor);
        return true;
    }

    public boolean put(Player player, AEditor editor) {
        String editorId = getEditorId(editor);
        return editorId != null && put(player, editorId);
    }

    @Nullable
    public AEditor pop(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        return session == null ? null : session.pop();
    }

    public boolean setEditor(Player player, String editorId) {
        if (editorId.isEmpty()) sessionFor(player).setEditor(null);
        AEditor editor = getEditor(editorId);
        if (editor == null) return false;
        sessionFor(player).setEditor(editor);
        return true;
    }

    public boolean setEditor(Player player, AEditor editor) {
        String editorId = getEditorId(editor);
        return editorId != null && setEditor(player, editorId);
    }

    public void leaveEditor(Player player) {
        EditorSession session = sessions.remove(player.getUniqueId());
        if (session != null) session.setEditor(null);
    }

    /**
     * Unwinds to {@code parentId}, then optionally enters {@code childId}.
     * A null child ID intentionally means "remain in the parent".
     */
    public int goTo(Player player, String parentId, @Nullable String childId) {
        AEditor parent = getEditor(parentId);
        AEditor child = childId == null ? null : getEditor(childId);
        if (parent == null || (childId != null && child == null)) return 0;
        return sessionFor(player).goTo(parent, child);
    }

    public int goTo(Player player, AEditor parent, @Nullable AEditor child) {
        String parentId = getEditorId(parent);
        String childId = child == null ? null : getEditorId(child);
        return parentId == null || (child != null && childId == null) ? 0 : goTo(player, parentId, childId);
    }

    public boolean select(Player player, IEditable editable) {
        EditorSession session = sessions.get(player.getUniqueId());
        return session != null && session.select(editable);
    }

    public boolean deselect(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        return session != null && session.deselect();
    }

    @Nullable
    public IEditable getPlayerSelection(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        return session == null ? null : session.getPlayerSelection();
    }

    @Nullable
    public AEditor getPlayersCurrentEditor(Player player) {
        EditorSession session = sessions.get(player.getUniqueId());
        return session == null ? null : session.getCurrentEditor();
    }

    @Nullable
    public IEditable getPlayerSelection(Player player, AEditor editor) {
        EditorSession session = sessions.get(player.getUniqueId());
        return session == null ? null : session.getPlayerSelection(editor);
    }

    /** Terminates a registered editor and removes it from every player session. */
    public boolean terminateEditor(String editorId) {
        AEditor editor = getEditor(editorId);
        if (editor == null) return false;
        editorsById.remove(editorId);
        idsByEditor.remove(editor);
        editorOwners.remove(editor);
        sessions.values().forEach(session -> session.onEditorTerminate(editor));
        new EditorTerminateEvent(editor).callEvent();
        return true;
    }

    public boolean terminateEditor(AEditor editor) {
        String editorId = getEditorId(editor);
        return editorId != null && terminateEditor(editorId);
    }

    private EditorSession sessionFor(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), ignored -> new EditorSession(player));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        leaveEditor(event.getPlayer());
    }

    /**
     * The registry publishes lifecycle events. A deleted/unloaded editable is
     * removed from all editor views, deselected for all players, and terminates
     * any object-specific editor registered with it as owner.
     */
    @EventHandler
    public void onBlockRegistryUpdate(BlockRegistryUpdateEvent event) {
        if (event.getUpdateType() != BlockRegistryUpdateEvent.UpdateType.UNLOAD
                && event.getUpdateType() != BlockRegistryUpdateEvent.UpdateType.DELETE) {
            return;
        }

        UUID editableId = event.getRegistryObject().getUUID();
        sessions.values().forEach(session -> session.onEditableUnload(editableId));
        new ArrayList<>(editorsById.values()).forEach(editor -> editor.removeEditableObject(editableId));
        new ArrayList<>(editorOwners.entrySet()).forEach(entry -> {
            if (entry.getValue().equals(editableId)) terminateEditor(entry.getKey());
        });
    }

    @EventHandler
    public void onEditorTerminate(EditorTerminateEvent event) {
        sessions.values().forEach(session -> session.onEditorTerminate(event.getEditor()));
    }

    @EventHandler
    public void onPlayerRequestEditorNavigation(PlayerRequestEditorNavigationEvent event) {
        Player player = event.getPlayer();
        AEditor requestedEditor = event.getRequestedEditor();
        AEditor currentEditor = event.getCurrentEditor();

        if (currentEditor == null && requestedEditor != null) {
            put(player, event.getRequestedEditor());
        } else if (currentEditor != null && requestedEditor != null) {
            goTo(player, currentEditor, requestedEditor);
        }

        if (event.getRequestedPath() != null) {
            sessionFor(player).setDialogPath()
        }
    }
}
