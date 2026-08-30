# MCCodeCampLibrary — Agent Project Context

This file is the portable, repository-level project memory for AI coding agents. It is intentionally independent of any particular agent, IDE, machine, or account.

_Last reconnaissance: 2026-08-21._

## Agent working rules

1. Read this file before changing code.
2. Preserve existing user changes. Before editing, inspect `git status`; never reset, clean, or overwrite unrelated changes.
3. Treat IntelliJ/Maven configuration and the current source tree as authoritative. Planning documents describe direction, not implemented APIs.
4. Do not claim a build or test passed unless it was actually run and its output was checked.
5. Prefer small, focused changes. Keep the existing public API and legacy package/type spellings unless a rename is explicitly requested.
6. Bukkit/Paper world and entity operations must remain on the server thread unless the API explicitly permits otherwise.
7. When changing persistence or event registration, consider chunk unload/reload, duplicate listeners, malformed saved data, object removal, and plugin disable behavior.
8. Do not edit `.agentbridge/` databases or machine-specific IDE memory directly.

## Project identity and purpose

`MCCodeCampLibrary` is a Maven multi-module Minecraft Paper library and test-plugin project. Its broader purpose is to support a local Paper adventure/course platform where students learn Java progressively through in-world puzzles. The library provides reusable puzzle infrastructure and “training-wheel” wrappers while gradually exposing professional Java/Paper patterns.

Design principles from `Planning/`:

- Keep course content, student projects, the Paper server, and the shared library conceptually separate.
- Prefer immediate in-world feedback and story-driven tasks.
- Introduce concepts in tiers: variables/output → conditionals → loops → methods → collections → classes → inheritance → events → lambdas → scheduling.
- Student-authored code should receive dependencies rather than reach for global Bukkit state. Bukkit/Paper domain types and real event patterns are intentionally allowed.
- Use ordinary Java collections rather than bespoke puzzle containers.

## Repository layout

- `pom.xml` — parent aggregator, artifact `me.simoncrafter:MCCodeCampLibraryProject:1.0a`.
- `MCCodeCampLibrary/` — reusable library module, artifact `me.simoncrafter:mCCodeCampLibrary:1`.
- `MCCodeCampLibraryTestPlugin/` — runnable test harness plugin consuming the library.
- `Planning/` — design documents and feature backlog one level above this repository.
- `.idea/` — IntelliJ project configuration.
- `.agentbridge/` — AgentBridge conversation/memory data; do not edit its database by hand.
- `.claude/` — legacy/agent-specific memory location; it is ignored by Git and is not canonical.
- `src/` at repository root was untracked during reconnaissance; do not assume it belongs to the Maven modules without inspecting it.

## Build and runtime configuration

### Maven

The parent has two profiles:

- `library` (active by default): builds `MCCodeCampLibrary`.
- `dev`: builds `MCCodeCampLibraryTestPlugin` and then `MCCodeCampLibrary`.

The library uses Java 22 and Paper API `1.21.11-R0.1-SNAPSHOT` with `provided` scope. Important dependencies:

- `CraftersDisplayLibrary:v1.2.0b`
- `CraftersChatDialogs:3af1029bb1`
- WorldGuard `7.0.9` (`provided`)
- Gson `2.14.0`
- Configurate Gson `4.2.0`

The library currently has a Maven Shade plugin. The test plugin targets Java 21, depends on the library and Paper API, and has optional local deployment through `env.PLUGINSFOLDER`; it also has an optional `deploy-local.sh`-based deployment profile.

### Test plugin entry point

`MCCodeCampLibraryTestPlugin.onEnable()`:

1. Calls `MCCodeCampLib.init(this)`.
2. Registers `CourseEditCommand` for `/coursedit`.
3. Broadcasts `Reloaded!`.

`onDisable()` calls `MCCodeCampLib.onDisable()`.

`plugin.yml`: main class is `me.simoncrafter.mCCodeCampLibraryTestPlugin.MCCodeCampLibraryTestPlugin`, API version `1.21.11`, load `POSTWORLD`, command `coursedit`.

## Core architecture

### `MCCodeCampLib`

`utility/MCCodeCampLib` is currently a static singleton-style facade holding the plugin, `BlockMarkerRegistry`, `ItemsManager`, and a display-plugin iteration timestamp. `init` is idempotent. It:

- creates the block-marker registry;
- registers the `button` object type using `ActivationButton::new`;
- scans loaded chunks;
- initializes `ItemsManager`, CraftersDisplayLibrary, and CraftersChatDialogs;
- registers `Listeners`, `ActivationListeners`, `RegionActivationHandler`, and CraftersChatDialogs listeners;
- removes old display iterations.

`onDisable` currently only shuts down `HotbarMenu`.

### Marker persistence: `BlockMarkerRegistry`

`internal/registry/BlockMarkerRegistry` persists marker objects as a JSON string in each chunk’s `PersistentDataContainer`, under a plugin `NamespacedKey` named `blockmarker_registry`. Each object stores:

- type ID;
- author-facing ID;
- UUID;
- world and x/y/z location;
- Configurate configuration.

It reconstructs objects on chunk load, registers object types, creates new objects, saves/removes objects, and unregisters listeners on removal. Main public operations include `registerObjectType`, `getObjectTypeIDs`, `hasObject`, `createObject`, and save/remove by object or UUID.

Legacy spelling is part of the current API: `IBlockRegestryObject` and package `obstical` should not be renamed casually because that is a cross-project refactor.

### Inputs and activation events

`input.activation.ActivationButton` is an `IBlockRegestryObject` and Bukkit `Listener`. It persists normal/cooldown `BlockData`, an optional cooldown, and emits `ButtonIDEvent` when its exact block is right-clicked with the main hand.

The event hierarchy contains ID-based events for:

- buttons;
- pressure plates and deactivation;
- block/player/entity IDs;
- entity right-click, left-click, and kill;
- region-trigger enter/leave.

`internal/activation/ActivationListeners` is the global Bukkit bridge, but much of the older generic marker activation and NPC/entity code is commented out. Do not assume every TODO-listed input is active merely because an event class exists. `ActivationButton` currently handles its own right-click path.

### Editors and authoring tools

`commands/CourseEditCommand` currently supports:

- `getItems` — gives editor items (permission-gated);
- `reload` — emits `BlockRegistryUpdateEvent`;
- `hotbar` — exercises the hotbar menu;
- `add <type> <id> <x> <y> <z>` — creates a persisted registry object, including `~` relative coordinates;
- `editortest` — exercises `WorldMarkerEditor` with sample creatable objects.

`internal/editor` contains `AEditor`, `EditorManager`, `EditorItems`, `WorldMarkerEditor`, and `IEditable`. `internal/editor/hotbarmenue` contains `HotbarItem` and `HotbarMenu`, including navigation, per-player display, click callbacks, and cleanup.

### Obstacles / doors

The package is intentionally/legacy-spelled `obstical`.

`AOpenableObject` is the persisted base for openable obstacles. It owns:

- `OpenableRegion` cuboids;
- block enumeration and region grouping;
- `OpenableState`;
- location, ID, UUID, Configurate config, plugin and registry references;
- save/load integration through `BlockMarkerRegistry`.

Concrete door types are `PivotingDoor`, `SimpleDoor`, and `SlidingDoor`; `AAnimatedOpenableObject` is an animation base. `OpenableObjectStateChangeEvent` exists.

There is an explicit TODO in the block-region grouping algorithm around growing a rectangular region along Y. Also review `addBlock`/`removeBlock` semantics before relying on them: their names/implementation deserve verification.

There is a per-developer remote deployment path for the test plugin:

- `MCCodeCampLibraryTestPlugin/deploy-local.example.py` is the portable template and is tracked.
- A developer copies it to `MCCodeCampLibraryTestPlugin/deploy-local.py`; that personal file is gitignored.
- The Python launcher uses only the standard library and works on native Windows, Linux, and WSL.
- It uploads the Maven JAR to Pelican's client file-write endpoint. Configure `PELICAN_PANEL_URL`, `PELICAN_API_TOKEN`, and `PELICAN_SERVER_ID`; optionally set `PELICAN_PLUGIN_PATH`. Never commit the API token.
- The test-plugin POM activates the Python deployment profile only when `deploy-local.py` exists. Native Windows uses `python`; Unix/WSL uses `python3`. Normal builds do not deploy. Agents should check the developer's IDE environment variables before troubleshooting deployment.
- Existing Linux/WSL machines may still have the old ignored `deploy-local.sh`. The POM keeps a Unix-only compatibility profile for it, but new machines should migrate to the Python template. Agents should remind developers that the old `.sh` route is legacy and should not be copied into a Windows setup unchanged.
- The old `deploy-local.sh` profile is not automatically used by a native Windows Maven process. Run Maven inside WSL or migrate to `deploy-local.py`.
- See `MCCodeCampLibraryTestPlugin/DEPLOYMENT.md` for setup and migration details.

## Known gaps and cautions

The library’s own `MCCodeCampLibrary/src/main/java/.../todo.md` still lists substantial unfinished work:

- input creation/removal and additional input types (chat, sign, NPC/entity, dialogue);
- output helpers (chat, text displays, NPC dialogue, sound, particles, console, explosions);
- obstacle variants (sliding/swinging/fallover doors, bridges, movable blocks, elevators);
- NPC movement/dialogue/creation/removal;
- cooldown and other utility polish.

The broader planning backlog proposes sequence locks, maze generation, particles/sounds, display boards, NPC wrappers, scheduling, regions/world reset, task progress, hints, integrity checks, and friendly compiler/runtime diagnostics. Treat these as direction, not implemented API.

## Current working-tree caution

At reconnaissance time, branch `master` tracked `origin/master`, with pre-existing staged/unstaged edits in `.idea`, editor/library classes, the test-plugin POM and test-plugin class, plus untracked `.agentbridge/` and root `src/`. Preserve unrelated user changes; never reset or clean the repository without explicit instruction.

This document is a codebase map, not a build result. Use IntelliJ/Maven configuration as canonical and verify compilation/tests before claiming success.
