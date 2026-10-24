# Calagopus test-plugin deployment

`deploy-local.py` is tracked and credential-free. Configure variables in the IntelliJ **Maven run configuration**, not in source code. The launcher uses only Python's standard library (`python` on Windows; `python3` on Unix/WSL).

## Environment

| Variable | Value |
|---|---|
| `CALAGOPUS_PANEL_URL` | HTTPS panel URL, e.g. `https://panel.codeyourhome.eu` |
| `CALAGOPUS_API_TOKEN` | New Calagopus API key; never paste into chat or commit |
| `CALAGOPUS_SERVER_ID` | Full server UUID, not the old eight-character Pelican identifier |
| `CALAGOPUS_PLUGIN_PATH` | Optional; defaults to `/plugins/MCCodeCampLibraryTestPlugin.jar` |
| `CALAGOPUS_DEPLOY_DRY_RUN` | Optional `true`: validate only, no Python network or file writes |
| `PLUGINSFOLDER` | Optional local plugins folder |

The key needs server permissions **`files.create`** and **`files.read-content`**. Scope it to the test server where possible. `PELICAN_*` variable names remain fallback aliases during migration; they now use the Calagopus protocol and still require the **new key and full UUID**. Explicit `CALAGOPUS_*` values take precedence, including an empty value. `.env` files are not loaded automatically.

## Build / upload

From the repository root: `mvn -Pdev clean package`. Shading finishes before the Python deployment execution. `-Dexec.skip=true` disables the Python execution without skipping compilation or packaging. No remote variables means no remote upload; partial configuration fails rather than silently uploading elsewhere. No server reload or restart is performed.

The existing Maven local-copy profile still copies at `package` when `PLUGINSFOLDER` is set, independently of Python. For a completely side-effect-free Maven dry-run, unset `PLUGINSFOLDER` as well as setting `CALAGOPUS_DEPLOY_DRY_RUN=true`.

## Protocol and verification

Verified against the current panel's public `/openapi.json` and Calagopus upstream routes:

1. Inspect the existing target with `GET /api/client/servers/{uuid}/files/fingerprint?file=...&algorithm=sha256` (404 is allowed for a new file).
2. Request `GET /api/client/servers/{uuid}/files/upload`, which returns `{ "url": "..." }`.
3. POST a multipart field named **`files`** to the signed Wings URL with `directory` and `total_size` query parameters. The filename comes from the configured target path, not necessarily the local JAR name. Do **not** forward the panel bearer key to Wings.
4. Read the exact target fingerprint again and compare its SHA-256 to the built JAR. A mismatch fails the build.

The previous `/files/write` approach targets a size-limited text-editor route; the signed upload is the panel's file-upload workflow and avoids that limit. HTTPS verification remains enabled, redirects are refused, signed URLs and response bodies are never printed. The upload buffers one JAR plus multipart framing in memory.

## Tests

`python -m unittest discover -s MCCodeCampLibraryTestPlugin/tests -v`

Tests use dummy credentials and a protocol harness, never a real panel. A real upload must additionally be tested and SHA-256 verified with your local key before publishing migration changes.

## Legacy shell scripts

The Unix-only `deploy-local.sh` compatibility profile remains for existing developer setups, but it does not migrate its implementation to Calagopus. Prefer the tracked Python launcher. Do not enable both Python and legacy shell deployment simultaneously.
