package com.prefabgoetyaltars.client;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.structure.AltarPrefabType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT)
public final class ModItemModels {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new net.neoforged.neoforge.client.model.generators.ModelProvider<net.neoforged.neoforge.client.model.generators.ItemModelBuilder>(
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
    private static final class OptionalTextureModel extends net.neoforged.neoforge.client.model.generators.ItemModelBuilder {
        OptionalTextureModel(net.minecraft.resources.ResourceLocation location, net.neoforged.neoforge.common.data.ExistingFileHelper helper) {
            super(location, helper);
        }
        @Override
        public net.neoforged.neoforge.client.model.generators.ItemModelBuilder texture(String key, net.minecraft.resources.ResourceLocation texture) {
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
