package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.entranceDoor;

import me.simoncrafter.mCCodeCampLibrary.courseapi.IStudentTimingHelper;

public abstract class EntranceDoor implements IStudentTimingHelper {

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
