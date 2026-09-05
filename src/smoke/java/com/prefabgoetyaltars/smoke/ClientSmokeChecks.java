// SPDX-License-Identifier: MIT
package com.prefabgoetyaltars.smoke;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SmokeChecks.MOD_ID, value = Dist.CLIENT)
public final class ClientSmokeChecks {
    private static int readyTicks;
    private static boolean languageSelected;
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.END && minecraft.screen instanceof AccessibilityOnboardingScreen) {
            LogUtils.getLogger().info("ALTAR_SMOKE: opening title screen past first-run accessibility onboarding");
            minecraft.setScreen(new TitleScreen());
        }
        if (event.phase == TickEvent.Phase.END && minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
            if (!languageSelected && Boolean.getBoolean("altar.integrationSmoke")) {
                languageSelected = true;
                String language = com.prefabgoetyaltars.compat.revelation.RevelationCompat.isLoaded() ? "zh_cn" : "en_us";
                minecraft.getLanguageManager().setSelected(language); minecraft.options.languageCode = language;
                minecraft.reloadResourcePacks(); return;
            }
            if (++readyTicks == 30) {
                LogUtils.getLogger().info("ALTAR_SMOKE_CLIENT_TITLE_OK: title screen and resource reload complete");
                if (Boolean.getBoolean("altar.integrationSmoke")) AltarSmokeChecks.createWorld();
                else if (Boolean.getBoolean("altar.scannerSmoke")) ScannerSmokeChecks.createWorld();
                else minecraft.stop();
            }
        }
        if (Boolean.getBoolean("altar.integrationSmoke") && event.phase == TickEvent.Phase.END) AltarSmokeChecks.tick();
        if (Boolean.getBoolean("altar.scannerSmoke") && event.phase == TickEvent.Phase.END) ScannerSmokeChecks.tick();
    }
}
