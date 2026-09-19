package me.simoncrafter.mCCodeCampLibrary.internal.editor.events;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class PlayerClickEditableObjectEvent extends PlayerEvent {

    private static HandlerList handlerList = new HandlerList();

    private IEditable clicked;
    private ClickType clickType;

    public PlayerClickEditableObjectEvent(@NotNull Player player, IEditable clicked, Player player1, ClickType clickType) {
        super(player);
        this.clicked = clicked;
        this.player = player1;
        this.clickType = clickType;
    }

    public IEditable getClicked() {
        return clicked;
    }

    public ClickType getClickType() {
        return clickType;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
    public enum ClickType {
        RIGHT,
        LEFT
    }
}
