"""Compile and run the real student feather handler at doubled Bukkit boundaries.
Prerequisite: mvn -Pdev -Dexec.skip=true compile, plus library dependency:build-classpath
with -Dmdep.outputFile=target/editor-classpath.txt. No server/deployment required.
"""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[2]
plugin = root / 'MCCodeCampLibraryTestPlugin'
library = root / 'MCCodeCampLibrary'
cp = (library / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(library / 'target/classes')
source = plugin / 'src/main/java/me/simoncrafter/mCCodeCampLibraryTestPlugin/yourCode/a4_flightFeather/Course.java'
with tempfile.TemporaryDirectory(prefix='flight-feather-') as out:
    subprocess.run(['javac', '--release', '21', '-cp', cp, '-d', out, str(source), str(plugin / 'tests/FlightFeatherHarness.java')], check=True)
    subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a4_flightFeather.FlightFeatherHarness'], check=True)
