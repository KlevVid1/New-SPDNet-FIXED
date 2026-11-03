#include <jni.h>
#include <wayland-client.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    JavaVM *jvm;
    jobject input_obj;
    jmethodID on_touch_down;
    jmethodID on_touch_up;
    jmethodID on_touch_motion;
    jmethodID on_touch_cancel;

    struct wl_display *display;
    struct wl_surface *window_surface;
    struct wl_registry *registry;
    struct wl_seat *seat;
    struct wl_touch *touch;
} WaylandTouchContext;

static WaylandTouchContext g_ctx;

static JNIEnv* get_jni_env(WaylandTouchContext *ctx) {
    if (!ctx->jvm) return NULL;
    JNIEnv *env = NULL;
    jint res = (*ctx->jvm)->GetEnv(ctx->jvm, (void**)&env, JNI_VERSION_1_6);
    if (res == JNI_EDETACHED) {
        if ((*ctx->jvm)->AttachCurrentThread(ctx->jvm, (void**)&env, NULL) != 0) {
            return NULL;
        }
    }
    return env;
}

static void touch_handle_down(void *data, struct wl_touch *wl_touch,
                              uint32_t serial, uint32_t time,
                              struct wl_surface *surface,
                              int32_t id, wl_fixed_t x, wl_fixed_t y) {
    WaylandTouchContext *ctx = (WaylandTouchContext *)data;
    double px = wl_fixed_to_double(x);
    double py = wl_fixed_to_double(y);
    fprintf(stderr, "[WaylandTouch] touch DOWN id=%d pos=(%.1f, %.1f) surface=%p\n", id, px, py, (void*)surface);

    JNIEnv *env = get_jni_env(ctx);
    if (!env || !ctx->input_obj || !ctx->on_touch_down) return;

    (*env)->CallVoidMethod(env, ctx->input_obj, ctx->on_touch_down, (jint)id, (jdouble)px, (jdouble)py);
}

static void touch_handle_up(void *data, struct wl_touch *wl_touch,
                            uint32_t serial, uint32_t time,
                            int32_t id) {
    WaylandTouchContext *ctx = (WaylandTouchContext *)data;
    fprintf(stderr, "[WaylandTouch] touch UP id=%d\n", id);

    JNIEnv *env = get_jni_env(ctx);
    if (!env || !ctx->input_obj || !ctx->on_touch_up) return;

    (*env)->CallVoidMethod(env, ctx->input_obj, ctx->on_touch_up, (jint)id);
}

static void touch_handle_motion(void *data, struct wl_touch *wl_touch,
                                uint32_t time,
                                int32_t id, wl_fixed_t x, wl_fixed_t y) {
    WaylandTouchContext *ctx = (WaylandTouchContext *)data;
    JNIEnv *env = get_jni_env(ctx);
    if (!env || !ctx->input_obj || !ctx->on_touch_motion) return;

    double px = wl_fixed_to_double(x);
    double py = wl_fixed_to_double(y);
    (*env)->CallVoidMethod(env, ctx->input_obj, ctx->on_touch_motion, (jint)id, (jdouble)px, (jdouble)py);
}

static void touch_handle_frame(void *data, struct wl_touch *wl_touch) {
    // End of touch frame
}

static void touch_handle_cancel(void *data, struct wl_touch *wl_touch) {
    WaylandTouchContext *ctx = (WaylandTouchContext *)data;
    JNIEnv *env = get_jni_env(ctx);
    if (!env || !ctx->input_obj || !ctx->on_touch_cancel) return;

    (*env)->CallVoidMethod(env, ctx->input_obj, ctx->on_touch_cancel, (jint)0);
}

static void touch_handle_shape(void *data, struct wl_touch *wl_touch,
                               int32_t id, wl_fixed_t major, wl_fixed_t minor) {}

static void touch_handle_orientation(void *data, struct wl_touch *wl_touch,
                                     int32_t id, wl_fixed_t orientation) {}

static const struct wl_touch_listener s_touch_listener = {
    .down = touch_handle_down,
    .up = touch_handle_up,
    .motion = touch_handle_motion,
    .frame = touch_handle_frame,
    .cancel = touch_handle_cancel,
    .shape = touch_handle_shape,
    .orientation = touch_handle_orientation
};

static void seat_handle_capabilities(void *data, struct wl_seat *seat, uint32_t caps) {
    WaylandTouchContext *ctx = (WaylandTouchContext *)data;
    fprintf(stderr, "[WaylandTouch] seat capabilities = 0x%x (WL_SEAT_CAPABILITY_TOUCH = %d)\n",
            caps, (caps & WL_SEAT_CAPABILITY_TOUCH) != 0);

    if (caps & WL_SEAT_CAPABILITY_TOUCH) {
        if (!ctx->touch) {
            ctx->touch = wl_seat_get_touch(seat);
            wl_touch_add_listener(ctx->touch, &s_touch_listener, ctx);
            fprintf(stderr, "[WaylandTouch] bound wl_touch listener successfully!\n");
        }
    } else if (ctx->touch) {
        wl_proxy_destroy((struct wl_proxy *)ctx->touch);
        ctx->touch = NULL;
        fprintf(stderr, "[WaylandTouch] wl_touch removed\n");
    }
}

static void seat_handle_name(void *data, struct wl_seat *seat, const char *name) {
    fprintf(stderr, "[WaylandTouch] seat name: %s\n", name ? name : "unknown");
}

static const struct wl_seat_listener s_seat_listener = {
    .capabilities = seat_handle_capabilities,
    .name = seat_handle_name
};

static void registry_handle_global(void *data, struct wl_registry *registry,
                                   uint32_t name, const char *interface, uint32_t version) {
    WaylandTouchContext *ctx = (WaylandTouchContext *)data;
    if (strcmp(interface, "wl_seat") == 0) {
        if (!ctx->seat) {
            uint32_t bind_version = version < 7 ? version : 7;
            ctx->seat = (struct wl_seat *)wl_registry_bind(registry, name, &wl_seat_interface, bind_version);
            wl_seat_add_listener(ctx->seat, &s_seat_listener, ctx);
            fprintf(stderr, "[WaylandTouch] bound wl_seat name %u (version %u)\n", name, bind_version);
        }
    }
}

static void registry_handle_global_remove(void *data, struct wl_registry *registry, uint32_t name) {
}

static const struct wl_registry_listener s_registry_listener = {
    .global = registry_handle_global,
    .global_remove = registry_handle_global_remove
};

JNIEXPORT jboolean JNICALL Java_com_badlogic_gdx_backends_lwjgl3_WaylandTouch_initNative(
    JNIEnv *env, jclass clazz, jlong displayHandle, jlong surfaceHandle, jobject inputObj) {

    fprintf(stderr, "[WaylandTouch] initNative: displayHandle=0x%lx, surfaceHandle=0x%lx\n",
            (unsigned long)displayHandle, (unsigned long)surfaceHandle);

    if (displayHandle == 0) {
        fprintf(stderr, "[WaylandTouch] error: displayHandle is 0\n");
        return JNI_FALSE;
    }

    memset(&g_ctx, 0, sizeof(g_ctx));
    (*env)->GetJavaVM(env, &g_ctx.jvm);
    g_ctx.input_obj = (*env)->NewGlobalRef(env, inputObj);

    jclass inputClass = (*env)->GetObjectClass(env, inputObj);
    g_ctx.on_touch_down = (*env)->GetMethodID(env, inputClass, "onTouchDown", "(IDD)V");
    g_ctx.on_touch_up = (*env)->GetMethodID(env, inputClass, "onTouchUp", "(I)V");
    g_ctx.on_touch_motion = (*env)->GetMethodID(env, inputClass, "onTouchMotion", "(IDD)V");
    g_ctx.on_touch_cancel = (*env)->GetMethodID(env, inputClass, "onTouchCancel", "(I)V");

    if (!g_ctx.on_touch_down || !g_ctx.on_touch_up || !g_ctx.on_touch_motion) {
        fprintf(stderr, "[WaylandTouch] error: failed to find touch callbacks on Java input class\n");
        return JNI_FALSE;
    }

    g_ctx.display = (struct wl_display *)displayHandle;
    g_ctx.window_surface = (struct wl_surface *)surfaceHandle;

    g_ctx.registry = wl_display_get_registry(g_ctx.display);
    wl_registry_add_listener(g_ctx.registry, &s_registry_listener, &g_ctx);

    // Initial roundtrips to bind seat and touch
    wl_display_roundtrip(g_ctx.display);
    if (g_ctx.seat) {
        wl_display_roundtrip(g_ctx.display);
    }

    fprintf(stderr, "[WaylandTouch] initNative completed: touch=%p\n", (void*)g_ctx.touch);
    return g_ctx.touch != NULL ? JNI_TRUE : JNI_FALSE;
}
