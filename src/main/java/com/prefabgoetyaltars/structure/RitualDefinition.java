package com.prefabgoetyaltars.structure;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import com.prefabgoetyaltars.compat.revelation.RevelationAltarRegistration;
import com.prefabgoetyaltars.compat.goetydelight.GoetyDelightAltarRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

public record RitualDefinition(String id, String translationKey, String structureResource, ResourceLocation previewTexture, String requiredModId) {
    private static final List<RitualDefinition> RITUALS = Stream.of(Stream.of(
            "adept_nether_ritual", "animation_ritual", "deep_ritual", "end_ritual", "expert_nether_ritual",
            "forge_ritual", "frost_ritual", "geoturgy_ritual", "magic_ritual", "necroturgy_ritual",
            "overgrown_ritual", "sabbath_ritual", "sky_ritual", "storm_ritual").map(id -> named(id, new ResourceLocation(PrefabGoetyAltars.MOD_ID, "textures/gui/structures/" + id + ".png"), null)),
            RevelationAltarRegistration.definitions().stream(),
            GoetyDelightAltarRegistration.definitions().stream()).flatMap(java.util.function.Function.identity()).toList();

    public RitualDefinition {
        Objects.requireNonNull(id); Objects.requireNonNull(translationKey); Objects.requireNonNull(structureResource);
        if (!id.matches("[a-z0-9_]{1,128}") || !translationKey.equals("ritual.prefab_goety_altars." + id)
                || !structureResource.startsWith("assets/prefab_goety_altars/structures/")
                || structureResource.contains("..") || structureResource.contains("\\") || !structureResource.endsWith(".gz")) {
            throw new IllegalArgumentException("Invalid ritual definition: " + id);
        }
        if (requiredModId != null && !requiredModId.matches("[a-z][a-z0-9_]{1,63}")) throw new IllegalArgumentException("Invalid mod id");
    }
    public RitualDefinition(String id, String translationKey, String structureResource, String requiredModId) {
        this(id, translationKey, structureResource, null, requiredModId);
    }
    public static RitualDefinition named(String id, String requiredModId) { return named(id, null, requiredModId); }
    public static RitualDefinition named(String id, ResourceLocation previewTexture, String requiredModId) {
        return new RitualDefinition(id, "ritual.prefab_goety_altars." + id,
                "assets/prefab_goety_altars/structures/" + id + ".gz", previewTexture, requiredModId);
    }
    public boolean isAvailable() { return requiredModId == null || isAvailable(ModList.get()::isLoaded); }
    public boolean isAvailable(Predicate<String> loaded) { return requiredModId == null || loaded.test(requiredModId); }
    public static List<RitualDefinition> all() { return RITUALS; }
    public static List<RitualDefinition> available() { return RITUALS.stream().filter(RitualDefinition::isAvailable).toList(); }
    public static List<RitualDefinition> available(Predicate<String> loaded) { return RITUALS.stream().filter(d -> d.isAvailable(loaded)).toList(); }
    public static Optional<RitualDefinition> find(String id) { return RITUALS.stream().filter(d -> d.id.equals(id)).findFirst(); }
    public static void warnMissingResources() {
        Stream.concat(RITUALS.stream(), Arrays.stream(AltarPrefabType.values()).filter(t -> t != AltarPrefabType.RITUAL)
                .map(t -> t.resolve(""))).forEach(d -> {
            if (RitualDefinition.class.getClassLoader().getResource(d.structureResource) == null)
                PrefabGoetyAltars.LOGGER.warn("Missing ritual structure: {}", d.structureResource);
        });
    }
}
