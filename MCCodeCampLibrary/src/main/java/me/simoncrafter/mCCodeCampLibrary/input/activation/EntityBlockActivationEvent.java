package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public abstract class EntityBlockActivationEvent extends EntityActivationEvent {

    private final Block block;

    public EntityBlockActivationEvent(@NotNull Entity entity, String id, Block block) {
        super(entity, id);
        this.block = block;
    }

    public Block getBlock() {
        return block;
    }
}
