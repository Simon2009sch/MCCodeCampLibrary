package me.simoncrafter.mCCodeCampLibrary.input.activation.playerBlockActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.PlayerBlockActivationEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ButtonActivationEvent extends PlayerBlockActivationEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public ButtonActivationEvent(@NotNull Player player, String id, Block block) {
        super(player, id, block);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
