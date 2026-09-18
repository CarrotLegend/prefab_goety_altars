package com.prefabgoetyaltars.structure;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.compat.goetydelight.GoetyDelightCompat;
import com.prefabgoetyaltars.compat.revelation.RevelationCompat;
import com.wuest.prefab.structures.base.BuildBlock;
import com.wuest.prefab.structures.base.PositionOffset;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.function.Predicate;

/** Removes only content owned by explicitly supported optional mods that are absent. */
public final class OptionalStructureContentFilter {
    private static final Map<String, String> OPTIONAL_NAMESPACES = Map.of(
            RevelationCompat.MOD_ID, RevelationCompat.MOD_ID,
            GoetyDelightCompat.MOD_ID, GoetyDelightCompat.MOD_ID);

    public static void apply(GoetyAltarStructure structure) { apply(structure, ModList.get()::isLoaded); }

    static void apply(GoetyAltarStructure structure, Predicate<String> loaded) {
        Set<OffsetKey> removed = new HashSet<>();
        ArrayList<BuildBlock> retained = new ArrayList<>();
        for (BuildBlock block : structure.getBlocks()) {
            BuildBlock filtered = filterBlock(block, loaded, removed);
            if (filtered != null) retained.add(filtered);
        }
        structure.setBlocks(retained);
        int tilesBefore = structure.tileEntities.size();
        structure.tileEntities.removeIf(tile -> removed.contains(OffsetKey.of(tile.getStartingPosition())));
        int entitiesBefore = structure.entities.size();
        structure.entities.removeIf(entity -> shouldRemove(entity.getEntityResource(), loaded));
        int removedTiles = tilesBefore - structure.tileEntities.size();
        int removedEntities = entitiesBefore - structure.entities.size();
        if (!removed.isEmpty() || removedTiles != 0 || removedEntities != 0) {
            PrefabGoetyAltars.LOGGER.info("Filtered optional altar content: blocks={} blockEntities={} entities={}",
                    removed.size(), removedTiles, removedEntities);
        }
    }

    private static BuildBlock filterBlock(BuildBlock block, Predicate<String> loaded, Set<OffsetKey> removed) {
        if (block == null || block.getStartingPosition() == null || block.getResourceLocation() == null)
            throw new IllegalArgumentException("Invalid structure block");
        ResourceLocation id = block.getResourceLocation();
        if (shouldRemove(id, loaded)) {
            collectPositions(block, removed);
            return null;
        }
        if (!ForgeRegistries.BLOCKS.containsKey(id))
            throw new IllegalArgumentException("Unavailable structure block: " + id);
        if (block.getSubBlock() != null) block.setSubBlock(filterBlock(block.getSubBlock(), loaded, removed));
        return block;
    }

    private static void collectPositions(BuildBlock block, Set<OffsetKey> removed) {
        if (block == null) return;
        if (block.getStartingPosition() != null) removed.add(OffsetKey.of(block.getStartingPosition()));
        collectPositions(block.getSubBlock(), removed);
    }

    private static boolean shouldRemove(ResourceLocation id, Predicate<String> loaded) {
        String modId = OPTIONAL_NAMESPACES.get(id.getNamespace());
        return modId != null && !loaded.test(modId);
    }

    static boolean isRegisteredEntity(ResourceLocation id) { return ForgeRegistries.ENTITY_TYPES.containsKey(id); }
    static boolean isRegisteredBlockEntity(ResourceLocation id) { return ForgeRegistries.BLOCK_ENTITY_TYPES.containsKey(id); }

    private record OffsetKey(int north, int south, int east, int west, int height) {
        static OffsetKey of(PositionOffset p) {
            return new OffsetKey(p.getNorthOffset(), p.getSouthOffset(), p.getEastOffset(), p.getWestOffset(), p.getHeightOffset());
        }
    }
    private OptionalStructureContentFilter() {}
}
