package com.prefabgoetyaltars.structure;

import com.wuest.prefab.structures.base.BuildBlock;
import com.wuest.prefab.structures.base.BuildProperty;
import com.wuest.prefab.structures.base.Structure;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;

public final class AltarMaterialReplacement {
    public static void apply(Structure structure, GoetyAltarConfiguration config) {
        Block altar = target(config.altarVariant().altarId(), "dark_altar");
        Block pedestal = target(config.pedestalVariant().pedestalId(), "pedestal");
        for (BuildBlock block : structure.getBlocks()) replace(block, altar, pedestal);
    }
    private static Block target(ResourceLocation id, String entityId) {
        if (!ForgeRegistries.BLOCKS.containsKey(id)) throw new IllegalArgumentException("Missing material block: " + id);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        var type = ForgeRegistries.BLOCK_ENTITY_TYPES.getValue(new ResourceLocation("goety", entityId));
        if (block == null || type == null || !type.isValid(block.defaultBlockState()))
            throw new IllegalArgumentException("Incompatible material BlockEntityType: " + id);
        return block;
    }
    private static void replace(BuildBlock source, Block altar, Block pedestal) {
        Block target = AltarMaterialVariant.isAltar(source.getResourceLocation()) ? altar
                : AltarMaterialVariant.isPedestal(source.getResourceLocation()) ? pedestal : null;
        if (target != null) {
            ResourceLocation targetId = ForgeRegistries.BLOCKS.getKey(target);
            var properties = new ArrayList<BuildProperty>();
            for (var property : source.getProperties()) {
                if (accepts(target, property.getName(), property.getValue())) {
                    var copy = new BuildProperty(); copy.setName(property.getName()); copy.setValue(property.getValue()); properties.add(copy);
                }
            }
            if (!source.getBlockStateData().isEmpty()) {
                CompoundTag original = source.getBlockStateDataTag();
                if (original == null) throw new IllegalArgumentException("Invalid material BlockState NBT");
                CompoundTag tag = original.copy(); tag.putString("Name", targetId.toString());
                CompoundTag compatible = new CompoundTag();
                CompoundTag old = original.getCompound("Properties");
                for (String key : old.getAllKeys()) if (accepts(target, key, old.getString(key))) compatible.putString(key, old.getString(key));
                tag.put("Properties", compatible); source.setBlockStateData(tag);
            }
            source.setProperties(properties);
            source.setBlockDomain(targetId.getNamespace()); source.setBlockName(targetId.getPath());
            source.setBlockState(null); 
            source.blockPos = null; source.centerOfBlock = null;
        }
        if (source.getSubBlock() != null) replace(source.getSubBlock(), altar, pedestal);
    }
    private static boolean accepts(Block target, String name, String value) {
        var property = target.getStateDefinition().getProperty(name);
        return property != null && property.getValue(value).isPresent();
    }
    private AltarMaterialReplacement() {}
}
