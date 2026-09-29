package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefab.neoforge.ModRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.loading.FMLEnvironment;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PrefabGoetyAltars.MOD_ID);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALTARS = TABS.register("altars", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.prefab_goety_altars"))
            .icon(() -> new ItemStack(ModRegistry.House))
            .displayItems((parameters, output) -> {
                ModItems.ALTARS.forEach(item -> output.accept(item.get()));
                if (!FMLEnvironment.production) output.accept(ModItems.SCANNER.get());
            })
            .build());
    private ModCreativeTabs() {}
}
