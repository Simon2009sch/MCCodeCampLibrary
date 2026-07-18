package me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.entityTargetEntityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation.EntityTargetIDEvent;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EntityLeftClickIDEvent extends EntityTargetIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public EntityLeftClickIDEvent(@NotNull Entity entity, String id, Entity target) {
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
