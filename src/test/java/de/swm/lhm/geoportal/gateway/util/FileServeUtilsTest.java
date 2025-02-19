package de.swm.lhm.geoportal.gateway.util;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.swm.lhm.geoportal.gateway.util.FileServeUtils.getFileAsResourceEnsuringAbsoluteRootPath;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class FileServeUtilsTest {

    @Test
    void getFileAsResourceRootPathNotAbsoluteTest() {

        StepVerifier.create(getFileAsResourceEnsuringAbsoluteRootPath("../../test/"))
                .expectError(ResponseStatusException.class)
                .verify();

    }

    @Test
    void getFileAsResourceRootPathAbsoluteTest() {

        StepVerifier.create(getFileAsResourceEnsuringAbsoluteRootPath("/srv/"))
                .expectError(ResponseStatusException.class)
                .verify();

    }

    @Test
    void createRedirectToRootPathShouldRedirect() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        RequestPath requestPath = mock(RequestPath.class);
        HttpHeaders headers = mock(HttpHeaders.class);
        when(requestPath.value()).thenReturn("/some/path");
        when(request.getPath()).thenReturn(requestPath);
        when(response.getHeaders()).thenReturn(headers);
        when(response.setComplete()).thenReturn(Mono.empty());

        Mono<Void> result = FileServeUtils.createRedirectToRootPath(request, response, "");

        StepVerifier.create(result).verifyComplete();
        verify(headers).setLocation(URI.create("/some/path/"));
        verify(response).setStatusCode(HttpStatus.PERMANENT_REDIRECT);
    }

    @Test
    void createRedirectToRootPathWithFileExtensionShouldError() {
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        RequestPath requestPath = mock(RequestPath.class);
        when(requestPath.value()).thenReturn("/some/path");
        when(request.getPath()).thenReturn(requestPath);

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> FileServeUtils.createRedirectToRootPath(request, response, "file.txt").block())
                .withMessageContaining("file file.txt was requested on path /some/path")
                .withMessageContaining(HttpStatus.FORBIDDEN.name());
    }

    @Test
    void shouldThrowResponseStatusException() {
        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> FileServeUtils.throwNotFound("my message"))
                .withMessageContaining("404 NOT_FOUND \"my message\"")
                .withMessageContaining(HttpStatus.NOT_FOUND.name());
    }

    @Test
    void getFileAsResourceShouldReturnResourceWhenFileExists() throws Exception {
        Path rootPath = Files.createTempDirectory("root");
        String fileName = "testFile.txt";
        Path filePath = rootPath.resolve(fileName);
        Files.createFile(filePath);

        Mono<Resource> result = FileServeUtils.getFileAsResource(rootPath.toString(), fileName);

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    try {
                        return r.getFile().equals(filePath.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();

        Files.deleteIfExists(filePath);
        Files.deleteIfExists(rootPath);
    }

    @Test
    void getFileAsResourceEnsuringAbsoluteRootPathShouldReturnResourceWhenFileExists() throws Exception {
        Path rootPath = Files.createTempDirectory("root");
        String fileName = "testFile.txt";
        Path filePath = rootPath.resolve(fileName);
        Files.createFile(filePath);

        Mono<Resource> result = FileServeUtils.getFileAsResourceEnsuringAbsoluteRootPath(rootPath.toString(), fileName);

        StepVerifier.create(result)
                .expectNextMatches(r -> {
                    try {
                        return r.getFile().equals(filePath.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .verifyComplete();

        Files.deleteIfExists(filePath);
        Files.deleteIfExists(rootPath);
    }

    @Test
    void getFileAsResourceShouldErrorWhenFileNotFound() {
        String basePath = "/";
        String nonExistentFile = "does-not-exist.txt";

        Mono<Resource> result = FileServeUtils.getFileAsResource(basePath, nonExistentFile);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.NOT_FOUND &&
                        throwable.getMessage().contains("could not be found"))
                .verify();
    }

    @Test
    void getFileAsResourceEnsuringAbsoluteRootPathShouldErrorWhenNonAbsoluteRootPath() {
        String nonAbsolutePath = "relativePath";
        String fileName = "file.txt";

        Mono<Resource> result = FileServeUtils.getFileAsResourceEnsuringAbsoluteRootPath(nonAbsolutePath, fileName);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof ResponseStatusException &&
                        ((ResponseStatusException) throwable).getStatusCode() == HttpStatus.FORBIDDEN &&
                        throwable.getMessage().contains("Root path is not absolute"))
                .verify();
    }

}