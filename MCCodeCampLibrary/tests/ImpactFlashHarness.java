package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

import java.lang.reflect.Proxy;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import me.simoncrafter.CraftersDisplayLibrary.display.cube.*;
import org.joml.*;

/** Production control-flow tests using a CDL boundary adapter; not client-rendering tests. */
public final class ImpactFlashHarness {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static World world() { return (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class[]{World.class}, (p,m,a) -> m.getName().equals("equals") ? p == a[0] : m.getName().equals("hashCode") ? System.identityHashCode(p) : null); }
    static final class Person {
        World world; Location location; boolean online = true; final UUID id = UUID.randomUUID();
        final Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class}, (p,m,a) -> switch(m.getName()) {
            case "getUniqueId" -> id; case "getWorld" -> world; case "getLocation" -> location; case "isOnline" -> online; default -> null;
        });
        Person(World w, double x, double y, double z) { world=w; location=new Location(w,x,y,z); }
    }
    static final class Access implements ImpactFlash.Tints {
        final Map<Player,CubeColorDisplay> current = new IdentityHashMap<>();
        final Map<CubeColorDisplay,Integer> alpha = new IdentityHashMap<>();
        final Set<CubeColorDisplay> removed = Collections.newSetFromMap(new IdentityHashMap<>());
        int configured, untints; boolean failUntint;
        public CubeColorDisplay tint(Player p) { CubeColorDisplay c=CubeColorDisplay.create(p.getLocation(),new Vector3f(1),new Quaternionf(),new CubeColorInformation(Color.WHITE),false); current.put(p,c); alpha.put(c,255); return c; }
        public CubeColorDisplay current(Player p) { return current.get(p); }
        public void configure(CubeColorDisplay c, Player p) { configured++; }
        public void color(CubeColorDisplay c, Color color) { alpha.put(c,color.getAlpha()); }
        public void untint(Player p) { untints++; if(failUntint) throw new IllegalStateException("registry failure"); current.remove(p); }
        public void remove(CubeColorDisplay c) { removed.add(c); }
    }
    public static void main(String[] args) {
        World w=world(), other=world();
        Person inside=new Person(w,0,0,0), edge=new Person(w,100,0,0), diagonal=new Person(w,80,80,0), outside=new Person(w,100.01,0,0), foreign=new Person(other,0,0,0);
        Access a=new Access(); ImpactFlash f=new ImpactFlash(List.of(inside.player,edge.player,diagonal.player,outside.player,foreign.player)::iterator,a);
        f.flash(new Location(w,0,0,0)); check(a.configured==2,"inclusive spherical same-world radius");
        CubeColorDisplay cube=a.current(inside.player); check(a.alpha.get(cube)==255,"initial opaque white");
        for(int i=1;i<=4;i++) { f.tick(); check(a.alpha.get(cube)==java.lang.Math.round(255f*(5-i)/5),"linear alpha tick "+i); check(!a.removed.contains(cube),"not early"); }
        f.tick(); check(a.removed.contains(cube),"removed exactly tick 5"); check(a.alpha.get(cube)==0,"transparent endpoint");
        f.flash(new Location(w,0,0,0)); CubeColorDisplay old=a.current(inside.player); CubeColorDisplay replacement=a.tint(inside.player); f.tick(); check(a.removed.contains(old),"displaced owned cube removed"); check(a.current(inside.player)==replacement,"replacement untouched");
        edge.online=false; f.tick(); check(a.current(edge.player)==null,"quit cleanup");
        inside.online=true; f.flash(new Location(w,0,0,0)); old=a.current(inside.player); inside.world=other; f.tick(); check(a.removed.contains(old),"world change cleanup");
        inside.world=w; f.flash(new Location(w,0,0,0)); old=a.current(inside.player); a.failUntint=true; f.close(); check(a.removed.contains(old),"fallback despite registry failure"); f.close();
        int count=a.configured; f.flash(new Location(w,0,0,0)); check(a.configured==count,"closed cannot respawn");
        System.out.println("ImpactFlash boundary tests passed: radius, 5 alpha steps, expiry, replacement, quit, world-change, failed-registry cleanup, close");
    }
}
