package me.simoncrafter.mCCodeCampLibrary.projectile;

import me.simoncrafter.CraftersDisplayLibrary.display.panel.BlockDisplayObject;
import me.simoncrafter.mCCodeCampLibrary.projectile.effect.MeteorVisuals;
import org.bukkit.Location;
import org.bukkit.Material;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.Objects;
import java.util.function.Consumer;

/** Harmless falling meteor. The manager owns ticking and removal; the caller owns impact effects. */
public final class MeteorProjectile extends CustomProjectile {
    private final Consumer<Location> onImpact;
    private BlockDisplayObject display;

    public MeteorProjectile(Consumer<Location> onImpact) {
        this.onImpact = Objects.requireNonNull(onImpact, "onImpact");
    }
    @Override protected void onShoot() {
        Location position = getLocation();
        Location anchor = MeteorVisuals.nearestAnchor(position);
        display = BlockDisplayObject.create(anchor, new Vector3f(4),
                position.toVector().subtract(anchor.toVector()).toVector3f(), Material.MAGMA_BLOCK.createBlockData());
        display.spawnDisplay().setPersistent(false);
    }
    @Override protected void onTravel(Location from, Location to) {
        if (display == null) return;
        display.LRotateAbsoluteNoUpdate(new Quaternionf().rotateXYZ(getAge() * .08f, getAge() * .11f, 0));
        display.moveAbsolute(to.toVector().subtract(display.getLocation().toVector()).toVector3f(), 1);
    }
    @Override protected void onHit(ProjectileHit hit) {
        try { onImpact.accept(getLocation()); } finally { remove(); }
    }
    @Override protected void onRemove() {
        if (display != null) { display.remove(); display = null; }
    }
}
