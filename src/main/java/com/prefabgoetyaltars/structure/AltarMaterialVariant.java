package com.prefabgoetyaltars.structure;

import net.minecraft.resources.ResourceLocation;
import java.util.Arrays;

public enum AltarMaterialVariant {
    SHADE_STONE("shade_stone", ""), STONE("stone", "_stone"), DEEPSLATE("deepslate", "_deepslate"),
    NETHER_BRICK("nether_brick", "_nether_brick"), BLACKSTONE("blackstone", "_blackstone"),
    END_STONE_BRICK("end_stone_brick", "_end_stone"), HIGHROCK("highrock", "_highrock"),
    MARBLE("marble", "_marble"), PRISMARINE_BRICK("prismarine_brick", "_prismarine"),
    CRYPT_STONE("crypt_stone", "_crypt_stone"), OMINOUS_STONE("ominous_stone", "_ominous_stone");
    private final String id;
    private final ResourceLocation altarId, pedestalId;
    AltarMaterialVariant(String id, String suffix) {
        this.id = id; altarId = new ResourceLocation("goety", "dark_altar" + suffix);
        pedestalId = new ResourceLocation("goety", "pedestal" + suffix);
    }
    public String id() { return id; }
    public String translationKey() { return "material.prefab_goety_altars." + id; }
    public ResourceLocation altarId() { return altarId; }
    public ResourceLocation pedestalId() { return pedestalId; }
    public AltarMaterialVariant next() { return values()[(ordinal() + 1) % values().length]; } // Local cycling only, never serialized.
    public static AltarMaterialVariant fromId(String id) {
        return Arrays.stream(values()).filter(v -> v.id.equals(id)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown material: " + id));
    }
    public static boolean isAltar(ResourceLocation id) { return Arrays.stream(values()).anyMatch(v -> v.altarId.equals(id)); }
    public static boolean isPedestal(ResourceLocation id) { return Arrays.stream(values()).anyMatch(v -> v.pedestalId.equals(id)); }
}
