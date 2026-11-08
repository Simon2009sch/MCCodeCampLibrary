"""Ad-hoc coordinator safety gates against actual production code and controlled backup futures."""
import os
from pathlib import Path
import subprocess
import tempfile
root=Path(__file__).resolve().parents[1]
cp=(root/'target/editor-classpath.txt').read_text().strip()+os.pathsep+str(root/'target/classes')
scratch=Path(os.environ['LOCALAPPDATA'])/'hermes/cache/scratch';scratch.mkdir(parents=True,exist_ok=True)
source=root/'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile/effect/ProjectileEffects.java'
with tempfile.TemporaryDirectory(prefix='hermes-verify-effect-gates-',dir=scratch) as out:
    result=subprocess.run(['javac','--release','22','-cp',cp,'-d',out,str(source),str(root/'tests/ProjectileEffectsHarness.java')])
    if result.returncode:raise SystemExit(result.returncode)
    raise SystemExit(subprocess.run(['java','-ea','-cp',out+os.pathsep+cp,'me.simoncrafter.mCCodeCampLibrary.projectile.effect.ProjectileEffectsHarness']).returncode)
