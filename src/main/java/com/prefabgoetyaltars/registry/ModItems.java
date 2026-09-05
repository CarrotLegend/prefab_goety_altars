package com.prefabgoetyaltars.registry;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.item.GoetyAltarItem;
import com.prefabgoetyaltars.structure.AltarPrefabType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, PrefabGoetyAltars.MOD_ID);
    public static final RegistryObject<Item> SCANNER = ITEMS.register("altar_structure_scanner", () -> new BlockItem(ModBlocks.SCANNER.get(), new Item.Properties()));
    public static final List<RegistryObject<Item>> ALTARS = java.util.Arrays.stream(AltarPrefabType.values()).map(ModItems::registerAltar).toList();

    public static RegistryObject<Item> registerAltar(AltarPrefabType definition) {
        return ITEMS.register(definition.id(), () -> new GoetyAltarItem(definition));
    }
    private ModItems() {}
}
