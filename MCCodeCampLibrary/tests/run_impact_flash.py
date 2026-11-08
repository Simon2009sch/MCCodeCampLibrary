"""Compile actual ImpactFlash against resolved APIs, run adapter-boundary tests (no deployment)."""
import os
from pathlib import Path
import subprocess
import tempfile
root = Path(__file__).resolve().parents[1]
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
scratch = Path('C:/Users/simon/AppData/Local/hermes/cache/scratch')
with tempfile.TemporaryDirectory(prefix='hermes-verify-impact-flash-', dir=scratch) as out:
    sources = [root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile/effect/ImpactFlash.java', root / 'tests/ImpactFlashHarness.java']
    result = subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, *map(str, sources)])
    if result.returncode:
        raise SystemExit(result.returncode)
    raise SystemExit(subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibrary.projectile.effect.ImpactFlashHarness']).returncode)
