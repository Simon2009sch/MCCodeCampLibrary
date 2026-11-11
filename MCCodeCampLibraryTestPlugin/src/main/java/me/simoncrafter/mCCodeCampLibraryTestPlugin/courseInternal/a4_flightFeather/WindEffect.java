package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a4_flightFeather;

import org.bukkit.Color;

/** A server-thread-owned, short-lived display tornado. */
public final class WindEffect implements AutoCloseable {
    static final class Particle {
        double x, y, z, vx, vy, vz;
        int age;
        Particle(double angle, double depth, double spread) {
            double radius = (1.0 - depth) * 0.9 + 0.12 + spread * 0.2;
            x = Math.cos(angle) * radius;
            z = Math.sin(angle) * radius;
            y = -0.12 - depth * 1.7;
            vx = -Math.sin(angle) * 0.14 + Math.cos(angle) * 0.055;
            vz = Math.cos(angle) * 0.14 + Math.sin(angle) * 0.055;
        }
        double radius() { return Math.hypot(x, z); }
        void step() {
            double r = radius();
            double pull = 0.012 / ((1 + r * r) * (1 + age * 0.12));
            vx -= x * pull;
            vz -= z * pull;
            vy -= 0.009;
            x += vx; y += vy; z += vz;
            age++;
        }
        float size() { return (float) (0.16 / (1 + radius() * 0.45)); }
        Color color() {
            double fade = Math.max(0, 1 - age / 30.0) / (1 + radius() * 0.3);
            int red = (int) (230 - Math.min(100, radius() * 32));
            return Color.fromARGB((int) (210 * fade), red, 225, 255);
        }
    }
    interface Owner {
        org.bukkit.Location location();
        boolean active(org.bukkit.World world);
    }
    interface Sprite {
        boolean valid();
        void update(org.bukkit.Location location, Particle particle);
        void remove();
    }
    private record Mote(Particle particle, Sprite sprite) { }
    private static final class Burst {
        final Owner owner;
        final org.bukkit.Location origin;
        final java.util.List<Mote> motes = new java.util.ArrayList<>();
        int age;
        Burst(Owner owner) { this.owner = owner; origin = owner.location().clone(); }
        void remove() { motes.forEach(m -> m.sprite.remove()); motes.clear(); }
    }
    private final java.util.Deque<Burst> bursts = new java.util.ArrayDeque<>();
    private final java.util.function.BiFunction<org.bukkit.Location, Particle, Sprite> factory;
    private final java.util.function.Predicate<org.bukkit.Location> loaded;
    private boolean closed;

    public WindEffect() {
        this(WindEffect::createSprite, location -> {
            org.bukkit.World world = location.getWorld();
            return world != null && org.bukkit.Bukkit.getWorld(world.getUID()) == world
                    && world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        });
    }
    WindEffect(java.util.function.BiFunction<org.bukkit.Location, Particle, Sprite> factory,
               java.util.function.Predicate<org.bukkit.Location> loaded) {
        this.factory = factory; this.loaded = loaded;
    }
    /** Call on the server thread. No task or listener is registered by this object. */
    public void burst(org.bukkit.entity.Player player) {
        java.util.Objects.requireNonNull(player, "player");
        burst(new Owner() {
            public org.bukkit.Location location() { return player.getLocation(); }
            public boolean active(org.bukkit.World world) { return player.isOnline() && player.getWorld() == world; }
        });
    }
    void burst(Owner owner) {
        if (closed) return;
        Burst burst = new Burst(owner);
        if (!owner.active(burst.origin.getWorld()) || !loaded.test(burst.origin)) return;
        if (bursts.size() == 4) bursts.removeFirst().remove();
        try {
            for (int i = 0; i < 28; i++) {
                Particle p = new Particle(i * Math.PI * 2 / 7 + Math.random() * 0.3,
                        (i + Math.random()) / 28.0, Math.random());
                org.bukkit.Location location = burst.origin.clone().add(p.x, p.y, p.z);
                if (loaded.test(location)) burst.motes.add(new Mote(p, factory.apply(location, p)));
            }
            bursts.addLast(burst);
        } catch (RuntimeException failure) {
            burst.remove();
            throw failure;
        }
    }
    /** Advance once per course tick, on the server thread. */
    public void tick() {
        var iterator = bursts.iterator();
        while (iterator.hasNext()) {
            Burst burst = iterator.next();
            if (++burst.age >= 30 || !burst.owner.active(burst.origin.getWorld()) || !loaded.test(burst.origin)) {
                burst.remove(); iterator.remove(); continue;
            }
            var motes = burst.motes.iterator();
            while (motes.hasNext()) {
                Mote mote = motes.next();
                mote.particle.step();
                org.bukkit.Location next = burst.origin.clone().add(mote.particle.x, mote.particle.y, mote.particle.z);
                if (!mote.sprite.valid() || !loaded.test(next)) {
                    mote.sprite.remove(); motes.remove();
                } else mote.sprite.update(next, mote.particle);
            }
            if (burst.motes.isEmpty()) iterator.remove();
        }
    }
    private static Sprite createSprite(org.bukkit.Location location, Particle p) {
        var display = me.simoncrafter.CraftersDisplayLibrary.display.panel.ColorDisplay.create(
                location, new org.joml.Vector3f(p.size()), new org.joml.Vector3f(),
                new org.joml.Quaternionf(), new org.joml.Quaternionf(), p.color(), false,
                org.bukkit.entity.Display.Billboard.CENTER);
        try {
            var entity = display.spawnDisplay();
            entity.setPersistent(false);
            entity.setGravity(false);
            display.setTeleportDuration(1);
        } catch (RuntimeException failure) {
            display.remove();
            throw failure;
        }
        return new Sprite() {
            public boolean valid() { return display.getEntity() != null && display.getEntity().isValid(); }
            public void update(org.bukkit.Location location, Particle particle) {
                display.scaleAbsolute(new org.joml.Vector3f(particle.size()), 1);
                display.setColor(particle.color());
                display.moveEntityStatic(location);
            }
            public void remove() { display.remove(); }
        };
    }
    /** Permanently stop this effect; safe to call repeatedly. */
    @Override public void close() {
        closed = true;
        bursts.forEach(Burst::remove);
        bursts.clear();
    }
}
