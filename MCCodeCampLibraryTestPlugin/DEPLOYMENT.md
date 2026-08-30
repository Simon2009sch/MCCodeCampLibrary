# Test-plugin remote deployment

This is a per-developer development-machine workflow. It is not required for normal library builds and must never contain committed credentials.

## Recommended Windows/Linux/WSL workflow

1. Copy `deploy-local.example.py` to `deploy-local.py` in this directory.
2. Keep `deploy-local.py` uncommitted; it is covered by the repository `.gitignore`.
3. Configure these environment variables in the IntelliJ Maven run configuration or in the shell that launches Maven:

   - `PELICAN_PANEL_URL` — panel base URL, without a trailing slash
   - `PELICAN_API_TOKEN` — Pelican **client** API token; never put it in Git or the POM
   - `PELICAN_SERVER_ID` — Pelican client-server identifier
   - `PELICAN_PLUGIN_PATH` — optional remote path; defaults to `/plugins/MCCodeCampLibraryTestPlugin.jar`

4. Run the normal test-plugin package build. When `deploy-local.py` exists, the POM automatically runs it after packaging and passes the generated JAR path as its only argument.

The launcher uses Python's standard library and works with native Windows Python, Linux `python3`, and WSL. It supports `PELICAN_DEPLOY_DRY_RUN=true` to validate the local artifact and configuration without uploading.

## Migration from the legacy `.sh` deployment

Existing Linux/WSL developer machines may still have the ignored `deploy-local.sh`. The Maven POM retains a Unix-only compatibility profile so that legacy setup continues to work when Maven is run in Linux/WSL.

The `.sh` route is now legacy:

- Do not copy an old `.sh` script unchanged into a native Windows Maven setup.
- New machines should copy `deploy-local.example.py` to `deploy-local.py` instead.
- A Windows developer who wants to keep the old script should run Maven inside WSL; a native Windows Maven process will not activate the legacy Bash profile.
- Agents working on another machine should point developers to this migration guide and must not request or commit their API token.

## API behavior

The Python launcher uploads the packaged JAR with Pelican's client API file-write endpoint:

`POST /api/client/servers/{server-id}/files/write?file={remote-plugin-path}`

It sends the JAR as binary data with a bearer token, reports the HTTP result, and returns a non-zero exit code for missing configuration, unreachable panels, or HTTP failures.
