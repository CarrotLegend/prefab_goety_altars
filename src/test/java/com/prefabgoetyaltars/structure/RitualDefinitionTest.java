package com.prefabgoetyaltars.structure;

import com.prefabgoetyaltars.compat.revelation.RevelationCompat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RitualDefinitionTest {
    @Test void stableRitualListsAndOptionalDependency() {
        var base = RitualDefinition.available(id -> false);
        assertEquals(14, base.size()); assertEquals(14, RitualDefinition.available(id -> true).size());
        assertEquals(base.stream().map(RitualDefinition::id).sorted().toList(), base.stream().map(RitualDefinition::id).toList());
        assertTrue(base.stream().allMatch(d -> d.requiredModId() == null));
        var master = AltarPrefabType.MASTER_FORGE_RITUAL.resolve("ignored");
        assertEquals(RevelationCompat.MOD_ID, master.requiredModId()); assertFalse(master.isAvailable(id -> false));
        assertTrue(RitualDefinition.find("master_forge_ritual").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> AltarPrefabType.RITUAL.resolve("master_forge_ritual"));
        for (var d : RitualDefinition.all()) assertNotNull(getClass().getClassLoader().getResource(d.structureResource()));
    }
    @Test void exactlyFourTypesAndFixedTypesIgnoreClientRitual() {
        assertEquals(java.util.List.of("ritual_altar", "all_ritual", "revelation_all_ritual", "master_forge_ritual"), java.util.Arrays.stream(AltarPrefabType.values()).map(AltarPrefabType::id).toList());
        assertEquals("master_forge_ritual", AltarPrefabType.MASTER_FORGE_RITUAL.resolve("all_ritual").id());
        assertEquals("all_ritual", AltarPrefabType.ALL_RITUAL.resolve("../../anything").id());
        assertEquals("revelation_all_ritual", AltarPrefabType.REVELATION_ALL_RITUAL.resolve("animation_ritual").id());
        assertThrows(IllegalArgumentException.class, () -> AltarPrefabType.RITUAL.resolve("all_ritual"));
        assertThrows(IllegalArgumentException.class, () -> AltarPrefabType.fromId("animation_ritual"));
    }
    @Test void explicitPreviewBindingsAndNullableCompatibility() {
        var definitions = new java.util.ArrayList<>(RitualDefinition.all());
        for (var type : AltarPrefabType.values()) if (type != AltarPrefabType.RITUAL) definitions.add(type.resolve("ignored"));
        assertEquals(17, definitions.size());
        var textures = new java.util.HashSet<net.minecraft.resources.ResourceLocation>();
        for (var d : definitions) {
            {
                assertEquals("textures/gui/structures/" + d.id() + ".png", d.previewTexture().getPath());
                assertTrue(textures.add(d.previewTexture()));
                assertNotNull(getClass().getClassLoader().getResource("assets/" + d.previewTexture().getNamespace() + "/" + d.previewTexture().getPath()));
            }
        }
        assertEquals(17, textures.size());
        assertNull(RitualDefinition.named("missing_test", null).previewTexture());
        assertNull(new RitualDefinition("test", "ritual.prefab_goety_altars.test", "assets/prefab_goety_altars/structures/test.gz", null).previewTexture());
    }
    @Test void stableMaterialIdsAndDisjointFamilies() {
        assertEquals(11, AltarMaterialVariant.values().length);
        for (var v : AltarMaterialVariant.values()) {
            assertEquals(v, AltarMaterialVariant.fromId(v.id()));
            assertTrue(AltarMaterialVariant.isAltar(v.altarId())); assertFalse(AltarMaterialVariant.isPedestal(v.altarId()));
            assertTrue(AltarMaterialVariant.isPedestal(v.pedestalId())); assertFalse(AltarMaterialVariant.isAltar(v.pedestalId()));
        }
        assertThrows(IllegalArgumentException.class, () -> AltarMaterialVariant.fromId("11"));
        assertThrows(IllegalArgumentException.class, () -> AltarMaterialVariant.fromId("../stone"));
    }
}
