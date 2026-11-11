package me.simoncrafter.mCCodeCampLibrary.internal.editor.area;

import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.CustomAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.actions.InputActions.LocationInputAction;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.buttons.Button;
import me.simoncrafter.CraftersChatDialogs.dialogs.prefabs.questions.GenericQuestion;
import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorDisplay;
import me.simoncrafter.CraftersDisplayLibrary.display.wireframecube.WireframeCubeColorInformation;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.AEditor;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.events.PlayerClickEditableObjectEvent;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarItem;
import me.simoncrafter.mCCodeCampLibrary.internal.editor.hotbarmenu.HotbarMenu;
import me.simoncrafter.mCCodeCampLibrary.obstical.AOpenableObject.OpenableRegion;
import me.simoncrafter.mCCodeCampLibrary.utility.MCCodeCampLib;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;
import java.util.function.*;

/** Reusable per-player cuboid drafts. Register with an owner; leave discards unsaved changes. */
public class CuboidAreaEditor extends AEditor {
    private final World world;
    private final Supplier<? extends Collection<OpenableRegion>> initial;
    private final BiConsumer<Player, List<OpenableRegion>> saveAction;
    private final Map<UUID, CuboidAreaSelection> drafts = new HashMap<>();
    private final Map<UUID, List<PositionObject>> outlines = new HashMap<>();

    /** Areas are absolute inclusive world block coordinates; only Save invokes the callback. */
    public CuboidAreaEditor(Plugin plugin, World world,
                            Supplier<? extends Collection<OpenableRegion>> initial,
                            BiConsumer<Player, List<OpenableRegion>> saveAction) {
        super(plugin);
        this.world = Objects.requireNonNull(world, "world");
        this.initial = Objects.requireNonNull(initial, "initial");
        this.saveAction = Objects.requireNonNull(saveAction, "saveAction");
    }

    /** Convenience adapter for marker-independent area input; Save calls back, Back pops. */
    public CuboidAreaEditor(Plugin plugin, World world, Collection<OpenableRegion> initial,
                            Consumer<Set<OpenableRegion>> saveAction) {
        this(plugin, world, () -> initial,
                (player, areas) -> saveAction.accept(new LinkedHashSet<>(areas)));
    }

    @Override protected void join(Player player) {
        super.join(player);
        drafts.put(player.getUniqueId(), new CuboidAreaSelection(initial.get()));
    }

    @Override protected void leave(Player player) {
        clearOutlines(player);
        drafts.remove(player.getUniqueId());
        super.leave(player);
    }

    /** Checks both editor membership and current navigation before any delayed callback. */
    protected final boolean active(Player player) {
        return drafts.containsKey(player.getUniqueId())
                && MCCodeCampLib.getEditorManager().getPlayersCurrentEditor(player) == this;
    }

    /** Gets this player's draft; never share it with other editor viewers. */
    protected final CuboidAreaSelection draft(Player player) { return drafts.get(player.getUniqueId()); }

    @Override protected void onPlayerClickObjectEvent(PlayerClickEditableObjectEvent event) { }

    @Override public void displayDialog(Player player, String path, Map<String, Object> context) {
        if (!active(player)) return;
        showHome(player);
    }

    /** Shows area controls; subclasses may add controls without replacing selection behavior. */
    protected void showHome(Player player) {
        if (!active(player)) return;
        refreshOutlines(player);
        GenericQuestion question = GenericQuestion.create(Component.text("Areas (" + draft(player).getAreas().size() + ")"));
        HotbarMenu menu = new HotbarMenu(getPlugin());
        addControl(question, menu, 0, Material.STICK, "Select corners: click two blocks", p ->
                p.sendMessage(Component.text("Use the corner tool on two blocks; repeat for more areas.")));
        // The selection tool deliberately retains reach; all other controls are menu-only.
        HotbarItem corners = item(Material.STICK, "Select corners (two block clicks)", true);
        corners.addBlockClickAction(event -> {
            if (!active(event.getPlayer())) return;
            Player p = event.getPlayer();
            selectCornerAt(p,event.getClickedBlock()!=null?event.getClickedBlock().getLocation():
                    p.isSneaking()?p.getEyeLocation():p.getLocation());
        });
        menu.setItemAt(0, corners);
        question.addButton(Button.create().text(Component.text("[Type corner coordinates]"))
                .addAction(CustomAction.create(this::typeCorner)));
        question.addButton(Button.create().text(Component.text("[Corner at player feet]"))
                .addAction(CustomAction.create(p->{if(active(p)){selectCornerAt(p,p.getLocation());showHome(p);}})));
        addControl(question, menu, 1, Material.PAPER, "Cancel corner", p -> { draft(p).cancelCorner(); showHome(p); });
        addControl(question, menu, 2, Material.SHEARS, "Remove last area", p -> {
            int count = draft(p).getAreas().size();
            if (count > 0) draft(p).removeArea(count - 1);
            showHome(p);
        });
        addControl(question, menu, 3, Material.BUCKET, "Clear areas", p -> { draft(p).clear(); showHome(p); });
        List<OpenableRegion> areas = draft(player).getAreas();
        CuboidAreaSelection expected = draft(player);
        for (int i = 0; i < areas.size(); i++) {
            OpenableRegion area = areas.get(i);
            question.addButton(Button.create().text(Component.text("[Remove area " + (i + 1) + ": " + area.getMin() + " → " + area.getMax() + "]"))
                    .addAction(CustomAction.create(p -> {
                        if (!active(p) || draft(p) != expected) return;
                        // Snapshot identity prevents stale chat buttons from removing a different index.
                        int index = draft(p).getAreas().indexOf(area);
                        if (index >= 0) draft(p).removeArea(index);
                        showHome(p);
                    })));
        }
        configureHome(player, question, menu);
        addControl(question, menu, 7, Material.EMERALD, "Save", p -> {
            save(p, draft(p).getAreas());
            p.sendMessage(Component.text("Saved."));
            showHome(p);
        });
        addControl(question, menu, 8, Material.BARRIER, "Back (discard unsaved changes)", p -> MCCodeCampLib.getEditorManager().pop(p));
        question.show(player, getUUID() + ":" + player.getUniqueId());
        menu.show(player);
    }

    /** Adds feature-specific controls at slots 4–6. */
    protected void configureHome(Player player, GenericQuestion question, HotbarMenu menu) { }

    /** Commits a detached snapshot only on explicit Save. */
    protected void save(Player player, List<OpenableRegion> areas) { saveAction.accept(player, areas); }

    /** Adds a guarded chat/hotbar action to the same menu. */
    protected final void addControl(GenericQuestion question, HotbarMenu menu, int slot,
                                    Material material, String label, Consumer<Player> action) {
        Consumer<Player> guarded = p -> {
            if (!active(p)) return;
            try { action.accept(p); }
            catch (IllegalArgumentException | IllegalStateException | java.io.UncheckedIOException e) {
                p.sendMessage(Component.text(e.getMessage(), net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        };
        question.addButton(Button.create().text(Component.text("[" + label + "]"))
                .addAction(CustomAction.create(guarded)));
        menu.setItemAt(slot, item(material, label, false).addBlockClickAction(e -> guarded.accept(e.getPlayer())));
    }

    /** Creates an editor item; world-targeting tools retain reach. */
    protected final HotbarItem item(Material material, String label, boolean worldTargeting) {
        ItemStack stack = new ItemStack(material);
        var meta = stack.getItemMeta();
        meta.customName(Component.text(label).decoration(TextDecoration.ITALIC, false));
        stack.setItemMeta(meta);
        HotbarItem item = new HotbarItem(getPlugin(), stack);
        if (material.isBlock()) item.setNoInteractMode(HotbarItem.NoInteractMode.Masquerade);
        else if (!worldTargeting) item.setNoInteractMode(HotbarItem.NoInteractMode.NoReach);
        return item;
    }

    private void typeCorner(Player player) {
        if(!active(player))return;
        CuboidAreaSelection expected=draft(player);
        LocationInputAction.create(p->location->{
            if(!active(p)||draft(p)!=expected)return;
            if(location.getWorld()==null)location.setWorld(world);
            selectCornerAt(p,location);showHome(p);
        }).reTry(true).addReTryAction(CustomAction.create(this::typeCorner))
                .addCancelAction(CustomAction.create(p->{if(active(p))showHome(p);})).run(player);
    }

    private void selectCornerAt(Player player,org.bukkit.Location location) {
        if(!active(player))return;
        if(!location.getWorld().equals(world)) {
            player.sendMessage(Component.text("Select corners in the marker's world."));return;
        }
        if(!Double.isFinite(location.getX())||!Double.isFinite(location.getY())||!Double.isFinite(location.getZ())
                ||Math.abs(location.getX())>29999984||Math.abs(location.getZ())>29999984
                ||location.getY()<world.getMinHeight()||location.getY()>=world.getMaxHeight()) {
            player.sendMessage(Component.text("Corner is outside world bounds."));return;
        }
        boolean complete=draft(player).selectCorner(location.toVector());
        player.sendMessage(Component.text(complete?"Area added. Select more corners or Save.":"First corner selected."));
        refreshOutlines(player);
    }

    private void refreshOutlines(Player player) {
        clearOutlines(player);
        List<PositionObject> owned = new ArrayList<>();
        outlines.put(player.getUniqueId(), owned);
        try {
            for (OpenableRegion area : draft(player).getAreas()) {
                var center = CuboidAreaSelection.center(area);
                var size = CuboidAreaSelection.size(area);
                var wire = WireframeCubeColorDisplay.create(center.toLocation(world),
                        new Vector3f((float) size.getX(), (float) size.getY(), (float) size.getZ()),
                        new Vector3f(), new Quaternionf(),
                        new WireframeCubeColorInformation(Color.fromARGB(100, 0, 255, 255)), true, .01f);
                owned.add(wire);
                wire.hideByDefault(true, true);
                wire.spawnDisplay();
                wire.showForPlayer(player, true);
            }
        } catch (RuntimeException | Error failure) {
            clearOutlines(player);
            throw failure;
        }
    }

    private void clearOutlines(Player player) {
        List<PositionObject> removed = outlines.remove(player.getUniqueId());
        if (removed != null) removed.forEach(PositionObject::remove);
    }
}
