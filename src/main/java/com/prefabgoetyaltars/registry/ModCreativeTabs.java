package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.wuest.prefab.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PrefabGoetyAltars.MOD_ID);
    public static final RegistryObject<CreativeModeTab> ALTARS = TABS.register("altars", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.prefab_goety_altars"))
            .icon(() -> new ItemStack(ModRegistry.StartHouse.get()))
            .displayItems((parameters, output) -> {
                ModItems.ALTARS.forEach(item -> {
                    if (((com.prefabgoetyaltars.item.GoetyAltarItem) item.get()).prefabType().isAvailable()) output.accept(item.get());
                });
                if (!FMLEnvironment.production) output.accept(ModItems.SCANNER.get());
            })
            .build());
    private ModCreativeTabs() {}
}
