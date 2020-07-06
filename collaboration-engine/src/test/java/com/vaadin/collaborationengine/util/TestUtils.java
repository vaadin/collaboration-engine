package com.vaadin.collaborationengine.util;

import java.lang.ref.WeakReference;

public class TestUtils {

    public static boolean isGarbageCollected(WeakReference<?> ref)
            throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            System.gc();
            if (ref.get() == null) {
                return true;
            }
        }
        return false;
    }

}
