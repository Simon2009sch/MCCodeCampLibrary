package me.simoncrafter.mCCodeCampLibrary.input.editor;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.simoncrafter.CraftersChatDialogs.dialogs.def.AbstractQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.ClearCharAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class WorldMarkerEditor extends AEditor {
    private static final Component EDITOR_HEADLINE = Component.text("World Marker Editor", NamedTextColor.GOLD, TextDecoration.BOLD).appendNewline();

    public WorldMarkerEditor(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected void join(Player player) {
        // Editors without viewers do not listen for registry events; reacquire loaded instances.
        var markers = MCCodeCampLib.getBlockMarkerRegistry().getRegisteredObjects();
        for (UUID uuid : getEditableObjects().keySet()) {
            if (markers.stream().noneMatch(marker -> marker.getUUID().equals(uuid))) removeEditableObject(uuid);
        }
        for (IBlockRegestryObject marker : markers) {
            if (marker instanceof IEditable editable) addEditableObject(editable);
        }
        super.join(player);
        showDialog(player, buildHomeDialog());
    }

    @Override
    protected void leave(Player player) {
        super.leave(player);
    }

    @Override
    protected void onPlayerClickObjectEvent(PlayerClickEditableObjectEvent event) {
        Player player = event.getPlayer();
        if (MCCodeCampLib.getEditorManager().getPlayerSelection(player) != event.getClicked()) {
            MCCodeCampLib.getEditorManager().select(player, event.getClicked());
        } else {
            MCCodeCampLib.getEditorManager().deselect(player);
        }
        // Marker editing is intentionally left to the object-specific editor.
    }



    @Override
    @EventHandler
    public void onBlockRegistryUpdate(BlockRegistryUpdateEvent event) {
        super.onBlockRegistryUpdate(event);
    }

    private boolean isActive(Player player) {
        return MCCodeCampLib.getEditorManager().getPlayersCurrentEditor(player) == this;
    }

    private GenericQuestion dialog(Component message) {
        return GenericQuestion.create(EDITOR_HEADLINE.append(message));
    }

    private Button actionButton(String label, NamedTextColor color, Consumer<Player> action) {
        return Button.create().text(Component.text("[" + label + "]", color, TextDecoration.BOLD))
                .addAction(CustomAction.create(player -> {
                    if (isActive(player)) action.accept(player);
                }));
    }

    private void showDialog(Player player, GenericQuestion dialog) {
        // Never reuse a question or its one-shot buttons between players.
        dialog.show(player, getUUID() + ":" + player.getUniqueId());
    }

    private void showHome(Player player) {
        if (isActive(player)) showDialog(player, buildHomeDialog());
    }

    private GenericQuestion buildHomeDialog() {
        return dialog(Component.text("Choose an option:", NamedTextColor.GRAY).decoration(TextDecoration.BOLD, false))
                .addButton(actionButton("Create", NamedTextColor.GREEN, this::createNewMarkerDialog))
                .addButton(Button.create().text(Component.text("[Edit]", NamedTextColor.GOLD, TextDecoration.BOLD)).setDisabled(true))
                .addButton(actionButton("Delete", NamedTextColor.RED, player -> showDialog(player, buildDeleteMenu())))
                .addButton(buildExitButton());
    }

    private HotbarMenu buildHomeHotbar() {
        HotbarMenu menu = new HotbarMenu(getPlugin());
        menu.setItemAt(2, createHotbarItem(Material.NETHER_STAR,
                Component.text("New Marker", NamedTextColor.GREEN),
                List.of(Component.text("Rightclick on a block or in air to create a new marker in the world", NamedTextColor.GRAY)))
                .addBlockClickAction(e -> {
                    Location loc;
                    if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_AIR || e.getClickedBlock() == null) {
                        loc = null;
                    } else {
                        loc = e.getClickedBlock().getLocation();
                    }
                    buildMarkerIdInput()
                })
        );


        return menu;
    }

    private void createNewMarkerDialog(Player player) {
        GenericQuestion menu = dialog(Component.text("Select the type of marker you want to create:"));
        MCCodeCampLib.getBlockMarkerRegistry().getObjectTypes().forEach((id, type) -> {
            Component name = type instanceof IEditorObjectDescriptor descriptor ? descriptor.getDisplayName() : Component.text(id);
            menu.addButton(Button.create().text(Component.text("[").append(name).append(Component.text("]")))
                    .addAction(CustomAction.create(p -> {
                        if (isActive(p)) buildMarkerIdInput(id, name, null).run(p);
                    })));
        });
        menu.addButton(actionButton("Cancel", NamedTextColor.GRAY, this::showHome));
        showDialog(player, menu);
    }

    private StringWithRulesInputAction buildMarkerIdInput(String type, Component name, Location loc) {
        StringWithRulesInputAction input = StringWithRulesInputAction.create(player -> id -> {
            if (!isActive(player)) return;
            if (!id.matches("[a-zA-Z0-9_]+")) return;
            var registry = MCCodeCampLib.getBlockMarkerRegistry();
            if (registry.hasObject(type, id)) {
                player.sendMessage(Component.text("An object of this type already uses that ID.", NamedTextColor.RED));
                buildMarkerIdInput(type, name, loc).run(player);
                return;
            }
            Location location = loc;
            if (location == null) {
                Block target = player.getTargetBlockExact(4, FluidCollisionMode.NEVER);
                location = target == null ? player.getLocation().getBlock().getLocation() : target.getLocation();
            }
            IBlockRegestryObject created = registry.createObject(type, id, location);
            showHome(player);
            player.sendMessage(Component.text(created == null ? "Could not create marker." : "Added new object: " + id,
                    created == null ? NamedTextColor.RED : NamedTextColor.GREEN));
        }).regexRule("^[a-zA-Z0-9_]+$")
                .prompt(EDITOR_HEADLINE.append(Component.text("Please input the ID of the new ")).append(name)
                        .appendNewline().append(Component.text("Use letters, numbers and underscores. Look at a block within four blocks; otherwise your feet are used. Type cancel to return.", NamedTextColor.GRAY)));
        input.addReTryAction(CustomAction.create(player -> {
                    if (isActive(player)) buildMarkerIdInput(type, name, null).run(player);
                }))
                .addCancelAction(CustomAction.create(this::showHome))
                .addTimeoutAction(CustomAction.create(this::showHome));
        input.reTry(true);
        return input;
    }

    private GenericQuestion buildDeleteMenu() {
        GenericQuestion menu = dialog(Component.text("Select a loaded marker to delete:"));
        MCCodeCampLib.getBlockMarkerRegistry().getRegisteredObjects().stream()
                .sorted(Comparator.comparing(IBlockRegestryObject::getTypeID).thenComparing(IBlockRegestryObject::getID))
                .forEach(marker -> menu.addButton(actionButton(marker.getTypeID() + ": " + marker.getID(), NamedTextColor.RED,
                        player -> showDialog(player, buildDeleteConfirmation(marker.getUUID())))));
        menu.addButton(actionButton("Cancel", NamedTextColor.GRAY, this::showHome));
        return menu;
    }

    private GenericQuestion buildDeleteConfirmation(UUID uuid) {
        var marker = MCCodeCampLib.getBlockMarkerRegistry().findRegisteredObject(uuid);
        String label = marker == null ? uuid.toString() : marker.getTypeID() + ": " + marker.getID();
        return dialog(Component.text("Permanently delete " + label + "?", NamedTextColor.RED))
                .addButton(actionButton("Confirm delete", NamedTextColor.RED, player -> {
                    removeObject(player, uuid);
                }))
                .addButton(actionButton("Cancel", NamedTextColor.GRAY, this::showHome));
    }

    private Button buildExitButton() {
        return Button.create()
                .text(Component.text("[Exit]", NamedTextColor.RED, TextDecoration.BOLD))
                .addAction(CustomAction.create(p -> {
                    exitPlayer(p);
                }));
    }

    private void removeObject(Player player, UUID uuid) {
        var registry = MCCodeCampLib.getBlockMarkerRegistry();
        // Resolve by UUID again: an unloaded/deleted marker must not delete a replacement with the same ID.
        if (registry.findRegisteredObject(uuid) == null) {
            showHome(player);
            player.sendMessage(Component.text("This marker is no longer loaded.", NamedTextColor.RED));
            return;
        }
        registry.removeObject(uuid);
        showHome(player);
        boolean removed = registry.findRegisteredObject(uuid) == null;
        player.sendMessage(Component.text(removed ? "Deleted marker." : "Could not delete marker.",
                removed ? NamedTextColor.GREEN : NamedTextColor.RED));
    }

    private void exitPlayer(Player player) {
        MCCodeCampLib.getEditorManager().leaveEditor(player);
    }

    private HotbarItem createHotbarItem(Material type, Component name, List<Component> lore) {
        ItemStack itemStack = new ItemStack(type);
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(name);
        if (!(lore != null && lore.isEmpty())) meta.lore(lore);
        itemStack.setItemMeta(meta);
        return new HotbarItem(getPlugin(), itemStack);
    }

    @Override
    public void displayDialog(Player player, String path) {

    }
}
