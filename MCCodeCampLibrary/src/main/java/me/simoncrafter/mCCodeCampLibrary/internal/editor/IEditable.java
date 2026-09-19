package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.function.BiConsumer;

public interface IEditable {

    /**
     * Called when player enters editor, in general, when this object should be shown to the player<br>
     * Should initialize display (if not already) and show to player
     * @param player
     */
    void showFor(Player player);

    /**
     * Called when player leaves editor, in general, when this object should be no longer shown to a player.<br>
     * Should hide the display from the player. If shown to no more players, clean up display instance
     * @param player
     */
    void hideFor(Player player);

    /**
     * Called when editor deems that the player wants to open the object specific editor
     * @param player The player requesting to open the editor
     */
    default void openEditor(Player player) {
        player.sendMessage(Component.text("This object hasn't implemented an editor!", NamedTextColor.RED));
    }

    /**
     * Is called when the editor deems that the player wants to select this object<br>
     * This should show the selection display and any other related visuals
     * @param player The player in question
     */
    void select(Player player);

    /**
     * Is called when the editor deems that the player wants to deselect this object<br>
     * This should hide the selection display and any other visuals
     * @param player The player in question
     */
    void deselect(Player player);


    /**
     * Returns the UUID of this object. Can be just for editing or just a general uuid of this object
     * @return
     */
    UUID getUUID();
}
