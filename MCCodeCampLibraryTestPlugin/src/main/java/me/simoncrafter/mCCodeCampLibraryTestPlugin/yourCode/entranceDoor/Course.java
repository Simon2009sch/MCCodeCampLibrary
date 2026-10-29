package me.simoncrafter.mCCodeCampLibraryTestPlugin.yourCode.entranceDoor;

import me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.entranceDoor.*;

public class Course extends EntranceDoor {

    protected void onButtonPress() throws InterruptedException {
        open();
        pause(5000);
        close();
    }
}
