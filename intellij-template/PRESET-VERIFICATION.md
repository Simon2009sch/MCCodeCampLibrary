# Paper-only preset verification

Run: `python verify_paper_preset.py` (exit 0).

## Scope

- Official source: https://github.com/minecraft-dev/templates/tree/v3/bukkit ; descriptor derivations and LoaderClass.java.ft taken from upstream v3.
- Installed API: Minecraft Development 2026.1-1.8.17-base.jar; real ClassFqn, BuildSystemCoordinates, StringList, SemanticVersion and all three Paper derivations.
- Real renderer: IntelliJ IDEA 2026.1.4 Velocity, lib/* plus plugins/java/lib/* plus installed Minecraft Development/lib/*.
- Compiler/runtime: C:/Users/simon/.jdks/openjdk-26.0.1.
- Maven: IntelliJ plugins/maven/lib/maven3/bin/mvn.cmd, `-B -Dstyle.color=never -Dexec.skip=true clean verify`.
- Scrubbed all CALAGOPUS_*, PELICAN_* and PLUGINSFOLDER variables; no remote uploads, local plugin copies, project-repo writes or pushes.

## Evidence directory

`C:/Users/simon/AppData/Local/hermes/cache/scratch/mccodecamp-preset-verification/`

- descriptor.log: only MC_VERSION, PLUGIN_NAME, MAIN_CLASS, USE_PAPER_MANIFEST, INCLUDE_PLUGIN_LOADER, and the five optional metadata controls visible.
- generator.log: canonical templates validate; SHA256 comparison confirms the old generator entry point changes no template file, including deploy.py.ft.
- paper-selector-response.json: live installed-selector endpoint https://fill.papermc.io/v3/projects/paper returned 68 version values, including 1.21.11. Descriptor uses paper_versions with forceDropdown. This is an endpoint check, not a live wizard UI interaction.
- bukkit-render.log / bukkit-build.log: BUILD SUCCESS; Java 21, Paper 1.21.11-R0.1-SNAPSHOT; 1 Java source compiled; shade:3.5.3:shade executed.
- paper-loader-render.log / paper-loader-build.log: BUILD SUCCESS; same dependency/Java; 2 Java sources compiled; shading executed.
- results.json: final JAR paths, sizes and exact packaged manifests.
- legacy-java17-render.log: MC 1.18.2 -> Java 17, dependency 1.18.2-R0.1-SNAPSHOT, API version 1.18.
- bukkit-loader-render.log and paper-no-loader-render.log: remaining manifest/loader combinations render correctly.

Final built artifacts: `bukkit/target/template-probe-1.0-SNAPSHOT.jar` (3776 bytes), `paper-loader/target/template-probe-1.0-SNAPSHOT.jar` (4402 bytes). Sizes are from this verification run, not reproducibility guarantees.

## Behavior

plugin.yml and paper-plugin.yml are mutually exclusive. Empty description/authors/website/log prefix are omitted; default load is POSTWORLD. Populated metadata uses STARTUP in the test; description and author apostrophes are doubled correctly, and author colon-space is safely quoted. Loader source is generated only if selected, and its class is in the built Paper JAR. Manifest key is loader for Paper and paper-plugin-loader for Bukkit. Maven filters project.version to 1.0-SNAPSHOT.

All four existing POM profiles survive: deploy-local-jar, deploy-remote-python-windows, deploy-remote-python-unix, mccodecamp-library. Remote token activation is now env.CALAGOPUS_API_TOKEN; do not add duplicate PELICAN-token profiles that would double-upload when both tokens exist. Script PELICAN aliases can still be used by explicitly activating the appropriate OS remote profile. Default/local-copy/shade/optional-library configuration otherwise unchanged.

TDD evidence: initial visibility test failed against old preset (visible BUILD_COORDS/JAVA_VERSION/JDK, missing Paper controls), then passed. Real-render escaping regression failed for Alex: Dev / O'Brien before per-author YAML quoting was added, then passed.

Known limitations: wizard UI was not opened, and no server runtime loading was attempted. Build logs contain JDK 26 Unsafe/final-field and source/target system-module warnings; both Maven builds still exited 0. Library profile dependency resolution was not built (requires separately installed local artifact). The Calagopus deploy.py.ft has since been supplied and syntax-checked after real Velocity rendering. Deployment protocol tests pass against both generated variants. The MCCodeCamp project reactor also completed a real Calagopus upload; independent remote SHA-256 readback matched its built JAR. No server restart/reload was performed.

## Durable publication

Publish the complete templates/mccodecamp directory (including parent's updated deploy.py.ft) plus create_template.py, test_preset.py, VerifyPaperPreset.java, verify_paper_preset.py, README.md and this report. Legacy VerifyTemplate.java / verify_template.py were preserved but are not the updated verifier; use verify_paper_preset.py. Do not publish scratch artifacts, local IDE registration, tokens or personal environment files.
