"""API-boundary checks for the production persistent projectile target."""
import os
from pathlib import Path
import subprocess
import tempfile
import sys
root = Path(__file__).resolve().parents[1]
base = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary'
cp = (root / 'target/editor-classpath.txt').read_text().strip() + os.pathsep + str(root / 'target/classes')
sources = [base / 'input/activation/ProjectileTarget.java', base / 'input/activation/event/ProjectileTargetHitEvent.java', root / 'tests/ProjectileTargetHarness.java']
sources += list((base / 'projectile').rglob('*.java'))
scratch = Path(os.environ['LOCALAPPDATA']) / 'hermes/cache/scratch'
scratch.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='hermes-verify-projectile-target-', dir=scratch) as out:
    if '--contract-doubles' in sys.argv:
        print('Testing target against custom-projectile API contract doubles (not projectile implementation)', flush=True)
        sources = sources[:3]
        doubles = {
            'CustomProjectile.java': 'package me.simoncrafter.mCCodeCampLibrary.projectile; public class CustomProjectile { public org.bukkit.entity.Entity getShooter(){return null;} public org.bukkit.Location getLocation(){return null;} }',
            'ProjectileHit.java': 'package me.simoncrafter.mCCodeCampLibrary.projectile; public record ProjectileHit(org.bukkit.block.Block block, org.bukkit.entity.Entity entity, org.bukkit.util.Vector position, org.bukkit.block.BlockFace face) {}',
            'ProjectileHitEvent.java': '''package me.simoncrafter.mCCodeCampLibrary.projectile.event;
                import me.simoncrafter.mCCodeCampLibrary.projectile.*;
                public class ProjectileHitEvent extends org.bukkit.event.Event implements org.bukkit.event.Cancellable {
                    private final CustomProjectile projectile; private final ProjectileHit hit; private boolean cancelled;
                    public ProjectileHitEvent(CustomProjectile projectile, ProjectileHit hit) {this.projectile=projectile;this.hit=hit;}
                    public CustomProjectile getProjectile(){return projectile;} public ProjectileHit getHit(){return hit;}
                    public boolean isCancelled(){return cancelled;} public void setCancelled(boolean value){cancelled=value;}
                    public org.bukkit.event.HandlerList getHandlers(){return new org.bukkit.event.HandlerList();}
                }'''
        }
        for name, content in doubles.items():
            path = Path(out) / name
            path.write_text(content)
            sources.append(path)
    subprocess.run(['javac', '--release', '22', '-cp', cp, '-d', out, *map(str, sources)], check=True)
    subprocess.run(['java', '-ea', '-cp', out + os.pathsep + cp, 'me.simoncrafter.mCCodeCampLibrary.input.activation.ProjectileTargetHarness', *sys.argv[1:]], check=True)
