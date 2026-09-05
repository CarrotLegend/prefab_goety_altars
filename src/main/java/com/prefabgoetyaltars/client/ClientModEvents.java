package com.prefabgoetyaltars.client;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.client.screen.GoetyAltarScreen;
import com.prefabgoetyaltars.item.GoetyAltarItem;
import com.prefabgoetyaltars.registry.ModItems;
import com.wuest.prefab.proxy.ClientProxy;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModEvents {
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ModItems.ALTARS.forEach(registration -> {
            var item = (GoetyAltarItem) registration.get();
            ClientProxy.ModGuis.put(item, new GoetyAltarScreen(item));
        }));
    }

    @Mod.EventBusSubscriber(modid = PrefabGoetyAltars.MOD_ID, value = Dist.CLIENT)
    public static final class Interactions {
        @SubscribeEvent
        public static void rightClick(PlayerInteractEvent.RightClickBlock event) {
            if (event.getLevel().isClientSide && event.getFace() == Direction.UP
                    && event.getItemStack().getItem() instanceof GoetyAltarItem item
                    && ClientProxy.ModGuis.get(item) instanceof GoetyAltarScreen screen) {
                screen.prepare(event.getHand());
            }
        }
    }
    private ClientModEvents() {}
}
