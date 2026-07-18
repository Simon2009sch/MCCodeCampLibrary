package me.simoncrafter.mCCodeCampLibrary.obstical;

public interface IOpenable {
    void open(boolean skipTransition);
    void close(boolean skipTransition);
    default void open() {
        open(false);
    }
    default void close() {
        close(false);
    }
}
