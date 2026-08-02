package me.simoncrafter.mCCodeCampLibrary.input.activation.event.blockActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.BlockIDEvent;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PressurePlateDeactivationEvent extends Event implements BlockIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final String id;
    private final Block block;

    public PressurePlateDeactivationEvent(String id, Block block) {
        this.id = id;
        this.block = block;
    }

    @Override
    public String getID() {
        return id;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
