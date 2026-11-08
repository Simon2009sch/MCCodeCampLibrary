"""Compile actual production classes; API-boundary tests only, no deployment."""
import os
from pathlib import Path
import subprocess
import tempfile
root = Path(__file__).resolve().parents[1]
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
with tempfile.TemporaryDirectory(prefix='hermes-verify-surface-', dir='C:/Users/simon/AppData/Local/hermes/cache/scratch') as out:
    base = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile/effect'
    sources = [base / 'DestructionPolicy.java', base / 'SurfaceImpact.java', root / 'tests/SurfaceImpactHarness.java']
    subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, *map(str, sources)], check=True)
    subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibrary.projectile.effect.SurfaceImpactHarness'], check=True)
