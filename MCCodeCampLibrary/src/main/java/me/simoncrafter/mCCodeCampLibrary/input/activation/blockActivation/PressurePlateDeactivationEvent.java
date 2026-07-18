package me.simoncrafter.mCCodeCampLibrary.input.activation.blockActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.BlockIDEvent;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PressurePlateDeactivationEvent extends BlockIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public PressurePlateDeactivationEvent(String id, Block block) {
        super(id, block);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
