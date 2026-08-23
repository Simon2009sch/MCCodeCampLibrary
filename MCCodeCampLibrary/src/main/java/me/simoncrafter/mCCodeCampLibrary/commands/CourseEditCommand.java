package me.simoncrafter.mCCodeCampLibrary.commands;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditData;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditPlayerData;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfigEditQuestion.ConfigEditValues.ConfigEditListSection;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.EditorItems;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.WorldMarkerEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenue.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.internal.events.BlockRegistryUpdateEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.internal.registry.IBlockRegestryObject;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class CourseEditCommand implements TabExecutor, org.bukkit.command.CommandExecutor {

    WorldMarkerEditor testEditor = null;
    Plugin plugin;
    private final Map<UUID, ConfigEditPlayerData> configEditPlayers = new HashMap<>();
    private final Map<UUID, Map<String, Object>> courseEditValues = new HashMap<>();

    public CourseEditCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("You need to be a player to execute this!", NamedTextColor.RED));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 1 && args[0].equalsIgnoreCase("getItems") && player.hasPermission("mccodecamp.command.edit.getitems")) {
            player.give(EditorItems.getToggleVisibility());
            player.give(EditorItems.getCreateNew());
            player.give(EditorItems.getRemove());
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            new BlockRegistryUpdateEvent().callEvent();
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("hotbar")) {
            Plugin plugin = MCCodeCampLib.unsafePluginGetDoNotUseThisOutsideOfTesting();
            HotbarMenu parent = new HotbarMenu(plugin);
            HotbarMenu scroller = new HotbarMenu(plugin);

            Map<Player, Float> testValue = new HashMap<>();

            scroller.setItemAt(4, parent.createLoopbackItem(new ItemStack(Material.BLUE_CONCRETE)));
            scroller.addScrollAction(e -> {
                float amount = e.player().isSneaking() ? 0.1f : 1;
                amount *= e.direction() == HotbarMenu.ScrollDirection.UP?1:-1;

                float current = testValue.getOrDefault(e.player(), 0f);
                testValue.put(e.player(), current+amount);
                e.player().sendMessage("The new value is: §l" + (current+amount));
                scroller.setHeldItemSlotQuietly(e.player(), 4);
            });

            parent.setItemAt(0, new HotbarItem(plugin, new ItemStack(Material.TNT)).addBlockClickAction(e -> {
                e.getPlayer().getLocation().createExplosion(6, false, false);
            }));
            parent.setItemAt(4, new HotbarItem(plugin, new ItemStack(Material.BLUE_CONCRETE)).addBlockClickAction(e -> {
                scroller.show(e.getPlayer());
            }));
            parent.setItemAt(8, parent.createExitItem(new ItemStack(Material.BARREL)));

            parent.show(player);
            return true;
        } else if (args.length == 6 && args[0].equalsIgnoreCase("add") && player.hasPermission("mccodecamp.command.edit.add")) {
            String type = args[1];
            String id = args[2];

            BlockMarkerRegistry registry = MCCodeCampLib.getBlockMarkerRegistry();
            if (registry.hasObject(type, id)) {
                player.sendMessage(Component.text("A \"" + type + "\" marker with ID \"" + id + "\" already exists!", NamedTextColor.RED));
                return true;
            }

            Location location = parsePosition(player, args[3], args[4], args[5]);
            if (location == null) {
                player.sendMessage(Component.text("Invalid position. Use numbers or \"~\" relative coordinates.", NamedTextColor.RED));
                return true;
            }

            IBlockRegestryObject obj = registry.createObject(type, id, location);
            if (obj == null) {
                player.sendMessage(Component.text("Unknown marker type: \"" + type + "\"", NamedTextColor.RED));
                return true;
            }

            player.sendMessage(Component.text("Created " + type + " \"" + id + "\" at " +
                    location.getX() + " " + location.getY() + " " + location.getZ() + ".", NamedTextColor.GREEN));
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("editortest")) {
            if (testEditor == null) {
                List<WorldMarkerEditor.CreatableObject> objects = new ArrayList<>();

                ItemStack buttonAddItem = new ItemStack(Material.OAK_BUTTON);
                buttonAddItem.setData(DataComponentTypes.CUSTOM_NAME, Component.text("Add Button", NamedTextColor.GREEN));

                objects.add(new WorldMarkerEditor.CreatableObject(buttonAddItem, e -> {
                    if (e.getClickedBlock() == null) {
                        return;
                    }
                    StringWithRulesInputAction inputAction = new StringWithRulesInputAction(p -> str -> {
                        MCCodeCampLib.getBlockMarkerRegistry().createObject("button", str, e.getClickedBlock().getLocation());
                    });

                    inputAction.prompt(Component.text("Please input a id for this button"));
                    inputAction.regexRule(".*");
                    inputAction.run(e.getPlayer());
                }));
                testEditor = new WorldMarkerEditor(plugin, new ArrayList<>(), objects);
            }
            testEditor.addPlayer(player);
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("configedit")) {
            sendEditQuestion(player);
            return true;
        }

        return false;
    }

    private void sendEditQuestion(Player player) {
        sendEditQuestion(player, values -> saveCourseCallback(player, values));
    }

    private void sendEditQuestion(Player player, Consumer<Map<String, Object>> saveAction) {
        Map<String, Object> values = courseEditValues.computeIfAbsent(
                player.getUniqueId(), ignored -> ConfigEditData.makeMutable(Map.of(
                        "name", "example-course",
                        "displayName", "Example Course",
                        "description", "A sample course for learning ConfigEditQuestion.",
                        "enabled", true,
                        "maxPlayers", 4,
                        "dialogue", Map.of(
                                "title", "Welcome to the example course",
                                "messages", List.of(
                                        "Welcome to the course!",
                                        "This message is editable in chat.",
                                        "Use this as a base for Crafters Chat Dialogs."
                                ),
                                "endMessage", "Dialogue complete."
                        )
                ))
        );

        ConfigEditPlayerData playerData = configEditPlayers.computeIfAbsent(player.getUniqueId(), ignored -> new ConfigEditPlayerData());
        ConfigEditData data = ConfigEditData.create(values);

        data.setDeepRegexDisplayData("dialogue\\.messages", (path, current) -> {
            if (current instanceof ConfigEditListSection section) {
                section.showAddButton(true);
                section.showRemoveButtons(true);
                section.showMoveButtons(true);
            }
            return current;
        });

        ConfigEditQuestion question = ConfigEditQuestion.create(playerData, data)
                .showSaveChangesButton(true)
                .saveChangesAction(() -> saveAction.accept(values))
                .onReload(p -> sendEditQuestion(p, saveAction));

        question.setRootSection(data.getNewRootSection());
        question.show(player, "courseEdit");

        Button reloadButton = Button.create()
                .addAction(CustomAction.create(p -> sendEditQuestion(p, saveAction)))
                .text(Component.text("[Reload]", NamedTextColor.RED));
        player.sendMessage(reloadButton.compile());
    }

    private void saveCourseCallback(Player player, Map<String, Object> values) {
        player.sendMessage(Component.text(
                "Course editor data captured in memory. Add persistence here next.",
                NamedTextColor.GREEN));
    }

    /**
     * Creation hook used by the {@link WorldMarkerEditor.CreatableObject} action.
     * Replace the body with construction of the real course/dialogue object.
     */
    private void createCourseObject(Player player, Map<String, Object> values) {
        player.sendMessage(Component.text(
                "Course object created from the edited values.",
                NamedTextColor.GREEN));
    }

    private void tempClearBlocks(Block block, int depth) {
        if (depth <= 0) return;
        new BukkitRunnable(){
            @Override
            public void run() {
                block.setType(Material.AIR);
                Block topBlock = block.getLocation().clone().add(new Vector(0, 1, 0)).getBlock();
                Block bottomBlock = block.getLocation().clone().add(new Vector(0, -1, 0)).getBlock();
                Block rightBlock = block.getLocation().clone().add(new Vector(1, 1, 0)).getBlock();
                Block leftBlock = block.getLocation().clone().add(new Vector(-1, 1, 0)).getBlock();
                Block frontBlock = block.getLocation().clone().add(new Vector(0, 1, 1)).getBlock();
                Block backBlock = block.getLocation().clone().add(new Vector(0, 1, -1)).getBlock();
                if (topBlock.getType() == block.getType()) {
                    tempClearBlocks(topBlock, depth-1);
                }
                if (bottomBlock.getType() == block.getType()) {
                    tempClearBlocks(bottomBlock, depth-1);
                }
                if (rightBlock.getType() == block.getType()) {
                    tempClearBlocks(rightBlock, depth-1);
                }
                if (leftBlock.getType() == block.getType()) {
                    tempClearBlocks(leftBlock, depth-1);
                }
                if (frontBlock.getType() == block.getType()) {
                    tempClearBlocks(frontBlock, depth-1);
                }
                if (backBlock.getType() == block.getType()) {
                    tempClearBlocks(backBlock, depth-1);
                }
            }
        }.runTaskLater(plugin, 1);
    }

    /**
     * Parses a position made up of three coordinate tokens, each of which is either an absolute
     * number or a "~"/"~offset" coordinate relative to the given player's current position - matching
     * the syntax used by vanilla commands.
     *
     * @return the resolved location, or {@code null} if any of the tokens could not be parsed.
     */
    private @Nullable Location parsePosition(Player player, String xArg, String yArg, String zArg) {
        Location base = player.getLocation();
        Double x = parseCoordinate(xArg, base.getX());
        Double y = parseCoordinate(yArg, base.getY());
        Double z = parseCoordinate(zArg, base.getZ());
        if (x == null || y == null || z == null) {
            return null;
        }
        return new Location(base.getWorld(), x, y, z);
    }

    private @Nullable Double parseCoordinate(String token, double relativeBase) {
        if (token.isEmpty()) {
            return null;
        }
        if (token.charAt(0) == '~') {
            String offset = token.substring(1);
            if (offset.isEmpty()) {
                return relativeBase;
            }
            try {
                return relativeBase + Double.parseDouble(offset);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return matchingPrefix(List.of("getItems", "reload", "hotbar", "add", "editortest", "configedit"), args[0]);
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length == 2) {
                return matchingPrefix(new ArrayList<>(MCCodeCampLib.getBlockMarkerRegistry().getObjectTypeIDs()), args[1]);
            }
            if (args.length == 3) {
                return List.of();
            }
            if (args.length >= 4 && args.length <= 6 && sender instanceof Player player) {
                int axis = args.length - 4;
                return matchingPrefix(coordinateSuggestions(player, axis), args[args.length - 1]);
            }
        }

        return List.of();
    }

    /**
     * Mirrors vanilla position suggestions: the player's own coordinate on the given axis
     * (0 = x, 1 = y, 2 = z) plus the "~" relative marker.
     */
    private List<String> coordinateSuggestions(Player player, int axis) {
        Location location = player.getLocation();
        double coordinate = switch (axis) {
            case 0 -> location.getX();
            case 1 -> location.getY();
            default -> location.getZ();
        };
        return List.of("~", String.valueOf((int) Math.floor(coordinate)));
    }

    private List<String> matchingPrefix(List<String> options, String prefix) {
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                matches.add(option);
            }
        }
        return matches;
    }
}
