package com.prefabgoetyaltars.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class ModNetwork {
    private static final String PROTOCOL = "5";

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL);
        registrar.playToServer(BuildAltarPacket.TYPE, BuildAltarPacket.STREAM_CODEC, BuildAltarPacket::handle);
        registrar.playToServer(ScanAltarStructurePacket.TYPE, ScanAltarStructurePacket.STREAM_CODEC, ScanAltarStructurePacket::handle);
        registrar.playToServer(SyncAltarScannerPacket.TYPE, SyncAltarScannerPacket.STREAM_CODEC, SyncAltarScannerPacket::handle);
        registrar.playToClient(OpenAltarScannerPacket.TYPE, OpenAltarScannerPacket.STREAM_CODEC, OpenAltarScannerPacket::handle);
    }
    public static void sendToServer(CustomPacketPayload payload) { PacketDistributor.sendToServer(payload); }
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { PacketDistributor.sendToPlayer(player, payload); }
    private ModNetwork() {}
}
