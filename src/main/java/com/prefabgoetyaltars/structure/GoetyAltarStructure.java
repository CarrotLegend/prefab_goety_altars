package com.prefabgoetyaltars.structure;

import com.prefab.PrefabBase;
import com.prefab.structures.base.BuildBlock;
import com.prefab.structures.base.BuildProperty;
import com.prefab.structures.base.Structure;
import com.prefab.structures.config.StructureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public class GoetyAltarStructure extends Structure {
    private transient boolean mainBuildCompleted;
    private transient Map<BuildBlock, OriginalWaterState> originalWaterStates = new IdentityHashMap<>();

    private record OriginalWaterState(BuildProperty property, String propertyValue, BuildProperty addedProperty,
                                      String stateData, BlockState cachedState) {
        void restore(BuildBlock block) {
            if (addedProperty != null) block.getProperties().remove(addedProperty);
            if (property != null) property.setValue(propertyValue);
            block.setBlockStateData(stateData);
            block.setBlockState(cachedState);
        }
    }

    public static GoetyAltarStructure load(RitualDefinition definition) throws IOException {
        if (!definition.isAvailable()) throw new IllegalArgumentException("Missing required mod: " + definition.requiredModId());
        try (var resource = GoetyAltarStructure.class.getClassLoader().getResourceAsStream(definition.structureResource())) {
            if (resource == null) throw new IOException("Missing ritual structure: " + definition.structureResource());
        }
        try {
            GoetyAltarStructure structure = Structure.CreateInstance(definition.structureResource(), GoetyAltarStructure.class);
            if (structure == null) throw new IllegalArgumentException("Empty structure");
            OptionalStructureContentFilter.apply(structure);
            structure.validateDefinition();
            return structure;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid Prefab structure: " + definition.structureResource(), exception);
        }
    }

    private void validateDefinition() {
        if (getClearSpace() == null || getClearSpace().getShape() == null
                || getClearSpace().getStartingPosition() == null || getBlocks() == null || getBlocks().isEmpty()
                || tileEntities == null || entities == null) {
            throw new IllegalArgumentException("Missing structure data");
        }
        var shape = getClearSpace().getShape();
        if (shape.getDirection() == null || !shape.getDirection().getAxis().isHorizontal()
                || shape.getWidth() <= 0 || shape.getLength() <= 0 || shape.getHeight() <= 0) {
            throw new IllegalArgumentException("Invalid structure bounds");
        }
        for (BuildBlock block : getBlocks()) validateBlock(block);
        for (var tile : tileEntities) {
            if (tile == null || tile.getStartingPosition() == null || tile.getEntityDataTag() == null) {
                throw new IllegalArgumentException("Invalid block entity data");
            }
            var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(tile.getEntityDomain(), tile.getEntityName());
            if (!OptionalStructureContentFilter.isRegisteredBlockEntity(id))
                throw new IllegalArgumentException("Unavailable block entity: " + id);
        }
        for (var entity : entities) {
            if (entity == null || entity.getStartingPosition() == null || entity.getEntityDataTag() == null) {
                throw new IllegalArgumentException("Invalid entity data");
            }
            if (!OptionalStructureContentFilter.isRegisteredEntity(entity.getEntityResource()))
                throw new IllegalArgumentException("Unavailable entity: " + entity.getEntityResource());
        }
    }

    private static void validateBlock(BuildBlock block) {
        if (block == null || block.getStartingPosition() == null || block.getProperties() == null
                || block.getResourceLocation() == null || !BuiltInRegistries.BLOCK.containsKey(block.getResourceLocation())) {
            throw new IllegalArgumentException("Invalid or unavailable structure block");
        }
        if (block.getSubBlock() != null) validateBlock(block.getSubBlock());
    }

    public boolean canAccessFootprint(ServerLevel level, GoetyAltarConfiguration config, Player player) {
        var shape = getClearSpace().getShape();
        BlockPos start = getClearSpace().getStartingPosition().getRelativePosition(config.pos, shape.getDirection(), config.houseFacing);
        BlockPos end = start.relative(config.houseFacing.getCounterClockWise(), shape.getWidth() - 1)
                .relative(config.houseFacing.getOpposite(), shape.getLength() - 1).above(shape.getHeight());
        if (!allowed(level, start, player) || !allowed(level, end, player)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(start, end)) if (!allowed(level, pos, player)) return false;
        for (var block : getBlocks()) if (!blockAllowed(level, config, player, block)) return false;
        for (var tile : tileEntities) {
            if (!allowed(level, tile.getStartingPosition().getRelativePosition(config.pos, shape.getDirection(), config.houseFacing), player)) return false;
        }
        for (var entity : entities) {
            if (!allowed(level, entity.getStartingPosition().getRelativePosition(config.pos, shape.getDirection(), config.houseFacing), player)) return false;
        }
        return true;
    }

    private boolean blockAllowed(ServerLevel level, GoetyAltarConfiguration config, Player player, BuildBlock block) {
        return allowed(level, block.getStartingPosition().getRelativePosition(config.pos, getClearSpace().getShape().getDirection(), config.houseFacing), player)
                && (block.getSubBlock() == null || blockAllowed(level, config, player, block.getSubBlock()));
    }

    private static boolean allowed(ServerLevel level, BlockPos pos, Player player) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.hasChunkAt(pos) && level.mayInteract(player, pos);
    }

    @Override
    protected boolean BeforeBuilding(StructureConfiguration configuration, net.minecraft.world.level.Level world, BlockPos originalPos, Player player) {
        AltarMaterialReplacement.apply(this, (GoetyAltarConfiguration) configuration);
        return super.BeforeBuilding(configuration, world, originalPos, player);
    }

    @Override
    protected Boolean WaterReplacedWithCobbleStone(StructureConfiguration configuration, BuildBlock buildBlock,
            Level level, BlockPos originalPos, Block block, BlockState blockState, Player player) {
        var id = buildBlock.getResourceLocation();
        if (id == null || !id.getNamespace().equals("goety") || !id.getPath().equals("dark_altar")
                || !level.dimensionType().ultraWarm() || !blockState.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return super.WaterReplacedWithCobbleStone(configuration, buildBlock, level, originalPos, block, blockState, player);
        }

        BuildProperty waterProperty = buildBlock.getProperty(BlockStateProperties.WATERLOGGED.getName());
        String stateData = buildBlock.getBlockStateData();
        BlockState taggedState = stateData.isEmpty() ? null : buildBlock.getBlockStateFromDataTag();
        if (taggedState != null && taggedState.getBlock() != block) {
            return super.WaterReplacedWithCobbleStone(configuration, buildBlock, level, originalPos, block, blockState, player);
        }
        boolean waterlogged = taggedState != null
                ? taggedState.getValue(BlockStateProperties.WATERLOGGED)
                : waterProperty != null ? Boolean.parseBoolean(waterProperty.getValue())
                : blockState.getValue(BlockStateProperties.WATERLOGGED);
        boolean propertyIsWet = waterProperty != null && Boolean.parseBoolean(waterProperty.getValue());
        if (waterlogged || propertyIsWet) {
            BuildProperty addedProperty = null;
            originalWaterStates.putIfAbsent(buildBlock, new OriginalWaterState(waterProperty,
                    waterProperty == null ? null : waterProperty.getValue(), null, stateData, buildBlock.getBlockState()));
            if (propertyIsWet) {
                waterProperty.setValue("false");
            } else if (waterlogged && taggedState == null && waterProperty == null) {
                addedProperty = new BuildProperty();
                addedProperty.setName(BlockStateProperties.WATERLOGGED.getName());
                addedProperty.setValue("false");
                buildBlock.getProperties().add(addedProperty);
            }
            if (taggedState != null && waterlogged) {
                CompoundTag tag = buildBlock.getBlockStateDataTag().copy();
                CompoundTag properties = tag.getCompound("Properties");
                properties.putString(BlockStateProperties.WATERLOGGED.getName(), "false");
                tag.put("Properties", properties);
                buildBlock.setBlockStateData(tag);
            }
            if (addedProperty != null) {
                OriginalWaterState original = originalWaterStates.get(buildBlock);
                originalWaterStates.put(buildBlock, new OriginalWaterState(original.property(), original.propertyValue(),
                        addedProperty, original.stateData(), original.cachedState()));
            }
        }
        return super.WaterReplacedWithCobbleStone(configuration, buildBlock, level, originalPos, block, blockState, player);
    }

    @Override
    public boolean BuildStructure(StructureConfiguration configuration, ServerLevel world, BlockPos originalPos, Player player) {
        mainBuildCompleted = false;
        originalWaterStates = new IdentityHashMap<>();
        boolean success = false;
        try {
            success = super.BuildStructure(configuration, world, originalPos, player) && mainBuildCompleted;
            return success;
        } finally {
            originalWaterStates.forEach((block, original) -> original.restore(block));
            originalWaterStates.clear();
            if (!success) {
                var queued = PrefabBase.structuresToBuild.get(player);
                if (queued != null) {
                    queued.remove(this);
                    if (queued.isEmpty()) PrefabBase.structuresToBuild.remove(player);
                }
            }
        }
    }

    @Override
    public void AfterBuilding(StructureConfiguration configuration, ServerLevel world, BlockPos originalPos, Player player) {
        super.AfterBuilding(configuration, world, originalPos, player);
        mainBuildCompleted = true;
    }
}
