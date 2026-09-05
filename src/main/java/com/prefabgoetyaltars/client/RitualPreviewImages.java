package com.prefabgoetyaltars.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;
import com.prefabgoetyaltars.PrefabGoetyAltars;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod.EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RitualPreviewImages {
    public record Size(int width, int height) {
        public Size { if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid preview size"); }
        public Size fit(int maxWidth, int maxHeight) {
            double scale = Math.min((double) Math.max(1, maxWidth) / width, (double) Math.max(1, maxHeight) / height);
            return new Size(Math.max(1, (int) Math.floor(width * scale)), Math.max(1, (int) Math.floor(height * scale)));
        }
    }
    private static final Map<ResourceLocation, Optional<Size>> CACHE = new HashMap<>();
    private static int generation;
    @SubscribeEvent public static void registerReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> invalidate());
    }
    public static void invalidate() { CACHE.clear(); generation++; }
    public static int generation() { return generation; }
    public static Optional<Size> resolve(ResourceManager manager, ResourceLocation texture) {
        if (texture == null) return Optional.empty();
        return CACHE.computeIfAbsent(texture, key -> {
            try (var stream = manager.getResource(key).orElseThrow(() -> new IOException("Resource not found")).open();
                 var image = NativeImage.read(stream)) {
                return Optional.of(new Size(image.getWidth(), image.getHeight()));
            } catch (IOException | RuntimeException e) {
                if (!FMLEnvironment.production) PrefabGoetyAltars.LOGGER.warn("Missing ritual preview texture: {} ({})", key, e.toString());
                return Optional.empty();
            }
        });
    }
    private RitualPreviewImages() {}
}
