"""Compile actual production classes; API-boundary tests only, no deployment."""
import os
from pathlib import Path
import subprocess
import tempfile
root = Path(__file__).resolve().parents[1]
cp = os.environ.get('SURFACE_IMPACT_CLASSPATH')
if not cp:
    cp = (root / 'target/editor-classpath.txt').read_text().strip()
cp += os.pathsep + str(root / 'target/classes')
with tempfile.TemporaryDirectory(prefix='hermes-verify-surface-') as out:
    base = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile/effect'
    sources = [base / 'DestructionPolicy.java', base / 'SurfaceImpact.java', root / 'tests/SurfaceImpactHarness.java']
    subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, *map(str, sources)], check=True)
    subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibrary.projectile.effect.SurfaceImpactHarness'], check=True)
