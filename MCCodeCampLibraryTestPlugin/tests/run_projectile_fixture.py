"""Ad-hoc test-plugin fixture checks; uses the real Paper API and minimal Bukkit doubles."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
library = root / 'MCCodeCampLibrary'
cp = (library / 'target/editor-classpath.txt').read_text().strip()
cp += os.pathsep + str(library / 'target/classes')
cp += os.pathsep + str(root / 'MCCodeCampLibraryTestPlugin/target/classes')
source = root / 'MCCodeCampLibraryTestPlugin/src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin/testing/ProjectileTestSupport.java'
scratch = Path(os.environ['LOCALAPPDATA']) / 'hermes/cache/scratch'
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='hermes-verify-projectile-fixture-', dir=scratch) as out:
    args = ['javac', '--release', '22', '-cp', cp, '-d', out, str(source), str(source.with_name('ProjectileTestWand.java')), str(source.with_name('ProjectileRestoreItem.java')), str(Path(__file__).with_name('ProjectileTestSupportHarness.java'))]
    result = subprocess.run(args)
    if result.returncode:
        raise SystemExit(result.returncode)
    result = subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibraryTestPlugin.testing.ProjectileTestSupportHarness'])
    raise SystemExit(result.returncode)
