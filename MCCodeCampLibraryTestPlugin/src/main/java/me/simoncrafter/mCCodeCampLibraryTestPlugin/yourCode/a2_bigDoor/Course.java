package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a2_bigDoor;

import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a2_bigDoor.BigDoor;

public class Course extends BigDoor {

    boolean doorState = true;

    protected void onButtonPress() throws InterruptedException {
        if (doorState) {
            open(RIGHT_DOOR);
            open(LEFT_DOOR);
        } else {
            close(RIGHT_DOOR);
            close(LEFT_DOOR);
        }
        doorState = !doorState;
    }
}
