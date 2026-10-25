package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.mCCodeCampLibrary.input.activation.ActivationButton;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorManager;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/** Empty button-editor starting point; add editing controls in showHomeDialog. */
public class ButtonEditor extends AEditor {

    private final ActivationButton button;

    public ButtonEditor(Plugin plugin, ActivationButton button) {
        super(plugin);
        this.button = button;
    }

    @Override
    protected void onPlayerClickObjectEvent(PlayerClickEditableObjectEvent event) {
        // Add object-click behavior here when needed.
    }

    @Override
    public void displayDialog(Player player, String path, Map<String, Object> context) {
        if (MCCodeCampLib.getEditorManager().getPlayersCurrentEditor(player) != this) return;
        switch (path) {
            default -> showHomeDialog(player, context);
        }
    }

    private void showHomeDialog(Player player, Map<String, Object> context) {
        GenericQuestion.create(Component.text("Button Editor", NamedTextColor.GOLD, TextDecoration.BOLD))
                // Add your editor buttons here.
                .addButton(Button.create()
                        .text(Component.text("[Back]", NamedTextColor.RED))
                        .addAction(CustomAction.create(this::back)))
                .show(player, getUUID() + ":" + player.getUniqueId());

        HotbarMenu menu = new HotbarMenu(getPlugin());
        // Add your editor items here.
        ItemStack backStack = new ItemStack(Material.BARRIER);
        ItemMeta meta = backStack.getItemMeta();
        meta.customName(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        backStack.setItemMeta(meta);
        HotbarItem backItem = new HotbarItem(getPlugin(), backStack);
        backItem.setNoInteractMode(HotbarItem.NoInteractMode.Masquerade);
        backItem.addBlockClickAction(event -> back(event.getPlayer()));
        menu.setItemAt(8, backItem);
        menu.show(player);
    }

    private void back(Player player) {
        if (MCCodeCampLib.getEditorManager().getPlayersCurrentEditor(player) != this) return;
        MCCodeCampLib.getEditorManager().pop(player);
    }

    @Override
    public void onBlockRegistryUpdate(BlockRegistryUpdateEvent event) {
        super.onBlockRegistryUpdate(event);
        BlockRegistryUpdateEvent.UpdateType type = event.getUpdateType();

        // if button was altered outside of this editor (delete, unload or reload)
        if (event.getRegistryObject() == button && (type == BlockRegistryUpdateEvent.UpdateType.DELETE || type == BlockRegistryUpdateEvent.UpdateType.UNLOAD || type == BlockRegistryUpdateEvent.UpdateType.RELOAD)) {
            EditorManager manager = MCCodeCampLib.getEditorManager();
            for (Player player : getPlayers()) {
                //manager.goTo();
            }
        }
    }
}
