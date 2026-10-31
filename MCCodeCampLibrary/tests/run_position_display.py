"""Compile the real EditableDisplay and run its CDL-hierarchy harness (no Paper server)."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
classpath = (root / "target/editor-classpath.txt").read_text().strip()
source = root / "src/main/java/me/simoncrafter/mCCodeCampLibrary/internal/editor/display/EditableDisplay.java"
harness = root / "tests/PositionEditableDisplayHarness.java"
with tempfile.TemporaryDirectory(prefix="hermes-verify-position-display-", dir=os.environ["TMPDIR"]) as output:
    subprocess.run(["javac", "--release", "22", "-cp", classpath, "-d", output, str(source), str(harness)], check=True)
    subprocess.run(["java", "-ea", "-cp", output + os.pathsep + classpath,
                    "me.simoncrafter.mCCodeCampLibrary.internal.editor.display.PositionEditableDisplayHarness"], check=True)
