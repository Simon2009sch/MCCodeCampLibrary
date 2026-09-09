# Mind organization file

This is a notepad to organize my mind and learning/adjusting to the data structure and flow.
___


## Data
### Block Marker Registry
This is the object that should parse the PDC from chunks that are loaded. It handles parsing the raw data like the object 
type _(depicted as type id as a String)_. It triggers a constructor of that corresponding type when it finds an entry
with a corresponding type.

When the registry should be the single **Source of truth** in the structure.

#### Updating
When something changes with an entry the registry should send out an event that contains identifying information about
what object updated and how. (Editing, Deleting, Creating) All the editor should listen for this event.



### Editor
The editor should maintain a list of players that are currently acting in that editor. The editing state is volatile
so when a player dies, disconnects, or otherwise disengages with the editor they should be automatically ejected.<br>
The editor also holds a list of all the objects you should be able to edit from that editor.

#### Joining
When a player joins the editor they should be added to the editors list of editing players. They should be shown the
displays of the objects in that editor. They should get the editor hotbar.

#### Leaving
When a player leaves the editor by either: Leaving on their own accord, getting kicked out, dying or leaving the game, 
they should be removed out of the editors list of players. The displays of the objects inside that editor should be hidden
for that player.

#### De/Selecting
When a player clicks on a display or selects it trough other means (like a listing of all objects the players get in chat)
they should be served the editing hotbar and chat editor of that specific object.

When a player deselects a object by selecting another, leaving the editor, or just using the deselect option, the editors
home (the one they got when they joined the editor) hotbar and chat editor should be served again.

When selecting or deselecting the players selector should be updated accordingly (`Map<Player, UUID> selector`)

#### Creating/Deleting Un/Loading
When crating/deleting un/loading an object the editor doesn't need to care about if it was unloaded or deleted, for it the
object is just not there anymore (same with creating). The registry will still call different events/same event with different
data, but the editor just looks, is the object available or not.


### Editables (IEditalbe)
This object should have a UUID and a reference to the object they're editing. The editables handle the writing to the
registry and serving the object specific interfaces (chat, hotbar) to the players that select that object.

Editables should have different derivatives that add capabilities like Movement, Rotation or Scaling. For independence from
the Block Marker Registry and future capability for entity and other generic editors, this should be modular.

The IEditable is a capability interface which means it declares that that object hast the capability to be edited in an
editor. When a change happens the live state of the object gets changed, the display inside the editor (if exists) gets 
updated to the new data and the new data should get saved back to the registry for persistence.

# AI

## Learning-session summary

### What was started

- Replaced the old `AEditor` / `WorldMarkerEditor` direction with `Editor`, `EditorManager`, `EditorSession`, and `EditorFrame`.
- Added an editor stack per player session so parent and child editors can be navigated generically.
- Moved the idea of selection into `EditorFrame`, because selection belongs to one player at one level of their editor session.
- Began redefining `IEditable` as a capability implemented by registry objects such as `ActivationButton`.
- Kept displays owned by the editable object, while editors decide which players should be shown those objects.

### Intended editor model

```text
Registry object / IEditable
    owns persistent object data, gameplay behaviour, and its editor display

Editor
    owns the set of objects available in one editing context and the basic tools

EditorSession
    owns one player's editor stack and the selection at every stack level

EditorManager
    owns all player sessions and performs generic navigation
```

### Selection versus detailed editing

Selecting an object and opening its detailed editor are different actions:

```text
Select object
    → show selection highlight
    → enable basic WorldMarkerEditor actions such as move, rotate, scale, or delete

Open details
    → push an object-specific child editor
    → edit fine details such as button cooldown, ID, and block data
```

Do not create a child editor merely because an object was selected. A child editor is justified only when the object needs its own editing context, tools, or nested navigation.

### Navigation goal

```text
Back once
    → pop one child editor

Jump from a deep child to another object under a parent editor
    → pop child frames until that parent
    → push the new object's child editor

Switch to an unrelated root editor
    → close the complete old session stack
    → start a fresh session with the new root editor
```

A popped frame must hide and clear its selection before its editor leaves. Parent selection can then be shown again when the parent becomes active.

### Current review notes

The design direction is sound but the refactor is incomplete:

- `EditorSession.pop()` calls `frame.getSelection().hideSelection(player)` without checking for `null`.
- `EditorSession.goTo(...)` removes child editors directly instead of using the same selection-cleanup logic as `pop()`.
- `EditorManager` does not yet create an `EditorSession` before calling `sessions.get(player)`.
- `Editor` still stores `playerSelection`, which duplicates the selection now stored in `EditorFrame`; keep selection in the session/frame instead.
- `IEditable` and `ActivationButton` are currently out of sync. `ActivationButton` still implements the old editable methods and has not implemented `showFor`, `hideFor`, `openEditor`, `showSelection`, and `hideSelection`.
- A registry update event should become the single path for committed display refreshes. Editor-only previews, if added later, are separate from committed updates.

## Simple todo board

### Now

- [ ] Make `IEditable` and `ActivationButton` use one matching contract.
- [ ] Add null-safe selection clearing to `EditorFrame` / `EditorSession.pop()`.
- [ ] Make `goTo(...)` clean popped frames exactly like `pop()`.
- [ ] Add an `openRoot(...)` or equivalent method that creates and registers a player `EditorSession`.
- [ ] Remove duplicate selection ownership from `Editor`.
- [ ] Implement one basic selection flow in the world-marker editor: select, replace selection, deselect, leave.

### Next

- [ ] Implement editable viewer tracking: first viewer spawns the display; last viewer despawns it.
- [ ] Add registry change events for object availability and committed updates.
- [ ] Make active editors add/remove editables when registry objects become available/unavailable.
- [ ] Add basic selected-object tools: move, rotate, scale, delete.

### Later

- [ ] Add `open details` as a separate action that opens an object-specific child editor.
- [ ] Define generic editor navigation APIs for back, jump-to-parent, and root-editor switching.
- [ ] Decide how concurrent live edits of one object by several players should behave.
