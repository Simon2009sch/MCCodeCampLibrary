package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.a1_entranceDoor;

import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a1_entranceDoor.*;

public class Course extends EntranceDoor {

    protected void onButtonPress() throws InterruptedException {
        open();
        pause(5000);
        close();
    }
}
