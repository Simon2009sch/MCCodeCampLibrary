# Meteor, Terrain Restore and Projectile Effects Implementation Plan

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task where available. Read the repository's `AGENTS.md` and `java-paper-plugin-development` skill first. This document is a durable continuation/handoff, not evidence that unfinished work passed.

**Goal:** Extend the existing student projectile/test-wand system with a backed-up destructive meteor, animated radial fire wave, white impact flash, native snowball burst, destructive rounded raycast, and newest-first restore item.

**Architecture:** Preserve `CustomProjectile` and the existing `ItemsManager` lifecycle. A main-thread effects coordinator must gate damage by world, wait for durable FAWE backups before any mutations, own the visual/damage/tint lifecycle, and expose the test-plugin item/console integration. Backups use a serial owned worker and file-backed schematics; callbacks returning to Bukkit must be marshalled to the main thread.

**Tech Stack:** Java 22 library / Java 21 test plugin, Paper 1.21.11, CraftersDisplayLibrary v1.2.5a, installed FastAsyncWorldEdit via WorldEdit API, Maven, Calagopus console/upload, Home Assistant Companion notifications.

---

## 1. Workspace and preservation rules

- Repository: `C:/Coden/INTELIJ/MCCodeCampProject/MCCodeCampLibrary`.
- Branch: **`tdot`**, tracking `origin/tdot`. Do not merge this AI work into `master`.
- There is substantial PRE-EXISTING UNCOMMITTED work from earlier requests. Do not reset, clean, stash everything, overwrite it, or commit/push without authorization.
- At this handoff, the new meteor-related component files have appeared on disk, but the parent has NOT independently verified or integrated them yet.
- Custom item/projectile infrastructure and build/upload helper from earlier work were already built, deployed, and reloaded successfully. Do not confuse those earlier successes with completion of this new meteor feature.
- Temporary verifiers belong under `C:/Users/simon/AppData/Local/hermes/cache/scratch`, with `hermes-verify-` prefix. The terminal's actual TMPDIR has sometimes pointed to Windows Temp; use the explicit Hermes scratch path.
- Reusable PROJECT-specific utilities may live under this repository's `tools/`; generic reusable utilities belong under `C:/Users/simon/hermes-tools/`.
- All newly registered listeners, tasks, display wrappers and worker executors require explicit idempotent cleanup. User stressed this for all future work.

## 2. Exact requested behavior and user decisions

### Meteor trigger and descent

- A custom fireball impact should trigger a falling magma-block meteor animation, preferably a reusable `MeteorProjectile` class.
- Spawn animation entities at the nearest player in the same world for tracking, use a stationary raw entity anchor and animate local translations. Always calculate visible/effect positions as **entity/world anchor + display translation**, not the raw entity location alone.
- Keep anchors stationary during radial motion. Avoid per-tick teleports and per-display scheduler tasks.
- There is a possible wording tension between 'nearest-player anchor' and 'entity stays in the center'; the chosen design is a stationary render anchor near the nearest player, with the actual visual starting at the impact epicenter via local translation. Explain this explicitly if needed; never apply burning at the raw player anchor.

### World safety policy

- **Animations run in every world.**
- Final normal behavior: **block AND entity damage only in world named exactly `testworld`**. User will create that world using Multiverse.
- Missing `testworld` must fail closed: visual-only behavior, no damage in another world as fallback.
- User explicitly authorized LIVE destructive tests in the normal world named **`world`**, at **x=-1368, y=70, z=3350** only. They said it is far away and their player will be above it.
- SUPERSEDED permission: user is back from dinner and building. Do NOT teleport their player, move their camera, or take Minecraft controls. When testing is ready, send an IN-GAME chat invitation with a clickable run_command teleport button so they can choose to come. Destruction is authorized only around the supplied test center (about 2000-block radius); avoid their building vicinity. Keep fixed test commands much more narrowly scoped than this maximum.
- Before finishing, remove/disarm any live-test override and verify `testworld`-only damage again. Do not leave a broad `world` damage setting enabled.
- Prefer a console-only, bounded, ephemeral live-test permit for that exact location; never globally enable normal-world wand damage. The permit must reset on plugin reload/restart.
- Do not create a Multiverse world without asking; the user said THEY will create `testworld`.

### Backup and restore

- Use FAWE to save ORIGINAL blocks to a FILE **before destructive changes**.
- One backup per meteor event, covering explosion damage AND the directly affected moving-display area, with generous margin for natural fire.
- User explicitly wants **natural fire spread unchanged**, not contained at a backup boundary. A finite-margin backup cannot guarantee restoration of unlimited subsequent spread: document this honestly.
- Proposed meteor bounds: x/z epicenter +/-90, full world min/max height. This covers the first 50 destructive blocks + up to 5-block visual radius + margin. Validate the volume/resource budget before use.
- Raycast gets its own backup of the whole capsule bounds before clearing.
- Backups persist across server crash/restart and plugin reload.
- Restore item restores ALL saved backups in newest-first order, then progressively older ones. Include air and block NBT/state so overlapping edits return to their original state.
- Delete a backup ONLY after successful complete restoration/flush. Failures retain it and stop the restore chain; do not skip the most recent failed/missing-world entry.
- Stop/cancel active destructive effects and pending generation callbacks before restoring. Prevent snapshot/restore races and callbacks applying damage after restore or plugin disable.

### Meteor ground impact

- AFTER successful backup, create real block-damaging explosions on a roughly regular surface grid inside a 30-block radius.
- Pace explosions across ticks rather than doing all at once. Proposed spacing ~7 blocks and one explosion per tick; calculate/test exact grid, don't guess count.
- Use real Bukkit explosions with breakBlocks=true; do not accidentally rely on an entity-source mobGriefing setting that suppresses requested block damage.
- At the surface near the center, randomly convert SOME stone-like blocks to magma/blackstone, not all. Prefer after the explosion grid has exposed the crater surface.
- Any block mutation or entity damage must still pass the world/test permit and backup-completion gates.

### Radial block-display wave

- Multiple displays start at the meteor impact and move radially outward following the world's surface.
- Initial scale is **(2,2,2)**; grow to **(10,10,10)** (five times original).
- Material timeline: YELLOW_CONCRETE -> ORANGE_CONCRETE -> MAGMA_BLOCK -> ORANGE_STAINED_GLASS -> GRAY_STAINED_GLASS -> remove.
- Travel **150 blocks TOTAL**: first **50 blocks** apply terrain/entity effects; next **100 blocks** are visual-only.
- Burning/corrosion is calculated from actual translated visual position and current scale/proximity.
- Remove leaves and foliage, convert grass blocks to dirt, ignite flammable blocks and nearby entities during the destructive first 50 blocks only.
- Pace/deduplicate block work. A suggested queue aggregates affected x/z columns and vertical spans, processing a bounded number each tick instead of editing thousands of blocks for each display callback.
- Do not change fire-spread game rules. Native fire may keep burning until restore.

### Impact screen flash

- Every same-world player within a spherical 100-block radius gets an opaque WHITE screen tint that fades to transparent over **40 ticks**.
- Use the existing CDL screen tinter, not a fabricated replacement.
- Explicitly remove owned tint entities after fade if library cleanup does not.
- Exact-instance ownership matters: don't remove a different feature's replacement tint.
- Cleanup on disconnect/world change/disable too.

### Snowball impact

- Preserve the snow block's animated movement, travel-facing rotation and removal behavior already implemented.
- On accepted custom hit, launch **100 normal vanilla snowballs** over a full sphere, with the original projectile shooter where Bukkit's ProjectileSource API permits it.
- Play impact particles.
- Cancellation must suppress the burst.
- Tag burst snowballs so a shared damage guard can cancel their damage outside `testworld`/the scoped live test. Normal vanilla snowballs can otherwise manage their own collision/despawn; don't create per-snowball tasks/listeners.

### Destructive raycast

- Range **64 blocks**, radius **4 blocks**, aimed along travel direction, rounded ends (capsule).
- Clear block centers inside the capsule to AIR, only after successful backup and only where damage is permitted.
- It must pass through the existing blocking terrain to excavate the whole path, not stop at the first block.
- In other worlds, visuals only.

## 3. Phone updates (authorized)

User asked for live phone updates on interesting milestones, blockers needing their input, and completion.

- Home Assistant recipient was resolved: device tracker `device_tracker.simon_schnaller_iphone` has friendly name `SIphone14`.
- Use `ha_notify` with `owner="simon"`, `device_type="phone"`, per-call `devices='["mobile_app_siphone14"]'`, group `mccodecamp-development`.
- No durable device-map modification was made.
- Two messages have been accepted by HA (HTTP 200): work continuing/scope confirmation, and FAWE/Multiverse prerequisites confirmed. Acceptance is not proof the user read the message.
- Keep updates meaningful, not per tool call. Don't claim a feature works until verified. If a destructive live test is blocked, say why and ask via phone/chat as appropriate.
- Relevant skill: `homeassistant`; load tool schemas before use in a new chat.

## 4. Live server facts already retrieved

- Calagopus panel: `https://panel.codeyourhome.eu`.
- Exact server UUID: **`798320a9-6e00-40ff-9b37-6b9062531212`** (user originally supplied short ID `798320a9`; the full UUID was resolved through get_server).
- Panel name: `MCodeCampBuildserver`.
- Latest `plugins` output confirmed active plugins include FastAsyncWorldEdit, Multiverse-Core, WorldGuard, MCCodeCampLibraryTestPlugin, DisplayTestPlugin, PlugManX.
- Player: `__Crafter__`, UUID `4984999a-09e7-4d2d-aa81-e0b89d59382a`.
- Last observed player Pos: `[-1377.05609529178, 116.03983864395849, 3352.9508864797817]`; Dimension NBT: `minecraft:overworld`. Refresh before moving/testing, since this can change.
- The server previously logged chunk-loading stalls while the user was moving. Don't assume new effects caused those old errors. Use paced operations and avoid unbounded main-thread chunk loads.
- Before live testing, refresh server running state, player world/position/rotation/gamemode as needed and preserve original camera pose if moving them.
- User authorized testing ONLY the coordinate above, not arbitrary destructive experimentation elsewhere.

## 5. Current source baseline and component APIs

Library package root:
`MCCodeCampLibrary/src/main/java/me/simoncrafter/mCCodeCampLibrary/`

Existing architecture:
- `projectile/CustomProjectile.java`: `onShoot`, `onTick`, `move`, `hit`, `onTravel`, `onHit`, `onRemove`, one-shot shoot; manager centrally ticks. Cancellable custom hit event fires before own behavior.
- `projectile/ProjectileManager.java`: reentrancy-guarded tick, world/chunk unload cleanup, close.
- `projectile/SnowballProjectile.java`: previously spawned centered snow BlockDisplayObject, used `LRotateAbsoluteNoUpdate` then `moveAbsolute(localOffset,1)`, and removed its wrapper. A child is now adding the native burst; reread latest file before touching it.
- `input/item/ACustomItemHandler.java` + `ItemsManager.java`: existing item system now supports air/block clicks, entity right-click (precise/generic dedupe), pre-attack clicks, block-break suppression, identity-safe unregister and close.
- `utility/MCCodeCampLib.java`: owns central loop and closes projectile/item managers; library init is once per classloader, same-classloader reinit after disable explicitly rejected.

Test-plugin package root:
`MCCodeCampLibraryTestPlugin/src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin/`

- `testing/ProjectileTestSupport.java`: registers four `ProjectileTestWand` handlers, button kit and target debug. Marker IDs remain `projectile_test_items` (`button`) and `projectile_test_target` (`projectile_target`). It does NOT place world markers.
- `testing/ProjectileTestWand.java`: existing ACustomItemHandler, mode-specific default items/data, cooldown, shoot path. Currently uses `ProjectileTestSupport.createProjectile(mode)`; integrate coordinator here rather than adding another independent PDC listener.
- `MCCodeCampLibraryTestPlugin.java`: constructs/closes test support around library init/disable.
- Need to add restore item through the SAME item manager, add it to kit, and unregister it on fixture close.

### Files now present from delegated work (NOT independently parent-verified yet)

1. `projectile/effect/backup/BackupStore.java`
2. `projectile/effect/backup/TerrainBackups.java`
   - constructor `TerrainBackups(Plugin, Path)`
   - nested `Bounds(int minX,int minY,int minZ,int maxX,int maxY,int maxZ)`
   - nested `Backup(long sequence, UUID worldId, String worldName, Bounds bounds, Path schematicPath)`
   - `CompletableFuture<Backup> capture(World, Bounds)`
   - `CompletableFuture<Integer> restoreAll()`
   - `int backupCount()`, `boolean isBusy()`, `close()`
3. `projectile/MeteorProjectile.java`
   - constructor `MeteorProjectile(Consumer<Location> onImpact)`
   - parent should shoot from target+~70y downward and exclude entities so it falls to ground.
4. `projectile/effect/MeteorVisuals.java`
   - constructor `(Plugin)`
   - static `nearestAnchor(Location)`
   - `startImpact(Location center, boolean destructive, BiConsumer<Location,Double> burnAt)`
   - `tick()`, `isActive()`, `close()`; currently implements Listener for owned cleanup.
5. `projectile/effect/ImpactFlash.java`
   - constructor `(Plugin)`, `flash(Location)`, `tick()`, `close()`.
6. `projectile/ExcavatingRaycastProjectile.java`
   - constructor `(BiConsumer<Location,Location> excavate)`
   - constants RADIUS=4, MAX_DISTANCE=64.
7. `projectile/effect/Capsule.java`
   - constructor `(Vector start, Vector end, double radius)`
   - `contains(double x,double y,double z)` and inclusive integer minX/minY/minZ/maxX/maxY/maxZ accessors.
8. `SnowballProjectile.BURST_KEY`: public NamespacedKey `mccodecamp:snowball_burst`, BOOLEAN true on spawned native burst snowballs.

Inspect actual current implementations and final child reports; do not blindly assume the proposed contracts remained unchanged.

### Delegation ownership at handoff

- Backup service: `sa-0-fe5b4026`, task under batch `deleg_d2c70739`; still reported running at handoff.
- Meteor visuals: `sa-1-95a58de1`, same batch; still reported running.
- Snow burst/capsule/raycast: `sa-2-dc1e8706`, same batch; no longer listed live, result may arrive later.
- Screen flash: `sa-0-8fe9541f`, batch `deleg_f8318b41`; no longer listed live, result may arrive later.
- Do not wait/poll transcripts. Use delivered summaries, and independently inspect/verify shared files. In a different chat these children may no longer exist; on-disk files and tests are the continuation source of truth.
- No agent was authorized to commit or deploy.

## 6. Tinter/API findings already researched

CDL source jar:
`C:/Users/simon/.m2/repository/com/github/Simon2009sch/CraftersDisplayLibrary/CraftersDisplayLibrary/v1.2.5a/CraftersDisplayLibrary-v1.2.5a-sources.jar`

- `effect.viewtinter.ViewTinter.tintPlayer(Player, Color, IViewTinterFunction, int)` returns CubeColorDisplay.
- `getTintDisplay`, `untintPlayer` available; `FadeOutTinter(Color,40)` exists.
- In the resolved source, tint visibility calls were commented out; impact helper must explicitly hide recursively, show only for owning player, and mark owned native children nonpersistent.
- Use exact owned-display identity when cleaning up; don't untint a later replacement from another effect.
- `BlockDisplayObject` is centered by CDL. `moveAbsolute` is LOCAL transform translation, NOT world position.
- Neutralize raw entity yaw/pitch when applying a world-facing quaternion, or shooter rotation gets applied twice.
- Stage scale/rotation with NoUpdate then one animated move push; avoid starting a separate rotation animation task each tick.

## 7. Parent implementation still needed (do these next)

### Task A: Inspect/review and test component work

1. Read the new component files above, plus their new harnesses.
2. Confirm FAWE copy/paste really waits for complete flush and that file deletion cannot precede successful restoration.
3. Review use of async WorldEdit/FAWE API versus Bukkit main-thread-only operations.
4. Review stationary display anchors and callback positions, 50/150 cutoffs, material/scale timeline, cleanup and screen tint ownership.
5. Run focused tests (commands below). Record exact results, distinguishing boundary doubles from real-server evidence.
6. Tell phone when the durable-backup gate or first meaningful component set is verified.

### Task B: Add a tested damage policy

Proposed create: `projectile/effect/DestructionPolicy.java` (name may change, keep plan updated).

1. Write tests for exact world name `testworld`, missing/other worlds denied, visuals unaffected.
2. Add an ephemeral, console-only live permit restricted to world `world` and the approved horizontal epicenter (-1368,3350), not broad normal-world access.
3. Burst-snowball damage outside permitted world/region must be cancelled using the BURST_KEY, without affecting ordinary vanilla projectiles unrelated to this feature.
4. Test disarm/reload resets and no fallback when `testworld` does not exist.

### Task C: Add paced surface mutation/explosion job

Proposed create: `projectile/effect/SurfaceImpact.java` (or small equivalent).

1. Write a pure explosion-grid planner test: points centered on epicenter, within radius30, regular spacing, no duplicates.
2. Only create a destructive job after backup capture future SUCCESS on main thread.
3. Pace real explosions, with surface lookup that does not explode only above leaf canopy when ground is intended.
4. Queue/deduplicate leaf/foliage/grass/fire changes near actual wave translation. Apply bounded work per tick.
5. Ignite nearby entities only during allowed first50 interval and within translated proximity; don't ignite display/interaction helper entities.
6. Randomly recolor a subset of exposed center stone to magma/blackstone after excavation.
7. Respect original backup bounds for all directly applied edits. Natural fire is deliberately not confined.
8. Track useful counters for live verification: explosion attempts/accepted, changed blocks, ignited entities, active displays/jobs.
9. Stop all queues on restore/disable; no late queued changes after backup restoration.

### Task D: Add main-thread effects coordinator

Proposed create: `projectile/effect/ProjectileEffects.java` or test-plugin equivalent.

1. Own TerrainBackups, MeteorVisuals, ImpactFlash, one sync per-tick driver and any damage guards.
2. Fireball trigger: accepted hit -> launch falling MeteorProjectile; no destruction on original fireball impact before meteor ground impact.
3. Meteor ground impact -> decide permit, capture one snapshot for all direct mutations+margin, then flash/start waves/explosion job. In other worlds skip snapshot/damage but still animate/flash.
4. Ray callback -> Capsule bounds -> backup -> paced AIR edits for block centers inside capsule. Visual-only outside permitted world.
5. Marshal all future callbacks back to Bukkit main thread and verify plugin enabled/current generation before acting.
6. Serialize/reject overlapping heavy destructive jobs with clear player feedback; prevent backup/restore races and server overload.
7. Restore operation first cancels tracked dangerous projectile starters, falling meteors, waves and damage queues; invalidate pending callbacks. Refuse or wait cleanly if capture is still running. Then call restoreAll and report count/errors.
8. Explicit close unregisters guards, cancels tick driver, removes visuals/tints, stops worker and prevents late mutation/deletion.

### Task E: Integrate items and console test/status hooks

Files:
- Modify `testing/ProjectileTestWand.java`
- Modify `testing/ProjectileTestSupport.java`
- Modify `MCCodeCampLibraryTestPlugin.java`
- Modify `MCCodeCampLibraryTestPlugin/src/main/resources/plugin.yml` as required.
- Create `testing/RestoreBackupsItem.java` (proposed).

1. Keep existing item IDs/kit marker IDs. Route FIREBALL_LIKE and RAYCAST to coordinator behavior; snowball uses updated SnowballProjectile.
2. Replace stale 'harmless' lore/messages: visual-only outside testworld; real destructive behavior inside it.
3. Add restore item (e.g. CLOCK) through ACustomItemHandler/ItemsManager; restore on accepted main-hand right click, report async status, unregister on close.
4. Add console-only test/status command, e.g. `projectileeffects status`, `live-meteor`, `live-ray`, `restore`, `disarm`. Restrict live commands to exact approved location and ephemeral permit. These are proposals, not existing commands.
5. Status must expose damage world/live permit/backups/pending work so final safety can be verified by console.
6. Ensure FAWE load order (softdepend/depend) and use provided APIs, not shading another WorldEdit into the plugin.

### Task F: Automated verification and deployment

1. Run new component/policy/planner/integration tests and existing regression harnesses.
2. Run both library and development Maven builds without deployment first, using `-Dexec.skip=true`.
3. Only then run the existing project-local build/upload/reload tool, which verifies SHA-256 and a NEW PlugMan console success line.
4. Read server console for actual enable errors. Build success does not prove live rendering or FAWE schematic roundtrip.
5. Phone update: code/build/backup gate verified, proceeding to authorized live test.

### Task G: Authorized live test, restore, final restriction

1. Refresh player pose/world and server state. Preserve camera pose before moving them. User permits teleporting, not unrelated world changes.
2. Make sure backup directory/state and test status are observable; default destruction remains testworld-only.
3. Temporarily arm ONLY the bounded live-test permit and trigger meteor centered at (-1368,70,3350) in `world`.
4. Use computer_use capture first, scoped to Minecraft; do not steal foreground unless actually required and user is not active. DirectInput can require verified escalation. Do not guess screen coordinates.
5. Observe actual magma descent, white tint fade/removal, explosion grid, moving material/scale wave and terrain effects. Check no fire/entity effects after50 while visuals continue150.
6. Verify FAWE backup files exist BEFORE destructive operation; verify actual terrain changed afterward. Do not test with fabricated API data.
7. Test rounded ray excavation at the same authorized area with its own backup. Avoid aiming at unrelated structures.
8. Test restore item/service newest-first, including overlap. Confirm saved baseline blocks return and files are deleted only after success. If restore fails, keep backups and notify user; do not claim safe completion.
9. Disarm live override and verify status shows `destructive-world=testworld`, `live=false`. No destructive normal-world override may remain enabled.
10. Restore player's original camera pose if appropriate; remove only owned temporary entities/tints, never global kill/unrelated displays.
11. Phone completion: what passed, any remaining visual/performance caveat, restriction/restore status.

## 8. Exact verification commands

Working directory: repository root above. Native Windows Maven path:

```text
C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.4/plugins/maven/lib/maven3/bin/mvn.cmd
```

Build/type/test before deployment:

```text
"C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.4/plugins/maven/lib/maven3/bin/mvn.cmd" -B -q test -Dexec.skip=true
"C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.4/plugins/maven/lib/maven3/bin/mvn.cmd" -B -q -Pdev test -Dexec.skip=true
"C:/Program Files/JetBrains/IntelliJ IDEA 2026.1.4/plugins/maven/lib/maven3/bin/mvn.cmd" -B -q -pl MCCodeCampLibrary dependency:build-classpath -Dmdep.outputFile=target/editor-classpath.txt
```

New on-disk focused runners (inspect their current contents before using; some agents are still finishing):

```text
python MCCodeCampLibrary/tests/run_terrain_backups.py
python MCCodeCampLibrary/tests/run_meteor_visuals.py
python MCCodeCampLibrary/tests/run_impact_flash.py
python MCCodeCampLibrary/tests/run_excavating.py
python MCCodeCampLibrary/tests/run_snowball_display.py
```

Existing regression checks:

```text
python MCCodeCampLibrary/tests/run_projectiles.py
python MCCodeCampLibrary/tests/run_projectile_target.py
python MCCodeCampLibrary/tests/run_custom_items.py
python MCCodeCampLibrary/tests/run_item_cooldown.py
python MCCodeCampLibraryTestPlugin/tests/run_projectile_fixture.py
python -m unittest discover -s tools/tests -v
git diff --check
```

Actual deploy/reload (already tested working earlier):

```text
python tools/build-and-reload.py --ide-env
```

- Explicitly reads the existing private IntelliJ Maven run environment from `.idea/workspace.xml`; do NOT print or commit it.
- Same API key as existing uploader supports resources/logs/console too. Never hardcode/paste it into source/chat.
- `--dry-run` does local validation only; `--no-reload` builds/uploads/verifies only.
- Actual upload target: `/plugins/MCCodeCampLibraryTestPlugin.jar` on server UUID above.
- Console reload command: `plugman reload MCCodeCampLibraryTestPlugin`.
- If server starting/stopping/restarting, skip reload; never power-cycle it. Don't retry an ambiguously submitted command automatically.
- Read back exact remote SHA-256 and fresh console result before claiming success.

## 9. Current verified status checklist

- [x] Requirements/permissions clarified, including 150 total radius travel, 64/r4 rounded ray, one margin backup, natural fire unchanged.
- [x] Phone destination resolved; initial and prerequisite messages accepted by Home Assistant.
- [x] FastAsyncWorldEdit and Multiverse confirmed on live server; player near approved location.
- [x] Existing previous projectile/item/build-tool baseline exists and was deployed earlier.
- [x] New component source/test files have appeared on disk from delegates.
- [x] Independently ran component runners: terrain backups; surface-impact/policy; meteor visuals (97,683 assertions); impact flash; excavation; snow display. Coordinator gate harness also passes: backup failure, restore lock, visual-only world and late-close callback. Boundary tests are not live-server evidence.
- [x] Implemented damage policy and ephemeral scoped live permit; focused policy runner passes.
- [x] Implemented paced SurfaceImpact; focused boundary runner passes despite previous delegate ending rate-limited. Parent inspected and independently exercised its on-disk implementation.
- [ ] Complete coordinator integration review, cancellation/restore generations and regression checks (gate harness passes).
- [ ] Integrate updated wands, restore item and test/status commands; delegated as deleg_7d72a4a8, no deployment authorized for child.
- Library Maven `test -Dexec.skip=true` passed. Existing projectile, target, custom item, item cooldown and tool regression checks passed. `git diff --check` passed with CRLF warnings only.
- [x] Parent ran development Maven test and fixture/coordinator boundary runners: exit 0. New test-wand integration, CLOCK restore item (`ProjectileRestoreItem`) and console-only `ProjectileEffectsCommand` compile.
- [x] Built/uploaded/reloaded new effects with `python tools/build-and-reload.py --ide-env`: BUILD SUCCESS, remote SHA-256 fadd0f7250063abe28d7818f70780cb549ccb594ca4643ecfd3b108bbdd2aff9, fresh PlugMan reload verified. Console readback: phase=IDLE, destructive-world=testworld, live=false, backups=0, pending=0, explosions=0, changedBlocks=0, burnedEntities=0.
- LIVE TEST BLOCKER: latest `list` readback reports 0 players online. No destructive test triggered, no player moved. Wait for user to reconnect before sending their requested clickable in-game teleport invitation. No invite was sent to an offline player.
- [ ] Perform authorized live visual and destructive test at exact location.
- [ ] Verify actual FAWE restore roundtrip and delete-after-success.
- [ ] Confirm final testworld-only damage restriction and no lingering owned entities/tasks/listeners.
- [ ] Final phone update and concise user report.

## Latest requested revision (supersedes earlier numerical/visual settings)

- Clouds: straight radial outward travel; slow self-rotation (not orbit), both translation and rotation decelerate toward the 150-block edge. Follow non-tree terrain and water surface, ignoring tree leaves/trunks rather than climbing canopy.
- Material order: MAGMA_BLOCK -> YELLOW_STAINED_GLASS -> ORANGE_STAINED_GLASS -> WHITE_STAINED_GLASS -> GRAY_STAINED_GLASS -> BLACK_STAINED_GLASS -> remove.
- Flash shortened to **5 ticks**. Parent changed the production constant after observing the revised harness fail, then reran it successfully (all fade/cleanup boundary checks pass).
- Explosion surface density approximately 3x, power **6.4** (double previous3.2); grid triggered together in one server tick after backup, plus ONE central power30 explosion. Preflight the entire blast neighborhood before any explosion. Warn of main-thread load; never bypass backup or world restrictions.
- 'Higher replacements' interpreted as larger/denser magma-blackstone scar (proposed radius24, 60% replacement, up to3 stone layers instead of radius12/24%/one layer).
- Delegation deleg_850235da: meteor visual changes/tests owned by sa-0-2f71c11a; surface impact changes/tests/docs by sa-1-115ef3e1. Neither may deploy/commit. Parent must inspect and independently run results, full dev build, then verify deployment if performed.
- Revised implementation verified by parent: meteor runner 287,461 assertions, surface runner grid61->177 (2.90x density) plus central30, flash5tick runner, development Maven test and coordinator safety gates all exit0. `git diff --check` passes.
- Revised build deployed/reloaded successfully; remote SHA-256 `2be9f59817d1c6f8281abb8756e8a7d9ab7076624a8269a1ff4939de82d18d09`. Fresh console status phase=IDLE, destructive-world=testworld, live=false, backups=0,pending=0,explosions=0,changedBlocks=0,burnedEntities=0. No destructive test or player teleport triggered. In-game appearance and live FAWE restoration remain unverified.

## 10. Continuation instructions for a fresh chat

1. Read this plan and `AGENTS.md`; load Java/Paper, TDD, Calagopus, Home Assistant and computer-use skills as relevant.
2. Inspect `git status`, current new files and tests. Do not assume active subagents survive a chat change.
3. Treat delegated reports as unverified until inspecting/testing their actual artifacts. No later chat should repeat destructive live tests merely because it lacks transcript context.
4. Update this checklist and an evidence section below after each verified milestone (test commands/results, backup IDs/files, live status and restoration outcomes). Keep secrets out.
5. If an interrupted live test might already have modified terrain, inspect backup files and server status BEFORE re-triggering anything. Restoration/safety takes priority over another visual demo.
6. Keep final damage restricted to `testworld`; scoped normal-world permission was for this authorized test, not an ongoing default.

### Evidence / progress log to extend

- 2026-11-08 18:52 CET: plan saved. Server prerequisites and user permissions verified. New component work delegated and files present; parent integration and new live testing not yet performed. No NEW meteor/raycast destructive command has been sent by the parent at this point.
- Screen-flash child completed. Parent independently ran `python MCCodeCampLibrary/tests/run_impact_flash.py`: exit 0, covering radius, 40 alpha steps, expiry, replacement ownership, quit/world-change, failed-registry cleanup and close. This is API-boundary evidence, not live rendering or full integration. `ImpactFlash.java` still needs coordinator wiring and real-server visual verification.
