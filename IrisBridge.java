package dev.frameguard;

import java.lang.reflect.Method;

/** Доступ к Iris API через reflection, чтобы мод собирался и работал и без Iris. */
public final class IrisBridge {
    private static boolean available;
    private static Object config;
    private static Method areShadersEnabled;
    private static Method setShadersEnabledAndApply;

    static {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            config = api.getMethod("getConfig").invoke(instance);
            Class<?> cfg = Class.forName("net.irisshaders.iris.api.v0.IrisApiConfig");
            areShadersEnabled = cfg.getMethod("areShadersEnabled");
            setShadersEnabledAndApply = cfg.getMethod("setShadersEnabledAndApply", boolean.class);
            available = true;
        } catch (Throwable t) {
            available = false;
        }
    }

    private IrisBridge() {}

    public static boolean isAvailable() {
        return available;
    }

    public static boolean areShadersEnabled() {
        if (!available) return false;
        try {
            return (boolean) areShadersEnabled.invoke(config);
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setShaders(boolean enabled) {
        if (!available) return;
        try {
            setShadersEnabledAndApply.invoke(config, enabled);
        } catch (Throwable ignored) {
        }
    }
}
