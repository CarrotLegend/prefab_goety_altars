package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.dev.scan.AltarStructureScannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PrefabGoetyAltars.MOD_ID);
    public static final RegistryObject<BlockEntityType<AltarStructureScannerBlockEntity>> SCANNER = BLOCK_ENTITIES.register("altar_structure_scanner",
            () -> BlockEntityType.Builder.of(AltarStructureScannerBlockEntity::new, ModBlocks.SCANNER.get()).build(null));
    private ModBlockEntities() {}
}
