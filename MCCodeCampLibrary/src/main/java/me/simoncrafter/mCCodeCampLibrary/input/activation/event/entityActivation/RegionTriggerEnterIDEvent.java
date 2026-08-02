package me.simoncrafter.mCCodeCampLibrary.input.activation.event.entityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.EntityIDEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RegionTriggerEnterIDEvent extends Event implements EntityIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Entity entity;
    private final String id;

    public RegionTriggerEnterIDEvent(@NotNull Entity entity, String id) {
        this.entity = entity;
        this.id = id;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
