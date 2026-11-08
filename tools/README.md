# Build, upload and reload the test plugin

From the repository root:

```text
python tools/build-and-reload.py --ide-env
```

`--ide-env` explicitly reads the existing private IntelliJ Maven run configuration in `.idea/workspace.xml`. If there is more than one deployment configuration, provide its exact name:

```text
python tools/build-and-reload.py --ide-env "MCCodeCampLibraryProject [clean,package]"
```

If `CALAGOPUS_PANEL_URL`, `CALAGOPUS_API_TOKEN` and the **full** `CALAGOPUS_SERVER_ID` are already in the process environment, omit `--ide-env`. Credentials are never stored in this script or passed as command-line arguments. Like the existing uploader, this tool does not automatically load arbitrary `.env` files.

## What it does

1. Runs the existing `mvn -B -Pdev clean package` routine. The existing Maven deployment execution performs the upload after shading.
2. Reads back the exact remote JAR's SHA-256 and compares it to the built artifact. A mismatch prevents reload.
3. Checks server state and uptime immediately before issuing `plugman reload MCCodeCampLibraryTestPlugin` once.
4. Verifies a **new** PlugMan success message from the console; an earlier reload message does not count.

Offline/starting/stopping servers are left alone. If a restart or an ambiguous network failure occurs after command submission, the tool does not retry the command. It never starts/stops/restarts the server. A timeout or failed verification exits nonzero rather than claiming success.

This is a remote-only helper: inherited `PLUGINSFOLDER` is removed from the build child's environment so it cannot also copy a JAR to an unrelated local server. `CALAGOPUS_PLUGIN_PATH` is honored, defaulting to `/plugins/MCCodeCampLibraryTestPlugin.jar`.

The API key needs access to upload/fingerprint, server resources, logs and console-command routes. HTTPS verification remains enabled, redirects are refused, and known key text is redacted from Maven output.

## Options

```text
python tools/build-and-reload.py --ide-env --dry-run
python tools/build-and-reload.py --ide-env --no-reload
python tools/build-and-reload.py --ide-env --maven "C:/path/to/mvn.cmd"
```

`--dry-run` validates local configuration/Maven discovery only; it does not build, connect or write remotely. `--no-reload` still builds, uploads and verifies the JAR. Maven discovery uses the project wrapper, PATH, then IntelliJ's bundled Maven on Windows.

## Offline tests

```text
python -m unittest discover -s tools/tests -v
```

The tests use protocol doubles, never real panel credentials. Live execution additionally verifies the actual uploaded hash and a fresh console acknowledgement.
