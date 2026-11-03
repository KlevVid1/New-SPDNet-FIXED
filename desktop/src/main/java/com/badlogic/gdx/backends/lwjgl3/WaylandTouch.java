package com.badlogic.gdx.backends.lwjgl3;

public class WaylandTouch {
    private static boolean loaded = false;
    private static boolean initialized = false;

    static {
        try {
            System.loadLibrary("wayland_touch");
            loaded = true;
            System.out.println("[WaylandTouch] Loaded libwayland_touch.so via System.loadLibrary");
        } catch (Throwable t) {
            try {
                String appDir = System.getenv("APP_DIR");
                if (appDir != null) {
                    System.load(appDir + "/lib/libwayland_touch.so");
                    loaded = true;
                    System.out.println("[WaylandTouch] Loaded " + appDir + "/lib/libwayland_touch.so");
                } else {
                    System.err.println("[WaylandTouch] System.loadLibrary failed and APP_DIR not set: " + t.getMessage());
                }
            } catch (Throwable t2) {
                System.err.println("[WaylandTouch] Failed to load libwayland_touch.so: " + t2.getMessage());
            }
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static synchronized boolean init(long displayHandle, long surfaceHandle, WaylandLwjgl3Input input) {
        if (!loaded) {
            System.err.println("[WaylandTouch] Cannot init: native library not loaded");
            return false;
        }
        if (initialized) {
            return true;
        }
        try {
            boolean success = initNative(displayHandle, surfaceHandle, input);
            if (success) {
                initialized = true;
                System.out.println("[WaylandTouch] Successfully initialized Wayland touch input!");
            } else {
                System.err.println("[WaylandTouch] initNative returned false");
            }
            return success;
        } catch (Throwable t) {
            System.err.println("[WaylandTouch] Error initializing Wayland touch: " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }

    private static native boolean initNative(long displayHandle, long surfaceHandle, WaylandLwjgl3Input input);
}

