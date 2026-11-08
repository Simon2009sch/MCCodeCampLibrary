package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

public final class SurfaceImpactHarness {
    static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static final class Terrain {
        final java.util.Map<String,org.bukkit.Material> blocks=new java.util.HashMap<>();
        int explosions,writes,queries; boolean loaded=true,primary=true;
        java.util.List<org.bukkit.entity.Entity> entities=new java.util.ArrayList<>();
        int ignitions;
        boolean outerMissing;
        final java.util.List<Float> powers=new java.util.ArrayList<>();
        final java.util.Set<String> blastColumns=new java.util.HashSet<>();
        org.bukkit.entity.Entity entity(Class<?> type,double x,double y,double z) {
            java.util.UUID id=java.util.UUID.randomUUID();
            return (org.bukkit.entity.Entity)java.lang.reflect.Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},(p,m,a)->switch(m.getName()) {
                case "getLocation" -> new org.bukkit.Location(world,x,y,z);
                case "getUniqueId" -> id; case "getFireTicks" -> 0;
                case "setFireTicks" -> { ignitions++; yield null; }
                default -> null;
            });
        }
        final org.bukkit.World world=(org.bukkit.World)java.lang.reflect.Proxy.newProxyInstance(org.bukkit.World.class.getClassLoader(),new Class[]{org.bukkit.World.class},(p,m,a)->switch(m.getName()) {
            case "getMinHeight" -> -4; case "getMaxHeight" -> 24;
            case "isChunkLoaded" -> loaded && !(outerMissing && (int)a[0]==4 && (int)a[1]==4);
            case "getBlockAt" -> block((int)a[0],(int)a[1],(int)a[2]);
            case "getNearbyEntities" -> { queries++; yield entities; }
            case "createExplosion" -> { check((boolean)a[5],"break blocks"); check(a[6]==null,"source null"); check((double)a[1]==1.0,"ground not canopy"); check(!(boolean)a[4],"no explosion fire"); powers.add((float)a[3]); if(explosions>0) check(blastColumns.add((int)Math.floor((double)a[0])+":"+(int)Math.floor((double)a[2])),"unique grid blast"); else check((double)a[0]==0 && (double)a[2]==0,"central first"); explosions++; yield true; }
            case "equals" -> p==a[0]; case "hashCode" -> System.identityHashCode(p);
            default -> null;
        });
        org.bukkit.block.Block block(int x,int y,int z) {
            String key=x+":"+y+":"+z;
            return (org.bukkit.block.Block)java.lang.reflect.Proxy.newProxyInstance(org.bukkit.block.Block.class.getClassLoader(),new Class[]{org.bukkit.block.Block.class},(p,m,a)->switch(m.getName()) {
                case "getX" -> x; case "getY" -> y; case "getZ" -> z;
                case "getType" -> blocks.getOrDefault(key,y<=0?org.bukkit.Material.STONE:y==20?org.bukkit.Material.OAK_LEAVES:org.bukkit.Material.AIR);
                case "setType" -> { check(Math.abs(x)<=90 && Math.abs(z)<=90,"backup bounds"); check((boolean)a[1],"normal physics"); blocks.put(key,(org.bukkit.Material)a[0]); writes++; yield null; }
                default -> null;
            });
        }
        SurfaceImpact job() { return new SurfaceImpact(new org.bukkit.Location(world,0,1,0),()->primary,new java.util.Random(3),m->m!=org.bukkit.Material.AIR,m->m==org.bukkit.Material.OAK_LEAVES); }
    }
    static void terrainTests() {
        Terrain t=new Terrain(); SurfaceImpact job=t.job();
        t.blocks.put("0:-1:0",org.bukkit.Material.DIRT);
        t.blocks.put("4:0:0",org.bukkit.Material.DIRT);
        t.loaded=false; job.tick(); check(t.explosions==0,"unloaded pauses");
        t.loaded=true; t.outerMissing=true; job.tick(); check(t.explosions==0 && t.writes==0,"whole blast waits for outer power30 reach chunks");
        t.outerMissing=false; job.tick(); check(t.explosions==SurfaceImpact.grid().size()+1,"whole grid plus central in one tick");
        check(t.powers.getFirst()==30f && t.powers.subList(1,t.powers.size()).stream().allMatch(p->p==6.4f),"one central30 and doubled grid power");
        for(int i=0;i<150;i++) { int old=t.explosions; job.tick(); check(t.explosions==old,"blast runs once"); }
        check(job.isIdle(),"drained grid and scars");
        long surface=t.blocks.keySet().stream().filter(k->k.split(":")[1].equals("0")).count();
        check(surface>900 && surface<1400,"denser but not blanket 60 percent scars: "+surface);
        check(t.blocks.keySet().stream().anyMatch(k->{String[] a=k.split(":");return Integer.parseInt(a[0])*Integer.parseInt(a[0])+Integer.parseInt(a[2])*Integer.parseInt(a[2])>144;}),"scar footprint beyond old radius12");
        check(t.blocks.keySet().stream().anyMatch(k->k.split(":")[1].equals("-2")),"scars reach three layers");
        check(t.blocks.keySet().stream().noneMatch(k->Integer.parseInt(k.split(":")[1]) < -2),"depth bounded to3");
        check(t.blocks.values().contains(org.bukkit.Material.MAGMA_BLOCK) && t.blocks.values().contains(org.bukkit.Material.BLACKSTONE),"mixed scar materials");
        check(t.blocks.get("0:-1:0")==org.bukkit.Material.DIRT && !t.blocks.containsKey("0:-2:0"),"scar stops at non-stone layer");
        check(t.blocks.get("4:0:0")==org.bukkit.Material.DIRT,"non-stone surface preserved");
        check(t.blocks.keySet().stream().allMatch(k->{String[] a=k.split(":");return Integer.parseInt(a[0])*Integer.parseInt(a[0])+Integer.parseInt(a[2])*Integer.parseInt(a[2])<=576;}),"scar radius24 bound");
        Terrain cancelled=new Terrain(); SurfaceImpact stopped=cancelled.job(); stopped.cancel(); stopped.tick();
        check(cancelled.writes==0 && cancelled.explosions==0,"cancel before first tick prevents every mutation");
        int queries=t.queries;
        job.enqueueBurn(new org.bukkit.Location(t.world,50.01,20,0),5.0);
        check(t.queries==queries,"late burn rejected");
        t.entities.add(t.entity(org.bukkit.entity.Player.class,30,20,0));
        t.entities.add(t.entity(org.bukkit.entity.Item.class,31,20,0));
        t.entities.add(t.entity(org.bukkit.entity.Display.class,30,20,0));
        t.entities.add(t.entity(org.bukkit.entity.Interaction.class,30,20,0));
        job.enqueueBurn(new org.bukkit.Location(t.world,30,20,0),5.0);
        check(t.ignitions==2 && job.burnedEntities()==2,"players/items burn, helpers excluded");
        check(t.queries==queries+1,"translated accepted");
        int writes=t.writes; job.tick(); check(t.writes>writes,"translated foliage removed");
        check(t.blocks.get("30:20:0")==org.bukkit.Material.AIR,"translated position not anchor");
        job.enqueueBurn(new org.bukkit.Location(t.world,0,20,0),5.0);
        job.cancel(); writes=t.writes; job.tick(); job.enqueueBurn(new org.bukkit.Location(t.world,0,20,0),5.0); job.tick();
        check(job.isIdle() && writes==t.writes,"cancel prevents late edits");
        check(job.burnedEntities()==2,"cancel retains telemetry");
        Terrain busy=new Terrain(); SurfaceImpact paced=busy.job();
        for(int i=0;i<96;i++) paced.enqueueBurn(new org.bukkit.Location(busy.world,0,20,0),5.0);
        paced.enqueueBurn(new org.bukkit.Location(busy.world,25,20,0),5.0);
        paced.tick(); check(busy.writes<=256,"128 column budget (one leaf and fire per column)");
        check(busy.blocks.get("25:20:0")==null,"distinct later columns deferred");
        paced.tick(); check(busy.blocks.get("25:20:0")==org.bukkit.Material.AIR,"duplicate columns merged, next batch runs");
        int before=busy.queries;
        paced.enqueueBurn(new org.bukkit.Location(t.world,0,20,0),5.0);
        check(busy.queries==before,"cross world rejected");
        t.primary=false;
        try { job.tick(); throw new AssertionError("thread guard missing"); } catch(IllegalStateException expected) {}
    }
    public static void main(String[] args) {
        DestructionPolicy p = new DestructionPolicy();
        check(p.permits("testworld",0,0,false), "exact testworld default");
        check(!p.permits("Testworld",0,0,true), "case sensitive");
        check(!p.permits("world",-1368,3350,true), "default disarmed");
        p.armLiveTest();
        check(p.permits("world",-1367,3350,true), "inclusive live boundary");
        check(!p.permits("world",-1367,3351,true), "circular boundary");
        check(!p.permits("world",-1368,3350,false), "explicit request mandatory");
        check(p.permitsEntityDamage("world",-1278,3350), "damage inclusive 90");
        check(!p.permitsEntityDamage("world",-1277.99,3350), "damage outside");
        check(!p.permits(null,0,0,true) && !p.permits("testworld",Double.NaN,0,false), "invalid fails closed");
        p.disarmLiveTest(); check(!p.liveArmed() && !p.permitsEntityDamage("world",-1368,3350), "disarm");
        check(!new DestructionPolicy().liveArmed(), "no persisted arming");
        java.util.Set<String> points = new java.util.HashSet<>();
        for (int[] point : SurfaceImpact.grid()) {
            check(point[0]*point[0]+point[1]*point[1]<=900,"grid circle");

            check(points.add(point[0]+":"+point[1]),"unique grid");
        }
        int oldCount=0; for(int x=-28;x<=28;x+=7) for(int z=-28;z<=28;z+=7) if(x*x+z*z<=900) oldCount++;
        double ratio=(double)points.size()/oldCount;
        check(points.contains("0:0") && ratio>2.8 && ratio<3.2,"approximately triple area density: "+ratio);
        System.out.println("Grid "+oldCount+" -> "+points.size()+", density ratio="+ratio+", plus one central power30");
        terrainTests();
        System.out.println("DestructionPolicy and SurfaceImpact boundary tests passed");
    }
}
