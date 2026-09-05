package com.prefabgoetyaltars.network;

import com.prefabgoetyaltars.structure.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuildAltarPacketTest {
    private static final BlockPos POS = new BlockPos(-123,80,456);
    private GoetyAltarConfiguration config(AltarPrefabType type, Direction direction, AltarMaterialVariant altar, AltarMaterialVariant pedestal) {
        return new GoetyAltarConfiguration(type, "animation_ritual", altar, pedestal, POS, direction);
    }
    @Test void roundTripAllTypesDirectionsHandsAndIndependentMaterials() {
        for (var type : AltarPrefabType.values()) for (var dir : Direction.Plane.HORIZONTAL)
            for (var hand : InteractionHand.values()) for (var altar : AltarMaterialVariant.values()) for (var pedestal : AltarMaterialVariant.values()) {
                var config = config(type, dir, altar, pedestal); var packet = BuildAltarPacket.from(config, hand);
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try { BuildAltarPacket.encode(packet, buffer); var read = BuildAltarPacket.decode(buffer);
                    assertEquals(packet, read); assertEquals(config.WriteToCompoundTag(), read.validatedConfiguration().WriteToCompoundTag());
                } finally { buffer.release(); }
            }
    }
    @Test void missingInvalidAndConflictingFieldsFailClosed() {
        var c = config(AltarPrefabType.RITUAL, Direction.NORTH, AltarMaterialVariant.MARBLE, AltarMaterialVariant.BLACKSTONE);
        for (String key : new String[]{"prefabType","ritualId","altarVariant","pedestalVariant","hitX","hitY","hitZ","wareHouseFacing"}) {
            var tag = c.WriteToCompoundTag(); tag.remove(key);
            assertThrows(IllegalArgumentException.class, () -> new GoetyAltarConfiguration().ReadFromCompoundTag(tag));
        }
        for (String key : new String[]{"prefabType","altarVariant","pedestalVariant","wareHouseFacing"}) {
            var tag = c.WriteToCompoundTag(); tag.putString(key,"invalid");
            assertThrows(IllegalArgumentException.class, () -> new GoetyAltarConfiguration().ReadFromCompoundTag(tag));
        }
        for (String[] fields : new String[][]{
                {"ritual_altar","frost_ritual","marble","blackstone"}, {"ritual_altar","animation_ritual","stone","blackstone"},
                {"ritual_altar","animation_ritual","marble","stone"}, {"all_ritual","animation_ritual","marble","blackstone"}}) {
            var p = new BuildAltarPacket(fields[0],fields[1],fields[2],fields[3],POS,Direction.NORTH,InteractionHand.MAIN_HAND,c.WriteToCompoundTag());
            assertThrows(IllegalArgumentException.class,p::validatedConfiguration);
        }
        assertThrows(IllegalArgumentException.class, () -> new BuildAltarPacket("ritual_altar","animation_ritual","marble","blackstone",POS.above(),Direction.NORTH,InteractionHand.MAIN_HAND,c.WriteToCompoundTag()).validatedConfiguration());
    }
    @Test void fixedTypesCanonicalizeSpoofedRitualIds() {
        for (var type : new AltarPrefabType[]{AltarPrefabType.ALL_RITUAL, AltarPrefabType.REVELATION_ALL_RITUAL, AltarPrefabType.MASTER_FORGE_RITUAL}) {
            var c = config(type,Direction.WEST,AltarMaterialVariant.SHADE_STONE,AltarMaterialVariant.DEEPSLATE);
            var tag = c.WriteToCompoundTag(); tag.putString("ritualId","untrusted_path");
            var p = new BuildAltarPacket(type.id(),"master_forge_ritual","shade_stone","deepslate",POS,Direction.WEST,InteractionHand.OFF_HAND,tag);
            var read = p.validatedConfiguration(); assertEquals("",read.ritualId()); assertEquals(type.resolve("").id(),read.definition().id());
        }
    }
    @Test void malformedWireDirectionAndHandRejected() {
        for (String[] parts : new String[][]{{"up","main_hand"},{"invalid","main_hand"},{"north","invalid"}}) {
            var b = new FriendlyByteBuf(Unpooled.buffer());
            try { b.writeUtf("ritual_altar").writeUtf("animation_ritual").writeUtf("shade_stone").writeUtf("shade_stone");
                b.writeBlockPos(POS); b.writeUtf(parts[0]); b.writeUtf(parts[1]);
                assertThrows(IllegalArgumentException.class, () -> BuildAltarPacket.decode(b));
            } finally { b.release(); }
        }
    }
}
