package me.simoncrafter.mCCodeCampLibrary.input.activation.entityBlockActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.EntityBlockActivationEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PressurePlateActivationEventEntity extends EntityBlockActivationEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PressurePlateActivationEventEntity(@NotNull Entity entity, String id, Block block) {
        super(entity, id, block);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
