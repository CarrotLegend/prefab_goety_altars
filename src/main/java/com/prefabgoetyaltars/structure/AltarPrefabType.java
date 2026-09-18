package com.prefabgoetyaltars.structure;

import java.util.Arrays;

public enum AltarPrefabType {
    RITUAL("ritual_altar", null),
    ALL_RITUAL("all_ritual", RitualDefinition.named("all_ritual", 
            new net.minecraft.resources.ResourceLocation(com.prefabgoetyaltars.PrefabGoetyAltars.MOD_ID, "textures/gui/structures/all_ritual.png"), null));

    private final String id;
    private final RitualDefinition fixedRitual;
    AltarPrefabType(String id, RitualDefinition fixedRitual) { this.id = id; this.fixedRitual = fixedRitual; }
    public String id() { return id; }
    public String translationKey() { return "item.prefab_goety_altars." + id; }
    public boolean isAvailable() { return true; }
    public RitualDefinition resolve(String ritualId) {
        if (this == RITUAL) return RitualDefinition.find(ritualId).orElseThrow(() -> new IllegalArgumentException("Unknown ritual: " + ritualId));
        return fixedRitual;
    }
    public static AltarPrefabType fromId(String id) {
        return Arrays.stream(values()).filter(v -> v.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown prefab type: " + id));
    }
}
