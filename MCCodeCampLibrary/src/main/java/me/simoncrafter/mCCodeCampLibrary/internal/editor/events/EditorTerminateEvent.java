package me.simoncrafter.mCCodeCampLibrary.internal.editor.events;

import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class EditorTerminateEvent extends Event {

    private static HandlerList handlerList = new HandlerList();
    private AEditor editor;

    public EditorTerminateEvent(AEditor editor) {
        this.editor = editor;
    }

    public AEditor getEditor() {
        return editor;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
