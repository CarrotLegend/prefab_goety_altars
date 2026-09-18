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
    public AltarPrefabType prefabType() { return prefabType; }
}
