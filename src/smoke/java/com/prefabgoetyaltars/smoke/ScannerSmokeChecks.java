package com.prefabgoetyaltars.smoke;

import com.mojang.logging.LogUtils;
import com.prefabgoetyaltars.client.screen.AltarStructureScannerScreen;
import com.prefabgoetyaltars.dev.scan.*;
import com.prefabgoetyaltars.network.*;
import com.prefabgoetyaltars.registry.ModBlocks;
import com.wuest.prefab.Prefab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

/** Runs only with -Psmoke -PscannerSmoke; creates its own disposable world. */
public final class ScannerSmokeChecks {
    private static final BlockPos POS = new BlockPos(8, 80, 8);
    private static int stage;
    private static int ticks;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    private static long started;
    private static int prefabScanners;

    public static void createWorld() {
        started = System.currentTimeMillis(); stage = 1;
        var settings = new LevelSettings("Scanner development test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
        Minecraft.getInstance().createWorldOpenFlows().createFreshLevel("scanner-smoke-" + started, settings,
                new WorldOptions(12345L, false, false), access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    public static void tick() {
        if (stage == 0 || stage == 99) return;
        var mc = Minecraft.getInstance();
        if (failure != null || System.currentTimeMillis() - started > 180_000) {
            LogUtils.getLogger().error("SCANNER_SMOKE_FAILED stage=" + stage, failure); stage = 99; mc.stop(); return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        try {
            if (stage == 1) {
                stage = 2; prefabScanners = Prefab.proxy.structureScanners.size();
                mc.getSingleplayerServer().execute(() -> serverCheck(() -> setupAndExport()));
            } else if (stage == 2 && ready && ++ticks > 40) {
                stage = 3; ticks = 0;
                mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(POS.getCenter(), Direction.UP, POS, false));
            } else if (stage == 3 && mc.screen instanceof AltarStructureScannerScreen screen && ++ticks > 40) {
                try (var screenshot = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    screenshot.writeToFile(Path.of(System.getProperty("altar.smokeProject")).resolve(".cache/scanner-gui.png"));
                }
                check(screen.canInteract(), "client authorization");
                check(screen.previewBounds() != null, "preview range");
                // Edit the same widgets a user edits, then exercise Esc/close save through C2S.
                var fields = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
                fields.get(0).setValue("-2"); fields.get(6).setValue("Saved Draft");
                check(screen.previewBounds().corner().equals(POS.east(2).north()), "live preview updated");
                screen.onClose(); check(!(mc.screen instanceof AltarStructureScannerScreen), "close cleared preview");
                var invalid = new AltarStructureScannerConfig().GetCompoundTag(); invalid.putInt("blocksWide", 0);
                ModNetwork.CHANNEL.sendToServer(new ScanAltarStructurePacket(POS, invalid));
                stage = 4; ticks = 0;
            } else if (stage == 4 && ++ticks > 40) {
                stage = 5; ready = false;
                mc.getSingleplayerServer().execute(() -> serverCheck(() -> {
                    var level = mc.getSingleplayerServer().overworld();
                    var scanner = (AltarStructureScannerBlockEntity) level.getBlockEntity(POS);
                    check(scanner.configuration().blocksToTheLeft == -2 && scanner.configuration().structureZipName.equals("Saved Draft"), "network save");
                    var tag = scanner.saveWithFullMetadata();
                    var restored = new AltarStructureScannerBlockEntity(POS, scanner.getBlockState()); restored.load(tag);
                    check(restored.configuration().GetCompoundTag().equals(scanner.configuration().GetCompoundTag()), "BlockEntity reload");
                    check(!Files.exists(DevStructurePaths.getStructureOutputDirectory().resolve("network_fixture_" + started + ".gz")), "save did not export");
                    AltarScannerService.open(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0), POS); ready = true;
                }));
            } else if (stage == 5 && ready && mc.screen instanceof AltarStructureScannerScreen screen && ++ticks > 70) {
                stage = 6; ticks = 0;
                var fields = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();
                fields.get(6).setValue("network_fixture_" + started);
                var button = screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance)
                        .map(net.minecraft.client.gui.components.Button.class::cast).filter(b -> b.getMessage().equals(Component.translatable("scanner.prefab_goety_altars.scan"))).findFirst().orElseThrow();
                button.onPress(); check(mc.screen == null, "Scan submitted and closed");
            } else if (stage == 6 && ++ticks > 40) {
                stage = 7; ticks = 0; ready = false;
                mc.getSingleplayerServer().execute(() -> serverCheck(() -> {
                    var output = DevStructurePaths.getStructureOutputDirectory().resolve("network_fixture_" + started + ".gz");
                    check(PrefabStructureExport.validate(output) != null, "Scan packet exported to isolated project");
                    LogUtils.getLogger().info("SCANNER_SMOKE_PACKET_EXPORT_OK: {}", output);
                    if (Boolean.getBoolean("altar.scannerSourceSmoke")) {
                        Path expected = Path.of(System.getProperty("altar.smokeProject")).resolve("src/main/resources").resolve(DevStructurePaths.RESOURCE_DIRECTORY).toRealPath();
                        check(output.getParent().equals(expected), "normal runClient exports to source resources");
                        Files.delete(output); // Only this test's uniquely named, already validated export.
                    }
                    AltarScannerService.open(mc.getSingleplayerServer().getPlayerList().getPlayers().get(0), POS); ready = true;
                }));
            } else if (stage == 7 && ready && mc.screen instanceof AltarStructureScannerScreen && ++ticks > 40) {
                stage = 8; ticks = 0;
                mc.getSingleplayerServer().execute(() -> serverCheck(() -> mc.getSingleplayerServer().overworld().removeBlock(POS, false)));
            } else if (stage == 8 && ++ticks > 40) {
                check(!(mc.screen instanceof AltarStructureScannerScreen), "removed scanner closes GUI");
                check(Prefab.proxy.structureScanners.size() == prefabScanners, "Prefab scanner state untouched");
                LogUtils.getLogger().info("SCANNER_SMOKE_INTEGRATION_OK: native export, NBT, permissions, save packet, GUI draft, reload and removal cleanup");
                stage = 99; mc.stop();
            }
        } catch (Throwable e) { failure = e; }
    }

    private static void setupAndExport() throws Exception {
        var server = Minecraft.getInstance().getSingleplayerServer(); var level = server.overworld(); var player = server.getPlayerList().getPlayers().get(0);
        player.teleportTo(level, 8.5, 81, 10.5, 180, 25); player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true; player.onUpdateAbilities();
        level.setBlockAndUpdate(POS, ModBlocks.SCANNER.get().defaultBlockState().setValue(AltarStructureScannerBlock.FACING, Direction.NORTH));
        check(AltarScannerService.accessible(player, POS) != null, "authorized scanner");
        var nonOp = net.minecraftforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ScannerNonOp"));
        nonOp.setGameMode(GameType.CREATIVE); nonOp.setPos(8.5, 81, 10.5);
        check(AltarScannerService.accessible(nonOp, POS) != null, "non OP accepted");
        nonOp.setGameMode(GameType.SURVIVAL); check(AltarScannerService.accessible(nonOp, POS) != null, "non OP survival accepted");
        expectRejected(() -> AltarScannerService.accessible(player, POS.above()));
        player.setGameMode(GameType.SURVIVAL); check(AltarScannerService.accessible(player, POS) != null, "survival accepted"); player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true; player.onUpdateAbilities();
        player.teleportTo(level, 100.5, 81, 100.5, 0, 0); expectRejected(() -> AltarScannerService.accessible(player, POS)); player.teleportTo(level, 8.5, 81, 10.5, 180, 25);
        var config = new AltarStructureScannerConfig().anchored(POS, Direction.NORTH); config.blocksWide = 2; config.blocksLong = 2; config.structureZipName = "scanner_fixture";
        var bounds = ScannerBounds.of(config);
        var outsideHeight = config.anchored(new BlockPos(8, level.getMaxBuildHeight() - 1, 8), Direction.NORTH);
        check(!ScannerBounds.of(outsideHeight).isLoadedAndInsideWorld(level), "inclusive top exceeds build height");
        var unloaded = config.anchored(new BlockPos(100000, 80, 100000), Direction.NORTH);
        check(!ScannerBounds.of(unloaded).isLoadedAndInsideWorld(level), "unloaded chunks rejected");
        var outsideBorder = config.anchored(new BlockPos(30000000, 80, 30000000), Direction.NORTH);
        check(!ScannerBounds.of(outsideBorder).isLoadedAndInsideWorld(level), "world border rejected");
        for (var pos : BlockPos.betweenClosed(bounds.min(), bounds.max())) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS.north(), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        level.setBlockAndUpdate(POS.north(2), Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(POS.north().east(), Blocks.CHEST.defaultBlockState());
        var chest = (ChestBlockEntity) level.getBlockEntity(POS.north().east());
        chest.setCustomName(Component.literal("中文祭坛测试")); chest.setItem(0, new ItemStack(Items.DIAMOND, 3)); chest.setChanged();
        var root = Path.of(System.getProperty("altar.smokeProject"));
        if (!Boolean.getBoolean("altar.scannerSourceSmoke")) check(!DevStructurePaths.getStructureOutputDirectory().startsWith(root.resolve("src")), "isolated test exports stay outside real resources");
        Path directory = Files.createTempDirectory(root.resolve(".cache"), "scanner-export-");
        var file = PrefabStructureExport.scan(level, config, directory); var structure = PrefabStructureExport.validate(file);
        check(structure.getBlocks().size() == 3 && structure.tileEntities.size() == 1, "air excluded; water/state/NBT retained");
        try (var gzip = new GZIPInputStream(Files.newInputStream(file))) {
            var json = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            check(json.contains("中文祭坛测试") && json.contains("minecraft:diamond") && json.contains("water") && json.contains("axis"), "UTF-8 state and inventory NBT");
        }
        Path fixture = root.resolve(".cache/scanner-fixtures/assets/prefab_goety_altars_smoke/scanner_fixture.gz"); Files.createDirectories(fixture.getParent()); Files.copy(file, fixture, StandardCopyOption.REPLACE_EXISTING);
        LogUtils.getLogger().info("SCANNER_SMOKE_EXPORT_OK: {}", file);
        level.setBlockAndUpdate(POS.south(2), Blocks.STONE.defaultBlockState());
        player.setGameMode(GameType.SURVIVAL);
        LogUtils.getLogger().info("SCANNER_FIX_SURVIVAL_NON_OP_OK: survival player and non-OP fake player accepted");
        ready = true;
    }

    private static void expectRejected(Runnable action) { try { action.run(); } catch (IllegalArgumentException expected) { return; } throw new AssertionError("Request was not rejected"); }
    private static void check(boolean condition, String description) { if (!condition) throw new AssertionError(description); }
    private static void serverCheck(Checked action) { try { action.run(); } catch (Throwable e) { failure = e; } }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private ScannerSmokeChecks() {}
}

