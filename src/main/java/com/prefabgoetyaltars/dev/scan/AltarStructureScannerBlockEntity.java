package com.prefabgoetyaltars.dev.scan;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class AltarStructureScannerBlockEntity extends BlockEntity {
    private AltarStructureScannerConfig configuration = new AltarStructureScannerConfig();
    public AltarStructureScannerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.SCANNER.get(), pos, state); }
    public AltarStructureScannerConfig configuration() { return configuration.anchored(worldPosition, getBlockState().getValue(AltarStructureScannerBlock.FACING)); }
    public void setConfiguration(AltarStructureScannerConfig config) {
        configuration = config.anchored(worldPosition, getBlockState().getValue(AltarStructureScannerBlock.FACING));
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("scannerConfig", configuration().GetCompoundTag());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        configuration = new AltarStructureScannerConfig();
        if (tag.contains("scannerConfig")) {
            try { configuration = AltarStructureScannerConfig.fromTag(tag.getCompound("scannerConfig")); }
            catch (IllegalArgumentException exception) { PrefabGoetyAltars.LOGGER.warn("Reset invalid scanner configuration at {}", worldPosition, exception); }
        }
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
