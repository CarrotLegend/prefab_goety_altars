// SPDX-License-Identifier: MIT
package com.prefabgoetyaltars.structure;

import com.wuest.prefab.structures.config.StructureConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import java.util.Objects;

public final class GoetyAltarConfiguration extends StructureConfiguration {
    private AltarPrefabType prefabType = AltarPrefabType.RITUAL;
    private String ritualId = "";
    private AltarMaterialVariant altarVariant = AltarMaterialVariant.SHADE_STONE;
    private AltarMaterialVariant pedestalVariant = AltarMaterialVariant.SHADE_STONE;

    public GoetyAltarConfiguration() {}
    public GoetyAltarConfiguration(AltarPrefabType type, String ritualId, AltarMaterialVariant altar,
                                   AltarMaterialVariant pedestal, BlockPos pos, Direction facing) {
        this.prefabType = Objects.requireNonNull(type); this.ritualId = type == AltarPrefabType.RITUAL ? Objects.requireNonNull(ritualId) : "";
        this.altarVariant = Objects.requireNonNull(altar); this.pedestalVariant = Objects.requireNonNull(pedestal);
        this.pos = pos.immutable(); this.houseFacing = facing;
        validate();
    }
    public AltarPrefabType prefabType() { return prefabType; }
    public String ritualId() { return ritualId; }
    public AltarMaterialVariant altarVariant() { return altarVariant; }
    public AltarMaterialVariant pedestalVariant() { return pedestalVariant; }
    public RitualDefinition definition() { return prefabType.resolve(ritualId); }
    private void validate() {
        if (houseFacing == null || !houseFacing.getAxis().isHorizontal()
                || (prefabType == AltarPrefabType.RITUAL && !ritualId.matches("[a-z0-9_]{1,128}")))
            throw new IllegalArgumentException("Invalid ritual configuration");
    }
    @Override protected CompoundTag CustomWriteToCompoundTag(CompoundTag tag) {
        tag.putString("prefabType", prefabType.id()); tag.putString("ritualId", ritualId);
        tag.putString("altarVariant", altarVariant.id()); tag.putString("pedestalVariant", pedestalVariant.id()); return tag;
    }
    @Override protected void CustomReadFromNBTTag(CompoundTag tag, StructureConfiguration config) {
        var result = (GoetyAltarConfiguration) config;
        result.prefabType = AltarPrefabType.fromId(tag.getString("prefabType"));
        result.ritualId = result.prefabType == AltarPrefabType.RITUAL ? tag.getString("ritualId") : "";
        result.altarVariant = AltarMaterialVariant.fromId(tag.getString("altarVariant"));
        result.pedestalVariant = AltarMaterialVariant.fromId(tag.getString("pedestalVariant"));
    }
    @Override public GoetyAltarConfiguration ReadFromCompoundTag(CompoundTag tag) {
        if (tag == null) throw new IllegalArgumentException("Missing configuration");
        for (String key : new String[]{"prefabType", "ritualId", "altarVariant", "pedestalVariant", "wareHouseFacing"})
            if (!tag.contains(key, Tag.TAG_STRING)) throw new IllegalArgumentException("Missing field: " + key);
        for (String key : new String[]{"hitX", "hitY", "hitZ"})
            if (!tag.contains(key, Tag.TAG_INT)) throw new IllegalArgumentException("Missing field: " + key);
        var result = (GoetyAltarConfiguration) super.ReadFromCompoundTag(tag, new GoetyAltarConfiguration());
        result.validate(); return result;
    }
}
