package me.simoncrafter.mCCodeCampLibrary.internal.editor.events;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerRequestEditorNavigationEvent extends PlayerEvent implements Cancellable {

    private static HandlerList handlerList = new HandlerList();
    private boolean cancelled = false;
    private String requestedPath;
    private String currentPath;
    private AEditor currentEditor;
    private AEditor requestedEditor;

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, AEditor requestedEditor, AEditor currentEditor, String currentPath, String requestedPath) {
        super(player);
        this.requestedEditor = requestedEditor;
        this.currentEditor = currentEditor;
        this.currentPath = currentPath;
        this.requestedPath = requestedPath;
    }

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, AEditor currentEditor, String currentPath, String requestedPath) {
        super(player);
        this.currentEditor = currentEditor;
        this.currentPath = currentPath;
        this.requestedPath = requestedPath;
        requestedEditor = currentEditor;
    }

    public PlayerRequestEditorNavigationEvent(@NotNull Player player, String requestedPath, String currentPath, AEditor requestedEditor) {
        super(player);
        this.requestedPath = requestedPath;
        this.currentPath = currentPath;
        this.requestedEditor = requestedEditor;
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

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

}
