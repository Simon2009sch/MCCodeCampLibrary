package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface IEditorObjectDescriptor {
    /**
     * @return Returns the description of this object
     */
    default String getDescription() {
        return getDisplayKey("description");
    }

    /**
     * @return Returns the displayname this object should be seen under in editors
     */
    default String getDisplayName() {
        return getDisplayKey("displayname");
    }


    /**
     * @return Returns the item id (e.g. minecraft:dirt) for displaying
     */
    default String getDisplayItem() {
        return getDisplayKey("displayItem");
    }

    /**
     * Returns the component with that key
     * @param key The "search query"
     * @return The text component that was found. If not found, returns empty string
     */
    @NotNull String getDisplayKey(String key);
}
