package com.prefabgoetyaltars.client;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.client.screen.AltarStructureScannerScreen;
import com.prefabgoetyaltars.dev.scan.AltarStructureScannerConfig;
import com.prefabgoetyaltars.network.OpenAltarScannerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT)
public final class ScannerClient {
    public static void open(OpenAltarScannerPacket packet) {
        var screen = new AltarStructureScannerScreen(packet.pos(), AltarStructureScannerConfig.fromTag(packet.config()));
        if (screen.canInteract()) Minecraft.getInstance().setScreen(screen);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AltarStructureScannerScreen screen) || !screen.canInteract()) return;
        var bounds = screen.previewBounds();
        if (bounds == null) return;
        var camera = event.getCamera().getPosition();
        var pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var buffers = minecraft.renderBuffers().bufferSource();
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), bounds.box(), 0.2F, 1.0F, 0.6F, 1.0F);
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }
    private ScannerClient() {}
}
