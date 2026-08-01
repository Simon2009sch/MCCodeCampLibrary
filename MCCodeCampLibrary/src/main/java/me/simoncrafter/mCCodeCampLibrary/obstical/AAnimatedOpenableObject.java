package me.simoncrafter.mCCodeCampLibrary.obstical;

import me.simoncrafter.CraftersDisplayLibrary.builder.StructureBuilder;
import me.simoncrafter.CraftersDisplayLibrary.core.PositionObject;
import me.simoncrafter.mCCodeCampLibrary.obstical.events.OpenableObjectStateChangeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class AAnimatedOpenableObject extends AOpenableObject {

    private int animationDuration;
    private Vector pivotPosition;
    private boolean addCollision;
    private boolean disassemble = true;
    protected BiConsumer<PositionObject, Integer> openAnimationFunction = null;
    protected BiConsumer<PositionObject, Integer> closeAnimationFunction = null;
    private PositionObject doorObject = null;

    @Override
    protected void readConfig() {
        ConfigurationNode config = getConfig();

        animationDuration = config.node("animationDuration").getInt(20);

        pivotPosition = readVector(config.node("pivotpoint"), 0.5, 0.5, 0.5);

        addCollision = config.node("addCollision").getBoolean(false);

        disassemble = config.node("disassemble").getBoolean(true);
    }

    @Override
    public void open(boolean skipTransition) {
        if (getState() != OpenableState.CLOSED && getState() != null) {
            return;
        }
        setState(OpenableState.TRANSITION);
        new OpenableObjectStateChangeEvent(getID(), OpenableState.CLOSED, OpenableState.TRANSITION).callEvent();
        List<Material> blocksToIgnore = new ArrayList<>();
        blocksToIgnore.add(Material.AIR);
        blocksToIgnore.add(Material.WATER);
        blocksToIgnore.add(Material.LAVA);
        if (doorObject == null) {
            List<Vector> absoluteBlocks = getBlocks().stream().map(v -> getLocation().clone().add(v).toVector()).toList();
            doorObject = StructureBuilder.assembleOutOfBlocks(getLocation().getWorld(), getLocation().clone().add(pivotPosition).toVector(), absoluteBlocks, blocksToIgnore, addCollision, true, Material.AIR, true);
            Bukkit.broadcast(Component.text("Spawning display!"));
        }
        if (openAnimationFunction != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    openAnimationFunction.accept(doorObject, animationDuration);
                }
            }.runTaskLater(getPlugin(), 2);
        } else {
            Bukkit.broadcast(Component.text("Animation open function is null"));
        }
        scheduleState(OpenableState.OPENED, () -> {}, animationDuration);
    }


    private void scheduleState(OpenableState newState, @Nullable Runnable task, int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                setState(newState);
                Bukkit.broadcast(Component.text("Running Task"));
                if (task != null) task.run();
            }
        }.runTaskLater(getPlugin(), delay);
    }


    @Override
    public void close(boolean skipTransition) {
        if (getState() != OpenableState.OPENED && getState() != null) {
            return;
        }
        setState(OpenableState.TRANSITION);
        new OpenableObjectStateChangeEvent(getID(), OpenableState.OPENED, OpenableState.TRANSITION).callEvent();
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



    public int getAnimationDuration() {
        return animationDuration;
    }

    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
        try {
            getConfig().node("animationDuration").set(animationDuration);
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("There was an error while trying to serialize animationDuration into config!");
        }
    }

    public Vector getPivotPosition() {
        return pivotPosition.clone();
    }

    public void setPivotPosition(Vector pivotPosition) {
        this.pivotPosition = pivotPosition;
        try {
            getConfig().node("pivotpoint").from(writeVector(pivotPosition));
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("There was an error while trying to serialize a vector into config!");
        }
    }

    public boolean isAddCollision() {
        return addCollision;
    }

    public void setAddCollision(boolean addCollision) {
        this.addCollision = addCollision;
        try {
            getConfig().node("addCollision").set(addCollision);
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("There was an error while trying to serialize addCollision into config!");
        }
    }

    public boolean isDisassemble() {
        return disassemble;
    }

    public void setDisassemble(boolean disassemble) {
        this.disassemble = disassemble;
        try {
            getConfig().node("disassemble").set(disassemble);
        } catch (SerializationException e) {
            getPlugin().getLogger().warning("There was an error while trying to serialize disassemble into config!");
        }
    }
}
