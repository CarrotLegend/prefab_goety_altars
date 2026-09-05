package com.prefabgoetyaltars.dev.scan;

import com.prefabgoetyaltars.network.*;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ScannerTest {
    @TempDir Path temporary;

    @Test void failureReasonsAreDistinctAndBlankDraftRemainsSaveable() {
        assertEquals(ScannerException.Reason.NO_SENDER, assertThrows(ScannerException.class, () -> AltarScannerService.accessible(null, BlockPos.ZERO)).reason());
        assertEquals(ScannerException.Reason.MISSING_BLOCK_ENTITY, assertThrows(ScannerException.class, () -> AltarScannerService.requireScannerEntity(null)).reason());
        var config = new AltarStructureScannerConfig();
        assertDoesNotThrow(() -> AltarStructureScannerConfig.fromTag(config.GetCompoundTag()));
        assertEquals(ScannerException.Reason.INVALID_NAME, assertThrows(ScannerException.class, () -> AltarStructureScannerConfig.normalizeName("")).reason());
        config.blocksWide = 0;
        assertEquals(ScannerException.Reason.INVALID_RANGE, assertThrows(ScannerException.class, config::validate).reason());
        var bad = new AltarStructureScannerConfig().GetCompoundTag(); bad.remove("structureZipName");
        assertEquals(ScannerException.Reason.INVALID_NAME, assertThrows(ScannerException.class, () -> AltarStructureScannerConfig.fromTag(bad)).reason());
        assertEquals(ScannerException.Reason.WRITE_FAILED, assertThrows(ScannerException.class, () -> PrefabStructureExport.writeValidated(temporary, "empty", p -> {})).reason());
        assertEquals(ScannerException.Reason.CORRUPT_EXPORT, assertThrows(ScannerException.class, () -> PrefabStructureExport.writeValidated(temporary, "broken", p -> gzip(p, "null"))).reason());
    }

    @Test void directionsOffsetsAndInclusiveHeight() {
        for (Direction facing : Direction.Plane.HORIZONTAL) for (int left : new int[]{-128, 0, 128}) for (int forward : new int[]{-128, 0, 128}) {
            var c = new AltarStructureScannerConfig(); c.direction = facing; c.blockPos = new BlockPos(7, 100, -9);
            c.blocksToTheLeft = left; c.blocksParallel = forward; c.blocksDown = 3;
            c.blocksWide = 3; c.blocksLong = 4; c.blocksTall = 2;
            var b = ScannerBounds.of(c);
            int dx = facing.getStepX(), dz = facing.getStepZ();
            assertEquals(new BlockPos(7 + dz * left + dx * forward, 97, -9 - dx * left + dz * forward), b.corner());
            assertEquals(b.corner().offset(dx * 3 - dz * 2, 2, dz * 3 + dx * 2), b.otherCorner());
            assertEquals(36, b.box().getXsize() * b.box().getYsize() * b.box().getZsize());
            assertEquals(3, b.clear().getShape().getWidth()); assertEquals(4, b.clear().getShape().getLength());
            c.blocksWide = c.blocksLong = c.blocksTall = 1;
            assertEquals(ScannerBounds.of(c).corner().above(), ScannerBounds.of(c).otherCorner()); assertEquals(2, c.volume());
        }
    }

    @Test void nbtAndAllPacketsRoundTrip() {
        var c = new AltarStructureScannerConfig(); c.blockPos = new BlockPos(2, 77, -9); c.direction = Direction.WEST;
        c.blocksToTheLeft = -14; c.blocksParallel = -7; c.structureZipName = "My Altar";
        assertEquals(c.GetCompoundTag(), AltarStructureScannerConfig.fromTag(c.GetCompoundTag()).GetCompoundTag());
        var copy = c.copy(); copy.blocksLong = 5; assertEquals(1, c.blocksLong);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var scan = new ScanAltarStructurePacket(c.blockPos, c.GetCompoundTag()); ScanAltarStructurePacket.encode(scan, buf); assertEquals(scan, ScanAltarStructurePacket.decode(buf));
            var save = new SyncAltarScannerPacket(c.blockPos, c.GetCompoundTag()); SyncAltarScannerPacket.encode(save, buf); assertEquals(save, SyncAltarScannerPacket.decode(buf));
            var open = new OpenAltarScannerPacket(c.blockPos, c.GetCompoundTag()); OpenAltarScannerPacket.encode(open, buf); assertEquals(open, OpenAltarScannerPacket.decode(buf));
        } finally { buf.release(); }
        var bad = c.GetCompoundTag(); bad.remove("blocksWide"); assertThrows(IllegalArgumentException.class, () -> AltarStructureScannerConfig.fromTag(bad));
        var vertical = c.GetCompoundTag(); vertical.putInt("direction", 1); assertThrows(IllegalArgumentException.class, () -> AltarStructureScannerConfig.fromTag(vertical));
        assertThrows(IllegalArgumentException.class, () -> AltarStructureScannerConfig.fromTag(null));
    }

    @Test void dimensionVolumeAndPermissionBoundaries() {
        var c = new AltarStructureScannerConfig(); c.blocksWide = 100; c.blocksLong = 100; c.blocksTall = 99;
        assertEquals(1_000_000, c.volume()); assertDoesNotThrow(c::validate);
        c.blocksTall = 100; assertThrows(IllegalArgumentException.class, c::validate);
        c.blocksLong = c.blocksWide = 1; c.blocksTall = 128; assertDoesNotThrow(c::validate);
        c.blocksTall = 129; assertThrows(IllegalArgumentException.class, c::validate);
        c.blocksTall = 0; assertThrows(IllegalArgumentException.class, c::validate);
        assertTrue(AltarScannerService.withinDistance(0));
        assertTrue(AltarScannerService.withinDistance(256));
        assertFalse(AltarScannerService.withinDistance(256.001));
        assertFalse(AltarScannerService.withinDistance(Double.NaN));
        assertFalse(AltarScannerService.withinDistance(-1));
    }

    @Test void namesAndProjectPathRestrictions() throws Exception {
        assertEquals("my_altar_01", AltarStructureScannerConfig.normalizeName("  My  Altar!01 "));
        for (String name : new String[]{"", "  ", "中文", "CON", "prn", "LPT9", "aux", "nul", "a".repeat(129)})
            assertThrows(IllegalArgumentException.class, () -> AltarStructureScannerConfig.normalizeName(name));
        assertEquals("_escape", AltarStructureScannerConfig.normalizeName("../escape"));
        DevStructurePaths.outputDirectory(temporary);
        assertEquals(temporary.resolve(DevStructurePaths.EXPORT_DIRECTORY).toRealPath(), DevStructurePaths.outputDirectory(temporary));
        Files.writeString(temporary.resolve("build.gradle"), "// test project");
        Files.writeString(temporary.resolve("gradle.properties"), "mod_id=other");
        DevStructurePaths.outputDirectory(temporary);
        assertEquals(temporary.resolve(DevStructurePaths.EXPORT_DIRECTORY).toRealPath(), DevStructurePaths.outputDirectory(temporary));
        Files.writeString(temporary.resolve("gradle.properties"), "mod_id=prefab_goety_altars");
        Path run = Files.createDirectories(temporary.resolve("run/deeper"));
        assertEquals(run.resolve(DevStructurePaths.EXPORT_DIRECTORY), DevStructurePaths.outputDirectory(run));
        Files.createDirectories(temporary.resolve("src/main/resources"));
        Path output = DevStructurePaths.outputDirectory(run);
        assertEquals(temporary.resolve("src/main/resources/assets/prefab_goety_altars/structures").toRealPath(), output);
    }

    @Test void existingAndCorruptFilesNeverPublish() throws Exception {
        Files.writeString(temporary.resolve("existing.gz"), "keep me");
        assertThrows(FileAlreadyExistsException.class, () -> PrefabStructureExport.writeValidated(temporary, "existing", p -> fail("must not write")));
        assertEquals("keep me", Files.readString(temporary.resolve("existing.gz")));
        assertThrows(ScannerException.class, () -> PrefabStructureExport.writeValidated(temporary, "empty", p -> {}));
        assertThrows(ScannerException.class, () -> PrefabStructureExport.writeValidated(temporary, "bad_json", p -> gzip(p, "not JSON")));
        assertThrows(ScannerException.class, () -> PrefabStructureExport.writeValidated(temporary, "null_json", p -> gzip(p, "null")));
        Path corrupt = temporary.resolve("corrupt.gz"); gzip(corrupt, "{}");
        byte[] bytes = Files.readAllBytes(corrupt); bytes[bytes.length - 8] ^= 1; Files.write(corrupt, bytes);
        assertThrows(IOException.class, () -> PrefabStructureExport.validate(corrupt));
        try (var paths = Files.list(temporary)) { assertEquals(2, paths.count()); }
    }

    @Test void nativeWriterPublishesOnceAndRaceDoesNotReplace() throws Exception {
        var structure = new com.wuest.prefab.structures.base.Structure();
        structure.setClearSpace(ScannerBounds.of(new AltarStructureScannerConfig()).clear());
        var result = PrefabStructureExport.writeValidated(temporary, "Native Export", p -> com.wuest.prefab.structures.base.Structure.CreateStructureFile(structure, p.toString()));
        assertEquals("native_export.gz", result.getFileName().toString());
        assertEquals(1, PrefabStructureExport.validate(result).getClearSpace().getShape().getWidth());
        assertThrows(FileAlreadyExistsException.class, () -> PrefabStructureExport.writeValidated(temporary, "race", p -> {
            com.wuest.prefab.structures.base.Structure.CreateStructureFile(structure, p.toString());
            try { Files.writeString(temporary.resolve("race.gz"), "concurrent file"); } catch (IOException e) { throw new UncheckedIOException(e); }
        }));
        assertEquals("concurrent file", Files.readString(temporary.resolve("race.gz")));
        try (var files = Files.list(temporary)) { assertEquals(2, files.count()); }
    }

    @Test void symbolicDirectoryCannotEscapeProject() throws Exception {
        Path project = Files.createDirectory(temporary.resolve("project"));
        Path outside = Files.createDirectory(temporary.resolve("outside"));
        Files.writeString(project.resolve("build.gradle"), "// project"); Files.writeString(project.resolve("gradle.properties"), "mod_id=prefab_goety_altars");
        try { Files.createSymbolicLink(project.resolve("src"), outside); }
        catch (IOException | UnsupportedOperationException e) {
            if (!System.getProperty("os.name").startsWith("Windows")) org.junit.jupiter.api.Assumptions.abort("Symbolic links unavailable: " + e.getMessage());
            // Directory junctions do not need Windows' symbolic-link privilege.
            var junction = new ProcessBuilder("cmd.exe", "/c", "mklink", "/J", project.resolve("src").toString(), outside.toString()).redirectErrorStream(true).start();
            String output = new String(junction.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, junction.waitFor(), output);
        }
        try {
            assertThrows(IOException.class, () -> DevStructurePaths.outputDirectory(project));
            assertFalse(Files.exists(outside.resolve("main")));
        } finally {
            // Delete the link itself before JUnit walks the temporary directory tree.
            Files.delete(project.resolve("src"));
        }
    }

    private static void gzip(Path path, String content) {
        try (var out = new GZIPOutputStream(Files.newOutputStream(path))) { out.write(content.getBytes(StandardCharsets.UTF_8)); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}


