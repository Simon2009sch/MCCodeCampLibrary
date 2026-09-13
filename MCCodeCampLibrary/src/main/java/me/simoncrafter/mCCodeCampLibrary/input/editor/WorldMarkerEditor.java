package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.SelectionButton;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class WorldMarkerEditor extends AEditor {

    private final static Component EDITOR_HEADLINE = Component.text("World Marker Editor", NamedTextColor.GOLD, TextDecoration.BOLD).appendNewline();



    public WorldMarkerEditor(Plugin plugin) {
        super(plugin);
        question = GenericQuestion.create(EDITOR_HEADLINE
                        .append(MiniMessage.miniMessage()
                        .deserialize("<grey>You can edit the world with your hotbar or by <white>choosing one of these options:<reset>")))
                .addButton(Button.create()
                        .text(Component.text("[Create]", NamedTextColor.GREEN, TextDecoration.BOLD))
                        .addAction(CustomAction.create(this::createNewMarkerDialog))
                )
                .addButton(Button.create()
                        .text(Component.text("[Edit]", NamedTextColor.GOLD, TextDecoration.BOLD))
                        .setDisabled(true)
                )
                .addButton(Button.create()
                        .text(Component.text("[Delete]", NamedTextColor.RED, TextDecoration.BOLD))
                        .setDisabled(true)
                );
    }

    @Override
    protected void onLeftClick(Player player, UUID clicked) {

    }

    @Override
    protected void onRightClick(Player player, UUID clicked) {

    }

    private void createNewMarkerDialog(Player player) {

        Map<Player, String> playerInputs = new HashMap<>();

        CustomAction returnToHomeQuestion = CustomAction.create(question::show);
        GenericQuestion markerCreationDialog = GenericQuestion.create(
                EDITOR_HEADLINE.append(MiniMessage.miniMessage().deserialize("<white>Select the type of marker you want to create:")));
        MCCodeCampLib.getBlockMarkerRegistry().getObjectTypes().forEach((id, obj) -> {
            Component text;
            if (obj instanceof IEditorObjectDescriptor desc) {
                text = desc.getDescription();
            } else {
                text = Component.text(id);
            }

            StringWithRulesInputAction idInputAction = StringWithRulesInputAction.create(p -> s -> playerInputs.put(p, s))
                    .regexRule("^[a-zA-Z0-9_]*$")
                    .prompt(EDITOR_HEADLINE.append(Component.text("Please input the ID of the new ")
                                    .append(text))
                            .appendNewline()
                            .append(Component.text("Can contain numbers, letters(upper and lowercase) and Underscores", NamedTextColor.GRAY))
                            .appendNewline()
                            .append(Component.text("The block you are looking at at the point of sending will be chosen as the new location of the button. If no block is looked at, your players feet will be taken.", NamedTextColor.GRAY))
                    );
            idInputAction.addReTryAction(CustomAction.create(idInputAction::run))
                    .addCancelAction(returnToHomeQuestion)
                    .addTimeoutAction(returnToHomeQuestion)
                    .addSuccessAction(CustomAction.create(p -> {
                        Block targetBlock = player.getTargetBlockExact(4, FluidCollisionMode.NEVER);
                        Location loc = targetBlock == null ? player.getLocation().getBlock().getLocation() : targetBlock.getLocation();
                        MCCodeCampLib.getBlockMarkerRegistry().createObject(id, playerInputs.get(player), loc);
                    }));

            markerCreationDialog.addButton(Button.create()
                    .text(Component.text("[")
                            .append(text)
                            .append(Component.text("]")))
                    .addAction(idInputAction));
        });

    }

}
