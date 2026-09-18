package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "4";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PrefabGoetyAltars.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.messageBuilder(BuildAltarPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuildAltarPacket::encode).decoder(BuildAltarPacket::decode)
                .consumerMainThread(BuildAltarPacket::handle).add();
        CHANNEL.messageBuilder(ScanAltarStructurePacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScanAltarStructurePacket::encode).decoder(ScanAltarStructurePacket::decode)
                .consumerMainThread(ScanAltarStructurePacket::handle).add();
        CHANNEL.messageBuilder(SyncAltarScannerPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SyncAltarScannerPacket::encode).decoder(SyncAltarScannerPacket::decode)
                .consumerMainThread(SyncAltarScannerPacket::handle).add();
        CHANNEL.messageBuilder(OpenAltarScannerPacket.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAltarScannerPacket::encode).decoder(OpenAltarScannerPacket::decode)
                .consumerMainThread(OpenAltarScannerPacket::handle).add();
    }
    private ModNetwork() {}
}
