package com.prefabgoetyaltars.structure;

import com.wuest.prefab.structures.base.BuildBlock;
import com.wuest.prefab.structures.base.Structure;
import com.wuest.prefab.structures.config.StructureConfiguration;
import com.wuest.prefab.structures.events.StructureEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.IOException;

public class GoetyAltarStructure extends Structure {
    private transient boolean mainBuildCompleted;

    public static GoetyAltarStructure load(RitualDefinition definition) throws IOException {
        if (!definition.isAvailable()) throw new IllegalArgumentException("Missing required mod: " + definition.requiredModId());
        try (var resource = GoetyAltarStructure.class.getClassLoader().getResourceAsStream(definition.structureResource())) {
            if (resource == null) throw new IOException("Missing ritual structure: " + definition.structureResource());
        }
        try {
            GoetyAltarStructure structure = Structure.CreateInstance(definition.structureResource(), GoetyAltarStructure.class);
            if (structure == null) throw new IllegalArgumentException("Empty structure");
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
        }
        for (var entity : entities) {
            if (entity == null || entity.getStartingPosition() == null) {
                throw new IllegalArgumentException("Invalid entity data");
            }
        }
    }

    private static void validateBlock(BuildBlock block) {
        if (block == null || block.getStartingPosition() == null || block.getProperties() == null
                || block.getResourceLocation() == null || !ForgeRegistries.BLOCKS.containsKey(block.getResourceLocation())) {
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
    public boolean BuildStructure(StructureConfiguration configuration, ServerLevel world, BlockPos originalPos, Player player) {
        mainBuildCompleted = false;
        boolean success = false;
        try {
            success = super.BuildStructure(configuration, world, originalPos, player) && mainBuildCompleted;
            return success;
        } finally {
            if (!success) {
                var queued = StructureEventHandler.structuresToBuild.get(player);
                if (queued != null) {
                    queued.remove(this);
                    if (queued.isEmpty()) StructureEventHandler.structuresToBuild.remove(player);
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
