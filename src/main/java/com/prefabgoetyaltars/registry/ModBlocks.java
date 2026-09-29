package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.dev.scan.AltarStructureScannerBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, PrefabGoetyAltars.MOD_ID);
    public static final DeferredHolder<Block, AltarStructureScannerBlock> SCANNER = BLOCKS.register("altar_structure_scanner", AltarStructureScannerBlock::new);
    private ModBlocks() {}
}
