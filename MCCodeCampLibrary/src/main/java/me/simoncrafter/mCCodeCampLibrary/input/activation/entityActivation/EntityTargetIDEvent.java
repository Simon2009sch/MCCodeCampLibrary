package me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.EntityIDEvent;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class EntityTargetIDEvent extends EntityIDEvent {

    private final Entity target;

    protected EntityTargetIDEvent(@NotNull Entity entity, String id, Entity target) {
        super(entity, id);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
