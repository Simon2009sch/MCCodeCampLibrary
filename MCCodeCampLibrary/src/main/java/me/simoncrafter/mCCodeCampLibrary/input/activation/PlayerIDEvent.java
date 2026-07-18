package me.simoncrafter.mCCodeCampLibrary.input.activation;

import me.simoncrafter.mCCodeCampLibrary.internal.events.IDEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerIDEvent extends IDEvent {

    private final Player player;

    protected PlayerIDEvent(@NotNull Player player, String id) {
        super(id);
        this.player = player;
    }

    public @NotNull Player getPlayer() {
        return player;
    }
}
