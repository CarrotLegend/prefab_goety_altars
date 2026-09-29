package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.item.GoetyAltarItem;
import com.prefabgoetyaltars.structure.AltarPrefabType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, PrefabGoetyAltars.MOD_ID);
    public static final DeferredHolder<Item, Item> SCANNER = ITEMS.register("altar_structure_scanner", () -> new BlockItem(ModBlocks.SCANNER.get(), new Item.Properties()));
    public static final List<DeferredHolder<Item, Item>> ALTARS = java.util.Arrays.stream(AltarPrefabType.values()).map(ModItems::registerAltar).toList();

    public static DeferredHolder<Item, Item> registerAltar(AltarPrefabType definition) {
        return ITEMS.register(definition.id(), () -> new GoetyAltarItem(definition));
    }
    private ModItems() {}
}
