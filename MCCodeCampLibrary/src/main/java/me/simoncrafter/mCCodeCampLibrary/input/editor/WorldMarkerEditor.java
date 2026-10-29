package me.simoncrafter.mCCodeCampLibrary.input.editor;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.DisplayOptions.DisplayOption;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.DisplayOptions.DisplayOptions;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.LocationInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfirmQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.effect.highlighter.BlockHighlighter;
import me.simoncrafter.CraftersDisplayLibrary.effect.highlighter.HighlightDisplayType;
import me.simoncrafter.mCCodeCampLibrary.internal.activation.StyledRegistryObjectType;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditable;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.IEditorObjectDescriptor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerChangeSelectionOfObjectEvent;
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
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;


import org.joml.Vector3f;

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


    // The yellow object-selection outline uses 0.05f.
    private static final float LOCATION_HIGHLIGHT_THICKNESS = 0.05f * 0.75f;
    private static final String CREATION_HIGHLIGHT_KEY = "object_creation";
    private final Map<UUID, Map<String, Map<Block, LocationHighlight>>> locationHighlights = new HashMap<>();

    private record LocationHighlight(Player player, Block block, WireframeCubeColorDisplay display) { }


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
        clearLocationHighlights(player.getUniqueId());
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

    }

    @EventHandler
    public void onPlayerChangeSelectionOfObject(PlayerChangeSelectionOfObjectEvent event) {
        if (event.getAction() == PlayerChangeSelectionOfObjectEvent.Action.SELECT) {
            onSelect(event);
        } else {
            onDeselect(event);
        }
    }

    private void onDeselect(PlayerChangeSelectionOfObjectEvent e) {

    }

    private void onSelect(PlayerChangeSelectionOfObjectEvent e) {
        Player player = e.getPlayer();
        if (!(e.getObject() instanceof IBlockRegestryObject marker)) return;

        if (DIALOG_OBJECT_SELECTOR.equals(MCCodeCampLib.getEditorManager().getPlayersCurrentDialogPath(player))
                && "object_deletion".equals(MCCodeCampLib.getEditorManager()
                .getPlayersCurrentDialogContext(player).get("purpose"))) {
            Map<String, Object> context = new HashMap<>(MCCodeCampLib.getEditorManager()
                    .getPlayersCurrentDialogContext(player));
            context.put(DIALOG_OBJECT_SELECTOR + "_result",
                    marker.getTypeID() + ":" + marker.getID());
            navigate(player, DIALOG_CONFIRM_DELETE, context);
        }

        if (player.getInventory().getItemInMainHand().getPersistentDataContainer().getOrDefault(HotbarItem.ITEM_KEY, PersistentDataType.STRING, "").endsWith("home_delete_item")) {
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

    private void highlightLocation(Player player, Location location) {
        highlightLocation(player, CREATION_HIGHLIGHT_KEY, location);
    }

    private void highlightLocation(Player player, String key, Location location) {
        if (location.getWorld() == null) return;
        UUID playerId = player.getUniqueId();
        Block block = location.getBlock();
        clearLocationHighlight(playerId, key, block);
        // BlockHighlighter has one display per block. Share only displays owned by this editor.
        WireframeCubeColorDisplay display = trackedLocationHighlights().stream()
                .filter(h -> h.block().equals(block) && BlockHighlighter.getHighlightDisplay(block) == h.display())
                .map(LocationHighlight::display).findFirst().orElse(null);
        if (display == null) {
            // Spawn transparent, hide recursively, and only then make the outline opaque.
            display = (WireframeCubeColorDisplay) BlockHighlighter.highlightBlock(block,
                    HighlightDisplayType.WIREFRAME, Color.fromARGB(0, 0, 0, 0));
            display.hideByDefault(true, true);
            display.setThickness(LOCATION_HIGHLIGHT_THICKNESS);
            display.setSeeThrough(true);
            display.scaleAbsolute(new Vector3f(1.05f, 1.05f, 1.05f), 0);
            display.setColor(Color.WHITE);
        }
        display.showForPlayer(player, true);

        locationHighlights.computeIfAbsent(playerId, ignored -> new HashMap<>())
                .computeIfAbsent(key, ignored -> new HashMap<>())
                .put(block, new LocationHighlight(player, block, display));
    }

    private List<LocationHighlight> trackedLocationHighlights() {
        return locationHighlights.values().stream().flatMap(groups -> groups.values().stream())
                .flatMap(blocks -> blocks.values().stream()).toList();
    }

    private void clearLocationHighlight(UUID playerId, String key, Block block) {
        Map<String, Map<Block, LocationHighlight>> groups = locationHighlights.get(playerId);
        if (groups == null) return;
        Map<Block, LocationHighlight> blocks = groups.get(key);
        if (blocks == null) return;
        LocationHighlight highlight = blocks.remove(block);
        if (highlight == null) return;
        if (blocks.isEmpty()) groups.remove(key);
        if (groups.isEmpty()) locationHighlights.remove(playerId);

        List<LocationHighlight> remaining = trackedLocationHighlights();
        if (remaining.stream().noneMatch(h -> h.display() == highlight.display()
                && h.player().getUniqueId().equals(playerId))) {
            highlight.display().hideForPlayer(highlight.player(), true);
        }
        if (remaining.stream().noneMatch(h -> h.display() == highlight.display())
                && BlockHighlighter.getHighlightDisplay(block) == highlight.display()) {
            BlockHighlighter.unhighlightBlock(block);
        }
    }

    private void clearLocationHighlights(UUID playerId, String key) {
        Map<String, Map<Block, LocationHighlight>> groups = locationHighlights.get(playerId);
        if (groups == null || !groups.containsKey(key)) return;
        for (Block block : new ArrayList<>(groups.get(key).keySet())) {
            clearLocationHighlight(playerId, key, block);
        }
    }

    private void clearLocationHighlights(UUID playerId) {
        Map<String, Map<Block, LocationHighlight>> groups = locationHighlights.get(playerId);
        if (groups == null) return;
        for (String key : new ArrayList<>(groups.keySet())) {
            clearLocationHighlights(playerId, key);
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != getPlugin()) return;
        for (UUID playerId : new ArrayList<>(locationHighlights.keySet())) {
            clearLocationHighlights(playerId);
        }
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

    private HotbarItem createHotbarItem(Material type, Component name, List<Component> lore, String referenceId) {
        return createHotbarItem(type, name, lore, referenceId, false);
    }

    private HotbarItem createHotbarItem(Material type, Component name, List<Component> lore) {
        return createHotbarItem(type, name, lore, "", false);
    }

    private HotbarItem createHotbarItem(Material type, Component name, List<Component> lore, String referenceId, boolean needsBlockTarget) {
        ItemStack itemStack = new ItemStack(type);
        ItemMeta meta = itemStack.getItemMeta();
        meta.customName(name);
        if (!(lore != null && lore.isEmpty())) meta.lore(lore);
        itemStack.setItemMeta(meta);
        return configureHotbarItem(new HotbarItem(getPlugin(), itemStack), needsBlockTarget, referenceId);
    }

    private HotbarItem configureHotbarItem(HotbarItem item, boolean needsBlockTarget, String referenceId) {
        // Block icons must not produce client-side placement; targeting tools still need reach.
        item.setNoInteractMode(item.getItem().getType().isBlock()
                ? HotbarItem.NoInteractMode.Masquerade
                : HotbarItem.NoInteractMode.Off);
        item.setReferenceID(referenceId);
        return item;
    }

    private void addPageNavigation(HotbarMenu menu, int pageCount) {
        menu.setItemAt(0, createHotbarItem(Material.BARRIER,
                Component.text("Cancel", DISPLAY_OPTION.colorPalette().ERROR()),
                List.of(Component.text("Return to the home screen", DISPLAY_OPTION.colorPalette().HINT()).decoration(TextDecoration.ITALIC, false)))
                .addBlockClickAction(e -> navigate(e.getPlayer(), "", new HashMap<>())));
        // HotbarMenu displays nine consecutive indices per page; reserve slots 0 and 8.
        for (int page = 0; page < pageCount; page++) {
            int pageOffset = page * 9;
            if (page > 0) {
                menu.setItemAt(pageOffset, menu.createPageChangeItem(
                        createHotbarItem(Material.ARROW, Component.text("Previous", DISPLAY_OPTION.colorPalette().SECONDARY()),
                                List.of(Component.text("Go to the previous page", DISPLAY_OPTION.colorPalette().HINT()))).getItem(),
                        page - 1).addHoverAction(pageHoverAction(menu, page - 1, 7)));
            }
            if (page < pageCount - 1) {
                menu.setItemAt(pageOffset + 8, menu.createPageChangeItem(
                        createHotbarItem(Material.ARROW, Component.text("Next", DISPLAY_OPTION.colorPalette().SECONDARY()),
                                List.of(Component.text("Go to the next page", DISPLAY_OPTION.colorPalette().HINT()))).getItem(),
                        page + 1).addHoverAction(pageHoverAction(menu, page + 1, 1)));
            }
        }
    }

    private Consumer<Player> pageHoverAction(HotbarMenu menu, int targetPage, int targetSlot) {
        return player -> {
            // Let the original held-slot event finish before moving the selection.
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                if (!inInThisEditor(player)) return;
                menu.show(player, targetPage);
                menu.setHeldItemSlotQuietly(player, targetSlot);
            });
        };
    }

    private Button homeButton() {
        return actionButton("Home", DISPLAY_OPTION.colorPalette().PRIMARY(), p -> navigate(p, "", new HashMap<>()));
    }

    private Button exitButton() {
        return actionButton("Exit", DISPLAY_OPTION.colorPalette().ERROR(), this::exitPlayer);
    }

    private HotbarItem exitHotbaritem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.customName(Component.text("Exit Editor", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return configureHotbarItem(new HotbarItem(getPlugin(), item), false, "").addBlockClickAction(e -> {
            exitPlayer(e.getPlayer());
        });
    }

    private void showHomeDialog(Player player, Map<String, Object> context) {
        // Every creation cancel path (chat, hotbar, or input cancellation) returns Home.
        clearLocationHighlights(player.getUniqueId(), CREATION_HIGHLIGHT_KEY);
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
                List.of(Component.text("Creates a new object of you choosing at the location you click", DISPLAY_OPTION.colorPalette().HINT())), "", true)
                .addBlockClickAction(e -> {
                    Block block = e.getClickedBlock();
                    Map<String, Object> arguments = new HashMap<>(objectCreationBasicArgumentMap);
                    if (block != null) {
                        arguments.put("generic_inputted_location", block.getLocation());
                        highlightLocation(e.getPlayer(), block.getLocation());
                    }
                    MCCodeCampLib.getEditorManager().deselect(player);
                    navigate(e.getPlayer(), DIALOG_TYPE_SELECTOR, arguments);
                })
        );
        hotbarMenu.setItemAt(4, createHotbarItem(
                Material.REDSTONE,
                Component.text("Delete", DISPLAY_OPTION.colorPalette().ERROR()),
                List.of(Component.text("Select an existing object to remove", DISPLAY_OPTION.colorPalette().HINT())), "home_delete_item")
                .addBlockClickAction(e -> {
                    MCCodeCampLib.getEditorManager().deselect(player);
                    navigate(e.getPlayer(), DIALOG_OBJECT_SELECTOR,
                            Map.of("purpose", "object_deletion"));
                }));
        hotbarMenu.setItemAt(7, exitHotbaritem());
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
        for (int objectTypeIndex = 0; objectTypeIndex < objectTypes.size(); objectTypeIndex++) {
            Map.Entry<String, RegistryObjectType> entry = objectTypes.get(objectTypeIndex);
            int hotbarIndex = (objectTypeIndex / 7) * 9 + 1 + objectTypeIndex % 7;

            Button button = Button.create() // create a button with an action that sends the player to the next dialog with the added context of what was selected
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
            HotbarItem hItem = configureHotbarItem(new HotbarItem(getPlugin(), item), false, "");
            hItem.addBlockClickAction(e -> {
                clonedContext.remove("!" + DIALOG_TYPE_SELECTOR + "_hotbarPage");
                clonedContext.put(DIALOG_TYPE_SELECTOR + "_result", entry.getValue().getTypeID());
                navigate(e.getPlayer(), nextDialogPath, clonedContext);
            });

            hotbarMenu.setItemAt(hotbarIndex, hItem);



            question.addButton(button);
        }
        addPageNavigation(hotbarMenu, pageCount);
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
        hotbarMenu.setItemAt(8, exitHotbaritem());
        hotbarMenu.show(player);
    }

    private void showLocationInputSelectionDialog(Player player, Map<String, Object> context) {
        if (context.get("generic_inputted_location") instanceof Location loc) {
            // Already highlighted immediately by the Home item; keep the existing display.
            afterLocationInputHelper(player, context, loc, false);
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
                ), "", true).addBlockClickAction(e -> {
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
        hotbarMenu.setItemAt(8, exitHotbaritem());
        hotbarMenu.show(player);
    }

    private void afterLocationInputHelper(Player player, Map<String, Object> context, Location inputtedLocation) {
        afterLocationInputHelper(player, context, inputtedLocation, true);
    }

    private void afterLocationInputHelper(Player player, Map<String, Object> context, Location inputtedLocation,
                                          boolean highlightSelection) {
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
                if (highlightSelection) highlightLocation(player, finishedLocation);
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
        for (int objectIndex = 0; objectIndex < objects.size(); objectIndex++) {
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

            hotbarMenu.setItemAt((objectIndex / 7) * 9 + 1 + objectIndex % 7,
                    createHotbarItem(Material.PAPER,
                            Component.text(obj.getTypeID() + ":" + obj.getID(), DISPLAY_OPTION.colorPalette().SECONDARY()),
                            List.of(Component.text("Click to select this object for deletion", DISPLAY_OPTION.colorPalette().HINT())))
                            .addBlockClickAction(e -> {
                                Map<String, Object> selectionContext = new HashMap<>(context);
                                selectionContext.put(DIALOG_OBJECT_SELECTOR + "_result", obj.getTypeID() + ":" + obj.getID());
                                navigate(e.getPlayer(), nextDialogPath, selectionContext);
                            }));
        }

        addPageNavigation(hotbarMenu, pageCount);
        question.show(player);
        hotbarMenu.show(player, hotbarPage);
    }

    private Component confirmationDetail(String label, String value) {
        return Component.text(label + ": ", DISPLAY_OPTION.colorPalette().HINT())
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(value).decoration(TextDecoration.ITALIC, true));
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
                .appendNewline().append(confirmationDetail("Type", registryObject.getTypeID()))
                .appendNewline().append(confirmationDetail("ID", registryObject.getID()))
                .appendNewline().append(confirmationDetail("World", location.getWorld().getName()))
                .appendNewline().append(confirmationDetail("Coordinates", location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));

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
                        confirmationDetail("Type", registryObject.getTypeID()),
                        confirmationDetail("ID", registryObject.getID()),
                        confirmationDetail("World", location.getWorld().getName()),
                        confirmationDetail("Coordinates", location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()),
                        Component.text("This cannot be undone", DISPLAY_OPTION.colorPalette().HINT())
                ))
                .addBlockClickAction(e -> {
                    removeRegistryEntry(registryObject);
                    navigate(e.getPlayer(), "", Map.ofEntries(Map.entry("home_message", Component.text("Removed object successfully!", DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }));
        hotbarMenu.setItemAt(5, createHotbarItem(Material.LIME_CONCRETE,
                Component.text("Cancel", DISPLAY_OPTION.colorPalette().SUCCESS()),
                List.of(Component.text("Keep the selected object", DISPLAY_OPTION.colorPalette().HINT()).decoration(TextDecoration.ITALIC, false)))
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
                .appendNewline().append(confirmationDetail("Type", type))
                .appendNewline().append(confirmationDetail("ID", id))
                .appendNewline().append(confirmationDetail("World", location.getWorld().getName()))
                .appendNewline().append(confirmationDetail("Coordinates", location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ()));

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
                        confirmationDetail("Type", type),
                        confirmationDetail("ID", id),
                        confirmationDetail("World", location.getWorld().getName()),
                        confirmationDetail("Coordinates", location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ())
                )).addBlockClickAction(e -> {
                    IBlockRegestryObject created = createNewRegistryEntry(type, id, location);
                    navigate(e.getPlayer(), "", Map.ofEntries(Map.entry("home_message", Component.text(
                            created == null ? "Could not create marker. Check the server log for the reason." : "Added new marker: " + id,
                            created == null ? NamedTextColor.RED : DISPLAY_OPTION.colorPalette().SUCCESS()))));
                }));
        hotbarMenu.setItemAt(5, createHotbarItem(Material.RED_CONCRETE,
                Component.text("Cancel", DISPLAY_OPTION.colorPalette().ERROR()),
                List.of(Component.text("Do not create this marker", DISPLAY_OPTION.colorPalette().HINT()).decoration(TextDecoration.ITALIC, false)))
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
