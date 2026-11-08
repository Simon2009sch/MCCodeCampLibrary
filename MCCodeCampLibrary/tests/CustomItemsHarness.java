import me.simoncrafter.mCCodeCampLibrary.input.item.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.*;
import org.spongepowered.configurate.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/** Executes actual production methods with doubles only at the Bukkit boundary. */
public class CustomItemsHarness {
    static int checks;
    static void check(boolean value, String name) { checks++; if (!value) throw new AssertionError(name); }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> c, InvocationHandler h) {
        return (T) Proxy.newProxyInstance(c.getClassLoader(), new Class[]{c}, (p,m,a)->switch(m.getName()) {
            case "hashCode" -> System.identityHashCode(p); case "equals" -> p==a[0];
            case "toString" -> c.getSimpleName(); default -> h.invoke(p,m,a);
        });
    }
    static class Stack extends ItemStack {
        Object data;
        final PersistentDataContainer pdc = proxy(PersistentDataContainer.class,(p,m,a)->switch(m.getName()) {
            case "has" -> data!=null && (a.length==1 || data instanceof String);
            case "get" -> data; case "set" -> {data=a[2]; yield null;} default -> null;
        });
        @Override public io.papermc.paper.persistence.PersistentDataContainerView getPersistentDataContainer(){return pdc;}
        @Override public boolean editPersistentDataContainer(java.util.function.Consumer<PersistentDataContainer> action){action.accept(pdc);return true;}
        @Override public Material getType(){return Material.STICK;}
        @Override public Stack clone(){Stack copy=new Stack();copy.data=data;return copy;}
    }
    static class Handler extends ACustomItemHandler {
        int clicks, entities, attacks; boolean vanilla;
        public void onEntityAttack(io.papermc.paper.event.player.PrePlayerAttackEntityEvent e){attacks++;if(vanilla)e.setCancelled(false);}
        public void onEntityInteract(PlayerInteractEntityEvent e){entities++;if(vanilla)e.setCancelled(false);}
        public String getItemID(){return "wand";}
        protected ItemStack getDefaultItemStack(){return new Stack();}
        protected ConfigurationNode getDefaultItemData(){return BasicConfigurationNode.root();}
        public void onInteract(PlayerInteractEvent e){clicks++; if(vanilla)e.setCancelled(false);}
    }
    static void call(ItemsManager manager,String name,Class<?> type,Object event)throws Exception {
        try { ItemsManager.class.getMethod(name,type).invoke(manager,event); }
        catch(NoSuchMethodException ex){throw new AssertionError("Missing routing/lifecycle API: "+name,ex);}
    }
    public static void main(String[] args)throws Exception {
        Logger log=Logger.getLogger("items-test");
        PluginManager pm=proxy(PluginManager.class,(p,m,a)->null);
        int[] tick={12};
        Server server=proxy(Server.class,(p,m,a)->switch(m.getName()) {
            case "getPluginManager" -> pm; case "getLogger" -> log; case "isPrimaryThread" -> true;
            case "getCurrentTick" -> tick[0]; case "getName","getVersion","getBukkitVersion" -> "test"; default -> null;
        });
        try{Bukkit.setServer(server);}catch(NoSuchElementException e){check(Bukkit.getServer()==server,"server installed");}
        Plugin plugin=proxy(Plugin.class,(p,m,a)->m.getName().equals("getLogger")?log:null);
        ItemsManager manager=new ItemsManager(plugin);
        Handler handler=new Handler();manager.registerCustomItem(handler);
        ItemStack wand=handler.createNewItem();
        PlayerInventory inventory=proxy(PlayerInventory.class,(p,m,a)->switch(m.getName()) {
            case "getItemInMainHand","getItemInOffHand","getItem" -> wand; default -> null;
        });
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()) {
            case "getInventory" -> inventory; case "getUniqueId" -> new UUID(0,1); default -> null;
        });
        Block block=proxy(Block.class,(p,m,a)->null);
        PlayerInteractEvent click=new PlayerInteractEvent(player,Action.LEFT_CLICK_BLOCK,wand,block,org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND);
        manager.onPlayerInteract(click);
        check(handler.clicks==1 && click.isCancelled(),"legacy block click once and cancelled");
        String mode=args.length==0?"unknown":args[0];
        if(mode.equals("unknown")) check(manager.createNewItem("missing")==null,"unknown id returns null");
        if(mode.equals("block")) {
            check(ACustomItemHandler.class.getMethod("onBlockInteract",PlayerInteractEvent.class)!=null,"block hook exists");
            Handler specialized=new Handler(){public void onBlockInteract(PlayerInteractEvent e){clicks+=10;}};
            ItemsManager specific=new ItemsManager(plugin);specific.registerCustomItem(specialized);specific.onPlayerInteract(click);
            check(specialized.clicks==10,"specialized block hook not legacy callback");
            handler.vanilla=true;manager.onPlayerInteract(click);check(!click.isCancelled(),"callback can restore vanilla");
        }
        if(mode.equals("entity")) {
            Entity entity=proxy(Entity.class,(p,m,a)->m.getName().equals("getUniqueId")?new UUID(0,2):null);
            PlayerInteractEntityEvent event=new PlayerInteractEntityEvent(player,entity,EquipmentSlot.OFF_HAND);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,event);
            check(event.isCancelled() && handler.entities==1,"entity action cancelled and dispatched");
            PlayerInteractAtEntityEvent precise=new PlayerInteractAtEntityEvent(player,entity,new org.bukkit.util.Vector(),EquipmentSlot.OFF_HAND);
            call(manager,"onPlayerInteractAtEntity",PlayerInteractAtEntityEvent.class,precise);
            check(precise.isCancelled() && handler.entities==1,"generic + precise packet pair once");
            call(manager,"onPlayerInteractAtEntity",PlayerInteractAtEntityEvent.class,precise);
            check(handler.entities==2,"precise-only event dispatched");
            PlayerInteractEntityEvent second=new PlayerInteractEntityEvent(player,entity,EquipmentSlot.OFF_HAND);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,second);
            check(second.isCancelled() && handler.entities==2,"precise + generic packet pair once");
            handler.vanilla=true;
            call(manager,"onPlayerInteractAtEntity",PlayerInteractAtEntityEvent.class,precise);
            PlayerInteractEntityEvent vanilla=new PlayerInteractEntityEvent(player,entity,EquipmentSlot.OFF_HAND);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,vanilla);
            check(!precise.isCancelled() && !vanilla.isCancelled() && handler.entities==3,"paired cancellation follows callback opt-in");
        }
        if(mode.equals("hands")) {
            ItemStack[] hands={new Stack(),wand};
            PlayerInventory inv=proxy(PlayerInventory.class,(p,m,a)->switch(m.getName()) {
                case "getItemInMainHand" -> hands[0]; case "getItemInOffHand" -> hands[1];
                case "getItem" -> hands[a[0]==EquipmentSlot.HAND?0:1]; default -> null;
            });
            Player who=proxy(Player.class,(p,m,a)->switch(m.getName()) {
                case "getInventory" -> inv; case "getUniqueId" -> new UUID(0,4); default -> null;
            });
            Entity entity=proxy(Entity.class,(p,m,a)->m.getName().equals("getUniqueId")?new UUID(0,5):null);
            PlayerInteractEntityEvent off=new PlayerInteractEntityEvent(who,entity,EquipmentSlot.OFF_HAND);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,off);
            check(off.isCancelled() && handler.entities==1,"entity uses offhand event slot");
            PlayerInteractEntityEvent main=new PlayerInteractEntityEvent(who,entity,EquipmentSlot.HAND);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,main);
            check(!main.isCancelled() && handler.entities==1,"ordinary main hand unchanged");
            var attack=new io.papermc.paper.event.player.PrePlayerAttackEntityEvent(who,entity,true);
            call(manager,"onPlayerAttackEntity",io.papermc.paper.event.player.PrePlayerAttackEntityEvent.class,attack);
            check(!attack.isCancelled() && handler.attacks==0,"attack never uses offhand custom item");
            BlockBreakEvent broken=new BlockBreakEvent(block,who);call(manager,"onBlockBreak",BlockBreakEvent.class,broken);
            check(!broken.isCancelled(),"ordinary main-hand tool still breaks blocks");
            hands[0]=wand;hands[1]=new Stack();
            main=new PlayerInteractEntityEvent(who,entity,EquipmentSlot.HAND);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,main);
            check(main.isCancelled() && handler.entities==2,"main hand routed separately");
            PlayerInteractEvent air=new PlayerInteractEvent(who,Action.RIGHT_CLICK_AIR,wand,null,org.bukkit.block.BlockFace.SELF,EquipmentSlot.OFF_HAND);
            manager.onPlayerInteract(air);check(handler.clicks==2,"air uses event stack rather than inventory slot");
            PlayerInteractEvent physical=new PlayerInteractEvent(who,Action.PHYSICAL,wand,block,org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND);
            manager.onPlayerInteract(physical);check(handler.clicks==2 && !physical.isCancelled(),"physical is not a tool click");
        }
        if(mode.equals("metadata")) {
            var original=ItemsManager.readItemConfig(wand);original.node("futureMetadata").set("keep");
            ((Stack)wand).data=ItemsManager.serializeJson(original);
            var updated=BasicConfigurationNode.root();updated.node("power").set(17);
            var setter=ACustomItemHandler.class.getDeclaredMethod("setItemData",ItemStack.class,ConfigurationNode.class);
            setter.setAccessible(true);setter.invoke(handler,wand,updated);
            var saved=ItemsManager.readItemConfig(wand);
            check(saved.node("id").getString("").equals("wand"),"data edit preserves registered id");
            check(saved.node("dataVersion").getInt()==1,"data edit preserves version");
            check(saved.node("futureMetadata").getString("").equals("keep"),"data edit preserves future metadata");
            check(ACustomItemHandler.getItemData(wand).node("power").getInt()==17,"data edit persists payload");
        }
        if(mode.equals("malformed")) {
            for(Object data:Arrays.asList("{broken", "[]", "42", "null", "{\"id\":[]}", "{\"id\":{}}", 42)) {
                Stack bad=new Stack();bad.data=data;
                PlayerInteractEvent event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_AIR,bad,null,org.bukkit.block.BlockFace.SELF,EquipmentSlot.HAND);
                int before=handler.clicks;manager.onPlayerInteract(event);
                check(handler.clicks==before && event.useItemInHand()!=Event.Result.DENY,"malformed/foreign data unchanged: "+data);
            }
            Handler numeric=new Handler(){public String getItemID(){return "42";}};manager.registerCustomItem(numeric);
            Stack bad=new Stack();bad.data="{\"id\":42}";
            PlayerInteractEvent event=new PlayerInteractEvent(player,Action.RIGHT_CLICK_AIR,bad,null,org.bukkit.block.BlockFace.SELF,EquipmentSlot.HAND);
            manager.onPlayerInteract(event);check(numeric.clicks==0,"numeric JSON ID is not a registered string ID");
        }
        if(mode.equals("attack")) {
            Entity entity=proxy(Entity.class,(p,m,a)->null);
            var attack=new io.papermc.paper.event.player.PrePlayerAttackEntityEvent(player,entity,false);
            call(manager,"onPlayerAttackEntity",io.papermc.paper.event.player.PrePlayerAttackEntityEvent.class,attack);
            check(handler.attacks==1 && attack.isCancelled(),"unattackable entity still reaches callback");
            var normal=new io.papermc.paper.event.player.PrePlayerAttackEntityEvent(player,entity,true);
            call(manager,"onPlayerAttackEntity",io.papermc.paper.event.player.PrePlayerAttackEntityEvent.class,normal);
            check(handler.attacks==2 && normal.isCancelled(),"normal attack cancelled");
            handler.vanilla=true;
            call(manager,"onPlayerAttackEntity",io.papermc.paper.event.player.PrePlayerAttackEntityEvent.class,normal);
            check(handler.attacks==3 && !normal.isCancelled(),"attack hook can restore vanilla");
        }
        if(mode.equals("break")) {
            BlockBreakEvent event=new BlockBreakEvent(block,player);
            call(manager,"onBlockBreak",BlockBreakEvent.class,event);
            check(event.isCancelled() && handler.clicks==1,"break prevented without repeated click");
        }
        if(mode.equals("reentrant")) {
            Handler closing=new Handler(){public void onEntityInteract(PlayerInteractEntityEvent e){
                try {ItemsManager.class.getMethod("close").invoke(manager);}catch(Exception ex){throw new RuntimeException(ex);}
            }};
            call(manager,"unregisterCustomItem",ACustomItemHandler.class,handler);manager.registerCustomItem(closing);
            Entity entity=proxy(Entity.class,(p,m,a)->m.getName().equals("getUniqueId")?new UUID(0,9):null);
            call(manager,"onPlayerInteractEntity",PlayerInteractEntityEvent.class,new PlayerInteractEntityEvent(player,entity));
            var cache=ItemsManager.class.getDeclaredField("entityClicks");cache.setAccessible(true);
            check(((Map<?,?>)cache.get(manager)).isEmpty(),"callback close cannot repopulate owned handler references");
        }
        if(mode.equals("lifecycle")) {
            Handler replacement=new Handler();
            call(manager,"unregisterCustomItem",ACustomItemHandler.class,replacement);
            check(manager.createNewItem("wand")!=null,"identity mismatch cannot unregister");
            call(manager,"unregisterCustomItem",ACustomItemHandler.class,handler);
            check(manager.createNewItem("wand")==null,"identity unregister removes owner");
            manager.registerCustomItem(replacement);
            call(manager,"unregisterCustomItem",ACustomItemHandler.class,handler);
            check(manager.createNewItem("wand")!=null,"stale owner cannot remove replacement");
            call(manager,"unregisterCustomItem",String.class,"wand");
            check(manager.createNewItem("wand")==null,"id unregister");
            manager.registerCustomItem(handler);
            org.bukkit.event.HandlerList list=PlayerInteractEvent.getHandlerList();
            list.register(new RegisteredListener(manager,(l,e)->{},EventPriority.NORMAL,plugin,false));
            check(Arrays.stream(list.getRegisteredListeners()).anyMatch(l->l.getListener()==manager),"listener registered at API boundary");
            ItemsManager.class.getMethod("close").invoke(manager);ItemsManager.class.getMethod("close").invoke(manager);
            check(manager instanceof AutoCloseable,"AutoCloseable lifecycle");
            check(Arrays.stream(list.getRegisteredListeners()).noneMatch(l->l.getListener()==manager),"close unregisters owned listeners");
            check(manager.createNewItem("wand")==null,"close clears handlers");
            int before=handler.clicks;click.setCancelled(false);manager.onPlayerInteract(click);
            check(handler.clicks==before && !click.isCancelled(),"closed manager inert");
            boolean rejected=false;try{manager.registerCustomItem(handler);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"closed manager cannot reactivate");
        }
        System.out.println("PASS custom items "+mode+": "+checks+" checks");
    }
}
