package me.simoncrafter.mCCodeCampLibraryTestPlugin.courseInternal.a2_bigDoor;

import me.simoncrafter.mCCodeCampLibrary.courseapi.trainingwheels.IStudentTimingTrainingWheel;

public abstract class BigDoor implements IStudentTimingTrainingWheel {

    private BigDoorCourseSegment segment;
    protected static final String LEFT_DOOR = "sliding_door:big_door_left";
    protected static final String RIGHT_DOOR = "sliding_door:big_door_right";

    void setSegment(BigDoorCourseSegment segment) {
        this.segment = segment;
    }

    protected final void open(String doorName) {
        segment.sOpen(doorName);
    }

    protected final void close(String doorName) {
        segment.sClose(doorName);
    }

    protected abstract void onButtonPress() throws InterruptedException;


}
