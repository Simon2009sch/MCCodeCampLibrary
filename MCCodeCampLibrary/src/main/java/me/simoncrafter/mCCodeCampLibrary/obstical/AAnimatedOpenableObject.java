package me.simoncrafter.mCCodeCampLibrary.obstical;

import me.simoncrafter.CraftersDisplayLibrary.builder.StructureBuilder;
import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.events.OpenableObjectStateChangeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AAnimatedOpenableObject extends AOpenableObject {

    protected int animationDuration;
    protected Vector pivotPosition;
    protected boolean addCollision;
    protected boolean disassemble = true;
    protected BiConsumer<PositionObject, Integer> openAnimationFunction = null;
    protected BiConsumer<PositionObject, Integer> closeAnimationFunction = null;
    private PositionObject doorObject = null;

    public AAnimatedOpenableObject(String ID, Location origen, Set<Vector> blocks, int animationDuration, Vector pivotPosition, boolean addCollision) {
        super(ID, origen, blocks);
        this.animationDuration = animationDuration;
        this.pivotPosition = pivotPosition;
        this.addCollision = addCollision;
    }

    @Override
    public void open(boolean skipTransition) {
        if (state != OpenableState.CLOSED && state != null) {
            return;
        }
        state = OpenableState.TRANSITION;
        new OpenableObjectStateChangeEvent(ID, OpenableState.CLOSED, OpenableState.TRANSITION).callEvent();
        List<Material> blocksToIgnore = new ArrayList<>();
        blocksToIgnore.add(Material.AIR);
        blocksToIgnore.add(Material.WATER);
        blocksToIgnore.add(Material.LAVA);
        if (doorObject == null) {
            List<Vector> absoluteBlocks = blocks.stream().map(v -> origen.clone().add(v).toVector()).toList();
            doorObject = StructureBuilder.assembleOutOfBlocks(origen.getWorld(), origen.clone().add(pivotPosition).toVector(), absoluteBlocks, blocksToIgnore, addCollision, true, Material.AIR, true);
            Bukkit.broadcast(Component.text("Spawning display!"));
        }
        if (openAnimationFunction != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    openAnimationFunction.accept(doorObject, animationDuration);
                }
            }.runTaskLater(plugin, 2);
        } else {
            Bukkit.broadcast(Component.text("Animation open function is null"));
        }
        scheduleState(OpenableState.OPENED, () -> {}, animationDuration);
    }


    private void scheduleState(OpenableState newState, @Nullable Runnable task, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                state = newState;
                Bukkit.broadcast(Component.text("Running Task"));
                if (task != null) task.run();
            }
        }.runTaskLater(plugin, delay);
    }


    @Override
    public void close(boolean skipTransition) {
        if (state != OpenableState.OPENED && state != null) {
            return;
        }
        state = OpenableState.TRANSITION;
        new OpenableObjectStateChangeEvent(ID, OpenableState.OPENED, OpenableState.TRANSITION).callEvent();
        if (closeAnimationFunction != null) {
            closeAnimationFunction.accept(doorObject, animationDuration);
        } else {
            Bukkit.broadcast(Component.text("Animation close function is null"));
        }
        scheduleState(OpenableState.CLOSED, () -> {
            if (disassemble) {
                StructureBuilder.disassembleOutOfObject(doorObject);
                Bukkit.broadcast(Component.text("Disassembling door"));
                doorObject.remove();
                doorObject = null;
            }
        }, animationDuration);
    }



    public void setDisassemble(boolean disassemble) {
        this.disassemble = disassemble;
    }
}
