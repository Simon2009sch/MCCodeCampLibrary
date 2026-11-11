#!/usr/bin/env python3
"""Compile and run the isolated wind-effect contract harness (JDK 25)."""
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'MCCodeCampLibraryTestPlugin/src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin/courseInternal/a4_flightFeather/WindEffect.java'
CDL = Path.home() / '.m2/repository/com/github/Simon2009sch/CraftersDisplayLibrary/CraftersDisplayLibrary/v1.2.5a/CraftersDisplayLibrary-v1.2.5a.jar'
cp = os.pathsep.join([str(CDL), (ROOT / 'MCCodeCampLibrary/target/editor-classpath.txt').read_text().strip()])
with tempfile.TemporaryDirectory(prefix='wind-effect-', dir=os.environ.get('TMPDIR')) as output:
    subprocess.run(['javac', '-cp', cp, '-d', output, str(SOURCE), str(Path(__file__).with_name('WindEffectHarness.java'))], check=True)
    subprocess.run(['java', '-ea', '-cp', output + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a4_flightFeather.WindEffectHarness'], check=True)
