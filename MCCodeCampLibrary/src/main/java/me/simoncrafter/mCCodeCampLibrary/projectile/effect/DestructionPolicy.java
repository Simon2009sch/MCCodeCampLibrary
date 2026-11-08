package me.simoncrafter.mCCodeCampLibrary.projectile.effect;

/** In-memory, exact-world destruction gate. A fresh instance always starts disarmed. */
public final class DestructionPolicy {
    private boolean armed;
    public DestructionPolicy() {}
    public boolean permits(String worldName, double x, double z, boolean explicitLiveRequest) {
        return finite(x,z) && ("testworld".equals(worldName)
                || (armed && explicitLiveRequest && "world".equals(worldName) && near(x,z,1)));
    }
    public boolean permitsEntityDamage(String worldName, double x, double z) {
        return finite(x,z) && ("testworld".equals(worldName)
                || (armed && "world".equals(worldName) && near(x,z,90)));
    }
    public void armLiveTest() { armed = true; }
    public void disarmLiveTest() { armed = false; }
    public boolean liveArmed() { return armed; }
    private static boolean finite(double x, double z) { return Double.isFinite(x) && Double.isFinite(z); }
    private static boolean near(double x,double z,double radius) {
        double dx=x+1368, dz=z-3350;
        return dx*dx+dz*dz <= radius*radius;
    }
}
