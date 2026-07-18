package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.internal.events.IDEvent;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class EntityIDEvent extends IDEvent {

    private final Entity entity;

    protected EntityIDEvent(@NotNull Entity entity, String id) {
        super(id);
        this.entity = entity;
    }

    public @NotNull Entity getEntity() {
        return entity;
    }
}
