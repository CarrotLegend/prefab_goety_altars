package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.item.GoetyAltarItem;
import com.prefabgoetyaltars.structure.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import java.io.IOException;
import java.util.Objects;

public record BuildAltarPacket(String prefabType, String ritualId, String altarVariant, String pedestalVariant,
                                BlockPos pos, Direction facing, InteractionHand hand, CompoundTag configuration) implements CustomPacketPayload {
    public static final Type<BuildAltarPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(PrefabGoetyAltars.MOD_ID, "build_altar"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildAltarPacket> STREAM_CODEC = StreamCodec.ofMember(BuildAltarPacket::write, BuildAltarPacket::decode);
    public BuildAltarPacket {
        Objects.requireNonNull(prefabType); Objects.requireNonNull(ritualId); Objects.requireNonNull(altarVariant);
        Objects.requireNonNull(pedestalVariant); Objects.requireNonNull(facing); Objects.requireNonNull(hand);
        pos = pos.immutable(); configuration = configuration == null ? new CompoundTag() : configuration.copy();
    }
    public static BuildAltarPacket from(GoetyAltarConfiguration config, InteractionHand hand) {
        return new BuildAltarPacket(config.prefabType().id(), config.ritualId(), config.altarVariant().id(),
                config.pedestalVariant().id(), config.pos, config.houseFacing, hand, config.WriteToCompoundTag());
    }
    private void write(RegistryFriendlyByteBuf b) {
        b.writeUtf(prefabType, 64); b.writeUtf(ritualId, 128); b.writeUtf(altarVariant, 64); b.writeUtf(pedestalVariant, 64);
        b.writeBlockPos(pos); b.writeUtf(facing.getSerializedName(), 16);
        b.writeUtf(hand == InteractionHand.MAIN_HAND ? "main_hand" : "off_hand", 16); b.writeNbt(configuration);
    }
    private static BuildAltarPacket decode(RegistryFriendlyByteBuf b) {
        String type = b.readUtf(64), ritual = b.readUtf(128), altar = b.readUtf(64), pedestal = b.readUtf(64);
        BlockPos pos = b.readBlockPos(); Direction direction = Direction.byName(b.readUtf(16));
        if (direction == null || !direction.getAxis().isHorizontal()) throw new IllegalArgumentException("Invalid direction");
        InteractionHand hand = switch (b.readUtf(16)) {
            case "main_hand" -> InteractionHand.MAIN_HAND; case "off_hand" -> InteractionHand.OFF_HAND;
            default -> throw new IllegalArgumentException("Invalid interaction hand");
        };
        return new BuildAltarPacket(type, ritual, altar, pedestal, pos, direction, hand, b.readNbt());
    }
    public GoetyAltarConfiguration validatedConfiguration() {
        var config = new GoetyAltarConfiguration().ReadFromCompoundTag(configuration);
        if (AltarPrefabType.fromId(prefabType) != config.prefabType()
                || AltarMaterialVariant.fromId(altarVariant) != config.altarVariant()
                || AltarMaterialVariant.fromId(pedestalVariant) != config.pedestalVariant()
                || !pos.equals(config.pos) || facing != config.houseFacing
                || (config.prefabType() == AltarPrefabType.RITUAL && !ritualId.equals(config.ritualId())))
            throw new IllegalArgumentException("Packet and configuration disagree");
        return config;
    }
    public boolean matchesHeldItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GoetyAltarItem item && item.prefabType().id().equals(prefabType);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(BuildAltarPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) packet.build(player); });
    }
    private void build(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !player.getAbilities().mayBuild) return;
        if (!matchesHeldItem(player.getItemInHand(hand))) {
            PrefabGoetyAltars.LOGGER.warn("Rejected altar request: held item/type mismatch type={} hand={} player={}", prefabType, hand, player.getGameProfile().getName());
            player.sendSystemMessage(Component.translatable("message.prefab_goety_altars.invalid_request"));
            return;
        }
        var actualType = ((GoetyAltarItem) player.getItemInHand(hand).getItem()).prefabType();
        var world = player.serverLevel();
        if (!world.hasChunkAt(pos) || world.isOutsideBuildHeight(pos) || !world.getWorldBorder().isWithinBounds(pos)
                || !world.mayInteract(player, pos) || player.distanceToSqr(pos.getCenter()) > 64.0) return;
        RitualDefinition definition = null;
        try {
            var config = validatedConfiguration();
            definition = actualType.resolve(config.ritualId()); // Fixed types never use the client ritual ID.
            if (!definition.isAvailable()) {
                player.sendSystemMessage(Component.translatable("message.prefab_goety_altars.missing_dependency", definition.requiredModId()));
                PrefabGoetyAltars.LOGGER.warn("Rejected ritual {}: missing required mod {}", definition.id(), definition.requiredModId()); return;
            }
            var structure = GoetyAltarStructure.load(definition);
            if (!structure.canAccessFootprint(world, config, player)) {
                player.sendSystemMessage(Component.translatable("message.prefab_goety_altars.inaccessible")); return;
            }
            if (structure.BuildStructure(config, world, pos, player)) {
                player.getItemInHand(hand).shrink(1); player.containerMenu.broadcastChanges();
            } else player.sendSystemMessage(Component.translatable("message.prefab_goety_altars.build_failed"));
        } catch (IOException e) {
            PrefabGoetyAltars.LOGGER.warn("Missing or invalid ritual structure: {} for {}", definition == null ? "unresolved" : definition.structureResource(), player.getGameProfile().getName(), e);
            player.sendSystemMessage(Component.translatable("message.prefab_goety_altars.load_failed"));
        } catch (RuntimeException e) {
            PrefabGoetyAltars.LOGGER.warn("Rejected altar request type={} ritual={} player={}", prefabType, ritualId, player.getGameProfile().getName(), e);
            player.sendSystemMessage(Component.translatable("message.prefab_goety_altars.invalid_request"));
        }
    }
}
