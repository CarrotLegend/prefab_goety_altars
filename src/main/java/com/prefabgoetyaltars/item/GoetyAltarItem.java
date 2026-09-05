package com.prefabgoetyaltars.item;

import com.prefabgoetyaltars.structure.AltarPrefabType;
import com.wuest.prefab.structures.items.StructureItem;
import java.util.Objects;

public final class GoetyAltarItem extends StructureItem {
    private final AltarPrefabType prefabType;

    public GoetyAltarItem(AltarPrefabType prefabType) {
        super(new Properties());
        this.prefabType = Objects.requireNonNull(prefabType);
    }
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        if (!prefabType.isAvailable()) {
            if (context.getLevel().isClientSide && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(AltarPrefabType.missingDependencyMessage(), false);
            }
            return net.minecraft.world.InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    public AltarPrefabType prefabType() { return prefabType; }
}
