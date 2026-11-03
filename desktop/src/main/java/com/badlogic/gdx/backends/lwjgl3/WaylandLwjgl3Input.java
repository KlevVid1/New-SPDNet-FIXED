package com.badlogic.gdx.backends.lwjgl3;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.glutils.HdpiMode;

import java.util.Arrays;

public class WaylandLwjgl3Input extends DefaultLwjgl3Input {

    private boolean touchInitialized = false;

    private static final int MAX_POINTERS = 10;
    private final int[] activeWaylandIds = new int[MAX_POINTERS];
    private final int[] pointerX = new int[MAX_POINTERS];
    private final int[] pointerY = new int[MAX_POINTERS];

    public WaylandLwjgl3Input(Lwjgl3Window window) {
        super(window);
        Arrays.fill(activeWaylandIds, -1);
        tryInitTouch(window.getWindowHandle());
    }

    @Override
    public void windowHandleChanged(long windowHandle) {
        super.windowHandleChanged(windowHandle);
        tryInitTouch(windowHandle);
    }

    private void tryInitTouch(long windowHandle) {
        if (touchInitialized || windowHandle == 0) return;
        try {
            long wlDisplay = org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandDisplay();
            long wlSurface = org.lwjgl.glfw.GLFWNativeWayland.glfwGetWaylandWindow(windowHandle);
            System.out.println("[WaylandLwjgl3Input] tryInitTouch: wlDisplay=0x" + Long.toHexString(wlDisplay)
                    + ", wlSurface=0x" + Long.toHexString(wlSurface));
            if (wlDisplay != 0) {
                touchInitialized = WaylandTouch.init(wlDisplay, wlSurface, this);
            }
        } catch (Throwable t) {
            System.err.println("[WaylandLwjgl3Input] Failed to query GLFW Wayland handles: " + t.getMessage());
        }
    }

    private int allocateGdxPointer(int waylandId) {
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (activeWaylandIds[i] == waylandId) {
                return i;
            }
        }
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (activeWaylandIds[i] == -1) {
                activeWaylandIds[i] = waylandId;
                return i;
            }
        }
        return 0;
    }

    private int findGdxPointer(int waylandId) {
        for (int i = 0; i < MAX_POINTERS; i++) {
            if (activeWaylandIds[i] == waylandId) {
                return i;
            }
        }
        return -1;
    }

    public synchronized void onTouchDown(int waylandId, double x, double y) {
        int sx = (int)Math.round(x);
        int sy = (int)Math.round(y);
        if (window.getConfig().hdpiMode == HdpiMode.Pixels) {
            float xScale = window.getGraphics().getBackBufferWidth() / (float)window.getGraphics().getLogicalWidth();
            float yScale = window.getGraphics().getBackBufferHeight() / (float)window.getGraphics().getLogicalHeight();
            sx = (int)(sx * xScale);
            sy = (int)(sy * yScale);
        }

        int gdxPointer = allocateGdxPointer(waylandId);
        pointerX[gdxPointer] = sx;
        pointerY[gdxPointer] = sy;

        if (gdxPointer == 0) {
            deltaX = 0;
            deltaY = 0;
            mouseX = sx;
            mouseY = sy;
            mousePressed++;
            justTouched = true;
            justPressedButtons[Input.Buttons.LEFT] = true;
        }

        long time = System.nanoTime();
        System.out.println("[WaylandLwjgl3Input] onTouchDown: waylandId=" + waylandId
                + " -> gdxPointer=" + gdxPointer + " pos=(" + sx + ", " + sy + ")");
        window.getGraphics().requestRendering();
        eventQueue.touchDown(sx, sy, gdxPointer, Input.Buttons.LEFT, time);
    }

    public synchronized void onTouchMotion(int waylandId, double x, double y) {
        int sx = (int)Math.round(x);
        int sy = (int)Math.round(y);
        if (window.getConfig().hdpiMode == HdpiMode.Pixels) {
            float xScale = window.getGraphics().getBackBufferWidth() / (float)window.getGraphics().getLogicalWidth();
            float yScale = window.getGraphics().getBackBufferHeight() / (float)window.getGraphics().getLogicalHeight();
            sx = (int)(sx * xScale);
            sy = (int)(sy * yScale);
        }

        int gdxPointer = findGdxPointer(waylandId);
        if (gdxPointer == -1) {
            gdxPointer = allocateGdxPointer(waylandId);
        }
        pointerX[gdxPointer] = sx;
        pointerY[gdxPointer] = sy;

        if (gdxPointer == 0) {
            deltaX = sx - mouseX;
            deltaY = sy - mouseY;
            mouseX = sx;
            mouseY = sy;
        }

        long time = System.nanoTime();
        window.getGraphics().requestRendering();
        eventQueue.touchDragged(sx, sy, gdxPointer, time);
    }

    public synchronized void onTouchUp(int waylandId) {
        int gdxPointer = findGdxPointer(waylandId);
        int upX;
        int upY;
        if (gdxPointer != -1) {
            upX = pointerX[gdxPointer];
            upY = pointerY[gdxPointer];
            activeWaylandIds[gdxPointer] = -1;
        } else {
            gdxPointer = 0;
            upX = mouseX;
            upY = mouseY;
        }

        if (gdxPointer == 0) {
            mousePressed = Math.max(0, mousePressed - 1);
            mouseX = upX;
            mouseY = upY;
        }

        long time = System.nanoTime();
        System.out.println("[WaylandLwjgl3Input] onTouchUp: waylandId=" + waylandId
                + " -> gdxPointer=" + gdxPointer + " pos=(" + upX + ", " + upY + ")");
        window.getGraphics().requestRendering();
        eventQueue.touchUp(upX, upY, gdxPointer, Input.Buttons.LEFT, time);
    }

    public synchronized void onTouchCancel(int waylandId) {
        int gdxPointer = findGdxPointer(waylandId);
        int cancelX;
        int cancelY;
        if (gdxPointer != -1) {
            cancelX = pointerX[gdxPointer];
            cancelY = pointerY[gdxPointer];
            activeWaylandIds[gdxPointer] = -1;
        } else {
            gdxPointer = 0;
            cancelX = mouseX;
            cancelY = mouseY;
        }

        if (gdxPointer == 0) {
            mousePressed = Math.max(0, mousePressed - 1);
            mouseX = cancelX;
            mouseY = cancelY;
        }

        long time = System.nanoTime();
        System.out.println("[WaylandLwjgl3Input] onTouchCancel: waylandId=" + waylandId
                + " -> gdxPointer=" + gdxPointer);
        window.getGraphics().requestRendering();
        eventQueue.touchUp(cancelX, cancelY, gdxPointer, Input.Buttons.LEFT, time);
    }

    @Override
    public boolean isTouched() {
        return mousePressed > 0 || super.isTouched();
    }

    @Override
    public boolean isTouched(int pointer) {
        return pointer == 0 ? isTouched() : (pointer >= 0 && pointer < MAX_POINTERS && activeWaylandIds[pointer] != -1);
    }

    @Override
    public int getX(int pointer) {
        if (pointer >= 0 && pointer < MAX_POINTERS && activeWaylandIds[pointer] != -1) {
            return pointerX[pointer];
        }
        return super.getX(pointer);
    }

    @Override
    public int getY(int pointer) {
        if (pointer >= 0 && pointer < MAX_POINTERS && activeWaylandIds[pointer] != -1) {
            return pointerY[pointer];
        }
        return super.getY(pointer);
    }

    @Override
    public boolean isButtonPressed(int button) {
        if (button == Input.Buttons.LEFT && mousePressed > 0) {
            return true;
        }
        return super.isButtonPressed(button);
    }
}
