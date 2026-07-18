package me.simoncrafter.mCCodeCampLibrary.internal.editor;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.StringWithRulesInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.MessageAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.QuestionAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.ConfirmQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.YesNoQuestion;
import me.simoncrafter.CraftersDisplayLibrary.core.PropertyLock;
import me.simoncrafter.CraftersDisplayLibrary.core.interfaces.ICuboidDisplay;
import me.simoncrafter.CraftersDisplayLibrary.core.interfaces.IDisplayable;
import me.simoncrafter.CraftersDisplayLibrary.display.cube.CubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.panel.TextDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorInformation;
import me.simoncrafter.CraftersDisplayLibrary.effect.highlighter.BlockHighlighter;
import me.simoncrafter.CraftersDisplayLibrary.effect.highlighter.HighlightDisplayType;
import me.simoncrafter.CraftersDisplayLibrary.effect.highlighter.prefabs.PingHighlighter;
import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerEntry;
import me.simoncrafter.mCCodeCampLibrary.internal.BlockMarkerRegistry;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class LocationEditor {

    private static final String REGION_TRIGGER_PREFIX = "trigger_";
    private static final Color TRIGGER_REGION_COLOR = Color.fromARGB(180, 220, 60, 60);

    private static Map<Location, ICuboidDisplay> displayMap = new HashMap<>();
    private static Map<ProtectedRegion, WireframeCubeColorDisplay> worldguardRegionDisplays = new HashMap<>();
    private static Set<Player> players = new LinkedHashSet<>();

    private static Map<Player, Block> createNewPlayerTargetBlock = new HashMap<>();
    private static Map<Player, AtomicReference<String>> inputtedID = new HashMap<>();

    public static void showAddDialogToPlayer(Player player) {
        Set<Material> transparent = new HashSet<>();
        transparent.add(Material.AIR);

        if (createNewPlayerTargetBlock.containsKey(player)) {
            BlockHighlighter.unhighlightBlock(createNewPlayerTargetBlock.get(player));
        }

        createNewPlayerTargetBlock.put(player, player.getTargetBlock(transparent, 5));
        BlockHighlighter.highlightBlock(createNewPlayerTargetBlock.get(player), HighlightDisplayType.CUBE, new PingHighlighter(0.01f, 2f, Color.GREEN, 20), 200);

        GenericQuestion inputQuestion = GenericQuestion.create().question(Component.text("What type of input do you want to create?", NamedTextColor.BLUE))
                .addButton(getInputTypeButton(Component.text("[Button]", NamedTextColor.GREEN), "button"))
                .addButton(getInputTypeButton(Component.text("[Plate]", NamedTextColor.GREEN), "plate"));


        GenericQuestion question = GenericQuestion.create().question(Component.text("What do you want to create?", NamedTextColor.BLUE))
                .addButton(Button.create().text(Component.text("[Input]")).addAction(QuestionAction.create(inputQuestion)));
        question.show(player);
    }

    public static void showRemoveDialogToPlayer(Player player, Block block) {
        if (!MCCodeCampLib.getBlockMarkerRegistry()
                .hasMarker(block)) {
            return;
        }
        ConfirmQuestion confirmQuestion = ConfirmQuestion.create(Component.text("Do you really want to remove this entry?", NamedTextColor.RED))
                .addConfirmAction(CustomAction.create(p -> MCCodeCampLib.getBlockMarkerRegistry().removeMarker(block)))
                .addCancelAction(MessageAction.create(Component.text("Canceled", NamedTextColor.RED)));
        confirmQuestion.show(player);
    }

    private static Button getInputTypeButton(Component display, String type) {
        return Button.create()
                .text(display)
                .addAction(getIDInputAction(type));
    }

    private static StringWithRulesInputAction getIDInputAction(String type) {
        return StringWithRulesInputAction.create(p -> s -> {
                    AtomicReference<String> val = inputtedID.getOrDefault(p, new AtomicReference<>());
                    val.set(s);
                    inputtedID.put(p, val);
                })
                .prompt(Component.text("Please enter a ID for the new input! (String, lowercase, no spaces)", NamedTextColor.BLUE))
                .regexRule("^[a-zA-Z0-9_-]+$")
                .addSuccessAction(CustomAction.create(p -> {
                    MCCodeCampLib.getBlockMarkerRegistry().setMarker(createNewPlayerTargetBlock.get(p), type, inputtedID.get(p).get());
                    createNewPlayerTargetBlock.remove(p);
                    inputtedID.remove(p);
                }));
    }


    public static void showForPlayer(Player player) {
        if (displayMap.isEmpty()) {
            updateFromBlockMarkerCache();
        }
        if (worldguardRegionDisplays.isEmpty()) {
            updateWorldGuardRegionDisplays();
        }
        setVisibilityForEveryDisplayForPlayer(player, false);
        players.add(player);
    }

    public static void togglePlayer(Player player) {
        if (players.contains(player)) {
            hideForPlayer(player);
        } else {
            showForPlayer(player);
        }
    }

    public static void hideForPlayer(Player player) {
        setVisibilityForEveryDisplayForPlayer(player, true);
        players.remove(player);
        despawnAllDisplaysIfNoViewers();
    }

    public static boolean isVisibleForPlayer(Player player) {
        return players.contains(player);
    }

    public static void clearPlayerState(Player player) {
        createNewPlayerTargetBlock.remove(player);
        inputtedID.remove(player);
        players.remove(player);
        despawnAllDisplaysIfNoViewers();
    }

    /** Despawns every display and clears the caches once nobody is left viewing them, so they get rebuilt fresh next time someone toggles visibility on. */
    private static void despawnAllDisplaysIfNoViewers() {
        if (players.isEmpty()) {
            clearHighlights();
            clearWorldGuardRegionDisplays();
        }
    }


    private static void setVisibilityForEveryDisplayForPlayer(Player player, boolean hidden) {
        for (ICuboidDisplay display : displayMap.values()) {
            doForEveryChild(display, d -> {
                if (d instanceof IDisplayable hidable) {
                    if (hidden) {
                        hidable.hideForPlayer(player);
                    } else {
                        hidable.showForPlayer(player);
                    }
                }
            });
        }
        for (WireframeCubeColorDisplay display : worldguardRegionDisplays.values()) {
            if (hidden) {
                display.hideForPlayer(player);
            } else {
                display.showForPlayer(player);
            }
        }
    }

    private static void doForEveryChild(IDisplayable display, Consumer<IDisplayable> displayConsumer) {
        for (IDisplayable d : display.getChildren()) {
            displayConsumer.accept(d);
            doForEveryChild(d, displayConsumer);
        }
    }

    static void updateFromBlockMarkerCache() {
        Bukkit.broadcast(Component.text("Update!"));
        clearHighlights();
        Map<Location, BlockMarkerEntry> map = MCCodeCampLib.getBlockMarkerRegistry().getCache();
        for (Location l : map.keySet()) {
            BlockMarkerEntry entry = map.get(l);

            ICuboidDisplay display = BlockHighlighter.highlightBlock(l.getBlock(), getColorFromType(entry.type()));
            TextDisplay text = TextDisplay.create(l, new Vector3f(1, 1, 1), new Vector3f(), new Quaternionf());
            text.setBillboard(Display.Billboard.CENTER);
            text.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
            text.spawnDisplay();
            text.setSeeThrough(true);
            text.hideByDefault(true);
            text.moveAbsolute(new Vector3f(0.5f, 0.5f, 0.5f), 0);
            text.setText(Component.text(entry.id()));
            CubeColorDisplay cube = (CubeColorDisplay) display;
            cube.setSeeThrough(true);
            cube.hideByDefault(true);
            display.addChild(text);

            Bukkit.broadcast(Component.text(entry.type() + " " + entry.id()));
            displayMap.put(l, display);
        }
        for (Player p : players) {
            setVisibilityForEveryDisplayForPlayer(p, false);
        }
    }


    static void updateWorldGuardRegionDisplays() {
        clearWorldGuardRegionDisplays();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        for (World world : Bukkit.getWorlds()) {
            RegionManager manager = container.get(BukkitAdapter.adapt(world));
            if (manager == null) continue;

            for (ProtectedRegion region : manager.getRegions().values()) {
                if (region.getId().startsWith(REGION_TRIGGER_PREFIX)) {
                    displayWorldGuardRegion(world, region);
                }
            }
        }
        for (Player p : players) {
            setVisibilityForEveryDisplayForPlayer(p, false);
        }
    }

    private static void displayWorldGuardRegion(World world, ProtectedRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        Vector3f scale = new Vector3f(
                max.getX() - min.getX() + 1,
                max.getY() - min.getY() + 1,
                max.getZ() - min.getZ() + 1
        );
        Location center = BukkitAdapter.adapt(world, min.toVector3().add(max.toVector3()).add(1, 1, 1).multiply(0.5));

        TextDisplay text = TextDisplay.create(center, new Vector3f(1, 1, 1), new Vector3f(), new Quaternionf());
        text.setSeeThrough(true);
        text.setAlignment(org.bukkit.entity.TextDisplay.TextAlignment.CENTER);
        text.setBillboard(Display.Billboard.CENTER);
        text.setText(region.getId().substring(8)); // remove "trigger_"
        text.setPropertyLock(new PropertyLock(false, false, false, false, false, false, true, true, true, false, false, false));

        Vector3f textScale = new Vector3f(Math.max(scale.x*0.25f, 1), Math.max(scale.y*0.25f, 1), Math.max(scale.z*0.25f, 1));


        text.scaleAbsolute(textScale, 0);

        WireframeCubeColorDisplay display = WireframeCubeColorDisplay.create(
                center,
                scale,
                new Vector3f(),
                new Quaternionf(),
                new WireframeCubeColorInformation(TRIGGER_REGION_COLOR),
                true
        );
        display.addChild(text);
        text.spawnDisplay();
        display.spawnDisplay();
        display.hideByDefault(true);

        worldguardRegionDisplays.put(region, display);
    }

    private static void clearWorldGuardRegionDisplays() {
        for (WireframeCubeColorDisplay display : worldguardRegionDisplays.values()) {
            display.remove();
        }
        worldguardRegionDisplays = new HashMap<>();
    }

    private static Color getColorFromType(String type) {
        return switch (type) {
            case "button" -> Color.fromARGB(100, 200, 200, 50);
            case "plate" -> Color.fromARGB(100, 50, 200, 200);
            case "lever" -> Color.fromARGB(100, 200, 50, 200);
            default -> Color.fromARGB(100, 200, 200, 200);
        };
    }

    private static void clearHighlights() {
        for (ICuboidDisplay d : displayMap.values()) {
            BlockHighlighter.unhighlightBlock(d.getLocation().getBlock());
        }
        displayMap = new HashMap<>();
    }

}
