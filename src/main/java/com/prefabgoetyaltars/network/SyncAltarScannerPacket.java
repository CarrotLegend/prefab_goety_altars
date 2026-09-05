package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.dev.scan.AltarScannerService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record SyncAltarScannerPacket(BlockPos pos, CompoundTag config) {
    public static void encode(SyncAltarScannerPacket packet, FriendlyByteBuf buffer) { buffer.writeBlockPos(packet.pos); buffer.writeNbt(packet.config); }
    public static SyncAltarScannerPacket decode(FriendlyByteBuf buffer) { return new SyncAltarScannerPacket(buffer.readBlockPos(), buffer.readNbt()); }
    public static void handle(SyncAltarScannerPacket packet, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        AltarScannerService.submit(player, packet.pos, packet.config, false);
        context.get().setPacketHandled(true);
    }
}
