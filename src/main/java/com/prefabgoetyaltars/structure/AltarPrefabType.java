package com.prefabgoetyaltars.structure;

import com.prefabgoetyaltars.compat.revelation.RevelationCompat;
import net.minecraft.network.chat.Component;
import java.util.Arrays;

public enum AltarPrefabType {
    RITUAL("ritual_altar", null),
    ALL_RITUAL("all_ritual", RitualDefinition.named("all_ritual", 
            new net.minecraft.resources.ResourceLocation(com.prefabgoetyaltars.PrefabGoetyAltars.MOD_ID, "textures/gui/structures/all_ritual.png"), null)),
    REVELATION_ALL_RITUAL("revelation_all_ritual", RitualDefinition.named("revelation_all_ritual", 
            new net.minecraft.resources.ResourceLocation(com.prefabgoetyaltars.PrefabGoetyAltars.MOD_ID, "textures/gui/structures/revelation_all_ritual.png"), RevelationCompat.MOD_ID)),
    MASTER_FORGE_RITUAL("master_forge_ritual", RitualDefinition.named("master_forge_ritual",
            new net.minecraft.resources.ResourceLocation(com.prefabgoetyaltars.PrefabGoetyAltars.MOD_ID, "textures/gui/structures/master_forge_ritual.png"), RevelationCompat.MOD_ID));

    private final String id;
    private final RitualDefinition fixedRitual;
    AltarPrefabType(String id, RitualDefinition fixedRitual) { this.id = id; this.fixedRitual = fixedRitual; }
    public String id() { return id; }
    public String translationKey() { return "item.prefab_goety_altars." + id; }
    public boolean isAvailable() { return !requiresRevelation() || RevelationCompat.isLoaded(); }
    private boolean requiresRevelation() { return this == REVELATION_ALL_RITUAL || this == MASTER_FORGE_RITUAL; }
    public static Component missingDependencyMessage() { return Component.translatable("message.prefab_goety_altars.revelation_required"); }
    public RitualDefinition resolve(String ritualId) {
        if (this == RITUAL) return RitualDefinition.find(ritualId).orElseThrow(() -> new IllegalArgumentException("Unknown ritual: " + ritualId));
        return fixedRitual;
    }
    public static AltarPrefabType fromId(String id) {
        return Arrays.stream(values()).filter(v -> v.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown prefab type: " + id));
    }
}
