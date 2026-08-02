package me.simoncrafter.mCCodeCampLibrary.input.activation.event.entityActivation.entityTargetEntityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.entityActivation.EntityTargetIDEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityLeftClickIDEvent extends Event implements EntityTargetIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Entity entity;
    private final String id;
    private final Entity target;

    public EntityLeftClickIDEvent(@NotNull Entity entity, String id, Entity target) {
        this.entity = entity;
        this.id = id;
        this.target = target;
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
    public Entity getTarget() {
        return target;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
