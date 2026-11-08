#!/usr/bin/env python3
"""Build/upload this project's test plugin, verify its hash and conditionally reload it.

Credentials come from the existing CALAGOPUS_* environment or explicit --ide-env.
No credentials, arbitrary console commands or server power actions are stored here.
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import time
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, build_opener
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PLUGIN = 'MCCodeCampLibraryTestPlugin'
ARTIFACT = ROOT / f'{PLUGIN}/target/{PLUGIN}-1.jar'
_spec = importlib.util.spec_from_file_location('project_uploader', ROOT / f'{PLUGIN}/deploy-local.py')
uploader = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(uploader)
ANSI = re.compile(r'\x1b\[[0-?]*[ -/]*[@-~]')


def deployment_environment(environ, root=ROOT, ide_name=None):
    """Copy only deployment settings from an explicitly selected private IDE config."""
    values = dict(environ)
    if ide_name is not None:
        matches = []
        for config in ET.parse(root / '.idea/workspace.xml').getroot().findall('.//configuration'):
            if config.get('type') != 'MavenRunConfiguration' or (ide_name and config.get('name') != ide_name):
                continue
            settings = {n.get('key', n.get('name')): n.get('value') for n in config.iter()
                        if n.get('key', n.get('name')) and n.get('value') is not None}
            if all(settings.get('CALAGOPUS_' + suffix) for suffix in ('PANEL_URL', 'API_TOKEN', 'SERVER_ID')):
                matches.append(settings)
        if len(matches) != 1:
            raise ValueError('Expected one Maven deployment configuration; pass --ide-env "configuration name"')
        values = {k: v for k, v in values.items() if not k.startswith(('CALAGOPUS_', 'PELICAN_'))}
        values.update({k: v for k, v in matches[0].items() if k.startswith('CALAGOPUS_')})
    for suffix in ('PANEL_URL', 'API_TOKEN', 'SERVER_ID'):
        if not values.get('CALAGOPUS_' + suffix):
            raise ValueError('Missing CALAGOPUS_' + suffix + '; use the existing environment or explicit --ide-env')
    panel = uploader.https_url(values['CALAGOPUS_PANEL_URL'].rstrip('/'))
    server = values['CALAGOPUS_SERVER_ID']
    if not re.fullmatch(r'[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}', server):
        raise ValueError('CALAGOPUS_SERVER_ID must be the full UUID; short IDs are not repaired or guessed')
    remote = values.get('CALAGOPUS_PLUGIN_PATH') or f'/plugins/{PLUGIN}.jar'
    remote = '/' + remote.lstrip('/')
    if any(c in remote for c in ('\r', '\n', '"', '\\', '\x00')) or '..' in remote.split('/') or not remote.lower().endswith('.jar'):
        raise ValueError('Invalid CALAGOPUS_PLUGIN_PATH')
    values = {k: v for k, v in values.items() if not k.startswith('PELICAN_') and k != 'PLUGINSFOLDER'}
    values.update(CALAGOPUS_PANEL_URL=panel, CALAGOPUS_SERVER_ID=server,
                  CALAGOPUS_PLUGIN_PATH=remote, CALAGOPUS_DEPLOY_DRY_RUN='false')
    return values


class Panel:
    def __init__(self, env):
        self.base = env['CALAGOPUS_PANEL_URL'] + '/api/client/servers/' + env['CALAGOPUS_SERVER_ID']
        self.token = env['CALAGOPUS_API_TOKEN']
        self.opener = build_opener(uploader.NoRedirect())

    def request(self, suffix, payload=None):
        headers = {'Authorization': 'Bearer ' + self.token, 'Accept': 'application/json'}
        data = None
        if payload is not None:
            headers['Content-Type'] = 'application/json'
            data = json.dumps(payload).encode('utf-8')
        request = Request(self.base + suffix, data=data, headers=headers,
                          method='GET' if payload is None else 'POST')
        with self.opener.open(request, timeout=30) as response:
            body = response.read().decode('utf-8', errors='replace')
            if not body:
                return None
            try:
                return json.loads(body)
            except json.JSONDecodeError:
                return body

    def resources(self):
        return self.request('/resources')['resources']

    def fingerprint(self, remote):
        return self.request('/files/fingerprint?' + urlencode({'file': remote, 'algorithm': 'sha256'}))['fingerprint']

    def logs(self):
        text = self.request('/logs?lines=1000')
        if not isinstance(text, str):
            raise RuntimeError('Unexpected console response; reload cannot be verified')
        return ANSI.sub('', text).splitlines()

    def command(self, command):
        # Do not automatically retry: a network failure can follow a successful submission.
        self.request('/command', {'command': command})


def new_lines(before, after):
    """Extract new console lines even when the fixed-size log tail advances."""
    for overlap in range(min(len(before), len(after)), 0, -1):
        if before[-overlap:] == after[:overlap]:
            return after[overlap:]
    return after


def reload_plugin(panel, timeout=60):
    state = panel.resources()
    if state['state'] != 'running':
        print('Reload skipped: server is ' + state['state'])
        return 'skipped'
    baseline = panel.logs()
    current = panel.resources()  # Recheck immediately before the only write.
    if current['state'] != 'running' or current['uptime'] < state['uptime']:
        print('Reload skipped: server changed state or restarted')
        return 'skipped'
    panel.command('plugman reload ' + PLUGIN)
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        current = panel.resources()
        if current['state'] != 'running' or current['uptime'] < state['uptime']:
            raise RuntimeError('Reload verification interrupted by server restart; command will not be retried')
        lines = new_lines(baseline, panel.logs())
        if any(PLUGIN + ' has been reloaded.' in line and '[PlugMan' in line for line in lines):
            print('Reload verified: ' + PLUGIN)
            return 'reloaded'
        time.sleep(1)
    raise RuntimeError('Reload submitted but no fresh success message was observed; command will not be retried')


def verify_artifact(panel, artifact, remote):
    expected = hashlib.sha256(artifact.read_bytes()).hexdigest()
    if panel.fingerprint(remote).lower() != expected:
        raise RuntimeError('Remote SHA-256 does not match the built JAR; refusing reload')
    print('Remote SHA-256 verified: ' + expected)


def find_maven(explicit=None):
    if explicit:
        resolved = shutil.which(explicit) or (str(Path(explicit).resolve()) if Path(explicit).is_file() else None)
        if not resolved:
            raise ValueError('--maven does not point to a Maven launcher')
        return resolved
    wrapper = ROOT / ('mvnw.cmd' if os.name == 'nt' else 'mvnw')
    if wrapper.is_file():
        return str(wrapper)
    installed = shutil.which('mvn') or shutil.which('mvn.cmd')
    if installed:
        return installed
    if os.name == 'nt':
        jetbrains = Path(os.environ.get('ProgramFiles', 'C:/Program Files')) / 'JetBrains'
        launchers = sorted(jetbrains.glob('IntelliJ IDEA*/plugins/maven/lib/maven3/bin/mvn.cmd'), reverse=True)
        if launchers:
            return str(launchers[0])
    raise ValueError('Maven not found; supply --maven or add Maven to PATH')


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ide-env', nargs='?', const='', metavar='NAME', help='Explicitly load private Maven run environment from .idea/workspace.xml; optional configuration name')
    parser.add_argument('--maven', help='Maven launcher path when not on PATH')
    parser.add_argument('--no-reload', action='store_true', help='Build/upload/verify only')
    parser.add_argument('--dry-run', action='store_true', help='Validate configuration and print scope; no build, network or remote writes')
    args = parser.parse_args(argv)
    try:
        env = deployment_environment(os.environ, ROOT, args.ide_env)
        command = [find_maven(args.maven), '-B', '-Pdev', 'clean', 'package']
        print('Target server: ' + env['CALAGOPUS_SERVER_ID'])
        print('Remote JAR: ' + env['CALAGOPUS_PLUGIN_PATH'])
        if args.dry_run:
            print('Dry run: Maven dev clean/package + verified upload' + ('' if args.no_reload else ' + conditional PlugMan reload'))
            return 0
        # The existing Maven deployment execution uploads after shading. Do not bypass it.
        with subprocess.Popen(command, cwd=ROOT, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              encoding='utf-8', errors='replace') as build:
            for line in build.stdout:
                print(line.replace(env['CALAGOPUS_API_TOKEN'], '[REDACTED]'), end='')
            if build.wait():
                raise RuntimeError('Maven failed; no reload attempted')
        panel = Panel(env)
        verify_artifact(panel, ARTIFACT, env['CALAGOPUS_PLUGIN_PATH'])
        if not args.no_reload:
            reload_plugin(panel)
        return 0
    except HTTPError as error:
        print(f'Calagopus HTTP {error.code}; no console command is automatically retried')
    except (URLError, OSError):
        print('Network/TLS or local file error; no console command is automatically retried')
    except (ValueError, RuntimeError, KeyError, TypeError):
        # Configuration and protocol errors may originate in third-party responses: no raw bodies/URLs.
        print('Build/upload/reload failed; verify configuration, build output and server console before retrying')
    return 1


if __name__ == '__main__':
    raise SystemExit(main())
