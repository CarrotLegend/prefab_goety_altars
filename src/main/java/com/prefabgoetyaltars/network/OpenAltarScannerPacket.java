package com.prefabgoetyaltars.network;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public record OpenAltarScannerPacket(BlockPos pos, CompoundTag config) {
    public static void encode(OpenAltarScannerPacket packet, FriendlyByteBuf buffer) { buffer.writeBlockPos(packet.pos); buffer.writeNbt(packet.config); }
    public static OpenAltarScannerPacket decode(FriendlyByteBuf buffer) { return new OpenAltarScannerPacket(buffer.readBlockPos(), buffer.readNbt()); }
    public static void handle(OpenAltarScannerPacket packet, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.prefabgoetyaltars.client.ScannerClient.open(packet));
        context.get().setPacketHandled(true);
    }
}
