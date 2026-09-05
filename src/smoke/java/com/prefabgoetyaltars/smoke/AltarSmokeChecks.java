// SPDX-License-Identifier: MIT
package com.prefabgoetyaltars.smoke;

import com.mojang.logging.LogUtils;
import com.prefabgoetyaltars.client.screen.GoetyAltarScreen;
import com.prefabgoetyaltars.registry.ModItems;
import com.prefabgoetyaltars.structure.*;
import com.wuest.prefab.structures.gui.GuiStructure;
import com.wuest.prefab.structures.render.StructureRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;

/** Opt-in integration checks in a new isolated flat world; never included in the release jar. */
public final class AltarSmokeChecks {
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static int stage, ticks, index, observedStage = -1, heartbeat;
    private static boolean rejectBuild;
    private static long started;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static InteractionHand hand() { return index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND; }
    private record Case(AltarPrefabType type, String ritual, Direction facing, AltarMaterialVariant altar, AltarMaterialVariant pedestal) {}
    private static final java.util.List<Case> CASES = new java.util.ArrayList<>();
    private static final AltarMaterialVariant[] ALTARS = {AltarMaterialVariant.SHADE_STONE, AltarMaterialVariant.MARBLE, AltarMaterialVariant.END_STONE_BRICK, AltarMaterialVariant.OMINOUS_STONE};
    private static final AltarMaterialVariant[] PEDESTALS = {AltarMaterialVariant.SHADE_STONE, AltarMaterialVariant.BLACKSTONE, AltarMaterialVariant.PRISMARINE_BRICK, AltarMaterialVariant.DEEPSLATE};
    private static Case current() { return CASES.get(index); }
    private static RitualDefinition definition() { return current().type().resolve(current().ritual()); }
    private static net.minecraft.world.item.Item item(AltarPrefabType type) { return ModItems.ALTARS.stream().map(r -> (com.prefabgoetyaltars.item.GoetyAltarItem) r.get()).filter(i -> i.prefabType() == type).findFirst().orElseThrow(); }
    private static GoetyAltarConfiguration config() { return new GoetyAltarConfiguration(current().type(), current().ritual(), current().altar(), current().pedestal(), POS, direction()); }
    private static final java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> previewStates = new java.util.HashMap<>();
    private static Direction direction() { return current().facing(); }

    public static void createWorld() {
        Minecraft.getInstance().options.pauseOnLostFocus = false;
        started = System.currentTimeMillis(); stage = 1;
        int number = 0;
        for (var ritual : RitualDefinition.available()) {
            int n = number++ % 4; CASES.add(new Case(AltarPrefabType.RITUAL, ritual.id(), DIRECTIONS[n], ALTARS[n], PEDESTALS[n]));
        }
        for (var type : AltarPrefabType.values()) if (type != AltarPrefabType.RITUAL && type.isAvailable())
            for (int n = 0; n < 4; n++) CASES.add(new Case(type, "", DIRECTIONS[n], ALTARS[n], PEDESTALS[n]));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.level.BlockEvent.BreakEvent event) -> {
            if (rejectBuild) event.setCanceled(true);
        });
        var settings = new LevelSettings("Altar integration", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                new GameRules(), WorldDataConfiguration.DEFAULT);
        Minecraft.getInstance().createWorldOpenFlows().createFreshLevel("altar-smoke-" + started, settings,
                new WorldOptions(54321L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }
    private static void check(boolean value, String message) { if (!value) throw new IllegalStateException(message); }
    private static void server(Runnable action) {
        ready = false;
        Minecraft.getInstance().getSingleplayerServer().execute(() -> {
            try { action.run(); ready = true; } catch (Throwable e) { failure = e; }
        });
    }
    private static Object field(Object object, Class<?> type, String name) throws Exception {
        var field = type.getDeclaredField(name); field.setAccessible(true); return field.get(object);
    }
    private static Button button(GoetyAltarScreen screen, String name) throws Exception {
        return (Button) field(screen, GuiStructure.class, name);
    }
    private static GoetyAltarConfiguration draft(GoetyAltarScreen screen) throws Exception {
        return (GoetyAltarConfiguration) field(screen, GuiStructure.class, "configuration");
    }
    private static void press(GoetyAltarScreen screen, String name) throws Exception {
        ((Button) field(screen, GoetyAltarScreen.class, name)).onPress();
        PreviewSmokeChecks.checkImage(screen);
    }
    private static void setDirection(GoetyAltarScreen screen) throws Exception {
        if (index == 0) {
            Object previous = field(screen, GuiStructure.class, "selectedStructure");
            for (int n = 0; n < 3; n++) {
                var pedestal = draft(screen).pedestalVariant(); press(screen, "altarButton");
                check(draft(screen).pedestalVariant() == pedestal, "independent altar selector");
                Object fresh = field(screen, GuiStructure.class, "selectedStructure"); check(fresh != previous, "fresh preview instance"); previous = fresh;
                var altar = draft(screen).altarVariant(); press(screen, "pedestalButton");
                check(draft(screen).altarVariant() == altar, "independent pedestal selector");
            }
        }
        for (int n = 0; n < 15 && !draft(screen).ritualId().equals(current().ritual()); n++) press(screen, "ritualButton");
        for (int n = 0; n < 11 && draft(screen).altarVariant() != current().altar(); n++) press(screen, "altarButton");
        for (int n = 0; n < 11 && draft(screen).pedestalVariant() != current().pedestal(); n++) press(screen, "pedestalButton");
        for (int n = 0; n < 4 && draft(screen).houseFacing != direction(); n++) press(screen, "directionButton");
        check((field(screen, GoetyAltarScreen.class, "ritualButton") != null) == (current().type() == AltarPrefabType.RITUAL), "only rotating item has ritual selector");
        check(draft(screen).prefabType() == current().type() && draft(screen).ritualId().equals(current().ritual()), "actual interaction hand type and ritual");
        check(draft(screen).altarVariant() == current().altar() && draft(screen).pedestalVariant() == current().pedestal(), "independent materials");
        check(draft(screen).houseFacing == direction(), "direction button");
        var structure = (GoetyAltarStructure) field(screen, GuiStructure.class, "selectedStructure");
        previewStates.clear();
        for (var block : structure.getBlocks()) capture(block, structure);
    }
    private static void capture(com.wuest.prefab.structures.base.BuildBlock block, GoetyAltarStructure structure) {
        if (AltarMaterialVariant.isAltar(block.getResourceLocation()) || AltarMaterialVariant.isPedestal(block.getResourceLocation())) {
            var found = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(block.getResourceLocation());
            com.wuest.prefab.structures.base.BuildBlock.SetBlockState(config(), POS, block, found, found.defaultBlockState(), structure.getClearSpace().getShape().getDirection());
            var pos = block.getStartingPosition().getRelativePosition(POS, structure.getClearSpace().getShape().getDirection(), direction());
            previewStates.put(pos, block.getBlockState());
        }
        if (block.getSubBlock() != null) capture(block.getSubBlock(), structure);
    }
    private static void open() {
        var mc = Minecraft.getInstance();
        mc.gameMode.useItemOn(mc.player, hand(), new BlockHitResult(POS.getCenter(), Direction.UP, POS, false));
    }
    public static void tick() {
        if (stage == 0 || stage == 99) return;
        var mc = Minecraft.getInstance();
        if (failure != null || System.currentTimeMillis() - started > 600_000) {
            LogUtils.getLogger().error("ALTAR_INTEGRATION_FAILED index=" + index + " stage=" + stage, failure);
            stage = 99; mc.stop(); return;
        }
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        try {
            if (observedStage != stage || ++heartbeat % 200 == 0) {
                observedStage = stage;
                LogUtils.getLogger().info("ALTAR_TEST_PROGRESS index={} stage={} ready={} screen={} handItem={}", index, stage, ready,
                        mc.screen == null ? "none" : mc.screen.getClass().getName(), mc.player.getItemInHand(hand()));
            }
            if (stage == 1) {
                stage = 2; ticks = 0;
                server(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) level.getChunk(x, z);
                    for (var pos : BlockPos.betweenClosed(POS.offset(-32,-4,-32), POS.offset(32,40,32))) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                    level.setBlockAndUpdate(POS, Blocks.STONE.defaultBlockState());
                    player.teleportTo(level, 8.5, 81, 10.5, 180, 25);
                    player.setGameMode(GameType.CREATIVE); player.getAbilities().flying = true; player.onUpdateAbilities();
                    player.setItemInHand(hand(), new ItemStack(item(current().type()), 2));
                    player.setItemInHand(hand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND,
                            new ItemStack(item(current().type() == AltarPrefabType.RITUAL ? AltarPrefabType.ALL_RITUAL : AltarPrefabType.RITUAL), 2));
                    player.containerMenu.broadcastChanges();
                });
            } else if (stage == 2 && ready && ++ticks > 25) {
                stage = 3; ticks = 0;
                if (index == 0) {
                    net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(mc.level.enabledFeatures(), true, mc.level.registryAccess());
                    var displayed = com.prefabgoetyaltars.registry.ModCreativeTabs.ALTARS.get().getDisplayItems();
                    for (var type : AltarPrefabType.values()) check(displayed.stream().anyMatch(stack -> stack.is(item(type))) == type.isAvailable(), "creative visibility " + type);
                }
                open();
                if (index == 0 && mc.screen instanceof GoetyAltarScreen cancelScreen) {
                    button(cancelScreen, "btnCancel").onPress();
                    check(!(mc.screen instanceof GoetyAltarScreen), "native Cancel closes without building");
                    check(mc.player.getItemInHand(hand()).getCount() == 2, "Cancel does not consume");
                    open();
                }
            } else if (stage == 3 && mc.screen instanceof GoetyAltarScreen && ticks == 4 && (index <= 1 || current().type() != AltarPrefabType.RITUAL)) {
                try (var screenshot = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    screenshot.writeToFile(java.nio.file.Path.of(System.getProperty("altar.smokeProject"), ".cache", "native-gui-" + current().type().id() + "-" + (index == 1 ? "large" : "normal") + ".png"));
                }
                ticks++;
            } else if (stage == 3 && mc.screen instanceof GoetyAltarScreen screen && ++ticks > 5) {
                check(draft(screen).altarVariant() == AltarMaterialVariant.SHADE_STONE && draft(screen).pedestalVariant() == AltarMaterialVariant.SHADE_STONE, "fresh GUI defaults");
                check(((java.util.List<?>) field(screen, GoetyAltarScreen.class, "rituals")).size() == RitualDefinition.available().size(), "conditional ritual options");
                setDirection(screen);
                var beforeResize = draft(screen).WriteToCompoundTag();
                screen.resize(mc, screen.width, screen.height);
                check(beforeResize.equals(draft(screen).WriteToCompoundTag()), "resize preserves draft");
                PreviewSmokeChecks.checkImage(screen);
                if (index == 0) { PreviewSmokeChecks.run(screen); mc.options.guiScale().set(1); mc.resizeDisplay(); PreviewSmokeChecks.openPrefabReference(screen); stage = 8; ticks = 0; return; }
                check(button(screen, "btnBuild").active && button(screen, "btnVisualize").active, "missing PNG must not disable build/preview");
                button(screen, "btnVisualize").onPress();
                check(StructureRenderHandler.currentStructure instanceof GoetyAltarStructure, "native preview installed");
                if (index == 1) { mc.options.guiScale().set(2); mc.resizeDisplay(); }
                stage = 4; ticks = 0;
            } else if (stage == 8 && ++ticks > 6) {
                var screen = PreviewSmokeChecks.finishPrefabReference();
                button(screen, "btnVisualize").onPress();
                check(StructureRenderHandler.currentStructure instanceof GoetyAltarStructure, "native preview after reference");
                stage = 4; ticks = 0;
            } else if (stage == 4 && ++ticks > 5) {
                // Prefab consumes the first right-click to dismiss its active preview.
                open(); check(StructureRenderHandler.currentStructure == null, "native right-click dismisses preview");
                stage = 5; ticks = 0; open();
            } else if (stage == 5 && mc.screen instanceof GoetyAltarScreen screen && ++ticks > 5) {
                setDirection(screen); button(screen, "btnBuild").onPress();
                check(StructureRenderHandler.currentStructure == null, "build cleared own preview");
                stage = 6; ticks = 0;
            } else if (stage == 6 && ++ticks > 15) {
                stage = 7;
                server(() -> {
                    try {
                        var level = mc.getSingleplayerServer().overworld();
                        var player = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                        check(player.getItemInHand(hand()).getCount() == 1, "successful build consumes exactly one: " + definition().id());
                        check(player.getItemInHand(hand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND).getCount() == 2, "other hand unchanged");
                        for (var entry : previewStates.entrySet()) {
                            var expectedState = entry.getValue();
                            var actualState = level.getBlockState(entry.getKey());
                            // DarkAltarBlockEntity.tick sets LIT from checkCage() each tick.
                            // This delayed check compares all other properties and the exact block ID.
                            var lit = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
                            if (expectedState.hasProperty(lit) && actualState.hasProperty(lit)
                                    && AltarMaterialVariant.isAltar(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(expectedState.getBlock()))) {
                                if (!expectedState.getValue(lit).equals(actualState.getValue(lit)))
                                    LogUtils.getLogger().info("ALTAR_NATIVE_LIT_UPDATE: {} {} -> {}", entry.getKey(), expectedState.getValue(lit), actualState.getValue(lit));
                                expectedState = expectedState.setValue(lit, actualState.getValue(lit));
                            }
                            check(actualState.equals(expectedState), "preview/build stable material state mismatch at " + entry.getKey() + " expected=" + expectedState + " actual=" + actualState);
                        }
                        var structure = GoetyAltarStructure.load(definition());
                        for (var tile : structure.tileEntities) {
                            var pos = tile.getStartingPosition().getRelativePosition(POS, structure.getClearSpace().getShape().getDirection(), direction());
                            var actual = level.getBlockEntity(pos);
                            check(actual != null, "restored BlockEntity at " + pos);
                            var expected = tile.getEntityDataTag(); var saved = actual.saveWithFullMetadata();
                            check(expected.getString("id").equals(saved.getString("id")), "BlockEntity type");
                            for (String key : new String[]{"Items", "CustomName", "inventory"}) if (expected.contains(key)) check(expected.get(key).equals(saved.get(key)), "BlockEntity NBT " + key);
                        }
                        if (index == 0) {
                            var packet = com.prefabgoetyaltars.network.BuildAltarPacket.from(config(), hand());
                            var build = packet.getClass().getDeclaredMethod("build", net.minecraft.server.level.ServerPlayer.class);
                            build.setAccessible(true);
                            for (String invalid : new String[]{"not_a_ritual", "master_forge_ritual"}) {
                                var c = new GoetyAltarConfiguration(AltarPrefabType.RITUAL, invalid, current().altar(), current().pedestal(), POS, direction());
                                build.invoke(com.prefabgoetyaltars.network.BuildAltarPacket.from(c, hand()), player);
                                check(player.getItemInHand(hand()).getCount() == 1, "invalid/missing dependency consumes nothing");
                            }
                            var valid = config();
                            var invalidMaterial = new com.prefabgoetyaltars.network.BuildAltarPacket(valid.prefabType().id(), valid.ritualId(), "invalid", valid.pedestalVariant().id(), POS, direction(), hand(), valid.WriteToCompoundTag());
                            build.invoke(invalidMaterial, player); check(player.getItemInHand(hand()).getCount() == 1, "invalid material consumes nothing");
                            if (!com.prefabgoetyaltars.compat.revelation.RevelationCompat.isLoaded()) {
                                for (var lockedType : new AltarPrefabType[]{AltarPrefabType.REVELATION_ALL_RITUAL, AltarPrefabType.MASTER_FORGE_RITUAL}) {
                                player.setItemInHand(hand(), new ItemStack(item(lockedType), 2));
                                var denied = new GoetyAltarConfiguration(lockedType, "ignored", current().altar(), current().pedestal(), POS, direction());
                                build.invoke(com.prefabgoetyaltars.network.BuildAltarPacket.from(denied, hand()), player);
                                check(player.getItemInHand(hand()).getCount() == 2, "forced Revelation item rejected");
                                }
                                player.setItemInHand(hand(), new ItemStack(item(current().type()), 1));
                            }
                            rejectBuild = true;
                            try { build.invoke(packet, player); } finally { rejectBuild = false; }
                            check(player.getItemInHand(hand()).getCount() == 1, "Prefab rejected build consumes nothing");
                            LogUtils.getLogger().info("ALTAR_FAILED_BUILD_NO_CONSUMPTION_OK");
                        }
                        LogUtils.getLogger().info("ALTAR_INTEGRATION_CASE_OK: {} {} {} blockEntities={}", definition().id(), direction(), hand(), structure.tileEntities.size());
                    } catch (Exception e) { throw new RuntimeException(e); }
                });
            } else if (stage == 7 && ready) {
                if (++index == CASES.size()) {
                    LogUtils.getLogger().info("ALTAR_INTEGRATION_OK: {} GUI previews and packet builds; both hands; material states and native BlockEntities; success consumes one", CASES.size());
                    stage = 99; mc.stop();
                } else stage = 1;
            }
        } catch (Throwable e) { failure = e; }
    }
}

