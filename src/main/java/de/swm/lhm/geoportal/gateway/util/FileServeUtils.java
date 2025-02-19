package de.swm.lhm.geoportal.gateway.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;


@Slf4j
@UtilityClass
public class FileServeUtils {

    // https://stackoverflow.com/questions/71926588/spring-test-mock-static-method-globally

    public static final String FILE_COULD_NOT_BE_FOUND = "Requested file >%s< could not be found";
    public static final String ROOT_PATH_IS_NOT_ABSOLUTE = "Root path is not absolute";

    public static Mono<Void> createRedirectToRootPath(
            final ServerHttpRequest request,
            final ServerHttpResponse response,
            final String name
    ) {

        String path = request.getPath().value();
        String fileExtension = FilenameUtils.getExtension(name);

        if (!fileExtension.isEmpty()) {
            String msg = String.format(
                    "file %s was requested on path %s",
                    name,
                    path
            );
            log.atError().setMessage(msg).log();
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            msg
                    )
            );
        }

        response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        // path + / should redirect to index.html
        // otherwise the relative paths inside the index.html
        // cannot be loaded
        response.getHeaders().setLocation(URI.create(path + "/"));
        return response.setComplete();

    }

    public static void throwNotFound(String message
    ) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }


    public static Mono<Resource> getFileAsResource(String basePath, String filePath) {
        Path rootPath = Paths.get(basePath);
        Path normalizedFilePath = Paths.get(filePath).normalize();
        Path resolvedPath = rootPath.resolve(normalizedFilePath).normalize();

        log.debug(
                "trying to load file as resource, basePath = {}, filePath = {}, resolvedPath={}",
                basePath,
                filePath,
                resolvedPath
        );

        if (!resolvedPath.startsWith(rootPath)) {
            log.error("Access to {} forbidden as not in allowed folder {}", resolvedPath, rootPath);
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Access to forbidden folder"));
        }

        if (!resolvedPath.toFile().isFile()) {
            log.error("Could not find file {}", resolvedPath);
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            String.format(FILE_COULD_NOT_BE_FOUND, resolvedPath)
                    )
            );


        }

        return Mono.just(new FileSystemResource(resolvedPath)); // using nio2
    }


    public static Mono<Resource> getFileAsResourceEnsuringAbsoluteRootPath(
            String rootPath,
            String... morePathElements
    ) {

        log.debug(
                "trying to load file as resource, rootPath = {}, morePathElements = {}",
                rootPath,
                morePathElements
        );

        if (!isAbsolute(rootPath)) {
            log.error("{}, rootPath={}", ROOT_PATH_IS_NOT_ABSOLUTE, rootPath);
            return Mono.error(
                    new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            String.format(ROOT_PATH_IS_NOT_ABSOLUTE)
                    )
            );
        }

        File file = Paths.get(rootPath, morePathElements)
                .toFile();

        if (file.isFile()) {
            return Mono.just(
                    new FileSystemResource(file)
            );
        }

        log.error(
                "Could not find file {}, rootPath = {}, morePathElements = {}",
                file.getAbsolutePath(),
                rootPath,
                morePathElements
        );

        String filePath = Paths.get("", morePathElements)
                .toFile()
                .getPath();

        return Mono.error(
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        String.format(FILE_COULD_NOT_BE_FOUND, filePath)
                )
        );
    }

    private static boolean isAbsolute(final String path) {
        return FilenameUtils.getPrefixLength(path) != 0;
    }

}
