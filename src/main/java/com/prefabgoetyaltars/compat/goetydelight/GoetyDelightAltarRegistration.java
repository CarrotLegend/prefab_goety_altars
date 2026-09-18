package com.prefabgoetyaltars.compat.goetydelight;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.structure.RitualDefinition;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

public final class GoetyDelightAltarRegistration {
    public static List<RitualDefinition> definitions() {
        return List.of(RitualDefinition.named("culinary_ritual",
                new ResourceLocation(PrefabGoetyAltars.MOD_ID, "textures/gui/structures/culinary_ritual.png"),
                GoetyDelightCompat.MOD_ID));
    }
    private GoetyDelightAltarRegistration() {}
}
