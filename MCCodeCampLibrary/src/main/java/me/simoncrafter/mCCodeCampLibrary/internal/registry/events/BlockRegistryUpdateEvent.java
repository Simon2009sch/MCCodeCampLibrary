package me.simoncrafter.mCCodeCampLibrary.internal.registry.events;

import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class BlockRegistryUpdateEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private UpdateType updateType;
    private IBlockRegestryObject registryObject;

    public BlockRegistryUpdateEvent(UpdateType updateType, IBlockRegestryObject registryObject) {
        this.updateType = updateType;
        this.registryObject = registryObject;
    }

    public UpdateType getUpdateType() {
        return updateType;
    }

    public IBlockRegestryObject getRegistryObject() {
        return registryObject;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public enum UpdateType {
        DELETE,
        CREATE,
        UNLOAD,
        RELOAD,
        LOAD,
        EDIT
    }
}
