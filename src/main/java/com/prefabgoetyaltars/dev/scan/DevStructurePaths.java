package com.prefabgoetyaltars.dev.scan;

import com.prefabgoetyaltars.PrefabGoetyAltars;
import net.minecraftforge.fml.loading.FMLPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DevStructurePaths {
    public static final String RESOURCE_DIRECTORY = "assets/prefab_goety_altars/structures";
    public static final String EXPORT_DIRECTORY = "prefab_goety_altars_exports";

    public static Path getStructureOutputDirectory() throws IOException {
        return outputDirectory(FMLPaths.GAMEDIR.get());
    }

    public static Path outputDirectory(Path gameDirectory) throws IOException {
        Path game = gameDirectory.toAbsolutePath().normalize().toRealPath();
        Path project = findSourceProject(game);
        Path root = project == null ? game : project.toRealPath();
        Path output = project == null ? root.resolve(EXPORT_DIRECTORY) : root.resolve("src/main/resources").resolve(RESOURCE_DIRECTORY);
        output = output.normalize();
        Path existing = output;
        while (!Files.exists(existing, java.nio.file.LinkOption.NOFOLLOW_LINKS)) existing = existing.getParent();
        if (!existing.toRealPath().startsWith(root)) throw new IOException("Output directory escapes the project");
        Files.createDirectories(output);
        Path realOutput = output.toRealPath();
        if (!realOutput.startsWith(root)) throw new IOException("Output directory escapes the project");
        if (!Files.isDirectory(realOutput) || !Files.isWritable(realOutput)) throw new IOException("Export directory is not writable: " + realOutput);
        return realOutput;
    }

    public static Path findProjectRoot(Path gameDirectory) throws IOException {
        Path project = findSourceProject(gameDirectory);
        if (project == null) throw new IOException("No source project above " + gameDirectory);
        return project;
    }

    private static Path findSourceProject(Path gameDirectory) throws IOException {
        for (Path candidate = gameDirectory.toAbsolutePath().normalize(); candidate != null; candidate = candidate.getParent()) {
            Path properties = candidate.resolve("gradle.properties");
            if (Files.isRegularFile(candidate.resolve("build.gradle")) && Files.isRegularFile(properties)) {
                var values = new Properties();
                try (var reader = Files.newBufferedReader(properties)) { values.load(reader); }
                if (PrefabGoetyAltars.MOD_ID.equals(values.getProperty("mod_id"))) {
                    Path resources = candidate.resolve("src/main/resources");
                    Path existing = resources;
                    while (!Files.exists(existing, java.nio.file.LinkOption.NOFOLLOW_LINKS)) existing = existing.getParent();
                    if (!existing.toRealPath().startsWith(candidate.toRealPath())) throw new IOException("Source directory escapes the project: " + existing);
                    if (Files.isDirectory(resources)) return candidate;
                }
            }
        }
        return null;
    }
    private DevStructurePaths() {}
}

