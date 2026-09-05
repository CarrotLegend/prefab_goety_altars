package com.prefabgoetyaltars.dev.scan;

import com.google.gson.GsonBuilder;
import com.wuest.prefab.UppercaseEnumAdapter;
import com.wuest.prefab.structures.base.Structure;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public final class PrefabStructureExport {
    public static Path scan(ServerLevel level, AltarStructureScannerConfig config, Path outputDirectory) throws IOException {
        var bounds = ScannerBounds.of(config);
        bounds.validateWorld(level);
        return writeValidated(outputDirectory, config.structureZipName, temporary -> Structure.ScanStructure(
                level, config.blockPos, bounds.corner(), bounds.otherCorner(), temporary.toString(),
                bounds.clear(), config.direction, false, false));
    }

    public static Path writeValidated(Path directory, String inputName, Consumer<Path> prefabWriter) throws IOException {
        String name = AltarStructureScannerConfig.normalizeName(inputName);
        Files.createDirectories(directory);
        Path base = directory.toRealPath();
        Path target = base.resolve(name + ".gz").normalize();
        if (!target.getParent().equals(base)) throw new IOException("Invalid output path");
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new FileAlreadyExistsException(target.getFileName().toString());
        Path temporary = Files.createTempFile(base, ".altar-scan-", ".gz");
        try {
            prefabWriter.accept(temporary);
            if (Files.size(temporary) == 0) throw new ScannerException(ScannerException.Reason.WRITE_FAILED, "Prefab wrote an empty file: " + temporary);
            try { validate(temporary); }
            catch (IOException e) { throw new ScannerException(ScannerException.Reason.CORRUPT_EXPORT, "Prefab export failed validation: " + temporary, e); }
            return Files.move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Structure validate(Path file) throws IOException {
        if (!Files.isRegularFile(file) || Files.size(file) == 0) throw new IOException("Prefab produced no structure data");
        try (var input = new GZIPInputStream(Files.newInputStream(file))) {
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Direction.class, new UppercaseEnumAdapter()).create();
            Structure structure = gson.fromJson(json, Structure.class);
            if (structure == null || structure.getClearSpace() == null || structure.getClearSpace().getShape() == null
                    || structure.getClearSpace().getStartingPosition() == null || structure.getBlocks() == null
                    || structure.tileEntities == null || structure.entities == null) throw new IOException("Missing Prefab structure fields");
            var shape = structure.getClearSpace().getShape();
            if (shape.getDirection() == null || !shape.getDirection().getAxis().isHorizontal()
                    || shape.getWidth() < 1 || shape.getLength() < 1 || shape.getHeight() < 1) throw new IOException("Invalid Prefab clear space");
            return structure;
        } catch (RuntimeException exception) {
            throw new IOException("Unable to parse exported Prefab structure", exception);
        }
    }
    private PrefabStructureExport() {}
}
