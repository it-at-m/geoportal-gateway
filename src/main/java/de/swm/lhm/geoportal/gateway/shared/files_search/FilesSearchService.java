package de.swm.lhm.geoportal.gateway.shared.files_search;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class FilesSearchService {

    public static String normalizePath(String path) {

        if (path == null)
            throw new IllegalArgumentException("path cannot be null");

        return new File(
                path.trim()
                        .replace('/', File.separatorChar)
                        .replace('\\', File.separatorChar)
        ).toString()
                .replace("\\", "/");
    }

    public static Path prepareBaseDir(final String path) {

        String newPath = normalizePath(path);

        if (newPath.endsWith("" + File.separatorChar))
            newPath = path.substring(0, newPath.length() - ("" + File.separatorChar).length());

        return FileSystems.getDefault()
                .getPath(newPath)
                .normalize()
                .toAbsolutePath();

    }

    public static List<PathMatcher> prepareGlobPattern(Path basePath, String globPattern) {
        List<PathMatcher> result = new ArrayList<>();
        for (String pattern : Splitter.on(';').split(globPattern)) {
            pattern = normalizePath(pattern);
            if (pattern.startsWith("" + File.separatorChar)) {
                pattern = pattern.substring(("" + File.separatorChar).length());
            }
            if (pattern.startsWith(basePath.toString())) {
                pattern = pattern.substring(basePath.toString().length());
            }
            String path = normalizePath(basePath.toString());

            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            result.add(
                    FileSystems.getDefault()
                            .getPathMatcher("glob:" + path + "/" + pattern)
            );
        }
        return result;

    }

    public List<String> getFilenamesFromDirectory(
            String fileIdentifier,
            String dir,
            String globPattern
    ) throws IOException {

        Path basePath = prepareBaseDir(dir);
        final List<PathMatcher> matchers = prepareGlobPattern(basePath, globPattern);

        log.debug("getFileNamesFromDirectory started with parameters fileIdentifier: {}, dir: {}, globPattern: {}",
                fileIdentifier, dir, globPattern);

        List<String> result = new ArrayList<>();

        Files.walkFileTree(
                basePath,
                new SimpleFileVisitor<>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                        log.trace("checking file: {}", file);

                        if (!attrs.isRegularFile()) {
                            return FileVisitResult.CONTINUE;
                        }

                        for (PathMatcher matcher : matchers) {
                            if (matcher.matches(file)) {
                                log.trace("file matches: {}", file);
                                result.add(
                                        fileIdentifier
                                                + "./"
                                                + basePath.relativize(file)
                                                .toString()
                                                .replace("\\", "/")
                                );
                                break;
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                });

        log.debug("getFileNamesFromDirectory found {} files in dir: {}", result.size(), dir);

        return result;

    }

}