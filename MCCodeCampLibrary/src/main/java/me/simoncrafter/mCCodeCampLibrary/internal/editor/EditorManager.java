package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages sessions.
 */
public class EditorManager {

    private final Plugin plugin;

    private Map<Player, EditorSession> sessions = new HashMap<>();


    public EditorManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void put(Player player, Editor editor) {
        sessions.get(player).put(editor);
    }

    public Editor pop(Player player) {
        return sessions.get(player).pop();
    }

    public int goTo(Player player, Editor parent, Editor child) {
        return sessions.get(player).goTo(parent, child);
    }

    /**
     * Returns the current selection of the player
     * @param player The player in question
     * @return The IEditable object. Null if player has nothing selected
     */
    public IEditable getPlayerSelection(Player player) {
        return sessions.get(player).getPlayerSelection();
    }

    /**
     * Returns the selection of a player in a specific editor
     * @param player The player in question
     * @param editor The editor to search in
     * @return Returns the IEditable object. If editor isn't in players stack or player hasn't selected anything returns NULL
     */
    public IEditable getPlayerSelection(Player player, Editor editor) {
        return sessions.get(player).getPlayerSelection(editor);
    }

}
