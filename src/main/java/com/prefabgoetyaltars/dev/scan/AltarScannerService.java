package com.prefabgoetyaltars.dev.scan;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.network.ModNetwork;
import com.prefabgoetyaltars.network.OpenAltarScannerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class AltarScannerService {
    public static final double MAX_DISTANCE_SQUARED = 16 * 16;
    public static boolean withinDistance(double distanceSquared) {
        return Double.isFinite(distanceSquared) && distanceSquared >= 0 && distanceSquared <= MAX_DISTANCE_SQUARED;
    }

    public static AltarStructureScannerBlockEntity accessible(ServerPlayer player, BlockPos pos) {
        if (player == null) throw new ScannerException(ScannerException.Reason.NO_SENDER, "Packet has no sender");
        var level = player.serverLevel();
        if (level == null) throw new ScannerException(ScannerException.Reason.NO_WORLD, "Player has no server world");
        if (pos == null) throw new ScannerException(ScannerException.Reason.INVALID_RANGE, "Missing scanner position");
        if (!level.hasChunkAt(pos)) throw new ScannerException(ScannerException.Reason.UNLOADED, "Scanner chunk is not loaded: " + pos);
        double distance = player.distanceToSqr(pos.getCenter());
        if (!withinDistance(distance)) throw new ScannerException(ScannerException.Reason.TOO_FAR, "Scanner distance squared: " + distance);
        if (!(level.getBlockState(pos).getBlock() instanceof AltarStructureScannerBlock))
            throw new ScannerException(ScannerException.Reason.WRONG_BLOCK, "Target block: " + level.getBlockState(pos));
        return requireScannerEntity(level.getBlockEntity(pos));
    }

    public static AltarStructureScannerBlockEntity requireScannerEntity(net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (!(entity instanceof AltarStructureScannerBlockEntity scanner))
            throw new ScannerException(ScannerException.Reason.MISSING_BLOCK_ENTITY, "Expected altar scanner BlockEntity, found " + entity);
        return scanner;
    }

    public static void open(ServerPlayer player, BlockPos pos) {
        try {
            var scanner = accessible(player, pos);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenAltarScannerPacket(pos, scanner.configuration().GetCompoundTag()));
        } catch (RuntimeException e) { fail(player, pos, "open", e); }
    }

    public static void submit(ServerPlayer player, BlockPos pos, CompoundTag tag, boolean scan) {
        try {
            var scanner = accessible(player, pos);
            var config = AltarStructureScannerConfig.fromTag(tag).anchored(pos, scanner.configuration().direction);
            config.validate();
            if (scan) config.structureZipName = AltarStructureScannerConfig.normalizeName(config.structureZipName);
            scanner.setConfiguration(config);
            if (scan) {
                java.nio.file.Path directory;
                try { directory = DevStructurePaths.getStructureOutputDirectory(); }
                catch (java.io.IOException | SecurityException e) { throw new ScannerException(ScannerException.Reason.OUTPUT_DIRECTORY, "Cannot resolve export directory", e); }
                var file = PrefabStructureExport.scan(player.serverLevel(), config, directory);
                player.sendSystemMessage(Component.translatable("scanner.prefab_goety_altars.exported", file.toAbsolutePath().normalize().toString()));
                PrefabGoetyAltars.LOGGER.info("Scanner export: player={} dimension={} scanner={} file={}", player.getGameProfile().getName(), player.level().dimension().location(), pos, file);
            } else player.sendSystemMessage(Component.translatable("scanner.prefab_goety_altars.saved"));
        } catch (Exception e) { fail(player, pos, scan ? "scan" : "save", e); }
    }

    private static void fail(ServerPlayer player, BlockPos pos, String operation, Exception error) {
        PrefabGoetyAltars.LOGGER.warn("Scanner {} failed: player={} dimension={} scanner={} reason={}", operation,
                player == null ? "<none>" : player.getGameProfile().getName(),
                player == null || player.level() == null ? "<none>" : player.level().dimension().location(), pos, error.getMessage(), error);
        if (player == null) return;
        if (error instanceof java.nio.file.FileAlreadyExistsException existing) {
            player.sendSystemMessage(Component.translatable("scanner.prefab_goety_altars.exists", existing.getFile()));
        } else if (error instanceof ScannerException scannerError) player.sendSystemMessage(scannerError.playerMessage());
        else player.sendSystemMessage(Component.translatable("scanner.prefab_goety_altars.error.write_failed"));
    }
    private AltarScannerService() {}
}
