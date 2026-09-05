package com.prefabgoetyaltars.client;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.structure.AltarPrefabType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModItemModels {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new net.minecraftforge.client.model.generators.ModelProvider<net.minecraftforge.client.model.generators.ItemModelBuilder>(
                event.getGenerator().getPackOutput(), PrefabGoetyAltars.MOD_ID, "item", OptionalTextureModel::new, event.getExistingFileHelper()) {
            @Override
            public String getName() { return "Goety altar item models"; }

            @Override
            protected void registerModels() {
                java.util.Arrays.stream(AltarPrefabType.values()).forEach(definition -> {
                    // Hand-authored models supplied through --existing are authoritative.
                    if (!event.getExistingFileHelper().exists(modLoc("item/" + definition.id()),
                            net.minecraft.server.packs.PackType.CLIENT_RESOURCES, ".json", "models")) {
                        withExistingParent(definition.id(), mcLoc("item/generated"))
                                .texture("layer0", modLoc("item/" + definition.id()));
                    }
                });
            }
        });
    }
    private static final class OptionalTextureModel extends net.minecraftforge.client.model.generators.ItemModelBuilder {
        OptionalTextureModel(net.minecraft.resources.ResourceLocation location, net.minecraftforge.common.data.ExistingFileHelper helper) {
            super(location, helper);
        }
        @Override
        public net.minecraftforge.client.model.generators.ItemModelBuilder texture(String key, net.minecraft.resources.ResourceLocation texture) {
            if (texture.getNamespace().equals(PrefabGoetyAltars.MOD_ID)
                    && java.util.Arrays.stream(AltarPrefabType.values()).anyMatch(d -> texture.getPath().equals("item/" + d.id()))) {
                textures.put(key, texture.toString());
                return this;
            }
            return super.texture(key, texture);
        }
    }
    private ModItemModels() {}
}
