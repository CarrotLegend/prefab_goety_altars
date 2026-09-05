package com.prefabgoetyaltars.dev.scan;

import com.wuest.prefab.config.StructureScannerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.Locale;

public final class AltarStructureScannerConfig extends StructureScannerConfig {
    public static final int MAX_DIMENSION = 128;
    public static final long MAX_VOLUME = 1_000_000;

    public AltarStructureScannerConfig copy() {
        return fromTag(GetCompoundTag());
    }

    public AltarStructureScannerConfig anchored(BlockPos pos, Direction facing) {
        var result = copy();
        result.blockPos = pos.immutable();
        result.direction = facing;
        return result;
    }

    public static AltarStructureScannerConfig fromTag(CompoundTag tag) {
        if (tag == null) throw new ScannerException(ScannerException.Reason.INVALID_RANGE, "Missing scanner configuration");
        for (String key : new String[]{"blocksToTheLeft", "blocksParallel", "blocksDown", "blocksWide", "blocksLong", "blocksTall", "direction"}) {
            if (!tag.contains(key, Tag.TAG_INT)) throw new ScannerException(ScannerException.Reason.INVALID_RANGE, "Missing scanner field: " + key);
        }
        if (!tag.contains("structureZipName", Tag.TAG_STRING)) throw new ScannerException(ScannerException.Reason.INVALID_NAME, "Missing structure name");
        if (!tag.contains("pos", Tag.TAG_COMPOUND)) {
            throw new ScannerException(ScannerException.Reason.INVALID_RANGE, "Missing scanner position");
        }
        int facing = tag.getInt("direction");
        if (facing < 2 || facing > 5) throw new ScannerException(ScannerException.Reason.INVALID_RANGE, "Scanner direction must be horizontal");
        var result = new AltarStructureScannerConfig();
        result.ReadFromCompoundTag(tag);
        result.validate();
        return result;
    }

    public void validate() {
        if (structureZipName == null || structureZipName.length() > 128) throw new ScannerException(ScannerException.Reason.INVALID_NAME, "Invalid structure name length");
        if (blocksToTheLeft < -128 || blocksToTheLeft > 128 || blocksParallel < -128 || blocksParallel > 128
                || blocksDown < 0 || blocksDown > 128 || blocksWide < 1 || blocksWide > MAX_DIMENSION
                || blocksLong < 1 || blocksLong > MAX_DIMENSION || blocksTall < 1 || blocksTall > MAX_DIMENSION
                || volume() > MAX_VOLUME || blockPos == null || direction == null || !direction.getAxis().isHorizontal()
                ) {
            throw new ScannerException(ScannerException.Reason.INVALID_RANGE, "Invalid scanner range: offsets=" + blocksToTheLeft + "," + blocksParallel + "," + blocksDown + "; dimensions=" + blocksWide + "x" + blocksLong + "x" + blocksTall);
        }
    }

    public long volume() { return (long) blocksWide * blocksLong * (blocksTall + 1L); }

    public static String normalizeName(String input) {
        if (input == null || input.length() > 128) throw new ScannerException(ScannerException.Reason.INVALID_NAME, "Invalid structure name length");
        String name = input.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_");
        if (name.isEmpty() || name.matches("_+") || !name.matches("[a-z0-9_]+")
                || name.matches("con|prn|aux|nul|com[1-9]|lpt[1-9]")) {
            throw new ScannerException(ScannerException.Reason.INVALID_NAME, "Invalid or reserved structure name");
        }
        return name;
    }
}


