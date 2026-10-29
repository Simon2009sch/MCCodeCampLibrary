# EditableDisplay

`internal.editor.display.EditableDisplay` extends the display library's `PositionObject`.
There is no separate holder, geometry record, native renderer or click detector.
The nested `Style` record and its defaults are retained from the author's scaffold.

## Default usage

```java
EditableDisplay display = new EditableDisplay(
        location, plugin, Component.text(getID()), this::onRightClickDisplay);

display.showFor(player);
display.select(player);
display.deselect(player);
display.hideFor(player);
display.remove(); // On owner removal/chunk unload/plugin shutdown.
```

The default cube, label, selection wireframe and InteractionBox are direct children.
Their local translation is `(0.5, 0.5, 0.5)`, relative to the helper's block-origin location.
The normal cube uses scale `1.01`; the selection wireframe uses `1.05`; the hitbox uses `1`.
The scaffold's style defaults remain black with alpha 240, yellow selection, thickness 0.01.

Use inherited `setLocation`, `moveRelative`, rotation and scale methods. There is no extra
location synchronizer: CDL propagates the parent transform through its child hierarchy.

The wireframe is created on the first visible selection and overlays the normal cube for
that viewer. Cube, label and interaction remain available. Hidden selections are remembered without
allocating entities. Hiding clears that viewer's selection; the last viewer releases all layers.
`remove()` also clears viewer state. Owner code must call `hideFor` on disconnect/editor leave.
All calls must run on the server thread, after the normal CDL bootstrap (already done by
`MCCodeCampLib.init` in this project). Initial-spawn visibility behavior is left to CDL.

## Creation functions

Each layer has a separately replaceable constructor function:

- `setDisplayCreationMethod(Function<Style, PositionObject>)`
- `setLabelCreationMethod(Function<Style, PositionObject>)`
- `setSelectionCreationMethod(Function<Style, PositionObject>)`
- `setInteractionCreationMethod(BiFunction<Style, Consumer<PlayerInteractAtEntityEvent>, PositionObject>)`

Return a fresh **spawned** library object or a hierarchy of spawned children, anchored at
`display.getLocation()` with offsets expressed in its local transform. The helper attaches,
hides/shows and removes the returned root recursively. If a factory throws before returning,
it must clean up anything it has already spawned. Each layer must have its own root.

```java
display.setDisplayCreationMethod(style -> {
    CubeColorDisplay cube = CubeColorDisplay.create(
            display.getLocation(), new Vector3f(2, 3, 1), new Vector3f(0.5f),
            new Quaternionf(), new CubeColorInformation(style.displayColor()), true);
    cube.hideByDefault(true);
    cube.spawnDisplay();
    return cube;
});
```

Changing one factory does not resize the other layers: customize the selection and hitbox
constructors to match when changing the geometry. Defaults use CDL directly; there is no
special primitive factory or reimplementation of library rendering.

Set `setStyle(...)` and the constructors before the first show, or call `rebuild()` afterward.
Rebuild preserves viewers and selection. `setLabelText(...)` updates an existing default label.
`setOnRightClick(...)` forwards the original callback directly to the default InteractionBox;
custom backends receive it through their constructor (changing it rebuilds a custom backend).
The helper neither converts the event nor generates `PlayerClickEditableObjectEvent`: the
owner's callback can do that just as ActivationButton does.

## Buttons

ActivationButton delegates editor visibility, selection and cleanup to the same helper.
It keeps its original translucent green fill (ARGB 50, 0, 200, 50), with the current yellow
selection and thickness 0.01. Its activation event and cooldown logic are unchanged.
Relocation moves the marker; destruction removes the helper and disposes its cooldown.
Run `python MCCodeCampLibrary/tests/run_button_display.py` for its integration harness.

## Openable objects

`AOpenableObject` owns one default EditableDisplay at its origin, labeled with its ID.
Simple, pivoting and sliding doors inherit the same marker. Its editor show/hide/select/
deselect methods delegate to the helper; relocation moves the marker, and destroy removes it.
Right-clicks emit PlayerClickEditableObjectEvent with the openable object as the target.
This does not visualize block regions or implement a door property editor.

The integration harness can be run with `python MCCodeCampLibrary/tests/run_door_display.py`
after compiling the project. It exercises all existing door types with saved block regions,
plus callback forwarding, marker relocation, viewer delegation and destruction using
entity-free layer doubles and a minimal Bukkit boundary.

## Verification

Generate `target/editor-classpath.txt` with Maven's `dependency:build-classpath` goal, then run:

```text
python MCCodeCampLibrary/tests/run_position_display.py
```

Set `TMPDIR` to an appropriate scratch directory. The standalone harness compiles the actual
helper against the resolved dependencies. It uses real CDL PositionObject parent transforms
and test-only layer subclasses to check custom constructors, direct callback forwarding,
movement/scaling, lazy selection, per-viewer switching, rebuild and cleanup. It does not
exercise default entity spawning or prove in-game rendering/click behavior.

