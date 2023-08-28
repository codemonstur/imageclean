package imageclean;

import bobthebuildtool.pojos.buildfile.Project;
import bobthebuildtool.pojos.error.VersionTooOld;
import jcli.errors.InvalidCommandLine;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static bobthebuildtool.services.Update.requireBobVersion;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;
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
                walkList(path, classes, writer);
            else
                walkRecursively(path, classes, writer);
        }

        return 0;
    }

    private static List<Path> toPaths(final Project project, final List<String> paths) {
        if (paths != null) return paths.stream().map(Paths::get).collect(toList());
        return List.of(project.parentDir.resolve("src").resolve("main").resolve("images"));
    }

    private static void walkList(final Path path, final Path classes, final ExifRewriter writer)
            throws IOException {
        try (final var list = Files.list(path)) {
            for (final var inFile : list.collect(toList())) {
                try {
                    processImage(inFile, classes, path, writer);
                } catch (final ImageWriteException | ImageReadException e) {
                    throw new IOException("Failed to remove Exif data from image", e);
                }
            }
        }
    }

    private static void walkRecursively(final Path path, final Path classes, final ExifRewriter writer) throws IOException {
        Files.walkFileTree(path, new OnlyFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path inFile, final BasicFileAttributes attrs) throws IOException {
                try {
                    processImage(inFile, classes, path, writer);
                    return CONTINUE;
                } catch (final ImageWriteException | ImageReadException e) {
                    throw new IOException("Failed to remove Exif data from image", e);
                }
            }
        });
    }

    private static void processImage(final Path inFile, final Path classes, final Path root, final ExifRewriter writer)
                throws ImageWriteException, IOException, ImageReadException {
        if (!isRegularFile(inFile)) return;
        if (!inFile.endsWith(".jpg")) return;

        final var outFile = classes.resolve(root.relativize(inFile).toString());
        removeExifData(inFile, outFile, writer);
    }

    private static void removeExifData(final Path inFile, final Path outFile, final ExifRewriter writer)
            throws ImageWriteException, IOException, ImageReadException {
        try (final var in = Files.newInputStream(inFile);
             final var out = Files.newOutputStream(outFile)) {
            writer.removeExifMetadata(in, out);
        }
    }

}
