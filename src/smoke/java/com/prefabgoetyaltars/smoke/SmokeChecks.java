// SPDX-License-Identifier: MIT
package com.prefabgoetyaltars.smoke;

import com.mojang.logging.LogUtils;
import com.prefabgoetyaltars.registry.ModCreativeTabs;
import com.prefabgoetyaltars.registry.ModItems;
import com.prefabgoetyaltars.structure.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SmokeChecks.MOD_ID)
public final class SmokeChecks {
    public static final String MOD_ID = "prefab_goety_altars_smoke";
    public SmokeChecks(FMLJavaModLoadingContext context) {
        context.getModEventBus().addListener(this::loaded);
    }
    private void loaded(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            if (ModItems.ALTARS.size() != 4) throw new IllegalStateException("Definition/registration count mismatch");
            if (ModCreativeTabs.ALTARS.get().getIconItem().isEmpty()) throw new IllegalStateException("Missing creative tab icon");
            LogUtils.getLogger().info("ALTAR_SMOKE_COMMON_LOAD_OK: {} registered definitions and bound creative tab icon", ModItems.ALTARS.size());
            var definitions = new java.util.ArrayList<>(RitualDefinition.available());
            for (var type : AltarPrefabType.values()) if (type != AltarPrefabType.RITUAL && type.isAvailable()) definitions.add(type.resolve("ignored"));
            for (var definition : definitions) {
                try {
                    var structure = com.prefabgoetyaltars.structure.GoetyAltarStructure.load(definition);
                    LogUtils.getLogger().info("ALTAR_NATIVE_LOAD_OK: {} blocks={} blockEntities={}", definition.id(), structure.getBlocks().size(), structure.tileEntities.size());
                } catch (java.io.IOException e) { throw new IllegalStateException(definition.structureResource(), e); }
            }
            LogUtils.getLogger().info("ALTAR_REVELATION_PRESENT={}", com.prefabgoetyaltars.compat.revelation.RevelationCompat.isLoaded());
            MaterialSmokeChecks.run();
            if (Boolean.getBoolean("altar.scannerClasspathCheck")) {
                var structure = com.wuest.prefab.structures.base.Structure.CreateInstance("assets/prefab_goety_altars_smoke/scanner_fixture.gz", com.wuest.prefab.structures.base.Structure.class);
                if (structure == null || structure.getBlocks().size() != 3 || structure.tileEntities.size() != 1) throw new IllegalStateException("Scanner classpath fixture failed");
                LogUtils.getLogger().info("SCANNER_SMOKE_CLASSPATH_OK: native Structure.CreateInstance loaded processed fixture");
            }
        });
    }
}
