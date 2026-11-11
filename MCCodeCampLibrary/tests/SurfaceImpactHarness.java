package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

public final class SurfaceImpactHarness {
    static void check(boolean b, String message) { if (!b) throw new AssertionError(message); }
    static final class Terrain {
        final java.util.Map<String,org.bukkit.Material> blocks=new java.util.HashMap<>();
        int explosions,writes,queries,reads; boolean loaded=true,primary=true;
        java.util.List<org.bukkit.entity.Entity> entities=new java.util.ArrayList<>();
        int ignitions;
        boolean outerMissing;
        int minHeight=-4,maxHeight=24;
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
            case "getMinHeight" -> minHeight; case "getMaxHeight" -> maxHeight;
            case "isChunkLoaded" -> loaded && !(outerMissing && (int)a[0]==4 && (int)a[1]==4);
            case "getBlockAt" -> { reads++; check(loaded,"never access unloaded terrain"); yield block((int)a[0],(int)a[1],(int)a[2]); }
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
                case "setType" -> { check(Math.abs(x)<=90 && Math.abs(z)<=90,"backup bounds"); check(!(boolean)a[1],"bulk edits disable physics"); blocks.put(key,(org.bukkit.Material)a[0]); writes++; yield null; }
                default -> null;
            });
        }
        // Material hardness is a live Paper registry lookup in 1.21.11; double only that API boundary.
        SurfaceImpact job() { return new SurfaceImpact(new org.bukkit.Location(world,0,1,0),()->primary,new java.util.Random(3),m->m!=org.bukkit.Material.AIR,m->m==org.bukkit.Material.OAK_LEAVES,m->m!=org.bukkit.Material.BEDROCK && m!=org.bukkit.Material.BARRIER); }
    }
    static void terrainTests() {
        Terrain t=new Terrain(); SurfaceImpact job=t.job();
        t.blocks.put("0:-1:0",org.bukkit.Material.BEDROCK);
        t.blocks.put("4:0:0",org.bukkit.Material.BARRIER);
        t.loaded=false; job.tick(); check(t.explosions==0,"unloaded pauses");
        t.loaded=true; job.tick(); check(t.explosions==0,"no native terrain explosion batch");
        check(!job.isIdle(),"crater spread across ticks");
        int ticks=1;
        while(!job.isIdle() && ticks++<1000) { int old=t.reads; job.tick(); check(t.reads-old<=4096,"bounded scanned cells per tick"); }
        check(job.isIdle(),"drained grid and scars");
        check(t.blocks.get("0:0:0")==org.bukkit.Material.AIR,"solid crater core removed");
        check(t.blocks.get("0:-1:0")==org.bukkit.Material.BEDROCK && t.blocks.get("4:0:0")==org.bukkit.Material.BARRIER,"unbreakable blocks preserved");
        check(!t.blocks.containsKey("35:0:0"),"outside noisy sphere untouched");
        check(job.changedBlocks()==t.writes && job.explosions()==0,"compatible direct-write telemetry");
        System.out.println("Tick-budgeted crater: ticks="+ticks+", writes="+t.writes+", native explosions="+t.explosions);
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
        for(int i=0;i<20 && t.blocks.get("30:20:0")==null;i++) job.tick();
        check(t.blocks.get("30:20:0")==org.bukkit.Material.AIR,"translated position not anchor");
        job.enqueueBurn(new org.bukkit.Location(t.world,0,20,0),5.0);
        job.cancel(); writes=t.writes; job.tick(); job.enqueueBurn(new org.bukkit.Location(t.world,0,20,0),5.0); job.tick();
        check(job.isIdle() && writes==t.writes,"cancel prevents late edits");
        check(job.burnedEntities()==2,"cancel retains telemetry");
        Terrain busy=new Terrain(); SurfaceImpact paced=busy.job();
        for(int i=0;i<96;i++) paced.enqueueBurn(new org.bukkit.Location(busy.world,0,20,0),5.0);
        paced.enqueueBurn(new org.bukkit.Location(busy.world,25,20,0),5.0);
        paced.tick(); check(busy.reads<=4096,"shared scan budget");
        check(busy.blocks.get("25:20:0")==null,"distinct later columns deferred");
        for(int i=0;i<30 && busy.blocks.get("25:20:0")==null;i++) paced.tick();
        check(busy.blocks.get("25:20:0")==org.bukkit.Material.AIR,"duplicate columns merged, next batch runs");
        int before=busy.queries;
        paced.enqueueBurn(new org.bukkit.Location(t.world,0,20,0),5.0);
        check(busy.queries==before,"cross world rejected");
        t.primary=false;
        try { job.tick(); throw new AssertionError("thread guard missing"); } catch(IllegalStateException expected) {}
        Terrain deep=new Terrain(); deep.minHeight=-64; deep.maxHeight=320;
        SurfaceImpact sphere=deep.job();
        sphere.tick(); int beforePause=deep.reads, beforeWrites=deep.writes;
        deep.loaded=false; sphere.tick();
        check(deep.reads==beforePause && deep.writes==beforeWrites,"mid-crater unload pauses without access");
        deep.loaded=true;
        int tallTicks=0;
        while(!sphere.isIdle() && tallTicks++<1000) {
            int old=deep.reads; sphere.tick();
            check(deep.reads-old<=4096,"tall ground scans share hard read budget");
        }
        check(sphere.isIdle(),"deep sphere and resumable scars drain");
        int removedShell=0, retainedShell=0;
        for(int x=-34;x<=34;x++) for(int y=-34;y<=0;y++) for(int z=-34;z<=34;z++) {
            int distance=x*x+(y-1)*(y-1)+z*z;
            org.bukkit.Material m=deep.blocks.get(x+":"+y+":"+z);
            if(distance<29*29) check(m==org.bukkit.Material.AIR,"spherical core cleared");
            if(m==org.bukkit.Material.AIR) check(distance<=34*34,"removal stays inside radius34 sphere");
            if(distance>31*31 && distance<33*33) { if(m==org.bukkit.Material.AIR) removedShell++; else retainedShell++; }
        }
        check(removedShell>0 && retainedShell>0,"noisy shell is neither blanket nor empty");
        check(deep.blocks.values().contains(org.bukkit.Material.MAGMA_BLOCK) && deep.blocks.values().contains(org.bukkit.Material.BLACKSTONE),"post-crater mixed scars retained");
        Terrain partial=new Terrain(); SurfaceImpact stop=partial.job();
        for(int i=0;i<40;i++) stop.tick();
        check(partial.writes>0,"cancel during active crater");
        stop.cancel(); int saved=partial.writes; stop.tick();
        check(stop.isIdle() && partial.writes==saved,"mid-crater cancel prevents later writes");
        System.out.println("Tall-world sphere/scars: ticks="+tallTicks+", noisy shell removed="+removedShell+", retained="+retainedShell);
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

        terrainTests();
        System.out.println("DestructionPolicy and SurfaceImpact boundary tests passed");
    }
}
