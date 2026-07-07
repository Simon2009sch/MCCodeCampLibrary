package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class EntityActivationEvent extends ActivationEvent {

    private final Entity entity;

    protected EntityActivationEvent(@NotNull Entity entity, String id) {
        super(id);
        this.entity = entity;
    }

    public @NotNull Entity getEntity() {
        return entity;
    }
}
