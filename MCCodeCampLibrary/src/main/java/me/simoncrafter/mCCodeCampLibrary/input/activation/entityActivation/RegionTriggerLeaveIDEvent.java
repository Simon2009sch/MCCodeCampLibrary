package me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.EntityIDEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RegionTriggerLeaveIDEvent extends EntityIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public RegionTriggerLeaveIDEvent(@NotNull Entity entity, String id) {
        super(entity, id);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
