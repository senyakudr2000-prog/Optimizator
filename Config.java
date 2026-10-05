package dev.frameguard;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = B
            .comment("Включить автоматическое управление качеством")
            .define("enabled", true);

    public static final ModConfigSpec.IntValue TARGET_FPS = B
            .comment("Целевой FPS. Если средний FPS ниже, качество снижается")
            .defineInRange("targetFps", 35, 10, 240);

    public static final ModConfigSpec.IntValue RECOVER_MARGIN = B
            .comment("Насколько FPS должен превышать цель, чтобы качество начало возвращаться")
            .defineInRange("recoverMargin", 12, 3, 100);

    public static final ModConfigSpec.IntValue DOWN_SECONDS = B
            .comment("Сколько секунд подряд FPS должен быть ниже цели, чтобы снизить качество")
            .defineInRange("secondsBeforeDowngrade", 3, 1, 30);

    public static final ModConfigSpec.IntValue UP_SECONDS = B
            .comment("Сколько секунд подряд FPS должен быть высоким, чтобы вернуть качество")
            .defineInRange("secondsBeforeUpgrade", 12, 3, 120);

    public static final ModConfigSpec.BooleanValue ALLOW_SHADER_OFF = B
            .comment("Разрешить на самом высоком уровне экономии выключать шейдеры Iris (потом включит обратно)")
            .define("allowDisableShaders", false);

    public static final ModConfigSpec.BooleanValue ALLOW_RENDER_DISTANCE = B
            .comment("Разрешить на самом высоком уровне экономии снижать дальность прорисовки (вызывает перезагрузку чанков)")
            .define("allowLowerRenderDistance", false);

    public static final ModConfigSpec.IntValue MIN_RENDER_DISTANCE = B
            .comment("Нижняя граница дальности прорисовки, если она разрешена выше")
            .defineInRange("minRenderDistance", 4, 2, 32);

    public static final ModConfigSpec.BooleanValue SHOW_MESSAGES = B
            .comment("Показывать короткое сообщение над хотбаром при смене уровня")
            .define("showMessages", true);

    public static final ModConfigSpec SPEC = B.build();

    private Config() {}
}
