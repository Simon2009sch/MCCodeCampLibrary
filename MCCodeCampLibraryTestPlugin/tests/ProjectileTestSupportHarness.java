package me.simoncrafter.mCCodeCampLibraryTestPlugin.testing;

import me.simoncrafter.mCCodeCampLibrary.input.activation.ProjectileTarget;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.ProjectileTargetHitEvent;
import me.simoncrafter.mCCodeCampLibrary.input.activation.event.playerBlockActivation.ButtonIDEvent;
import me.simoncrafter.mCCodeCampLibrary.projectile.*;
import me.simoncrafter.mCCodeCampLibrary.input.item.ACustomItemHandler;
import me.simoncrafter.mCCodeCampLibrary.input.item.ItemsManager;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** API-boundary checks: fixture routing, harmless presets, formatted debug and cleanup. */
public class ProjectileTestSupportHarness {
    static String plain(Component component) {
        StringBuilder text = new StringBuilder(component instanceof net.kyori.adventure.text.TextComponent literal ? literal.content() : "");
        for (Component child : component.children()) text.append(plain(child));
        return text.toString();
    }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,InvocationHandler action) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            if(m.getName().equals("hashCode")) return System.identityHashCode(p);
            if(m.getName().equals("equals")) return p==a[0];
            return action.invoke(p,m,a);
        });
    }
    public static void main(String[] args) {
        var messages = new ArrayList<Component>();
        var logger=Logger.getLogger("projectile-fixture-test");
        Plugin plugin=proxy(Plugin.class,(p,m,a)->switch(m.getName()) {case "getName", "namespace"->"fixture-test"; case "getLogger"->logger; default->null;});
        PluginManager pm=proxy(PluginManager.class,(p,m,a)->{
            if(m.getName().equals("registerEvents") && a[0] instanceof ProjectileTestSupport) ButtonIDEvent.getHandlerList().register(new RegisteredListener((org.bukkit.event.Listener)a[0],(l,e)->{},EventPriority.NORMAL,plugin,false));
            return null;
        });
        Server server=proxy(Server.class,(p,m,a)->switch(m.getName()) {
            case "getPluginManager"->pm; case "isPrimaryThread"->true; case "getLogger"->logger;
            case "getName","getVersion","getBukkitVersion"->"test"; default->null;
        });
        try { Bukkit.setServer(server); } catch(NoSuchElementException expected) {check(Bukkit.getServer()==server,"API setup");}
        UUID playerID=UUID.randomUUID();
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()) {
            case "getUniqueId"->playerID; case "getName"->"Tester";
            case "sendMessage"->{ if(a[0] instanceof Component c) messages.add(c); yield null; }
            default->null;
        });
        int[] gives={0};
        List<ACustomItemHandler> registered = new ArrayList<>();
        ItemsManager items = new ItemsManager(plugin) {
            @Override public void registerCustomItem(ACustomItemHandler handler) { super.registerCustomItem(handler); registered.add(handler); }
        };
        ProjectileTestSupport support=new ProjectileTestSupport(plugin, items) { @Override protected void giveTestItems(Player p) {gives[0]++;} };
        check(registered.size()==5 && registered.stream().filter(handler -> handler instanceof ProjectileTestWand).count()==4,"four wands and restore item use the existing custom item registry");
        check(registered.stream().map(ACustomItemHandler::getItemID).distinct().count()==5,"each test wand has its own custom item ID");
        support.onButton(new ButtonIDEvent(player,"other",null));
        support.onButton(new ButtonIDEvent(player,ProjectileTestSupport.ITEM_BUTTON_ID,null));
        check(gives[0]==1,"only fixture button gives items");
        World world=proxy(World.class,(p,m,a)->m.getName().equals("getName")?"world":null);
        ProjectileTarget target=new ProjectileTarget() { @Override public String getID() {return ProjectileTestSupport.TARGET_ID;} };
        var hit=new ProjectileTargetHitEvent(target,new SnowballProjectile(),null,player,new Location(world,1.25,2.5,-3.75),null);
        support.onTarget(hit);
        check(messages.size()==1,"target debug sent to shooter");
        String text=plain(messages.getFirst());
        check(text.contains("Target: projectile_test_target") && text.contains("SnowballProjectile") && text.contains("Shooter: Tester"),"identification formatted");
        check(text.contains("1.250, 2.500, -3.750") && text.contains("unknown") && text.contains("Age:") && text.contains("Velocity:"),"hit details and missing face formatted");
        for(var mode:ProjectileMovement.values()) {
            CustomProjectile projectile=ProjectileTestSupport.createProjectile(mode);
            check(projectile.getMovement()==mode,"all four movement items map to matching presets");
            if(projectile instanceof DamageProjectile damaging) check(damaging.getDamage()==0,"fixture is harmless");
        }
        support.close(); support.close();
        check(ButtonIDEvent.getHandlerList().getRegisteredListeners().length==0,"fixture listener released");
        support.onButton(new ButtonIDEvent(player,ProjectileTestSupport.ITEM_BUTTON_ID,null)); support.onTarget(hit);
        check(gives[0]==1 && messages.size()==1,"closed fixture is inert");
        System.out.println("Projectile test fixture boundary checks passed");
    }
}
