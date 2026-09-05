package com.prefabgoetyaltars.smoke;

import com.prefabgoetyaltars.structure.*;
import com.wuest.prefab.structures.base.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Registry-backed tests, isolated from release sources and without world writes. */
final class MaterialSmokeChecks {
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    static void run() {
        try {
            for (var altar : AltarMaterialVariant.values()) for (var pedestal : AltarMaterialVariant.values()) {
                var structure = GoetyAltarStructure.load(RitualDefinition.find("animation_ritual").orElseThrow());
                var nbt = structure.tileEntities.stream().map(t -> t.getEntityDataTag().copy()).toList();
                var others = new IdentityHashMap<BuildBlock, String>();
                for (var block : structure.getBlocks()) if (!AltarMaterialVariant.isAltar(block.getResourceLocation()) && !AltarMaterialVariant.isPedestal(block.getResourceLocation())) others.put(block, snapshot(block));
                var config = new GoetyAltarConfiguration(AltarPrefabType.RITUAL, "animation_ritual", altar, pedestal, BlockPos.ZERO, Direction.NORTH);
                AltarMaterialReplacement.apply(structure, config);
                int a = 0, p = 0;
                for (var block : structure.getBlocks()) {
                    if (AltarMaterialVariant.isAltar(block.getResourceLocation())) { check(block.getResourceLocation().equals(altar.altarId()), "altar independent choice"); a++; }
                    if (AltarMaterialVariant.isPedestal(block.getResourceLocation())) { check(block.getResourceLocation().equals(pedestal.pedestalId()), "pedestal independent choice"); p++; }
                    if (others.containsKey(block)) check(others.get(block).equals(snapshot(block)), "unrelated block changed");
                }
                check(a == 1 && p == 12, "family counts");
                check(nbt.equals(structure.tileEntities.stream().map(t -> t.getEntityDataTag()).toList()), "BlockEntity NBT changed");
            }
            var structure = GoetyAltarStructure.load(RitualDefinition.find("animation_ritual").orElseThrow());
            var parent = new BuildBlock(); parent.setBlockDomain("minecraft"); parent.setBlockName("stone");
            var child = new BuildBlock(); child.setBlockDomain("goety"); child.setBlockName("dark_altar");
            var tag = new CompoundTag(); tag.putString("Name", "goety:dark_altar");
            var properties = new CompoundTag(); properties.putString("lit", "true"); properties.putString("waterlogged", "true"); properties.putString("facing", "south");
            tag.put("Properties", properties); child.setBlockStateData(tag); child.setBlockState(Blocks.STONE.defaultBlockState()); parent.setSubBlock(child); structure.getBlocks().add(parent);
            AltarMaterialReplacement.apply(structure, new GoetyAltarConfiguration(AltarPrefabType.RITUAL, "animation_ritual", AltarMaterialVariant.MARBLE, AltarMaterialVariant.STONE, BlockPos.ZERO, Direction.EAST));
            check(child.getResourceLocation().equals(AltarMaterialVariant.MARBLE.altarId()) && child.getBlockState() == null, "recursive cache invalidation");
            check(child.getBlockStateDataTag().getString("Name").equals("goety:dark_altar_marble"), "BlockState NBT Name");
            check(child.getBlockStateDataTag().getCompound("Properties").getString("lit").equals("true") && !child.getBlockStateDataTag().getCompound("Properties").contains("facing"), "compatible NBT properties");
            for (var variant : AltarMaterialVariant.values()) {
                var altar = ForgeRegistries.BLOCKS.getValue(variant.altarId()); var pedestal = ForgeRegistries.BLOCKS.getValue(variant.pedestalId());
                check(altar.getStateDefinition().getProperties().stream().map(p -> p.getName()).collect(java.util.stream.Collectors.toSet()).equals(Set.of("lit","waterlogged","occupied")), "altar properties");
                check(pedestal.getStateDefinition().getProperties().stream().map(p -> p.getName()).collect(java.util.stream.Collectors.toSet()).equals(Set.of("waterlogged","occupied")), "pedestal properties");
            }
            try { GoetyAltarStructure.load(RitualDefinition.named("missing_test_only", null)); throw new IllegalStateException("Missing resource accepted"); } catch (java.io.IOException expected) {}
            if (!com.prefabgoetyaltars.compat.revelation.RevelationCompat.isLoaded()) {
                check(!AltarPrefabType.REVELATION_ALL_RITUAL.isAvailable() && !AltarPrefabType.MASTER_FORGE_RITUAL.isAvailable(), "Revelation type gates");
                try { GoetyAltarStructure.load(AltarPrefabType.MASTER_FORGE_RITUAL.resolve("ignored")); throw new IllegalStateException("Missing dependency accepted"); } catch (IllegalArgumentException expected) {}
            }
            com.mojang.logging.LogUtils.getLogger().info("ALTAR_MATERIAL_MATRIX_OK: 121 combinations, 22 blocks/types/properties, untouched NBT/decorations, recursive BlockState NBT, missing resource/dependency gates");
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    private static String snapshot(BuildBlock block) {
        return block.getResourceLocation() + ":" + block.getBlockStateData() + ":" + block.getProperties().stream().map(p -> p.getName() + "=" + p.getValue()).toList();
    }
}
