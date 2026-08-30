#!/usr/bin/env python3
"""Upload the test-plugin JAR to a Pelican client server.

This personal copy is gitignored. Required environment variables:
  PELICAN_PANEL_URL, PELICAN_API_TOKEN, PELICAN_SERVER_ID
Optional: PELICAN_PLUGIN_PATH, PELICAN_DEPLOY_DRY_RUN
"""

from __future__ import annotations

import os
import shutil
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


def required_env(name: str) -> str:
    value = os.environ.get(name, "").strip()
    if not value:
        raise SystemExit(f"Missing required environment variable: {name}")
    return value


def main() -> int:
    if len(sys.argv) != 2 or sys.argv[1] in {"-h", "--help"}:
        print(f"Usage: {Path(sys.argv[0]).name} <jar-file>")
        return 0 if len(sys.argv) == 2 else 2

    artifact = Path(sys.argv[1]).resolve()
    if not artifact.is_file():
        raise SystemExit(f"JAR file does not exist: {artifact}")

    localTestserver = os.environ.get("PLUGINSFOLDER", "").strip()
    if localTestserver:
        localDir = Path(localTestserver)
        localDir.mkdir(parents=True, exist_ok=True)
        shutil.copy2(artifact, localDir / artifact.name)
        print(f"Copied {artifact.name} to local plugins folder {localDir}")
        


    panel_url = required_env("PELICAN_PANEL_URL").rstrip("/")
    api_token = required_env("PELICAN_API_TOKEN")
    server_id = required_env("PELICAN_SERVER_ID")
    remote_file = os.environ.get(
        "PELICAN_PLUGIN_PATH", "/plugins/MCCodeCampLibraryTestPlugin.jar"
    ).strip()
    if not remote_file.startswith("/"):
        remote_file = "/" + remote_file

    endpoint = (
        f"{panel_url}/api/client/servers/{server_id}/files/write?"
        + urlencode({"file": remote_file})
    )
    payload = artifact.read_bytes()
    print(f"Uploading {artifact} ({len(payload)} bytes)")
    print(f"Target: {panel_url} / server {server_id} / {remote_file}")

    if os.environ.get("PELICAN_DEPLOY_DRY_RUN", "").lower() in {"1", "true", "yes"}:
        print("Dry run: upload skipped")
        return 0

    request = Request(
        endpoint,
        data=payload,
        method="POST",
        headers={
            "Accept": "application/json",
            "Content-Type": "application/octet-stream",
            "Authorization": f"Bearer {api_token}",
        },
    )

    try:
        with urlopen(request, timeout=60) as response:
            response.read()
            print(f"Upload succeeded (HTTP {response.status})")
    except HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        print(f"Pelican upload failed (HTTP {error.code}): {detail}", file=sys.stderr)
        return 1
    except URLError as error:
        print(f"Could not reach Pelican: {error.reason}", file=sys.stderr)
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
