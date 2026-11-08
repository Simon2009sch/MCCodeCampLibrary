"""Production helper behavior tests using minimal Bukkit persistence boundary doubles."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
source = root / 'src/main/java/me/simoncrafter/mCCodeCampLibrary/utility/ItemCooldown.java'
stubs = {
'org/bukkit/NamespacedKey.java': 'package org.bukkit; public record NamespacedKey(String namespace, String key) { public NamespacedKey(org.bukkit.plugin.Plugin p,String key){this("plugin",key);} }',
'org/bukkit/plugin/Plugin.java': 'package org.bukkit.plugin; public interface Plugin {}',
'org/bukkit/Bukkit.java': 'package org.bukkit; public class Bukkit { public static int tick; public static int getCurrentTick(){return tick;} }',
'org/bukkit/persistence/PersistentDataType.java': 'package org.bukkit.persistence; public class PersistentDataType<P,C> { public static final PersistentDataType<String,String> STRING = new PersistentDataType<>(); }',
'org/bukkit/persistence/PersistentDataContainer.java': '''package org.bukkit.persistence; import java.util.*; import org.bukkit.NamespacedKey; @SuppressWarnings("unchecked") public class PersistentDataContainer { private final Map<NamespacedKey,Object> data=new HashMap<>(); public <P,C> C get(NamespacedKey k,PersistentDataType<P,C> t){return (C)data.get(k);} public <P,C> void set(NamespacedKey k,PersistentDataType<P,C> t,C v){data.put(k,v);} }''',
'org/bukkit/inventory/ItemStack.java': '''package org.bukkit.inventory; import org.bukkit.persistence.*; import java.util.function.Consumer; public class ItemStack { private final PersistentDataContainer data=new PersistentDataContainer(); public PersistentDataContainer getPersistentDataContainer(){return data;} public void editPersistentDataContainer(Consumer<PersistentDataContainer> edit){edit.accept(data);} }''',
}
with tempfile.TemporaryDirectory(prefix='hermes-verify-item-cooldown-', dir='C:/Users/simon/AppData/Local/hermes/cache/scratch') as temp:
    out = Path(temp)
    files = []
    for name, text in stubs.items():
        path = out / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)
        files.append(str(path))
    subprocess.run(['javac', '--release', '22', '-d', temp, *files, str(source), str(root / 'tests/ItemCooldownHarness.java')], check=True)
    subprocess.run(['java', '-ea', '-cp', temp, 'me.simoncrafter.mCCodeCampLibrary.utility.ItemCooldownHarness'], check=True)
