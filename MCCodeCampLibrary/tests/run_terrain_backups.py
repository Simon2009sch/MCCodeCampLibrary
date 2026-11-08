from pathlib import Path
import subprocess, tempfile, os
root=Path(__file__).resolve().parents[1]
cp=(root/'target/editor-classpath.txt').read_text().strip().replace('\\','/')
with tempfile.TemporaryDirectory(prefix='terrain-compile-',dir='C:/Users/simon/AppData/Local/hermes/cache/scratch') as out:
 sources=list((root/'src/main/java/me/simoncrafter/mCCodeCampLibrary/projectile/effect/backup').glob('*.java'))+[root/'tests/TerrainBackupsHarness.java']
 out=out.replace('\\','/')
 args=Path(out)/'javac.args'
 args.write_text('\n'.join(['--release','22','-cp','"'+cp+'"','-d','"'+out+'"']+['"'+str(p).replace('\\','/')+'"' for p in sources]))
 subprocess.run(['javac','@'+str(args)],check=True)
 env=dict(os.environ,TMPDIR='C:/Users/simon/AppData/Local/hermes/cache/scratch')
 subprocess.run(['java','-ea','-cp',out+';'+cp,'me.simoncrafter.mCCodeCampLibrary.projectile.effect.backup.TerrainBackupsHarness'],check=True,env=env)
