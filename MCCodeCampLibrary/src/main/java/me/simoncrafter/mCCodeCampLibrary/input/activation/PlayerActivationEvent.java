package me.simoncrafter.mCCodeCampLibrary.input.activation;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerActivationEvent extends ActivationEvent {

    private final Player player;

    protected PlayerActivationEvent(@NotNull Player player, String id) {
        super(id);
        this.player = player;
    }

    public @NotNull Player getPlayer() {
        return player;
    }
}
