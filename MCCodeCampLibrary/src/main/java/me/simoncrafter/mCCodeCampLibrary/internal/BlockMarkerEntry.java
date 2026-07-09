package me.simoncrafter.mCCodeCampLibrary.internal;

/**
 * type is a fixed, library-controlled token (e.g. "button") and goes into the
 * NamespacedKey itself. id is whatever a course author chose and is only ever
 * stored as the PDC value, never as part of the key — NamespacedKey keys are
 * restricted to lowercase/digits/./-/_, and ids aren't guaranteed to fit that.
 */
public record BlockMarkerEntry(String type, String id) {}