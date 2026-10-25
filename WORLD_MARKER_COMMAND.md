# World-marker testing command

Use `/coursedit worldmarker ...` in chat or `coursedit worldmarker ...` in the server console. Permission: `mccodecamp.command.edit.worldmarker` (OP by default in the test plugin).

These commands create/remove **registry entries**, not physical Minecraft blocks. Place any required world blocks separately. No confirmation is requested for `remove` or `set`.

## Commands

| Command | Result |
|---|---|
| `worldmarker help` | Syntax and a creation example |
| `worldmarker types` | JSON array of registered type IDs |
| `worldmarker list` | JSON array of currently loaded markers, with UUIDs, type, ID and location |
| `worldmarker create <json>` | Creates a persisted marker with the supplied config and prints its generated UUID |
| `worldmarker get <uuid>` | Prints **only the marker's config** as JSON; empty config is `{}` |
| `worldmarker set <uuid> <config-json>` | Replaces the entire config, persists it and reloads the runtime marker with the same UUID/type/ID/location |
| `worldmarker remove <uuid>` | Deletes the loaded marker and its chunk-PDC entry |

`set` is a replacement, not a merge. Fields omitted from the new config are removed. `{}` resets the config to empty/type defaults. The reload publishes `UNLOAD`/`LOAD`: active selections are reset and marker-owned editors may be closed, just as for an ordinary unload. Shared editors receive the reconstructed object.

## Creation JSON

The creation envelope has `type`, `id`, `location`, and optional `config`. **Only `config` is passed as marker configuration.** Do not place a serialized chunk's `objects` array in `config`.

```json
{
  "type": "button",
  "id": "ai_test_button",
  "location": {
    "world": "world",
    "x": 10,
    "y": 64,
    "z": 10
  },
  "config": {
    "blockdata": "minecraft:stone_button",
    "cooldownBlockdata": "minecraft:oak_button",
    "cooldown": 20
  }
}
```

A pasteable example (replace `world` with the actual loaded world name):

```text
/coursedit worldmarker create {"type":"button","id":"ai_test_button","location":{"world":"world","x":10,"y":64,"z":10},"config":{"blockdata":"minecraft:stone_button","cooldownBlockdata":"minecraft:oak_button","cooldown":20}}
```

Use the generated UUID in the following commands:

```text
/coursedit worldmarker get <uuid>
/coursedit worldmarker set <uuid> {"cooldown":40,"blockdata":"minecraft:stone_button","cooldownBlockdata":"minecraft:oak_button"}
/coursedit worldmarker remove <uuid>
```

## AI-generated JSON rules and limits

- Supply raw standard JSON, without Markdown fences or outer shell quotes when sending through Minecraft chat/the console. Quote JSON keys and strings with double quotes. Spaces inside JSON strings are supported.
- Creation requires an exact registered `type`, an ID containing only letters/digits/`_`/`-`, a loaded world name and finite numeric x/y/z coordinates. Relative `~` coordinates are not JSON numbers and are not supported here.
- Unknown creation/location envelope fields are rejected to catch AI typos. Configuration keys are type-specific and intentionally not restricted by this generic command.
- The root config must be an object, not an array/scalar/null. Nested objects, arrays, booleans and numbers are supported. JSON size is limited to 32,768 characters; the input surface may impose a smaller limit.
- JSON syntax/shape is validated before creating an entry or replacing config. Individual marker implementations interpret their fields; malformed type-specific values can still be rejected, logged or defaulted by that implementation. Check the server log when testing such values.
- `list`, `get`, `set` and `remove` operate on **loaded markers only**. Load the owning chunk first if its UUID cannot be found.
- Creation loads the target chunk, then rejects a duplicate loaded `(type,id)` pair. This protects the current in-memory registry, which indexes markers by type/ID; it is not a global search through every unloaded chunk.
- These commands deliberately do not reload/restart the server, execute strings from JSON, or import caller-supplied UUIDs.
