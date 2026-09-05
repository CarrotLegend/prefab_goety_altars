package com.prefabgoetyaltars.dev.scan;

import com.wuest.prefab.structures.base.BuildClear;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public record ScannerBounds(BlockPos corner, BlockPos otherCorner, BuildClear clear) {
    public static ScannerBounds of(AltarStructureScannerConfig config) {
        config.validate();
        BuildClear clear = new BuildClear();
        clear.getShape().setDirection(config.direction);
        clear.getShape().setWidth(config.blocksWide);
        clear.getShape().setLength(config.blocksLong);
        clear.getShape().setHeight(config.blocksTall);
        var shape = clear.getShape().Clone();
        shape.setWidth(shape.getWidth() - 1);
        shape.setLength(shape.getLength() - 1);
        var left = config.direction.getCounterClockWise();
        int down = Math.max(config.blocksDown, 0);
        clear.getStartingPosition().setHeightOffset(-down);
        clear.getStartingPosition().setHorizontalOffset(config.direction, config.blocksParallel);
        clear.getStartingPosition().setHorizontalOffset(left, config.blocksToTheLeft);
        var corner = config.blockPos.relative(left, config.blocksToTheLeft).relative(config.direction, config.blocksParallel).below(down);
        var other = corner.relative(config.direction, shape.getLength())
                .relative(config.direction.getClockWise(), shape.getWidth()).above(shape.getHeight());
        return new ScannerBounds(corner, other, clear);
    }

    public BlockPos min() { return new BlockPos(Math.min(corner.getX(), otherCorner.getX()), Math.min(corner.getY(), otherCorner.getY()), Math.min(corner.getZ(), otherCorner.getZ())); }
    public BlockPos max() { return new BlockPos(Math.max(corner.getX(), otherCorner.getX()), Math.max(corner.getY(), otherCorner.getY()), Math.max(corner.getZ(), otherCorner.getZ())); }
    public AABB box() { return new AABB(min(), max().offset(1, 1, 1)); }

    public boolean isLoadedAndInsideWorld(ServerLevel level) {
        try { validateWorld(level); return true; }
        catch (ScannerException e) { return false; }
    }

    public void validateWorld(ServerLevel level) {
        BlockPos min = min(), max = max();
        if (level.isOutsideBuildHeight(min) || level.isOutsideBuildHeight(max))
            throw new ScannerException(ScannerException.Reason.WORLD_HEIGHT, "Scan outside build height: " + min + " -> " + max);
        if (!level.getWorldBorder().isWithinBounds(min) || !level.getWorldBorder().isWithinBounds(max))
            throw new ScannerException(ScannerException.Reason.WORLD_BORDER, "Scan outside world border: " + min + " -> " + max);
        for (int x = min.getX() >> 4; x <= max.getX() >> 4; x++) {
            for (int z = min.getZ() >> 4; z <= max.getZ() >> 4; z++) {
                if (!level.hasChunk(x, z)) throw new ScannerException(ScannerException.Reason.UNLOADED, "Scan chunk not loaded: " + x + ", " + z);
            }
        }
    }
}
