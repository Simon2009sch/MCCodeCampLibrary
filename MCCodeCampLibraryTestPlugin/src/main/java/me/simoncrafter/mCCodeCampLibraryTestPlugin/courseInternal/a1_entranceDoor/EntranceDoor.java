package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a1_entranceDoor;

import me.simoncrafter.mCCodeCampLibrary.courseapi.trainingwheels.IStudentTimingTrainingWheel;

public abstract class EntranceDoor implements IStudentTimingTrainingWheel {

    private EntranceDoorCourseSegment segment;


    void setSegment(EntranceDoorCourseSegment segment) {
        this.segment = segment;
    }

    protected final void open() {
        segment.sOpen();
    }

    protected final void close() {
        segment.sClose();
    }

    protected abstract void onButtonPress() throws InterruptedException;


}
