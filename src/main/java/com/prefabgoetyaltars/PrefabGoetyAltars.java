// SPDX-License-Identifier: MIT
package com.prefabgoetyaltars;

import com.mojang.logging.LogUtils;
import com.prefabgoetyaltars.network.ModNetwork;
import com.prefabgoetyaltars.registry.ModCreativeTabs;
import com.prefabgoetyaltars.registry.ModItems;
import com.prefabgoetyaltars.registry.ModBlocks;
import com.prefabgoetyaltars.registry.ModBlockEntities;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(PrefabGoetyAltars.MOD_ID)
public final class PrefabGoetyAltars {
    public static final String MOD_ID = "prefab_goety_altars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PrefabGoetyAltars(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
        ModNetwork.register();
        bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(com.prefabgoetyaltars.structure.RitualDefinition::warnMissingResources));
    }
}
