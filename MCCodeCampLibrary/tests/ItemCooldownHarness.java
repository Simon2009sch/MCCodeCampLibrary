package me.simoncrafter.mCCodeCampLibrary.utility;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.atomic.AtomicLong;

public class ItemCooldownHarness {
    public static void main(String[] args) {
        AtomicLong tick = new AtomicLong(100);
        NamespacedKey key = new NamespacedKey("test", "cooldown");
        ItemCooldown cooldown = new ItemCooldown(key, tick::get, "session-one");
        ItemStack first = new ItemStack();
        require(cooldown.tryUse(first, 5), "first use");
        require(!cooldown.tryUse(first, 5), "same tick blocked");
        tick.set(104);
        require(!cooldown.tryUse(first, 5), "before expiry blocked");
        tick.set(105);
        require(cooldown.tryUse(first, 5), "exact expiry allowed");
        require(cooldown.tryUse(new ItemStack(), 5), "items independent");
        tick.set(106);
        require(new ItemCooldown(key, tick::get, "session-two").tryUse(first, 5),
                "restart resets even when new tick is greater than old tick");
        System.out.println("ItemCooldown session/independence passed");
        boolean rejected = false;
        try { cooldown.tryUse(first, -1); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "negative duration rejected");
        require(cooldown.tryUse(first, 0) && cooldown.tryUse(first, 0), "zero always allowed");
        first.editPersistentDataContainer(data -> data.set(key, org.bukkit.persistence.PersistentDataType.STRING, "session-one:broken"));
        require(cooldown.tryUse(first, 5), "malformed data resets");
        tick.set(1);
        require(cooldown.tryUse(first, 5), "clock regression resets stale state");
        tick.set(2);
        require(!cooldown.tryUse(first, 5), "no wall-clock progress while clock stands still");
        System.out.println("ItemCooldown validation/recovery passed");
        ItemCooldown.resetSession();
        ItemCooldown central = new ItemCooldown(key);
        ItemStack centralItem = new ItemStack();
        require(central.tryUse(centralItem, 2), "central first use");
        ItemCooldown.tick();
        require(!central.tryUse(centralItem, 2), "central before expiry");
        ItemCooldown.tick();
        require(central.tryUse(centralItem, 2), "central exact expiry");
        ItemCooldown.resetSession();
        for (int i = 0; i < 3; i++) ItemCooldown.tick();
        require(central.tryUse(centralItem, 2), "existing helper sees fresh session after reset");
        System.out.println("ItemCooldown central lifecycle passed");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
