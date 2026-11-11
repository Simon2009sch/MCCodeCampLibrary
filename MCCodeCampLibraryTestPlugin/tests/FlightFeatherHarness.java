package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a4_flightFeather;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

/** Executes real student movement/event code; only Bukkit boundaries are doubled. */
public final class FlightFeatherHarness {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void near(double actual, double expected, String message) { check(Math.abs(actual - expected) < 1e-9, message + ": " + actual); }
    static final class Person {
        boolean grounded, swimming, inWater, flying, gliding, vehicle;
        Vector direction = new Vector(3, 4, 0), velocity;
        int writes;
        final PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(), new Class[]{PlayerInventory.class}, (p, m, a) -> null);
        final Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class}, (p, m, a) -> switch (m.getName()) {
            case "getInventory" -> inventory;
            case "getLocation" -> new org.bukkit.Location(null, 0, 80, 0) { @Override public Vector getDirection() { return direction; } };
            case "isOnGround" -> grounded;
            case "isSwimming" -> swimming;
            case "isInWater" -> inWater;
            case "isFlying" -> flying;
            case "isGliding" -> gliding;
            case "isInsideVehicle" -> vehicle;
            case "setVelocity" -> { velocity = ((Vector) a[0]).clone(); writes++; yield null; }
            default -> null;
        });
        PlayerInteractEvent click(Action action, EquipmentSlot hand) { return new PlayerInteractEvent(player, action, null, null, null, hand); }
    }
    public static void main(String[] args) {
        Person p = new Person(); AtomicInteger bursts = new AtomicInteger();
        Course feather = new Course(player -> bursts.incrementAndGet(), item -> true);
        feather.onInteract(p.click(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        near(p.velocity.getY(), .05, "one block per second converted to ticks");
        near(p.velocity.getX(), 0, "launch vertical only"); check(bursts.get() == 1, "launch creates wind");
        feather.onInteract(p.click(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.HAND));
        check(p.writes == 2 && bursts.get() == 2, "block right click also launches");
        feather.onInteract(p.click(Action.LEFT_CLICK_AIR, EquipmentSlot.HAND));
        feather.onInteract(p.click(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND));
        check(p.writes == 2, "ignore left/offhand");
        feather.onSneak(new PlayerToggleSneakEvent(p.player, true));
        near(p.velocity.length(), .05, "fixed leap magnitude"); near(p.velocity.getX(), .03, "look x"); near(p.velocity.getY(), .04, "look y");
        near(p.direction.length(), 5, "do not mutate look vector"); check(bursts.get() == 3, "leap creates wind");
        feather.onSneak(new PlayerToggleSneakEvent(p.player, false));
        p.grounded = true; feather.onSneak(new PlayerToggleSneakEvent(p.player, true)); p.grounded = false;
        p.inWater = true; feather.onSneak(new PlayerToggleSneakEvent(p.player, true)); p.inWater = false;
        p.vehicle = true; feather.onSneak(new PlayerToggleSneakEvent(p.player, true)); p.vehicle = false;
        check(p.writes == 3, "only sneak onset in air on foot");
        Course ordinary = new Course(player -> { throw new AssertionError("ordinary item wind"); }, item -> false);
        ordinary.onSneak(new PlayerToggleSneakEvent(p.player, true)); check(p.writes == 3, "ordinary items do not leap");
        feather.close(); feather.onSneak(new PlayerToggleSneakEvent(p.player, true)); feather.onInteract(p.click(Action.RIGHT_CLICK_AIR, EquipmentSlot.HAND));
        check(p.writes == 3 && bursts.get() == 3, "deactivated handler inert");
        System.out.println("Flight feather movement checks passed: launch/leap at 1 block/second, main-hand actions, airborne gating, wind callbacks, cleanup");
    }
}
