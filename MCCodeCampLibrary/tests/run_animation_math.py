"""Runs the production animation math against the resolved Maven dependencies."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
scratch = Path.home() / 'AppData/Local/hermes/cache/scratch'
with tempfile.TemporaryDirectory(prefix='hermes-verify-animation-', dir=scratch) as temp:
    cpfile = scratch / 'tdot-classpath.txt'
    if not cpfile.exists():
        raise RuntimeError('Generate tdot-classpath.txt with Maven dependency:build-classpath first (see ANIMATED_OBJECTS.md).')
    cp = cpfile.read_text().strip() + os.pathsep + str(root / 'target/classes')
    sources = list((root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/animation').glob('*.java'))
    sources = [p for p in sources if p.name in ('AnimationTrack.java', 'AnimationPose.java', 'ProximityMath.java', 'AnimatedBlockRuntime.java', 'AnimationSettings.java', 'SchematicScene.java', 'AnimationRecoveryJournal.java', 'AnimationSourceClaims.java')]
    sources += list((root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/animation/schematic').glob('*.java'))
    harnesses = ['AnimationMathHarness', 'AnimatedRuntimeHarness', 'SchematicSceneHarness', 'AnimationRecoveryHarness']
    subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', temp, *map(str, sources), *[str(root / f'tests/{name}.java') for name in harnesses]], check=True)
    for name in harnesses:
        subprocess.run(['java', '-ea', '-cp', temp + os.pathsep + cp, f'me.simoncrafter.mCCodeCampLibrary.animation.{name}', temp], check=True)
