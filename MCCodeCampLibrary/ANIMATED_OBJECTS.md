# Schematic-backed world animations

Requires WorldEdit/FAWE on the server. Definitions use the existing `/coursedit worldmarker` JSON interface and chunk marker registry—not CDL display persistence. Schematics, checksummed manifests and small recovery journals live in `<plugin data folder>/animated/`.

## Authoring

1. Build the original model as real blocks.
2. Create `animated_blocks` or `proximity_blocks` through the world-marker editor, or JSON below. Areas use **absolute, block-inclusive** `min`/`max` corners; multiple areas are supported and overlaps deduplicated.
3. In the object's editor, select source areas. The corner tool accepts block clicks, air clicks (feet; sneak for eyes), or typed coordinates in chat. Cyan outlines use ARGB `(100,0,255,255)` and see-through rendering.
4. Enable **Edit originals as real blocks**, make changes, then **Save source schematic**. This also commits the area draft. The ordinary **Save** button saves area definitions only.
5. Finish editing to animate. A first-time object with no schematic stays inert; it never automatically backs up replacement air/barriers.

Console equivalents:

```text
coursedit worldmarker create <json>
coursedit worldmarker edit <uuid> true
coursedit worldmarker save <uuid>
coursedit worldmarker edit <uuid> false
coursedit worldmarker status <uuid>
```

`get <uuid>` returns the config; `set <uuid> <config-json>` replaces, not merges, it. Use `edit`, not raw JSON editing of `editMode`, to switch between animation and block editing. Load all selected source chunks before entering edit mode or saving. Existing snapshots may only be recaptured in edit mode. Explicit recapture atomically switches the manifest to a new schematic generation; previous generations are retained as backups.

## Continuous waterwheel

Example coordinates are illustrative; only run test fixtures in `testworld`.

```json
{
  "type": "animated_blocks",
  "id": "waterwheel_demo",
  "location": {"world":"testworld","x":100,"y":80,"z":100},
  "config": {
    "editMode": true,
    "blocks": [
      {"min":{"x":98,"y":78,"z":100},"max":{"x":102,"y":82,"z":100}}
    ],
    "pivot": {"x":100.5,"y":80.5,"z":100.5},
    "replacementBlock":"minecraft:barrier",
    "loop":true,
    "animation":[{"rotation":{"x":0,"y":0,"z":360},"time":200}]
  }
}
```

`pivot` is the **absolute model origin**. Keyframes are absolute transforms relative to that origin; each `time` is the number of server ticks to move from the preceding frame (initially identity) to this frame. Omitted components default to identity, not the previous frame's value. Translation is in blocks; scale is positive x/y/z. `rotation` is unwrapped XYZ Euler **degrees**, so a single 360-degree keyframe really spins. Alternatively use normalized `leftRotation` and `rightRotation` quaternions `{x,y,z,w}` with shortest-path interpolation; do not combine `rotation` and `leftRotation` in a frame. `loop:false` holds the last frame. Looping returns to identity; make the final transform match the first for seamless loops.

Each display entity stays at its own original integer block position, not at a shared model pivot. Only its transformation translation/rotation/scale changes, preserving per-block lighting.

The pivot editor uses chat movement controls and hotbar scrolling, relative to the marker anchor. Choose X/Y/Z with slots 0–2; normal scrolling moves one block, sneaking moves 0.1. Door area/pivot edits remain drafts until Save.

## Distance-driven assembly

```json
{
  "type":"proximity_blocks",
  "id":"assembly_demo",
  "location":{"world":"testworld","x":120,"y":80,"z":100},
  "config":{
    "editMode":true,
    "blocks":[{"min":{"x":120,"y":80,"z":100},"max":{"x":123,"y":80,"z":103}}],
    "storage":{"min":{"x":130,"y":78,"z":100},"max":{"x":138,"y":85,"z":108}},
    "nearDistance":3,
    "storageDistance":10,
    "activeDistance":50,
    "curveHeight":4,
    "includeSpectators":false
  }
}
```

Distance is measured **per original block center to the nearest player in that world**:

- At or inside 3 blocks: real, solid original block; no display.
- Between 3 and 10: air at the source; a display follows a quadratic curved path and rotates toward its original orientation. Progress depends directly on distance, so retreat reverses it.
- Between 10 and 50: display in storage, source air.
- Beyond 50, or no qualifying players: schematic originals restored and displays removed.

Storage uses stable scattered positions with at least one block between centers and coordinate-seeded −100° to +100° XYZ rotations. Too-small storage fails closed with a log message rather than silently overlapping blocks. Increase storage and re-enable animation after correcting it.

## Lifecycle and safety

- Main-thread world/entity operations; one shared library tick loop, no per-object repeating tasks/listeners.
- Source schematic and recovery intent are committed before replacing blocks. Runtime displays are nonpersistent, tagged and cleaned up by session on startup/chunk entity load.
- Source/visible-display chunk unload removes the object's displays and restores loaded source cells. Per-source-chunk recovery also works without the marker anchor loaded. No forced chunk loads during cleanup.
- Near/far restore includes block state and tile NBT; selected air is kept in backups. Gaps between selected areas are never pasted or cleared.
- Entering edit mode journals restore-to-edit intent, restores all loaded source chunks, then commits durable editing intent. That intent overrides stale chunk JSON after a crash. Finishing editing durably reactivates animation. Raw JSON replacement cannot change edit mode. Do not delete the recovery files while an animation is active.
- Maximum source selection: 3,000 cells; schematic bounding box: 250,000 cells. Storage is visual: rotated displays have no collision. Only restored originals are solid.
- Server ticks, not wall-clock time, drive playback. Playback restarts at the beginning after reload; persistent phase tracking is intentionally not included.
- Exact overlapping source cells are rejected across saved objects, including unloaded anchors and pending recovery. Deleted objects release claims after recovery completes. While active, models are authoritative scenery: normal breaking/placing, interaction and inventory transfers are blocked; use edit mode for changes. External commands and other plugins must respect the same ownership.

## Reusable APIs and verification

`CuboidAreaEditor` accepts a world, initial area supplier and Save callback. `PivotEditor` accepts an origin, per-player initial vector and Apply callback; neither depends on a door. `AnimatedSchematicStore` captures/loads exact selections and restores individual cells or predicates. `SchematicScene` converts samples to ephemeral displays; `AnimatedBlockRuntime` owns transitions and cleanup. See `ANIMATED_SCHEMATICS.md` for the lower-level store contract.

Ad-hoc checks:

```text
python MCCodeCampLibrary/tests/run_animation_math.py
python MCCodeCampLibrary/tests/run_animated_schematics.py
python MCCodeCampLibrary/tests/run_cuboid_areas.py
python -m unittest discover -s MCCodeCampLibrary/tests -p test_door_editor.py -v
```

For area/display runners, generate `MCCodeCampLibrary/target/editor-classpath.txt` with Maven `-pl MCCodeCampLibrary dependency:build-classpath -Dmdep.outputFile=target/editor-classpath.txt`; clean builds remove this file.

The math runner needs resolved dependencies in Hermes scratch `tdot-classpath.txt`, generated with Maven `-pl MCCodeCampLibrary dependency:build-classpath -Dmdep.outputFile=<absolute scratch path>/tdot-classpath.txt`. Java harnesses use real production methods with Paper boundary doubles. Live testworld checks additionally verified FAWE capture/recapture, manifest-generation reload, barrier replacements, untouched selection gaps, chest NBT restoration, animation reconstruction after plugin reload, edit-mode preservation across reload, fixed entity anchors, and overlap rejection. The user confirmed the restored bridge blocks are visible after the explicit client-update fix. A forced process crash/full server restart was not performed. Two demo markers remain near (3201,220,3201) and (3212,218,3201); temporary forceload tickets were removed.

## Entrance course area trigger

The entrance/hut course now invokes `onEnterArea()` for `RegionTriggerEnterIDEvent` ID **`hut_door`**. Existing `RegionActivationHandler` strips the WorldGuard prefix: name the WorldGuard region **`trigger_hut_door`**. The old `hut_door_button*` button IDs no longer start that course. The big double-door course is unchanged. This change does not create or move a region in the production world.
