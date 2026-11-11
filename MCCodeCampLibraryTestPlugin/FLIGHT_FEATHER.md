# Flight feather course

The fourth course follows the wand's split:

- `yourCode/a4_flightFeather/Course.java`: student-owned item appearance and movement.
- `courseInternal/a4_flightFeather/FlightFeatherCourseSegment.java`: item registration, button and sneak routing, animation ticking and cleanup.
- `courseInternal/a4_flightFeather/WindEffect.java`: temporary color-display wind animation.

Create a course button with ID **`get_flight_feather`** to give the item, just as `get_wand` gives the wand. The registered course/item ID is **`flightFeather`**. No world buttons are placed automatically.

With the feather in the main hand:

- Right-click air or a block: replace velocity with a vertical launch.
- Start sneaking while airborne: replace velocity with a fixed-length vector in the look direction. Releasing sneak, sneaking on the ground, swimming/in water or riding a vehicle does not trigger a leap.
- Each accepted action creates a short wind burst. The course owns animation ticking and removes its displays on deactivation/plugin disable.

**Units:** both velocities are literally **1 block per second**, converted to `0.05` blocks per tick. Minecraft gravity still applies; this is a very small impulse, not sustained flight. For a stronger, typical Minecraft launch of 1 block per tick (20 blocks/second), change `SPEED` in the student class to `1.0`.

## Offline verification

From the repository root:

```text
mvn -Pdev -Dexec.skip=true compile
mvn -f MCCodeCampLibrary/pom.xml dependency:build-classpath -Dmdep.outputFile=target/editor-classpath.txt
python MCCodeCampLibraryTestPlugin/tests/run_flight_feather.py
python MCCodeCampLibraryTestPlugin/tests/run_wind_effect.py
python -m unittest discover -s MCCodeCampLibraryTestPlugin/tests -p test_flight_feather_wiring.py -v
```

The Java harnesses run production movement/animation logic with doubled Bukkit boundaries; they do not verify client rendering or live-server physics. Packaging without remote deployment: unset `PLUGINSFOLDER` and run `mvn -Pdev -Dexec.skip=true package`.
