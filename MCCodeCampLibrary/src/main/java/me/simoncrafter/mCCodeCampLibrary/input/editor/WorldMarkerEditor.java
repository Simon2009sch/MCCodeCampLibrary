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
import net.kyori.adventure.text.minimessage.MiniMessage;
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
    private static final String DIALOG_CONFIRM_CREATE = "confirm_creation";
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

        if (!(event.getClicked() instanceof IBlockRegestryObject marker)) return;

        if (DIALOG_OBJECT_SELECTOR.equals(MCCodeCampLib.getEditorManager().getPlayersCurrentDialogPath(player))
                && "object_deletion".equals(MCCodeCampLib.getEditorManager()
                .getPlayersCurrentDialogContext(player).get("purpose"))) {
            Map<String, Object> context = new HashMap<>(MCCodeCampLib.getEditorManager()
                    .getPlayersCurrentDialogContext(player));
            context.put(DIALOG_OBJECT_SELECTOR + "_result",
                    marker.getTypeID() + ":" + marker.getID());
            navigate(player, DIALOG_CONFIRM_DELETE, context);
        }
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

    private void addPageNavigation(HotbarMenu menu, int page, int pageCount) {
        if (pageCount <= 1) return;
        if (page > 0) {
            menu.setItemAt(0, menu.createPageChangeItem(
                    createHotbarItem(Material.ARROW, Component.text("Previous", DISPLAY_OPTION.colorPalette().SECONDARY()),
                            List.of(Component.text("Go to the previous page", DISPLAY_OPTION.colorPalette().HINT()))).getItem(),
                    page - 1));
        }
        if (page < pageCount - 1) {
            menu.setItemAt(8, menu.createPageChangeItem(
                    createHotbarItem(Material.ARROW, Component.text("Next", DISPLAY_OPTION.colorPalette().SECONDARY()),
                            List.of(Component.text("Go to the next page", DISPLAY_OPTION.colorPalette().HINT()))).getItem(),
                    page + 1));
        }
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
        hotbarMenu.setItemAt(4, createHotbarItem(
                Material.REDSTONE,
                Component.text("Delete", DISPLAY_OPTION.colorPalette().ERROR()),
                List.of(Component.text("Select an existing object to remove", DISPLAY_OPTION.colorPalette().HINT())))
                .addBlockClickAction(e -> navigate(e.getPlayer(), DIALOG_OBJECT_SELECTOR,
                        Map.of("purpose", "object_deletion"))));
        hotbarMenu.setItemAt(7, hotbarMenu.createExitItem(new ItemStack(Material.BARRIER)));
        hotbarMenu.show(player);
    }

    private void showTypeSelectionDialog(Player player, Map<String, Object> context) {
        GenericQuestion question = GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_TYPE_SELECTOR + "_message", player).append(Component.text("Please select a object type:", DISPLAY_OPTION.colorPalette().PRIMARY())));
        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        Map<String, Object> clonedContext = new HashMap<>(context);
        String purposeContext = clonedContext.get("purpose") instanceof String s ? s : ""; // set to the extracted purpose or set to empty string

        int hotbarPage = clonedContext.get("!" + DIALOG_TYPE_SELECTOR + "_hotbarPage") instanceof Integer s ? s : 0;

        String nextDialogPath = switch (purposeContext) {
            case "object_creation" -> DIALOG_ID_INPUT;
            default -> "";
        };

        List<Map.Entry<String, RegistryObjectType>> objectTypes = new ArrayList<>(MCCodeCampLib.getBlockMarkerRegistry().getObjectTypes().entrySet());
        int pageCount = Math.max(1, (objectTypes.size() + 6) / 7);
        hotbarPage = Math.max(0, Math.min(hotbarPage, pageCount - 1));
        int firstObjectType = hotbarPage * 7;
        int lastObjectType = Math.min(firstObjectType + 7, objectTypes.size());

        for (int objectTypeIndex = firstObjectType; objectTypeIndex < lastObjectType; objectTypeIndex++) {
            Map.Entry<String, RegistryObjectType> entry = objectTypes.get(objectTypeIndex);
            int hotbarIndex = 1 + objectTypeIndex - firstObjectType;

            Button button = Button.create() // create a button with a action that sends the player to the next dialog with the added context of what was selected
                    .addAction(CustomAction.create(p -> {
                        clonedContext.remove("!" + DIALOG_TYPE_SELECTOR + "_hotbarPage");
                        clonedContext.put(DIALOG_TYPE_SELECTOR + "_result", entry.getValue().getTypeID());
                    }))
                    .addAction(navigateAction(nextDialogPath, clonedContext));

            Material material = null;
            Component hoverText = null;
            Component name = null;

            // get the displayname if available or default to ID
            if (entry.getValue() instanceof StyledRegistryObjectType styled) {
                name = MiniMessage.miniMessage().deserialize(styled.getDisplayName());
                if (!styled.getDescription().isEmpty()) {
                    hoverText = MiniMessage.miniMessage().deserialize(styled.getDescription());
                }
                if (!styled.getDisplayItem().isEmpty()) {
                    material = Material.getMaterial(styled.getDisplayItem());
                }

            } else {
                name = Component.text(entry.getValue().getTypeID());
            }

            if (material == null) {
                material = Material.STICK;
            }

            // create display item for hotbar
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            button.text(name);
            meta.displayName(name);

            // enshure variables are set and only set in button if they exist
            if (hoverText != null) {
                button.hoverText(hoverText);
                meta.lore(List.of(hoverText));
            }

            item.setItemMeta(meta);
            HotbarItem hItem = new HotbarItem(getPlugin(), item);
            hItem.addBlockClickAction(e -> {
                clonedContext.remove("!" + DIALOG_TYPE_SELECTOR + "_hotbarPage");
                clonedContext.put(DIALOG_TYPE_SELECTOR + "_result", entry.getValue().getTypeID());
                navigate(e.getPlayer(), nextDialogPath, clonedContext);
            });

            hotbarMenu.setItemAt(hotbarIndex, hItem);



            question.addButton(button);
        }
        addPageNavigation(hotbarMenu, hotbarPage, pageCount);
        question.show(player);
        hotbarMenu.show(player, hotbarPage);
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

        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        hotbarMenu.setItemAt(0, createHotbarItem(Material.NAME_TAG,
                Component.text("Enter ID in chat", DISPLAY_OPTION.colorPalette().SECONDARY()),
                List.of(Component.text("Use the chat prompt to enter the marker ID", DISPLAY_OPTION.colorPalette().HINT()))));
        hotbarMenu.setItemAt(8, hotbarMenu.createExitItem(new ItemStack(Material.BARRIER)));
        hotbarMenu.show(player);
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

        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        hotbarMenu.setItemAt(0, createHotbarItem(Material.NAME_TAG,
                Component.text("Enter coordinates in chat", DISPLAY_OPTION.colorPalette().SECONDARY()),
                List.of(Component.text("Use the chat prompt to enter x y z", DISPLAY_OPTION.colorPalette().HINT()))));
        hotbarMenu.setItemAt(8, hotbarMenu.createExitItem(new ItemStack(Material.BARRIER)));
        hotbarMenu.show(player);
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
                nextDialogPath = DIALOG_CONFIRM_CREATE;
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
                Map<String, Object> confirmationContext = new HashMap<>(context);
                confirmationContext.put(DIALOG_TYPE_SELECTOR + "_result", type);
                confirmationContext.put(DIALOG_ID_INPUT + "_result", id);
                confirmationContext.put(DIALOG_GROUP_LOCATION_INPUT + "_result", finishedLocation);
                navigate(player, DIALOG_CONFIRM_CREATE, confirmationContext);
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


        List<IBlockRegestryObject> objects = new ArrayList<>(MCCodeCampLib.getBlockMarkerRegistry().getRegisteredObjects());
        int hotbarPage = context.get("!" + DIALOG_OBJECT_SELECTOR + "_hotbarPage") instanceof Integer p ? p : 0;
        int pageCount = Math.max(1, (objects.size() + 6) / 7);
        hotbarPage = Math.max(0, Math.min(hotbarPage, pageCount - 1));
        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        int firstObject = hotbarPage * 7;
        int lastObject = Math.min(firstObject + 7, objects.size());

        for (int objectIndex = firstObject; objectIndex < lastObject; objectIndex++) {
            IBlockRegestryObject obj = objects.get(objectIndex);
            Component buttonText = Component.text("[" + obj.getTypeID(), DISPLAY_OPTION.colorPalette().HINT())
                    .append(Component.text(":", DISPLAY_OPTION.colorPalette().HINT()))
                    .append(Component.text(obj.getID(), DISPLAY_OPTION.colorPalette().SECONDARY()))
                    .append(Component.text("]", DISPLAY_OPTION.colorPalette().HINT()));

            Button button = Button.create()
                    .text(buttonText)
                    .addAction(CustomAction.create(p -> {
                        Map<String, Object> selectionContext = new HashMap<>(context);
                        selectionContext.put(DIALOG_OBJECT_SELECTOR + "_result", obj.getTypeID() + ":" + obj.getID());
                        navigate(p, nextDialogPath, selectionContext);
                    }));
            question.addButton(button);

            hotbarMenu.setItemAt(1 + objectIndex - firstObject,
                    createHotbarItem(Material.PAPER,
                            Component.text(obj.getTypeID() + ":" + obj.getID(), DISPLAY_OPTION.colorPalette().SECONDARY()),
                            List.of(Component.text("Click to select this object for deletion", DISPLAY_OPTION.colorPalette().HINT())))
                            .addBlockClickAction(e -> {
                                Map<String, Object> selectionContext = new HashMap<>(context);
                                selectionContext.put(DIALOG_OBJECT_SELECTOR + "_result", obj.getTypeID() + ":" + obj.getID());
                                navigate(e.getPlayer(), nextDialogPath, selectionContext);
                            }));
        }

        addPageNavigation(hotbarMenu, hotbarPage, pageCount);
        question.show(player);
        hotbarMenu.show(player, hotbarPage);
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
            objectDisplay = MiniMessage.miniMessage().deserialize(des.getDisplayName());
        } else {
            objectDisplay = Component.text(registryObject.getID());
        }

        Location location = registryObject.getLocation();
        Component review = Component.text("Delete marker", DISPLAY_OPTION.colorPalette().PRIMARY())
                .appendNewline().append(Component.text("Type: " + registryObject.getTypeID(), DISPLAY_OPTION.colorPalette().HINT()))
                .appendNewline().append(Component.text("ID: " + registryObject.getID(), DISPLAY_OPTION.colorPalette().HINT()))
                .appendNewline().append(Component.text("World: " + location.getWorld().getName(), DISPLAY_OPTION.colorPalette().HINT()))
                .appendNewline().append(Component.text("Coordinates: " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DISPLAY_OPTION.colorPalette().HINT()));

        GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_CONFIRM_DELETE + "_message", player)
                .append(review))
                .addButton(actionButton("Confirm", DISPLAY_OPTION.colorPalette().ERROR(), p -> {
                    removeRegistryEntry(registryObject);
                    navigate(p, "", Map.ofEntries(Map.entry("home_message", Component.text("Removed object successfully!", DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }))
                .addButton(actionButton("Cancel", DISPLAY_OPTION.colorPalette().SUCCESS(), p -> navigate(p, "", Map.ofEntries(Map.entry("home_message", Component.text("Cancelled removal of object", DISPLAY_OPTION.colorPalette().ERROR())))))
                ).show(player);

        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        hotbarMenu.setItemAt(3, createHotbarItem(Material.RED_CONCRETE,
                Component.text("Confirm deletion", DISPLAY_OPTION.colorPalette().ERROR()),
                List.of(
                        Component.text("Type: " + registryObject.getTypeID(), DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("ID: " + registryObject.getID(), DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("World: " + location.getWorld().getName(), DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("Coordinates: " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("This cannot be undone", DISPLAY_OPTION.colorPalette().HINT())
                ))
                .addBlockClickAction(e -> {
                    removeRegistryEntry(registryObject);
                    navigate(e.getPlayer(), "", Map.ofEntries(Map.entry("home_message", Component.text("Removed object successfully!", DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }));
        hotbarMenu.setItemAt(5, createHotbarItem(Material.LIME_CONCRETE,
                Component.text("Cancel", DISPLAY_OPTION.colorPalette().SUCCESS()),
                List.of(Component.text("Keep the selected object", DISPLAY_OPTION.colorPalette().HINT())))
                .addBlockClickAction(e -> navigate(e.getPlayer(), "", Map.ofEntries(Map.entry("home_message", Component.text("Cancelled removal of object", DISPLAY_OPTION.colorPalette().ERROR()))))));
        hotbarMenu.show(player);

    }

    private void showConfirmCreationDialog(Player player, Map<String, Object> context) {
        String type = context.get(DIALOG_TYPE_SELECTOR + "_result") instanceof String s ? s : null;
        String id = context.get(DIALOG_ID_INPUT + "_result") instanceof String s ? s : null;
        Location location = context.get(DIALOG_GROUP_LOCATION_INPUT + "_result") instanceof Location l ? l : null;
        if (type == null || id == null || location == null || location.getWorld() == null) {
            sendErrorToPlayer(player, "Could not prepare the marker for creation.");
            return;
        }

        Component review = Component.text("Create marker", DISPLAY_OPTION.colorPalette().PRIMARY())
                .appendNewline().append(Component.text("Type: " + type, DISPLAY_OPTION.colorPalette().HINT()))
                .appendNewline().append(Component.text("ID: " + id, DISPLAY_OPTION.colorPalette().HINT()))
                .appendNewline().append(Component.text("World: " + location.getWorld().getName(), DISPLAY_OPTION.colorPalette().HINT()))
                .appendNewline().append(Component.text("Coordinates: " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DISPLAY_OPTION.colorPalette().HINT()));

        GenericQuestion.create(createGenericQuestionHeader(context, DIALOG_CONFIRM_CREATE + "_message", player).append(review))
                .addButton(actionButton("Confirm", DISPLAY_OPTION.colorPalette().SUCCESS(), p -> {
                    MCCodeCampLib.getPluginLogger().info("Creating new World Marker entry in registry");
                    IBlockRegestryObject created = createNewRegistryEntry(type, id, location);
                    navigate(p, "", Map.ofEntries(Map.entry("home_message", Component.text(
                            created == null ? "Could not create marker. Check the server log for the reason." : "Added new marker: " + id,
                            created == null ? NamedTextColor.RED : DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }))
                .addButton(actionButton("Cancel", DISPLAY_OPTION.colorPalette().ERROR(), p ->
                        navigate(p, "", Map.ofEntries(Map.entry("home_message", Component.text("Cancelled marker creation", DISPLAY_OPTION.colorPalette().ERROR()))))))
                .show(player);

        HotbarMenu hotbarMenu = new HotbarMenu(getPlugin());
        hotbarMenu.setItemAt(3, createHotbarItem(Material.LIME_CONCRETE,
                Component.text("Confirm creation", DISPLAY_OPTION.colorPalette().SUCCESS()),
                List.of(
                        Component.text("Type: " + type, DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("ID: " + id, DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("World: " + location.getWorld().getName(), DISPLAY_OPTION.colorPalette().HINT()),
                        Component.text("Coordinates: " + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ(), DISPLAY_OPTION.colorPalette().HINT())
                )).addBlockClickAction(e -> {
                    IBlockRegestryObject created = createNewRegistryEntry(type, id, location);
                    navigate(e.getPlayer(), "", Map.ofEntries(Map.entry("home_message", Component.text(
                            created == null ? "Could not create marker. Check the server log for the reason." : "Added new marker: " + id,
                            created == null ? NamedTextColor.RED : DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }));
        hotbarMenu.setItemAt(5, createHotbarItem(Material.RED_CONCRETE,
                Component.text("Cancel", DISPLAY_OPTION.colorPalette().ERROR()),
                List.of(Component.text("Do not create this marker", DISPLAY_OPTION.colorPalette().HINT())))
                .addBlockClickAction(e -> navigate(e.getPlayer(), "", Map.ofEntries(Map.entry("home_message", Component.text("Cancelled marker creation", DISPLAY_OPTION.colorPalette().ERROR()))))));
        hotbarMenu.show(player);
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
            case DIALOG_CONFIRM_CREATE -> showConfirmCreationDialog(player, context);
            case DIALOG_CONFIRM_DELETE -> showConfirmDeletionDialog(player, context);
            default -> showHomeDialog(player, context);
        }
    }
}
