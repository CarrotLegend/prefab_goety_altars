package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenAltarScannerPacket(BlockPos pos, CompoundTag config) implements CustomPacketPayload {
    public static final Type<OpenAltarScannerPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrefabGoetyAltars.MOD_ID, "open_altar_scanner"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenAltarScannerPacket> STREAM_CODEC = StreamCodec.ofMember(OpenAltarScannerPacket::write, OpenAltarScannerPacket::decode);
    private void write(RegistryFriendlyByteBuf buffer) { buffer.writeBlockPos(pos); buffer.writeNbt(config); }
    private static OpenAltarScannerPacket decode(RegistryFriendlyByteBuf buffer) { return new OpenAltarScannerPacket(buffer.readBlockPos(), buffer.readNbt()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(OpenAltarScannerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> com.prefabgoetyaltars.client.ScannerClient.open(packet));
    }
}
