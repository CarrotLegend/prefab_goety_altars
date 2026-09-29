package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.dev.scan.AltarStructureScannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PrefabGoetyAltars.MOD_ID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AltarStructureScannerBlockEntity>> SCANNER = BLOCK_ENTITIES.register("altar_structure_scanner",
            () -> BlockEntityType.Builder.of(AltarStructureScannerBlockEntity::new, ModBlocks.SCANNER.get()).build(null));
    private ModBlockEntities() {}
}
