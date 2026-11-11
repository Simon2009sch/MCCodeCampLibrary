"""Ad-hoc behavior test of the production click-handler body with minimal editor boundary doubles."""
from pathlib import Path
import subprocess
import tempfile
root=Path(__file__).resolve().parents[1]
source=(root/'src/main/java/me/simoncrafter/mCCodeCampLibrary/input/editor/WorldMarkerEditor.java').read_text()
start=source.index('    protected void onPlayerClickObjectEvent(')
brace=source.index('{',start)
depth=1
end=brace+1
while depth:
    depth += (source[end]=='{')-(source[end]=='}')
    end+=1
method=source[start:end]
java='''import java.util.*;
public class ClickHarness {
 static class Player {}
 static class Editable {int opens;void openEditor(Player p){opens++;}}
 static class Manager {
  Object current; Editable selected;int selects,deselects;
  Object getPlayersCurrentEditor(Player p){return current;}
  Editable getPlayerSelection(Player p){return selected;}
  void select(Player p,Editable e){selected=e;selects++;}
  void deselect(Player p){selected=null;deselects++;}
 }
 static class MCCodeCampLib {static final Manager manager=new Manager();static Manager getEditorManager(){return manager;}}
 record PlayerClickEditableObjectEvent(Player getPlayer,Editable getClicked){}
 METHOD
 public static void main(String[] args){
  ClickHarness editor=new ClickHarness();Player p=new Player();Editable object=new Editable();
  Manager manager=MCCodeCampLib.manager;manager.current=editor;
  editor.onPlayerClickObjectEvent(new PlayerClickEditableObjectEvent(p,object));
  if(object.opens!=1||manager.selected!=object)throw new AssertionError("Click must select and open object's subeditor");
  editor.onPlayerClickObjectEvent(new PlayerClickEditableObjectEvent(p,object));
  if(object.opens!=2||manager.deselects!=0||manager.selects!=1)throw new AssertionError("Selected object should reopen, never toggle off");
  manager.current=new Object();
  editor.onPlayerClickObjectEvent(new PlayerClickEditableObjectEvent(p,object));
  if(object.opens!=2)throw new AssertionError("Parent editor handled a click while its child was active");
  System.out.println("PASS production handler: select+open, repeated click, ignore inactive parent");
 }
}
'''.replace('METHOD',method)
scratch=Path.home()/'AppData/Local/hermes/cache/scratch'
with tempfile.TemporaryDirectory(prefix='hermes-verify-editor-click-',dir=scratch) as temp:
    path=Path(temp)/'ClickHarness.java'
    path.write_text(java)
    subprocess.run(['javac','--release','22','-d',temp,str(path)],check=True)
    subprocess.run(['java','-ea','-cp',temp,'ClickHarness'],check=True)
