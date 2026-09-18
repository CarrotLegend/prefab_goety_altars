package com.prefabgoetyaltars.compat.revelation;

import java.util.List;

import com.prefabgoetyaltars.structure.RitualDefinition;
import com.prefabgoetyaltars.PrefabGoetyAltars;
import net.minecraft.resources.ResourceLocation;

public final class RevelationAltarRegistration {
    public static List<RitualDefinition> definitions() {
        return List.of(RitualDefinition.named("master_forge_ritual",
                new ResourceLocation(PrefabGoetyAltars.MOD_ID, "textures/gui/structures/master_forge_ritual.png"),
                RevelationCompat.MOD_ID));
    }
    private RevelationAltarRegistration() {}
}
