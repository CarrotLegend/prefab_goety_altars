package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.dev.scan.AltarScannerService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record ScanAltarStructurePacket(BlockPos pos, CompoundTag config) {
    public static void encode(ScanAltarStructurePacket packet, FriendlyByteBuf buffer) { buffer.writeBlockPos(packet.pos); buffer.writeNbt(packet.config); }
    public static ScanAltarStructurePacket decode(FriendlyByteBuf buffer) { return new ScanAltarStructurePacket(buffer.readBlockPos(), buffer.readNbt()); }
    public static void handle(ScanAltarStructurePacket packet, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        AltarScannerService.submit(player, packet.pos, packet.config, true);
        context.get().setPacketHandled(true);
    }
}
