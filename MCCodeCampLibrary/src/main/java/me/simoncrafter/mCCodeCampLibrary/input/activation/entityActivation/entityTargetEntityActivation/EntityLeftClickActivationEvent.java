package me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.EntityTargetActivationEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityLeftClickActivationEvent extends EntityTargetActivationEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public EntityLeftClickActivationEvent(@NotNull Entity entity, String id, Entity target) {
        super(entity, id, target);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
