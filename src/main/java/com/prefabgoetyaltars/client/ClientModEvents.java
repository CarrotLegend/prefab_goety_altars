package com.prefabgoetyaltars.client;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.client.screen.GoetyAltarScreen;
import com.prefabgoetyaltars.item.GoetyAltarItem;
import com.prefabgoetyaltars.registry.ModItems;
import com.prefab.ClientModRegistryBase;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT)
public final class ClientModEvents {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModItems.ALTARS.forEach(registration -> {
            var item = (GoetyAltarItem) registration.get();
            ClientModRegistryBase.ModGuis.put(item, new GoetyAltarScreen(item));
        }));
    }

    @EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT)
    public static final class Interactions {
        @SubscribeEvent
        public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide && event.getFace() == Direction.UP
                    && event.getItemStack().getItem() instanceof GoetyAltarItem item
                    && ClientModRegistryBase.ModGuis.get(item) instanceof GoetyAltarScreen screen) {
                screen.prepare(event.getHand());
            }
        }
    }
    private ClientModEvents() {}
}
