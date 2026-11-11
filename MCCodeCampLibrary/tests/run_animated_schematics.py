from pathlib import Path
import subprocess,tempfile,os
root=Path(__file__).resolve().parents[1]
cpfile=root/'target/editor-classpath.txt'
if not cpfile.exists(): cpfile=Path.home()/'AppData/Local/hermes/cache/scratch/tdot-classpath.txt'
cp=str(root/'target/classes')+os.pathsep+cpfile.read_text().strip()
scratch='C:/Users/simon/AppData/Local/hermes/cache/scratch'
os.environ['TMPDIR']=scratch
with tempfile.TemporaryDirectory(prefix='animated-compile-',dir=scratch) as out:
 sources=list((root/'src/main/java/me/simoncrafter/mCCodeCampLibrary/animation/schematic').glob('*.java'))+[root/'tests'/name for name in ['AnimatedSchematicFilesHarness.java','AnimatedSelectionHarness.java','AnimatedStoreBoundaryHarness.java']]
 args=Path(out)/'javac.args'
 args.write_text('\n'.join(['--release','22','-cp','"'+cp.replace('\\','/')+'"','-d','"'+out.replace('\\','/')+'"']+['"'+str(p).replace('\\','/')+'"' for p in sources]))
 subprocess.run(['javac','@'+str(args)],check=True)
 for name in ['AnimatedSchematicFilesHarness','AnimatedSelectionHarness','AnimatedStoreBoundaryHarness']:
  subprocess.run(['java','-ea','-cp',out+';'+cp,'me.simoncrafter.mCCodeCampLibrary.animation.schematic.'+name],check=True)
