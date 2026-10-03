package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.DisplayOptions.DisplayOption;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.DisplayOptions.DisplayOptions;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.LocationInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfirmQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.StyledRegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerRequestEditorNavigationEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.RegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.Consumer;

public class WorldMarkerEditor extends AEditor {
    private static final Component EDITOR_HEADLINE = Component.empty().append(Component.text("World Marker Editor", NamedTextColor.GOLD, TextDecoration.BOLD)).appendNewline();
    private static final DisplayOption DISPLAY_OPTION = new DisplayOption(DisplayOptions.ColorPalettes.GREEN_ISH, DisplayOptions.SoundOptions.DEFAULT);

    private static final String DIALOG_TYPE_SELECTOR = "type_selector";
    private static final String DIALOG_OBJECT_SELECTOR = "object_selector";
    private static final String DIALOG_CONFIRM_DELETE = "confirm_deletion";
    private static final String DIALOG_ID_INPUT = "id_input";
    private static final String DIALOG_GROUP_LOCATION_INPUT = "location_input";
    private static final String DIALOG_LOCATION_INPUT_SELECTION = DIALOG_GROUP_LOCATION_INPUT + "_selection";
    private static final String DIALOG_LOCATION_INPUT_TYPING = DIALOG_GROUP_LOCATION_INPUT + "_typing";


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
    }

    @Override
    protected void leave(Player player) {
        super.leave(player);
        if (getPlayers().contains(player)) HotbarMenu.forceCloseDialog(player);
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

    private boolean inInThisEditor(Player player) {
        return MCCodeCampLib.getEditorManager().getPlayersCurrentEditor(player) == this;
    }

    private GenericQuestion dialog(Component message) {
        return GenericQuestion.create(EDITOR_HEADLINE.append(message));
    }

    private Button actionButton(String label, TextColor color, Consumer<Player> action) {
        return Button.create().text(Component.text("[" + label + "]", color))
                .addAction(CustomAction.create(player -> {
                    if (inInThisEditor(player)) action.accept(player);
                }));
    }

    private void showDialog(Player player, GenericQuestion dialog) {
        // Never reuse a question or its one-shot buttons between players.
        dialog.show(player, getUUID() + ":" + player.getUniqueId());
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

    private Button homeButton() {
        return actionButton("Home", DISPLAY_OPTION.colorPalette().PRIMARY(), p -> navigate(p, "", new HashMap<>()));
    }

    private Button exitButton() {
        return actionButton("Exit", DISPLAY_OPTION.colorPalette().ERROR(), this::exitPlayer);
    }

    private void showHomeDialog(Player player, Map<String, Object> context) {
        Map<String, Object> clonedContext = new HashMap<>(context);

        Map<String, Object> objectCreationBasicArgumentMap = Map.ofEntries(
                Map.entry("purpose", "object_creation"),
                Map.entry("!" + DIALOG_TYPE_SELECTOR  + "_show_home_button", true),
                Map.entry("!" + DIALOG_GROUP_LOCATION_INPUT + "_only_block_loc", true));

        GenericQuestion.create(createGenericQuestionHeader(context, "home_message", player).append(Component.text("What do you want to do?", DISPLAY_OPTION.colorPalette().PRIMARY())))
                .addButton(
                        actionButton("Create", DISPLAY_OPTION.colorPalette().SUCCESS(), p -> {
                            navigate(p, DIALOG_TYPE_SELECTOR, objectCreationBasicArgumentMap);
                        })
                )
                .addButton(
                        actionButton("Delete", DISPLAY_OPTION.colorPalette().ERROR(), p -> {
                            navigate(player, DIALOG_OBJECT_SELECTOR, Map.ofEntries(
                                    Map.entry("purpose", "object_deletion")
                            ));
                        })
                )
                .addButton(exitButton())
                .show(player);

        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        hotbarMenu.setItemAt(1, createHotbarItem(
                Material.NETHER_STAR,
                Component.text("Create", DISPLAY_OPTION.colorPalette().GREEN()),
                List.of(Component.text("Creates a new object of you choosing at the location you click", DISPLAY_OPTION.colorPalette().HINT())))
                .addBlockClickAction(e -> {
                    Block block = e.getClickedBlock();
                    Map<String, Object> arguments = new HashMap<>(objectCreationBasicArgumentMap);
                    if (block != null) {
                        arguments.put("generic_inputted_location", block.getLocation());
                    }
                    navigate(e.getPlayer(), DIALOG_TYPE_SELECTOR, arguments);
                })
        );
        hotbarMenu.show(player);
    }

    private void showTypeSelectionDialog(Player player, Map<String, Object> context) {
        GenericQuestion question = GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_TYPE_SELECTOR + "_message", player).append(Component.text("Please select a object type:", DISPLAY_OPTION.colorPalette().PRIMARY())));
        Map<String, Object> clonedContext = new HashMap<>(context);
        String purposeContext = clonedContext.get("purpose") instanceof String s ? s : ""; // set to the extracted purpose or set to empty string

        String nextDialogPath = switch (purposeContext) {
            case "object_creation" -> DIALOG_ID_INPUT;
            default -> "";
        };


        for (Map.Entry<String, RegistryObjectType> entry : MCCodeCampLib.getBlockMarkerRegistry().getObjectTypes().entrySet()) {

            Button button = Button.create() // create a button with a action that sends the player to the next dialog with the added context of what was selected
                    .addAction(CustomAction.create(p -> {
                        clonedContext.put(DIALOG_TYPE_SELECTOR + "_result", entry.getValue().getTypeID());
                    }))
                    .addAction(navigateAction(nextDialogPath, clonedContext));

            // get the displayname if available or default to ID
            if (entry.getValue() instanceof StyledRegistryObjectType styled) {
                button.text(styled.getDisplayName());
                if (!PlainTextComponentSerializer.plainText().serialize(styled.getDescription()).isEmpty()) {
                    button.hoverText(styled.getDescription());
                }
            } else {
                button.text(Component.text(entry.getValue().getTypeID()));
            }


            question.addButton(button);
        }
        if (context.containsKey("!" + DIALOG_TYPE_SELECTOR + "_show_home_button")) question.addButton(homeButton());
        question.show(player);
    }

    private void showIDInputDialog(Player player, Map<String, Object> context) {
        Map<String, Object> clonedContext = new HashMap<>(context);
        String purposeContext = clonedContext.get("purpose") instanceof String s ? s : ""; // set to the extracted purpose or set to empty string

        String nextDialogPath = switch (purposeContext) {
            case "object_creation" -> DIALOG_LOCATION_INPUT_SELECTION;
            default -> "";
        };


        StringWithRulesInputAction.create()
                .regexRule("^[a-zA-Z0-9_\\-]+$")
                .prompt(createGenericQuestionHeader(context, DIALOG_ID_INPUT + "_message", player).append(Component.text("Please enter an ID!", DISPLAY_OPTION.colorPalette().PRIMARY())))
                .reTry(true)
                .addReTryAction(navigateAction(DIALOG_ID_INPUT, context))
                .onResponse(p -> s -> {
                    clonedContext.put(DIALOG_ID_INPUT + "_result", s);
                    navigate(player, nextDialogPath, clonedContext);
                })
                .addCancelAction(navigateAction(""))
                .run(player);

        new HotbarMenu(getPlugin()).show(player);
    }

    private void showLocationInputSelectionDialog(Player player, Map<String, Object> context) {
        if (context.get("generic_inputted_location") instanceof Location loc) {
            afterLocationInputHelper(player, context, loc);
            return;
        }

        GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_LOCATION_INPUT_SELECTION + "_message", player).append(Component.text("Please select how you want to input a location", DISPLAY_OPTION.colorPalette().PRIMARY())))
                .addButton(actionButton("Player feet", DISPLAY_OPTION.colorPalette().SECONDARY(), p -> {
                    afterLocationInputHelper(p, context, p.getLocation());
                }))
                .addButton(actionButton("Player eyes", DISPLAY_OPTION.colorPalette().SECONDARY(), p -> {
                    afterLocationInputHelper(p, context, p.getEyeLocation());
                }))
                .addButton(actionButton("Target Block", DISPLAY_OPTION.colorPalette().SECONDARY(), p -> {
                    Block block = p.getTargetBlockExact(4, FluidCollisionMode.NEVER);
                    if (block == null) {
                        navigate(player, DIALOG_LOCATION_INPUT_SELECTION, context);
                        return;
                    }
                    afterLocationInputHelper(p, context, block.getLocation());
                }))
                .addButton(actionButton("Input Location", DISPLAY_OPTION.colorPalette().SECONDARY(),
                        p -> navigate(p, DIALOG_LOCATION_INPUT_TYPING, context))).show(player);

        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        hotbarMenu.setItemAt(1, createHotbarItem(
                Material.NETHER_STAR,
                Component.text("Select Location", DISPLAY_OPTION.colorPalette().GREEN()),
                List.of(
                        Component.text("Click a block to select a location", DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("Click air to select your player's location", DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("Shift click air to select your player's eye's location", DISPLAY_OPTION.colorPalette().HINT())
                )).addBlockClickAction(e -> {
                    Block block = e.getClickedBlock();
                    Player p = e.getPlayer();
                    if (block != null) {
                        afterLocationInputHelper(p, context, block.getLocation());
                        return;
                    }
                    if (p.isSneaking()) {
                        afterLocationInputHelper(p, context, p.getEyeLocation());
                        return;
                    }
                    afterLocationInputHelper(p, context, p.getLocation());
                })
        );
        hotbarMenu.show(player);
    }

    private void showLocationInputTypingDialog(Player player, Map<String, Object> context) {
        LocationInputAction.create(p -> l -> {
            // The dialog accepts "x y z" without a world. Resolve that form
            // against the world of the player who submitted the response.
            if (l.getWorld() == null) {
                l.setWorld(p.getWorld());
            }
            afterLocationInputHelper(p, context, l);
        })
                .reTry(true)
                .addReTryAction(navigateAction(DIALOG_LOCATION_INPUT_TYPING, context))
                .addCancelAction(navigateAction(""))
                .run(player);
    }

    private void afterLocationInputHelper(Player player, Map<String, Object> context, Location inputtedLocation) {
        String purposeContext = context.get("purpose") instanceof String s ? s : ""; // set to the extracted purpose or set to empty string

        Location finishedLocation = inputtedLocation; // formating location if necessary
        if (context.containsKey("!" + DIALOG_GROUP_LOCATION_INPUT + "_only_block_loc")) {
            finishedLocation = finishedLocation.getBlock().getLocation();
        }

        String nextDialogPath = "";
        switch (purposeContext) {
            case "object_creation" -> {
                nextDialogPath = "";
                String id = null;
                String type = null;


                if (context.get(DIALOG_ID_INPUT + "_result") instanceof String s) {
                    id = s;
                }
                if (context.get(DIALOG_TYPE_SELECTOR + "_result") instanceof String s) {
                    type = s;
                }
                if (id == null || type == null) { // errorhandeling case: thwors player to home scree without editing anything
                    sendErrorToPlayer(player, "Inputs wheren passed correctly. Report to developer!");
                    return;
                }
                MCCodeCampLib.getPluginLogger().info("Creating new World Marker entry in registry");
                IBlockRegestryObject created = createNewRegistryEntry(type, id, finishedLocation);
                player.sendMessage(Component.text(
                        created == null ? "Could not create marker. Check the server log for the reason."
                                : "Added new marker: " + id,
                        created == null ? NamedTextColor.RED : DISPLAY_OPTION.colorPalette().SUCCESS()));
                navigateAction("").run(player);
            }
            default -> nextDialogPath = "";
        };

    }

    private void showObjectSelectionDialog(Player player, Map<String, Object> context) {
        String purposeContext = context.get("purpose") instanceof String s ? s : "";
        Map<String, Object> clonedContext = new HashMap<>(context);

        String nextDialogPath = switch (purposeContext) {
            case "object_deletion" -> DIALOG_CONFIRM_DELETE;
            default -> "";
        };

        GenericQuestion question = GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_OBJECT_SELECTOR + "_message", player).append(Component.text("Please select a object", DISPLAY_OPTION.colorPalette().PRIMARY())));


        for (IBlockRegestryObject obj : MCCodeCampLib.getBlockMarkerRegistry().getRegisteredObjects()) {
            Component buttonText = Component.text("[" + obj.getTypeID(), DISPLAY_OPTION.colorPalette().HINT())
                    .append(Component.text(":", DISPLAY_OPTION.colorPalette().HINT()))
                    .append(Component.text(obj.getID(), DISPLAY_OPTION.colorPalette().SECONDARY()))
                    .append(Component.text("]", DISPLAY_OPTION.colorPalette().HINT()));

            Button button = Button.create()
                    .text(buttonText)
                    .addAction(CustomAction.create(p -> {
                        clonedContext.put(DIALOG_OBJECT_SELECTOR + "_result", obj.getTypeID() + ":" + obj.getID());
                        navigate(p, nextDialogPath, clonedContext);
                    }));
            question.addButton(button);
        }

        question.show(player);
    }

    private void showConfirmDeletionDialog(Player player, Map<String, Object> context) {
        String purposeContext = context.get("purpose") instanceof String s ? s : "";
        Map<String, Object> clonedContext = new HashMap<>(context);

        Object selectionResult = context.get(DIALOG_OBJECT_SELECTOR + "_result");
        IBlockRegestryObject registryObject;
        Component objectDisplay = Component.empty();
        if (selectionResult instanceof String s) {
            String[] split = s.split(":");
            registryObject = MCCodeCampLib.getBlockMarkerRegistry().findRegisteredObject(split[0], split[1]);
        } else {
            registryObject = null;
        }

        // nullcheck if for some reason the selected object isn't found
        if (registryObject == null) {
            sendErrorToPlayer(player, "Received invalid object!");
            return;
        }

        if (registryObject instanceof IEditorObjectDescriptor des) {
            objectDisplay = des.getDisplayName();
        } else {
            objectDisplay = Component.text(registryObject.getID());
        }

        GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_CONFIRM_DELETE + "_message", player)
                .append(Component.text("Confirm deletion of ", DISPLAY_OPTION.colorPalette().PRIMARY()))
                .append(objectDisplay))
                .addButton(actionButton("Confirm", DISPLAY_OPTION.colorPalette().ERROR(), p -> {
                    removeRegistryEntry(registryObject);
                    navigate(p, "", Map.ofEntries(Map.entry("home_message", Component.text("Removed object successfully!", DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }))
                .addButton(actionButton("Cancel", DISPLAY_OPTION.colorPalette().SUCCESS(), p -> navigate(p, "", Map.ofEntries(Map.entry("home_message", Component.text("Cancelled removal of object", DISPLAY_OPTION.colorPalette().ERROR())))))
                ).show(player);

    }

    private void removeRegistryEntry(String typeID, String ID) {
        MCCodeCampLib.getBlockMarkerRegistry().removeObject(typeID, ID);
    }
    private void removeRegistryEntry(IBlockRegestryObject obj) {
        MCCodeCampLib.getBlockMarkerRegistry().removeObject(obj);
    }


    private IBlockRegestryObject createNewRegistryEntry(String type, String id, Location location) {
        if (location == null || location.getWorld() == null) {
            MCCodeCampLib.getPluginLogger().warning("Cannot create world marker without a world location");
            return null;
        }
        Bukkit.broadcast(Component.text(type + " " + id + " " + location));
        IBlockRegestryObject created = MCCodeCampLib.getBlockMarkerRegistry().createObject(type, id, location);
        if (created == null) {
            MCCodeCampLib.getPluginLogger().warning("Could not create world marker type=" + type + " id=" + id);
        }
        return created;
    }

    private CustomAction navigateAction(String target) {
        return navigateAction(target, new HashMap<>());
    }

    private CustomAction navigateAction(String target, Map<String, Object> context) {
        return CustomAction.create(p -> navigate(p, target, context));
    }

    private void navigate(Player player, String target) {
        navigate(player, target, new HashMap<>());
    }

    private void navigate(Player player, String target, Map<String, Object> context) {
        new PlayerRequestEditorNavigationEvent(player, target, this, context).callEvent();
    }

    private void sendErrorToPlayer(Player player, String message) {
        navigate(player, "", Map.ofEntries(Map.entry("home_message", Component.text("ERROR: " + message, NamedTextColor.RED))));
    }

    private Map<String, Object> stripObjectsMeantForDialog(Map<String, Object> context, String dialog_key) {
        Map<String, Object> output = new HashMap<>();
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            if (!entry.getKey().startsWith("!" + dialog_key)) {
                output.put(entry.getKey(), entry.getValue());
            }
        }
        return output;
    }

    private Component createGenericQuestionHeader(Map<String, Object> context, String messageKey, Player player) {
        context.remove("home_message");
        Component questionHeader = EDITOR_HEADLINE;

        if (context.get("home_message") instanceof Component c) { // append the message line
            questionHeader.append(c).append(Component.text("\n").style(Style.style()));
        }
        return questionHeader;
    }

    @Override
    public void displayDialog(Player player, String path, Map<String, Object> context) {
        if (!inInThisEditor(player)) {
            return;
        }
        switch (path) {
            case DIALOG_TYPE_SELECTOR -> showTypeSelectionDialog(player, context);
            case DIALOG_ID_INPUT -> showIDInputDialog(player, context);
            case DIALOG_LOCATION_INPUT_SELECTION -> showLocationInputSelectionDialog(player, context);
            case DIALOG_LOCATION_INPUT_TYPING -> showLocationInputTypingDialog(player, context);
            case DIALOG_OBJECT_SELECTOR -> showObjectSelectionDialog(player, context);
            case DIALOG_CONFIRM_DELETE -> showConfirmDeletionDialog(player, context);
            default -> showHomeDialog(player, context);
        }
    }
}
