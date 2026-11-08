"""Pure capsule geometry and production ray at Bukkit API boundaries; no world writes."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
scratch = Path(os.environ['LOCALAPPDATA']) / 'hermes/cache/scratch'
source = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile'
with tempfile.TemporaryDirectory(prefix='hermes-verify-excavating-', dir=scratch) as out:
    sources = [source / 'effect/Capsule.java', source / 'ExcavatingRaycastProjectile.java',
               root / 'tests/CapsuleHarness.java', root / 'tests/ExcavatingRaycastHarness.java']
    subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, *map(str, sources)], check=True)
    for harness in ['CapsuleHarness', 'ExcavatingRaycastHarness']:
        subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibrary.projectile.' + harness], check=True)
