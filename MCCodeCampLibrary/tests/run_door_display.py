"""Ad-hoc door/helper behavior verification; not a live Paper entity test."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
source_root = root / "src/main/java/me/simoncrafter/mCCodeCampLibrary"
classpath = (root / "target/editor-classpath.txt").read_text().strip()
classpath += os.pathsep + str(root / "target/classes")
sources = [source_root / "internal/editor/display/EditableDisplay.java",
           source_root / "obstical/AOpenableObject.java",
           root / "tests/DoorEditableDisplayHarness.java"]
with tempfile.TemporaryDirectory(prefix="hermes-verify-door-display-", dir=os.environ["TMPDIR"]) as output:
    compiled = subprocess.run(["javac", "--release", "22", "-cp", classpath, "-d", output, *map(str, sources)])
    if compiled.returncode:
        raise SystemExit(compiled.returncode)
    tested = subprocess.run(["java", "-ea", "-cp", output + os.pathsep + classpath,
                             "me.simoncrafter.mCCodeCampLibrary.internal.editor.display.DoorEditableDisplayHarness"])
    if tested.returncode:
        raise SystemExit(tested.returncode)
