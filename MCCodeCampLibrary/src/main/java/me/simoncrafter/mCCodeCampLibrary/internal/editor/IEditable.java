package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.def.AbstractQuestion;
import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public interface IEditable {

    void showFor(Player player);
    void hideFor(Player player);

    void openEditor(Player player);

    void showSelection(Player player);
    void hideSelection(Player player);
}
