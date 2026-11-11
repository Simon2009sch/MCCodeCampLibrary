package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a4_flightFeather;

public final class WindEffectHarness {
    public static void main(String[] args) {
        WindEffect.Particle fresh = new WindEffect.Particle(0, 0.5, 0.5);
        WindEffect.Particle old = new WindEffect.Particle(0, 0.5, 0.5);
        old.age = 20;
        double initialVx = fresh.vx;
        fresh.step(); old.step();
        check(initialVx - old.vx < initialVx - fresh.vx, "center pull weakens with age at the same radius");
        WindEffect.Particle p = new WindEffect.Particle(0, 0.5, 0.5);
        double radius = p.radius();
        double y = p.y;
        double angle = Math.atan2(p.z, p.x);
        for (int i = 0; i < 20; i++) p.step();
        check(p.radius() > radius, "wind travels outward");
        check(p.y < y, "gravity pulls wind downward");
        check(Math.abs(Math.atan2(p.z, p.x) - angle) > 0.1, "wind swirls");
        check(p.size() < 0.16f, "distant wind shrinks");
        check(p.color().getBlue() > p.color().getRed(), "wind turns blue");
        check(p.color().getAlpha() < 200, "wind fades");
        System.out.println("PASS wind physics: outward swirl, gravity, shrink, blue fade");
        lifecycle();
    }
    static void lifecycle() {
        var sprites = new java.util.ArrayList<FakeSprite>();
        boolean[] loaded = {true};
        WindEffect effect = new WindEffect((location, particle) -> {
            FakeSprite sprite = new FakeSprite(); sprites.add(sprite); return sprite;
        }, location -> loaded[0]);
        FakeOwner owner = new FakeOwner();
        effect.burst(owner);
        check(sprites.size() == 28, "28 displays per burst");
        owner.location.setX(100);
        effect.tick();
        check(sprites.getFirst().last.getX() < 5, "burst origin stays fixed");
        for (int i = 0; i < 4; i++) effect.burst(owner);
        check(sprites.stream().filter(s -> !s.removed).count() == 112, "four-burst cap");
        for (int i = 0; i < 30; i++) effect.tick();
        check(sprites.stream().allMatch(s -> s.removed), "expiry removes all displays");
        effect.burst(owner); owner.online = false; effect.tick();
        check(sprites.stream().allMatch(s -> s.removed), "quit cleanup");
        owner.online = true; effect.burst(owner); owner.sameWorld = false; effect.tick();
        check(sprites.stream().allMatch(s -> s.removed), "world-change cleanup");
        owner.sameWorld = true; effect.burst(owner); loaded[0] = false; effect.tick();
        check(sprites.stream().allMatch(s -> s.removed), "unloaded world/chunk cleanup");
        loaded[0] = true; effect.burst(owner);
        sprites.getLast().valid = false; effect.tick();
        check(sprites.getLast().removed, "invalid entity cleanup");
        effect.close(); effect.close(); int count = sprites.size();
        effect.burst(owner); effect.tick();
        check(sprites.size() == count && sprites.stream().allMatch(s -> s.removed), "permanent idempotent close");
        System.out.println("PASS wind lifecycle: cap, fixed origin, expiry, quit, world change, unload, invalid, close");
    }
    static final class FakeOwner implements WindEffect.Owner {
        final org.bukkit.Location location = new org.bukkit.Location(null, 0, 80, 0);
        boolean online = true, sameWorld = true;
        public org.bukkit.Location location() { return location; }
        public boolean active(org.bukkit.World world) { return online && sameWorld; }
    }
    static final class FakeSprite implements WindEffect.Sprite {
        boolean removed, valid = true;
        org.bukkit.Location last;
        public boolean valid() { return valid; }
        public void update(org.bukkit.Location location, WindEffect.Particle p) { last = location; }
        public void remove() { removed = true; }
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
