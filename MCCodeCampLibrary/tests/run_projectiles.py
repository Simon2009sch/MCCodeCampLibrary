"""API-boundary projectile tests. Run Maven compile + dependency:build-classpath first.
Not a substitute for testing collisions/particles on a live Paper server.
"""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
classpath = (root / 'target/editor-classpath.txt').read_text().strip()
classpath += os.pathsep + str(root / 'target/classes')
scratch = Path(os.environ['LOCALAPPDATA']) / 'hermes/cache/scratch'
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='hermes-verify-projectiles-', dir=scratch) as output:
    result = subprocess.run(['javac', '--release', '22', '-cp', classpath, '-d', output,
                             str(root / 'tests/ProjectileHarness.java')])
    if result.returncode:
        raise SystemExit(result.returncode)
    result = subprocess.run(['java', '-ea', '-cp', output + os.pathsep + classpath,
                             'me.simoncrafter.mCCodeCampLibrary.projectile.ProjectileHarness'])
    raise SystemExit(result.returncode)
