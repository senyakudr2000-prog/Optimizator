package dev.frameguard;

import com.mojang.logging.LogUtils;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import org.slf4j.Logger;

public final class QualityController {
    private static final Logger LOG = LogUtils.getLogger();
    public static final int MAX_LEVEL = 3;

    private record Baseline(ParticleStatus particles, boolean shadows, CloudStatus clouds,
                            boolean ao, int blend, double entityScale, int renderDist) {}

    private static Boolean runtimeEnabled = null;
    private static int level = 0;

    private static long lastFrameNanos = 0;
    private static long accNanos = 0;
    private static int frames = 0;
    private static double lastFps = 0;

    private static int lowSec = 0;
    private static int highSec = 0;
    private static int upBackoff = 1;
    private static long lastUpgradeMs = 0;
    private static long warmupUntilMs = 0;

    private static Baseline baseline = null;
    private static boolean shadersDisabledByUs = false;

    private QualityController() {}

    // ---------- события ----------

    public static void onFrame(RenderFrameEvent.Pre event) {
        long now = System.nanoTime();
        if (lastFrameNanos == 0) {
            lastFrameNanos = now;
            return;
        }
        long dt = now - lastFrameNanos;
        lastFrameNanos = now;
        if (dt > 1_000_000_000L) return; // пауза/сворачивание, не считаем

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused() || !mc.isWindowActive()) {
            resetWindow();
            return;
        }
        if (!isEnabled()) return;
        if (System.currentTimeMillis() < warmupUntilMs) return;

        accNanos += dt;
        frames++;
        if (accNanos >= 1_000_000_000L) {
            lastFps = frames * 1.0e9 / accNanos;
            resetWindow();
            evaluate(mc, lastFps);
        }
    }

    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        warmupUntilMs = System.currentTimeMillis() + 15_000L; // даём миру прогрузиться
        resetWindow();
        lowSec = 0;
        highSec = 0;
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restoreAll(Minecraft.getInstance());
        resetWindow();
        lowSec = 0;
        highSec = 0;
        upBackoff = 1;
    }

    // ---------- логика ----------

    private static void evaluate(Minecraft mc, double fps) {
        int target = Config.TARGET_FPS.get();
        int margin = Config.RECOVER_MARGIN.get();

        if (fps < target) {
            lowSec++;
            highSec = 0;
            if (lowSec >= Config.DOWN_SECONDS.get() && level < MAX_LEVEL) {
                long now = System.currentTimeMillis();
                // если мы только что подняли качество и FPS опять упал, ждём дольше перед следующим подъёмом
                if (lastUpgradeMs != 0 && now - lastUpgradeMs < 60_000L) {
                    upBackoff = Math.min(8, upBackoff * 2);
                }
                lowSec = 0;
                setLevel(mc, level + 1);
            }
        } else if (fps > target + margin) {
            highSec++;
            lowSec = 0;
            if (level > 0 && highSec >= Config.UP_SECONDS.get() * upBackoff) {
                highSec = 0;
                lastUpgradeMs = System.currentTimeMillis();
                setLevel(mc, level - 1);
            }
        } else {
            lowSec = 0;
            highSec = 0;
        }
    }

    private static void setLevel(Minecraft mc, int newLevel) {
        if (newLevel < 0 || newLevel > MAX_LEVEL || newLevel == level) return;
        if (level == 0 && newLevel > 0) {
            baseline = capture(mc.options);
        }
        level = newLevel;
        if (level == 0) {
            restoreAll(mc);
            upBackoff = 1;
        } else {
            apply(mc, level);
        }
        LOG.info("[FrameGuard] уровень экономии {} (FPS ~{})", level, (int) lastFps);
        message(mc, "FrameGuard: уровень " + level + "/" + MAX_LEVEL + "  (FPS ~" + (int) lastFps + ")");
    }

    private static void apply(Minecraft mc, int lvl) {
        Options o = mc.options;
        Baseline b = baseline;
        if (b == null) return;

        // всегда начинаем с исходных настроек игрока, затем накладываем уровень
        restoreOptions(o, b);

        if (lvl >= 1) {
            setIfDiff(o.particles(), ParticleStatus.DECREASED);
            setIfDiff(o.entityShadows(), false);
        }
        if (lvl >= 2) {
            setIfDiff(o.particles(), ParticleStatus.MINIMAL);
            setIfDiff(o.cloudStatus(), CloudStatus.OFF);
            setIfDiff(o.ambientOcclusion(), false);
            setIfDiff(o.biomeBlendRadius(), Math.min(b.blend(), 1));
            setIfDiff(o.entityDistanceScaling(), Math.min(b.entityScale(), 0.75));
        }
        if (lvl >= 3) {
            setIfDiff(o.entityDistanceScaling(), Math.min(b.entityScale(), 0.5));
            if (Config.ALLOW_RENDER_DISTANCE.get()) {
                int rd = Math.max(Config.MIN_RENDER_DISTANCE.get(), b.renderDist() - 2);
                setIfDiff(o.renderDistance(), Math.min(rd, b.renderDist()));
            }
        }

        boolean wantShadersOff = lvl >= 3 && Config.ALLOW_SHADER_OFF.get();
        if (wantShadersOff) {
            if (IrisBridge.isAvailable() && IrisBridge.areShadersEnabled()) {
                IrisBridge.setShaders(false);
                shadersDisabledByUs = true;
            }
        } else if (shadersDisabledByUs) {
            IrisBridge.setShaders(true);
            shadersDisabledByUs = false;
        }
    }

    private static void restoreAll(Minecraft mc) {
        if (baseline != null && mc.options != null) {
            restoreOptions(mc.options, baseline);
        }
        baseline = null;
        level = 0;
        if (shadersDisabledByUs) {
            IrisBridge.setShaders(true);
            shadersDisabledByUs = false;
        }
    }

    private static Baseline capture(Options o) {
        return new Baseline(
                o.particles().get(),
                o.entityShadows().get(),
                o.cloudStatus().get(),
                o.ambientOcclusion().get(),
                o.biomeBlendRadius().get(),
                o.entityDistanceScaling().get(),
                o.renderDistance().get());
    }

    private static void restoreOptions(Options o, Baseline b) {
        setIfDiff(o.particles(), b.particles());
        setIfDiff(o.entityShadows(), b.shadows());
        setIfDiff(o.cloudStatus(), b.clouds());
        setIfDiff(o.ambientOcclusion(), b.ao());
        setIfDiff(o.biomeBlendRadius(), b.blend());
        setIfDiff(o.entityDistanceScaling(), b.entityScale());
        setIfDiff(o.renderDistance(), b.renderDist());
    }

    private static <T> void setIfDiff(OptionInstance<T> opt, T value) {
        if (!opt.get().equals(value)) {
            opt.set(value);
        }
    }

    private static void resetWindow() {
        accNanos = 0;
        frames = 0;
    }

    private static void message(Minecraft mc, String text) {
        if (Config.SHOW_MESSAGES.get() && mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), true);
        }
    }

    // ---------- для команды ----------

    public static boolean isEnabled() {
        return runtimeEnabled != null ? runtimeEnabled : Config.ENABLED.get();
    }

    public static void setEnabled(boolean value) {
        runtimeEnabled = value;
        if (!value) {
            restoreAll(Minecraft.getInstance());
            lowSec = 0;
            highSec = 0;
            upBackoff = 1;
        }
    }

    public static String status() {
        return "FrameGuard: " + (isEnabled() ? "включён" : "выключен")
                + ", уровень " + level + "/" + MAX_LEVEL
                + ", FPS ~" + (int) lastFps
                + ", цель " + Config.TARGET_FPS.get()
                + (IrisBridge.isAvailable() ? ", Iris найден" : ", Iris не найден");
    }
}
