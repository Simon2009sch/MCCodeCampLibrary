import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import com.demonwav.mcdev.creator.custom.model.*;
import com.demonwav.mcdev.creator.custom.derivation.*;
import com.demonwav.mcdev.util.SemanticVersion;
import com.google.gson.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public class VerifyPaperPreset {
 static String render(VelocityContext c,String s) throws Exception {var w=new StringWriter(); if(!Velocity.evaluate(c,w,"preset-test",s)) throw new Exception("Velocity failed"); return w.toString();}
 public static void main(String[] a) throws Exception {
  Path t=Path.of(a[0]), out=Path.of(a[1]); boolean paper=Boolean.parseBoolean(a[2]), optional=Boolean.parseBoolean(a[3]);
  var mc=SemanticVersion.Companion.parse(a.length>4?a[4]:"1.21.11");
  var javaVersion=new RecommendJavaVersionForMcVersionPropertyDerivation(17).derive(List.of(mc));
  var dependency=new FetchPaperDependencyVersionForMcVersion().derive(List.of(mc,"Maven"));
  System.out.println("Installed derivations: MC="+mc+" Java="+javaVersion+" dependency="+dependency);
  var c=new VelocityContext(); c.put("BUILD_COORDS",new BuildSystemCoordinates("me.simoncrafter","template-probe","1.0-SNAPSHOT")); c.put("MAIN_CLASS",new ClassFqn("me.simoncrafter.templateprobe.TemplateProbe"));
  c.put("PLUGIN_NAME","TemplateProbe"); c.put("MC_VERSION",mc); c.put("JAVA_VERSION",javaVersion); c.put("DEPENDENCY_VERSION",dependency); c.put("API_VERSION",new ExtractPaperApiVersionPropertyDerivation().derive(List.of(mc))); c.put("BUILD_SYSTEM","Maven"); c.put("LANGUAGE","Java"); c.put("USE_PAPER_MANIFEST",paper); c.put("INCLUDE_PLUGIN_LOADER",a.length>5?Boolean.parseBoolean(a[5]):paper); c.put("LOAD_AT",optional?"STARTUP":"POSTWORLD");
  if(optional){c.put("DESCRIPTION","A plugin: Simon's test");c.put("AUTHORS",new StringList(List.of("Simon","Alex: Dev","O'Brien")));c.put("WEBSITE","https://example.org/plugin");c.put("LOG_PREFIX","Probe");}
  var d=JsonParser.parseString(Files.readString(t.resolve("mccodecamp.mcdev.template.json"))).getAsJsonObject();
  for(var e:d.getAsJsonArray("files")){var f=e.getAsJsonObject(); if(f.has("condition")&&!render(c,"#if("+f.get("condition").getAsString()+")yes#end").equals("yes"))continue;
   var dest=out.resolve(render(c,f.get("destination").getAsString())); Files.createDirectories(dest.getParent()); Files.writeString(dest,render(c,Files.readString(t.resolve(f.get("template").getAsString()))));System.out.println("Rendered "+dest);
  }
 }
}
