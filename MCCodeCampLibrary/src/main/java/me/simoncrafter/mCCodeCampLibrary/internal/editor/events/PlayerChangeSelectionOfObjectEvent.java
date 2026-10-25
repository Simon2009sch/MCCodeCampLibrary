package me.simoncrafter.mCCodeCampLibrary.internal.editor.events;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerChangeSelectionOfObjectEvent extends PlayerEvent {

    private static HandlerList handlerList = new HandlerList();

    private IEditable object;
    private Action action;

    public PlayerChangeSelectionOfObjectEvent(@NotNull Player player, IEditable object, Action action) {
        super(player);
        this.object = object;
        this.action = action;
    }

    public IEditable getObject() {
        return object;
    }

    public Action getAction() {
        return action;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
    public enum Action {
        SELECT,
        DESELECT
    }
}
