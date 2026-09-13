package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.EditorTerminateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages sessions.
 */
public class EditorManager implements Listener {

    private final Plugin plugin;

    private Map<Player, EditorSession> sessions = new HashMap<>();


    public EditorManager(Plugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void put(Player player, AEditor editor) {
        sessions.get(player).put(editor);
    }

    public AEditor pop(Player player) {
        return sessions.get(player).pop();
    }

    /**
     * Moves the player to the editor while safely removing them in all the ones they where in the child-parent tree.
     * @param player The player in question
     * @param parent The parent of the editor you want to go to.
     * @param child The target editor you want to go to. If null stays in parent editor
     * @return Returns the number of steps it went outside of an editor
     */
    public int goTo(Player player, AEditor parent, AEditor child) {
        return sessions.get(player).goTo(parent, child);
    }

    public boolean select(Player player, IEditable editable) {
        return sessions.get(player).select(editable);
    }

    public boolean deselect(Player player) {
        return sessions.get(player).deselect();
    }

    public void leaveEditor(Player player) {
        setEditor(player, null);
    }

    public void setEditor(Player player, AEditor editor) {
        sessions.get(player).setEditor(editor);
    }

    /**
     * Returns the current selection of the player
     * @param player The player in question
     * @return The IEditable object. Null if player has nothing selected
     */
    public IEditable getPlayerSelection(Player player) {
        return sessions.get(player).getPlayerSelection();
    }

    public AEditor getPlayersCurrentEditor(Player player) {
        return sessions.get(player).getCurrentEditor();
    }

    /**
     * Returns the selection of a player in a specific editor
     * @param player The player in question
     * @param editor The editor to search in
     * @return Returns the IEditable object. If editor isn't in players stack or player hasn't selected anything returns NULL
     */
    public IEditable getPlayerSelection(Player player, AEditor editor) {
        return sessions.get(player).getPlayerSelection(editor);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        sessions.get(event.getPlayer()).setEditor(null);
    }

    @EventHandler
    public void onEditorTerminate(EditorTerminateEvent event) {
        sessions.forEach((p, s) -> {
            s.onEditorTerminate(event.getEditor());
        });
    }
}
