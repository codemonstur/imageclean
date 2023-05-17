package imageclean;

import bobthebuildtool.pojos.buildfile.Project;
import bobthebuildtool.pojos.error.VersionTooOld;
import jcli.errors.InvalidCommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static bobthebuildtool.services.Update.requireBobVersion;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.isRegularFile;
import static java.util.stream.Collectors.toList;
import static jcli.CliParserBuilder.newCliParser;

public enum BobPlugin {;

    private static final String DESCRIPTION = "Copies images from a resource directory to target/classes and cleans them of metadata";

    public static void installPlugin(final Project project) throws VersionTooOld {
        requireBobVersion("7");
        project.addCommand("imageclean", DESCRIPTION, BobPlugin::copyImages);
    }

    private static int copyImages(final Project project, final Map<String, String> env, final String[] args)
            throws InvalidCommandLine, IOException {

        final var arguments = newCliParser(CliImageClean::new).parse(args);
        final var paths = toPaths(project, arguments.paths);
        final var classes = project.getBuildTarget().resolve("classes");

        final var writer = new ExifRewriter();
        for (final var path : paths) {
            if (arguments.noRecursion)
                ;
            else walkRecursively(path, classes, writer);
        }

        return 0;
    }

    private static List<Path> toPaths(final Project project, final List<String> paths) {
        if (paths != null) return paths.stream().map(Paths::get).collect(toList());
        return List.of(project.parentDir.resolve("src").resolve("main").resolve("images"));
    }

    private static void walkList(final Path path, final Path classes, final ExifRewriter writer) throws IOException {
        try (final var list = Files.list(path)) {
            
            for (final var file : list) {

            }
        }
    }

    private static void walkRecursively(final Path path, final Path classes, final ExifRewriter writer) {
        Files.walkFileTree(path, new OnlyFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path inFile, BasicFileAttributes attrs) throws IOException {
                if (!isRegularFile(inFile)) return CONTINUE;
                if (!inFile.endsWith(".jpg")) return CONTINUE;

                final var outFile = classes.resolve(path.relativize(inFile).toString());
                try (final var in = Files.newInputStream(inFile);
                     final var out = Files.newOutputStream(outFile)) {
                    writer.removeExifMetadata(in, out);
                }

                return CONTINUE;
            }
        });
    }

}
