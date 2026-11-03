package com.badlogic.gdx.backends.lwjgl3.angle;

/**
 * Custom ANGLELoader for SPDNet.
 * On ARM Linux / Ubuntu Touch / Halium platforms, the system already provides
 * native hardware-accelerated OpenGL ES via EGL (libhybris / Mali GLES),
 * so no ANGLE translation library needs to be loaded.
 */
public class ANGLELoader {
    public static void load() {
        // Native GLES is provided by the OS / GPU driver on Linux ARM.
    }

    public static void postGlfwInit() {
        // No temporary files to clean up.
    }
}

