package me.simoncrafter.mCCodeCampLibrary.input.activation.entityActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.EntityActivationEvent;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class EntityTargetActivationEvent extends EntityActivationEvent {

    private final Entity target;

    protected EntityTargetActivationEvent(@NotNull Entity entity, String id, Entity target) {
        super(entity, id);
        this.target = target;
    }

    public Entity getTarget() {
        return target;
    }
}
