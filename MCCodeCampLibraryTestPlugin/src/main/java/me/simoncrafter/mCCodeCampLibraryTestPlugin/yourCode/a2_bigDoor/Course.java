package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a2_bigDoor;

import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a2_bigDoor.BigDoor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class Course extends BigDoor {

    protected void onButtonPress() throws InterruptedException {
        Bukkit.broadcast(Component.text(isDoorOpen(RIGHT_DOOR) + " " + isDoorOpen(RIGHT_DOOR) + " " + (!isDoorOpen(RIGHT_DOOR) || !isDoorOpen(LEFT_DOOR))));

        if (!isDoorOpen(RIGHT_DOOR) || !isDoorOpen(LEFT_DOOR)) {
            open(RIGHT_DOOR);
            open(LEFT_DOOR);
        } else {
            close(RIGHT_DOOR);
            close(LEFT_DOOR);
        }
    }
}
