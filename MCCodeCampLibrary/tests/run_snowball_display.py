"""Actual snowball/CDL behavior at mocked Bukkit boundaries, not live rendering.
Only CDL's static owner key is doubled to avoid a real JavaPlugin bootstrap.
"""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
scratch = Path(os.environ['LOCALAPPDATA']) / 'hermes/cache/scratch'
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='hermes-verify-snowball-display-', dir=scratch) as out:
    tags = Path(out) / 'Tags.java'
    tags.write_text('package me.simoncrafter.CraftersDisplayLibrary.core; public final class Tags { public static final org.bukkit.NamespacedKey CDL_ENTITY = new org.bukkit.NamespacedKey("test", "cdl_entity"); }')
    source = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile/SnowballProjectile.java'
    result = subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, str(tags), str(source), str(root / 'tests/SnowballDisplayHarness.java')])
    if result.returncode:
        raise SystemExit(result.returncode)
    result = subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibrary.projectile.SnowballDisplayHarness'])
    raise SystemExit(result.returncode)
