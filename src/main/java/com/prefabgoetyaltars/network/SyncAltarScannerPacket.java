package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.dev.scan.AltarScannerService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncAltarScannerPacket(BlockPos pos, CompoundTag config) implements CustomPacketPayload {
    public static final Type<SyncAltarScannerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrefabGoetyAltars.MOD_ID, "sync_altar_scanner"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncAltarScannerPacket> STREAM_CODEC = StreamCodec.ofMember(SyncAltarScannerPacket::write, SyncAltarScannerPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeNbt(config); }
    private static SyncAltarScannerPacket decode(RegistryFriendlyByteBuf buffer) { return new SyncAltarScannerPacket(buffer.readBlockPos(), buffer.readNbt()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(SyncAltarScannerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) AltarScannerService.submit(player, packet.pos, packet.config, false); });
    }
}
