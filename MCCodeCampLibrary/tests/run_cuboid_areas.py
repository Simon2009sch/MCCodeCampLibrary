"""Production cuboid model behavior and editor compilation; no live Paper test."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
src = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary'
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
sources = list((src / 'internal/editor/area').glob('*.java')) + [root / 'tests/CuboidAreaHarness.java',
    root / 'tests/DoorEditableDisplayHarness.java', root / 'tests/DoorPivotHarness.java']
for path in ['input/editor/DoorEditor.java', 'obstical/AOpenableObject.java', 'obstical/AAnimatedOpenableObject.java']:
    if (src / path).exists():
        sources.append(src / path)
with tempfile.TemporaryDirectory(prefix='hermes-verify-areas-', dir=os.environ['TMPDIR']) as out:
    subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, *map(str, sources)], check=True)
    subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp,
                    'me.simoncrafter.mCCodeCampLibrary.internal.editor.area.CuboidAreaHarness'], check=True)
    subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp,
                    'me.simoncrafter.mCCodeCampLibrary.internal.editor.display.DoorPivotHarness'], check=True)
