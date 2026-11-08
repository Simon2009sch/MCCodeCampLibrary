package me.simoncrafter.mCCodeCampLibrary.projectile;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/** A single precise impact. Exactly one target is required; face may be null for custom shapes. */
public record ProjectileHit(@Nullable Block block, @Nullable Entity entity, Vector position, @Nullable BlockFace face) {
    public ProjectileHit {
        if ((block == null) == (entity == null)) throw new IllegalArgumentException("Supply exactly one block or entity");
        position = Objects.requireNonNull(position, "position").clone();
        position.checkFinite();
    }
    @Override public Vector position() { return position.clone(); }
}
