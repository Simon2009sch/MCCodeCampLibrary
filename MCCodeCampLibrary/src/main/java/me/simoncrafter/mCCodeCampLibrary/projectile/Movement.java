package me.simoncrafter.mCCodeCampLibrary.projectile;
import org.bukkit.util.Vector;
/** Returns this tick's displacement; may update velocity for the next tick. */
@FunctionalInterface
public interface Movement {
    Vector move(CustomProjectile projectile);
}
