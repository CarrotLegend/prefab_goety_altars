package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.dev.scan.AltarStructureScannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PrefabGoetyAltars.MOD_ID);
    public static final RegistryObject<AltarStructureScannerBlock> SCANNER = BLOCKS.register("altar_structure_scanner", AltarStructureScannerBlock::new);
    private ModBlocks() {}
}
