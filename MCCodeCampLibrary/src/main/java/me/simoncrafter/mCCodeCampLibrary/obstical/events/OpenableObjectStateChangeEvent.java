package me.simoncrafter.mCCodeCampLibrary.obstical.events;

import me.simoncrafter.mCCodeCampLibrary.internal.events.IDEvent;
import me.simoncrafter.mCCodeCampLibrary.obstical.OpenableState;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class OpenableObjectStateChangeEvent extends IDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private OpenableState oldState;
    private OpenableState newState;

    public OpenableObjectStateChangeEvent(String id, OpenableState oldState, OpenableState newState) {
        super(id);
        this.oldState = oldState;
        this.newState = newState;
    }

    public OpenableState getOldState() {
        return oldState;
    }

    public OpenableState getNewState() {
        return newState;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
