# Projectile target block

`ProjectileTarget` implements `IBlockRegestryObject`, `IEditable`, and
`IEditorObjectDescriptor`. `MCCodeCampLib.init` registers its factory as
`projectile_target`. It does not extend `ActivationButton`: right-clicks
never trigger projectile activation. Editor display clicks only select/edit it.

Configuration, persisted by the public setters through `saveObject(this)`:

| Key | Default | Setter |
| --- | --- | --- |
| `detectVanillaProjectiles` | `false` | `setDetectVanillaProjectiles(boolean)` |
| `cooldown` | `0` ticks | `setCooldown(int)` |
| `blockdata` | absent | `setBlockData(BlockData)` |
| `cooldownBlockdata` | absent | `setCooldownBlockdata(BlockData)` |

Null block data clears the optional value. Invalid saved block data logs a warning
and is ignored. Negative setter cooldowns are rejected; negative saved durations
load as zero. Block data is not applied on initialization. With both visual states
configured, accepted hits apply the cooldown state and tick expiry restores the
normal state. A zero-duration target can trigger on every accepted hit. Changes to
duration affect future starts only.

Both impact handlers run at `MONITOR`, `ignoreCancelled = true`, and explicitly
check cancellation for safe direct invocation. Custom hits are pre-impact
notifications: targets observe an accepted hit, never change cancellation, and
emit noncancellable `ProjectileTargetHitEvent`. Do not cancel hits from a later
MONITOR listener; that violates Bukkit monitor conventions. Only a matching world
and all three block coordinates activate the target. Entity-only hits do not.

The notification exposes `getTarget()`, `getID()`, `getCustomProjectile()`,
`getVanillaProjectile()`, `getShooter()`, `getHitPosition()`, `getHitFace()`.
Exactly one projectile field is nonnull. Shooter and face may be null. Custom
positions come from the collision vector; vanilla positions come from Bukkit's
projectile location, not a promised exact ray-intersection position. Location
values are defensively copied. Vanilla dispenser/block shooters have no Entity
shooter and therefore report null.

Call `destroy()` on removal, chunk unload, and plugin disable. It unregisters
listeners, disposes the central cooldown, removes editor visuals, and makes the
instance inert. Reinitialization first destroys the old attachments. It does not
restore blocks on destruction (avoids chunk writes during unload). All lifecycle,
configuration and Bukkit operations belong on the server thread. The registry and
plugin own registration and disable integration; this class does not start tasks.

Run `python MCCodeCampLibrary/tests/run_projectile_target.py` from the repository
root after generating `target/editor-classpath.txt` using the existing editor test
setup. This is a standalone production-code/API-boundary harness, not an in-game
rendering or persistence round-trip test. While the custom projectile implementation
is being developed independently, `--contract-doubles` explicitly substitutes only
that API with temporary test doubles; it also exercises custom-hit activation,
precise hit-position forwarding, and cancellation. Its output is not evidence that
the real projectile implementation compiles or behaves correctly.
