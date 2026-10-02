package me.simoncrafter.mCCodeCampLibrary.internal.editor.events;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerRequestEditorNavigationEvent extends PlayerEvent implements Cancellable {

    private static HandlerList handlerList = new HandlerList();
    private boolean cancelled = false;
    private final String requestedPath;
    private final String currentPath;
    private final AEditor currentEditor;
    private final AEditor requestedEditor;
    private final Map<String, Object> eventContext;

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, String requestedPath, String currentPath, AEditor currentEditor, AEditor requestedEditor, Map<String, Object> eventContext) {
        super(player);
        this.requestedPath = requestedPath;
        this.currentPath = currentPath;
        this.currentEditor = currentEditor;
        this.requestedEditor = requestedEditor;
        this.eventContext = eventContext;
    }

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, String requestedPath, AEditor requestedEditor) {
        super(player);
        this.requestedPath = requestedPath;
        this.requestedEditor = requestedEditor;
        this.currentEditor = null;
        this.currentPath = "";
        this.eventContext = new HashMap<>();
    }

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, String requestedPath, AEditor requestedEditor, Map<String, Object> eventContext) {
        super(player);
        this.requestedPath = requestedPath;
        this.requestedEditor = requestedEditor;
        this.eventContext = eventContext;
        this.currentPath = "";
        this.currentEditor = null;
    }

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, String requestedPath, Map<String, Object> eventContext) {
        super(player);
        this.requestedPath = requestedPath;
        this.eventContext = eventContext;
        this.currentPath = "";
        this.currentEditor = null;
        this.requestedEditor = null;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }

    public String getRequestedPath() {
        return requestedPath;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public AEditor getCurrentEditor() {
        return currentEditor;
    }

    public AEditor getRequestedEditor() {
        return requestedEditor;
    }

    public Object getEventContext(String key) {
        return eventContext.get(key);
    }

    public Map<String, Object> getEventContextMap() {
        return new HashMap<>(eventContext);
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

}
