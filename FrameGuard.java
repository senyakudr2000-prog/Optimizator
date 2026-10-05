package dev.frameguard;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FrameGuard.MODID, dist = Dist.CLIENT)
public class FrameGuard {
    public static final String MODID = "frameguard";

    public FrameGuard(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);

        NeoForge.EVENT_BUS.addListener(QualityController::onFrame);
        NeoForge.EVENT_BUS.addListener(QualityController::onLogin);
        NeoForge.EVENT_BUS.addListener(QualityController::onLogout);
        NeoForge.EVENT_BUS.addListener(FrameGuard::registerCommands);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("frameguard")
                        .executes(ctx -> reply(ctx, QualityController.status()))
                        .then(Commands.literal("on").executes(ctx -> {
                            QualityController.setEnabled(true);
                            return reply(ctx, "FrameGuard: включён");
                        }))
                        .then(Commands.literal("off").executes(ctx -> {
                            QualityController.setEnabled(false);
                            return reply(ctx, "FrameGuard: выключен, настройки возвращены");
                        })));
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
