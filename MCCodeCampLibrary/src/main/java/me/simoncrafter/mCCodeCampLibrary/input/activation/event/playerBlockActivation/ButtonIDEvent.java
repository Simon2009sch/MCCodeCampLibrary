package me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation;

import me.simoncrafter.mCCodeCampLibrary.input.activation.event.PlayerBlockIDEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ButtonIDEvent extends Event implements PlayerBlockIDEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String id;
    private final Block block;

    public ButtonIDEvent(@NotNull Player player, String id, Block block) {
        this.player = player;
        this.id = id;
        this.block = block;
    }

    @Override
    public @NotNull Player getPlayer() {
        return player;
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
