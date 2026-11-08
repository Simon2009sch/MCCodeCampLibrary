"""Production custom-item API-boundary harness (no running Paper server)."""
import os
from pathlib import Path
import subprocess
import tempfile
import sys
root = Path(__file__).resolve().parents[1]
base = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/input/item'
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
scratch = Path('C:/Users/simon/AppData/Local/hermes/cache/scratch')
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='hermes-verify-custom-items-', dir=scratch) as out:
    # Isolate this unit from concurrent reactor builds; the facade double is logging-only.
    facade = Path(out) / 'MCCodeCampLib.java'
    facade.write_text('package me.simoncrafter.mCCodeCampLibrary.utility; public final class MCCodeCampLib { public static java.util.logging.Logger getPluginLogger() { return org.bukkit.Bukkit.getLogger(); } }')
    compiled = subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, str(facade), str(base / 'ItemsManager.java'), str(base / 'ACustomItemHandler.java'), str(root / 'tests/CustomItemsHarness.java')])
    if compiled.returncode:
        sys.exit(compiled.returncode)
    modes = sys.argv[1:] or ['unknown', 'block', 'entity', 'attack', 'break', 'hands', 'malformed', 'metadata', 'lifecycle', 'reentrant']
    for mode in modes:
        result = subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'CustomItemsHarness', mode])
        if result.returncode:
            sys.exit(result.returncode)
    print(f'PASS all {len(modes)} custom-item scenarios', flush=True)
