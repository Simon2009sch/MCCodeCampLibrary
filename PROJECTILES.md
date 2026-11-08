# Student projectiles, targets and item cooldowns (`tdot`)

All projectile/world/item operations run on the **server thread**. The hosting plugin must call `MCCodeCampLib.init(plugin)` and `MCCodeCampLib.onDisable()` as the test plugin already does. Initialization is once per plugin classloader: after shutdown, a fresh classloader is required (normal PlugMan reload/restart), rather than silently trying to reuse the disabled library.

## In-game test fixtures

The test plugin installs handlers for these marker IDs but does not place anything in the world:

| Marker type | ID | Behavior |
| --- | --- | --- |
| `button` | `projectile_test_items` | Gives four harmless preset wands plus a bow, arrows and vanilla snowballs |
| `projectile_target` | `projectile_test_target` | Sends formatted impact debug to the shooter |

Right-click a wand to shoot; each stack has a 10-tick cooldown and 64-block range. The wands are `ACustomItemHandler` implementations in the test plugin’s `testing/ProjectileTestWand.java`; `testing/ProjectileTestSupport.java` registers them with `ItemsManager` and gives them through `createNewItem`. The fixture does not separately dispatch item-click events. If you still have the earlier direct-tag test wands, click the kit button again to obtain the item-system versions. The debug target explicitly opts into vanilla-projectile hits when created/loaded. Debug shows projectile, shooter, world, precise/custom hit position, nullable face, and for custom shots age, travelled distance and velocity. Dispenser/unattributed impacts are sent to connected players who used the test-kit button. Fixture listeners are unregistered on plugin disable.

## Shoot a premade projectile

```java
import me.simoncrafter.mCCodeCampLibrary.projectile.*;

// player is a Bukkit Player. Speed is blocks per tick, not blocks per second.
new FireballProjectile(4).shoot(player, 1.2);
new ArrowProjectile(2).shoot(player, 2);
new SnowballProjectile().shoot(player, 1);
new RaycastProjectile(2).setMaxDistance(64).shoot(player, 1);
```

These are **custom simulated projectiles**, not spawned vanilla entities. Arrow and snowball presets approximate vanilla gravity/drag; fireballs travel straight. Fireballs damage entities but **do not explode, set fires or change blocks**. Snowballs are harmless and render a centered snow-block display plus their particle trail. The snow block faces the actual travelled direction (including gravity), and follows with CDL `moveAbsolute(localOffset, 1)` one-tick interpolation. It is temporary/nonpersistent and is removed together with its projectile. A raycast travels its whole remaining range in its first tick; its speed argument supplies a direction, not range.

Defaults: maximum distance `128` blocks, maximum lifetime `200` ticks, entity ray expansion `0.15` blocks. The configurable range is capped at `4096` blocks. Block collisions use Paper's exact block shapes with fluids/passable blocks ignored. Entities are ray-traced over the complete segment, so fast projectiles do not simply sample their endpoint. The shooter, spectators and display/interaction entities are excluded by default. The centerline is clipped before unloaded chunks or world-height limits. This guard is API-boundary-tested, not proof that Paper never inspects neighboring chunks for complex shapes; real-server boundary testing remains necessary. Entity ray size expands entity bounding boxes only; block collisions remain zero-width rays.

## Add callbacks without a new class

```java
new ParticleProjectile()
    .setMovement("snowball_like") // arrow_like, snowball_like, fireball_like, raycast
    .whenTick(projectile -> {
        // Check something each tick; getLocation()/getVelocity() return copies.
    })
    .whenHit(hit -> {
        if (hit.block() != null) {
            // A precise hit position and an optional BlockFace are available.
        }
        if (hit.entity() != null) {
            // React to an entity impact.
        }
    })
    .shoot(player, 1.5);
```

`whenHit` runs only after the hit event was accepted, before the projectile's own `onHit`. The default `onHit` removes the projectile. Call `remove()`/`close()` to stop early. Each instance can be shot once; create a new one for the next shot.

## Extend a projectile

```java
class PracticeBolt extends ParticleProjectile {
    PracticeBolt() {
        super(ProjectileMovement.ARROW_LIKE, org.bukkit.Particle.END_ROD);
    }

    @Override protected void onTick() {
        // Custom checks or particles at getLocation().
    }

    @Override protected org.bukkit.util.Vector move() {
        // Or simply inherit move() to use the selected movement preset.
        return super.move();
    }

    @Override protected void onHit(ProjectileHit hit) {
        // Your behavior belongs here: a cancelled hit never calls this hook.
        // hit.block() XOR hit.entity() is non-null. hit.face() may be null.
        super.onHit(hit); // Stop. Omit this if you deliberately retain the projectile.
    }
}
```

Movement is independently replaceable: `setMovement(projectile -> displacement)` supplies a per-tick displacement and can call `setVelocity` for the next tick. Alternatively override `move()`.

Advanced subclasses can override `hit(Location from, Vector displacement)`. Return `null` for **no collision**, or `new ProjectileHit(block, null, position, null)` / `new ProjectileHit(null, entity, position, null)` when a face cannot be determined. Exactly one target is mandatory, and the finite hit position must lie on the traced segment. For entity hits, `position()` is the world-space impact point; `face()` is the bounding-box face when provided by Paper. `onTravel(from,to)` supports continuous trails, including rays. Data-bearing particles need a custom renderer; `ParticleProjectile` accepts data-free particles.

For a retained/piercing projectile, override collision/movement as needed to avoid hitting the same surface again. Accepted retained hits end the current tick's movement; a raycast always finishes in one tick. Cancelled targets are ignored for the rest of the flight by the default collision detector. A custom detector must honor cancellation/progress itself; repeated non-progressing cancelled hits are bounded to avoid hanging the server.

## Cancel a hit

Use **`me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent`**, not Bukkit's vanilla event with the same simple name:

```java
@org.bukkit.event.EventHandler
public void onHit(me.simoncrafter.mCCodeCampLibrary.projectile.event.ProjectileHitEvent event) {
    if (/* your protection condition */ false) event.setCancelled(true);
}
```

The event fires **before** `whenHit` and `onHit`. Cancellation suppresses those behaviors and lets the projectile continue past that target. Travel visuals still show the path. A listener may also call `event.getProjectile().remove()` to stop the entire flight. Subclasses retain control of noncancelled impacts.

## Persistent target block

Place a solid block (for example a vanilla target block), then create a `projectile_target` marker at it through the world-marker editor or registry:

```java
var target = (me.simoncrafter.mCCodeCampLibrary.input.activation.ProjectileTarget)
    MCCodeCampLib.getBlockMarkerRegistry().createObject("projectile_target", "practice", block.getLocation());
if (target != null) {
    target.setDetectVanillaProjectiles(true); // false by default
    target.setCooldown(10);                  // ticks; default 0
}
```

The marker does not replace the physical block. It activates only for hits on its exact block, not entity hits nearby or right-clicks. Custom hits are always detected; opt-in vanilla support includes arrows, snowballs and other Bukkit projectiles. Listen for `input.activation.event.ProjectileTargetHitEvent` and compare `getID()`. It provides `getTarget()`, `getCustomProjectile()` or `getVanillaProjectile()`, nullable `getShooter()`, `getHitPosition()` and nullable `getHitFace()`. This is a noncancellable notification of an accepted impact; cancel the original projectile hit event to suppress it. Target notification does not replace the projectile's own impact behavior.

Settings persist in the chunk registry: `detectVanillaProjectiles`, `cooldown`, optional `blockdata` and `cooldownBlockdata`. Use setters or supply a Configurate node to `createObject` for initial config. No target-specific property editor has been added; creation/selection uses the normal world-marker editor.

## Per-item tick cooldown

```java
var cooldown = new me.simoncrafter.mCCodeCampLibrary.utility.ItemCooldown(
    new org.bukkit.NamespacedKey(plugin, "my_wand_cooldown"));

if (!cooldown.tryUse(itemStack, 20)) return; // Accepted use starts 20 ticks of cooldown.
// Perform the action. Rejected attempts do not restart/extend the cooldown.
```

Create the helper once per action type; it retains no items and creates no listeners/tasks. State lives in the supplied stack's PDC, independent of other items/players. Cloning a stack copies its current cooldown. Different keys permit independent actions on the same stack. Zero duration always allows; negative durations throw.

The central library loop advances a `long` tick clock. A random session nonce is stored alongside last-use tick. A crash/restart/plugin reload invalidates old-session cooldowns, **even if the new clock is already greater than the saved tick**. The first use in the new session is allowed. This deliberately resets cooldowns on restart rather than persisting remaining duration; no real time is used and downtime does not count as ticks. The wand now uses this helper with its configured cooldown; its existing explosion behavior is otherwise unchanged.

## Ownership and cleanup

- No per-projectile scheduler is created; one library loop owns ticking.
- Removal, expiry, world/chunk unload and plugin shutdown release active projectiles.
- Subclasses register owned event listeners with `listen(listener)` from `onShoot()`; removal unregisters them automatically. Release any additional owned resources in `onRemove()`.
- Registry removal, replacement, chunk unload and plugin disable destroy targets: unregistering handlers, disposing cooldowns and removing editor displays.
- Application-level listeners must likewise be unregistered when their owning course/object is no longer active.

## Verification

From the repository root (Maven must resolve the configured JDK/dependencies):

```text
mvn -B test -Dexec.skip=true
mvn -B -Pdev test -Dexec.skip=true
mvn -B -pl MCCodeCampLibrary dependency:build-classpath -Dmdep.outputFile=target/editor-classpath.txt
python MCCodeCampLibrary/tests/run_projectiles.py
python MCCodeCampLibrary/tests/run_snowball_display.py
python MCCodeCampLibrary/tests/run_projectile_target.py
python MCCodeCampLibrary/tests/run_item_cooldown.py
python MCCodeCampLibrary/tests/test_wand_cooldown.py
python MCCodeCampLibraryTestPlugin/tests/run_projectile_fixture.py
```

The standalone harnesses are **ad-hoc API-boundary verification**, not in-game tests or Maven/JUnit test counts. They cover collision/control flow, cancellation, presets, target flags/persistence/lifecycle and item clock/session recovery. Verify actual particle appearance and target interactions in-game after deployment.
