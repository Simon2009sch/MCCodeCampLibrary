package me.simoncrafter.mCCodeCampLibrary.courseapi;

public interface IStudentTimingHelper {

    default void pause(long milliseconds) throws InterruptedException {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("pause() must not run on the server thread");
        }

        Thread.sleep(milliseconds);
    }
}
