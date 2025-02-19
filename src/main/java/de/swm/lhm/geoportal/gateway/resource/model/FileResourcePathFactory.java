package de.swm.lhm.geoportal.gateway.resource.model;

import de.swm.lhm.geoportal.gateway.resource.ResourceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileResourcePathFactory {

    private final ResourceProperties resourceProperties;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public Optional<FileResourcePath> createFileResourcePath(String requestPath) {
        String filePath = matcher.extractPathWithinPattern("%s/**".formatted(resourceProperties.getEndpoint()), requestPath);
        String pattern = String.format("{unit}/%s/**", resourceProperties.getDocumentsFolder());
        boolean isMatch = matcher.match(pattern, filePath);

        if (isMatch) {
            String unit = matcher.extractUriTemplateVariables(pattern, filePath).get("unit");
            String filePathFromDocumentsFolder = matcher.extractPathWithinPattern("/%s/%s/**".formatted(unit, resourceProperties.getDocumentsFolder()), filePath);
            return Optional.of(FileResourcePath.builder()
                    .unit(unit)
                    .filePathWithinDocumentsFolder(filePathFromDocumentsFolder)
                    .build());
        } else {
            log.atInfo().setMessage(() -> String.format("FileResourcePath can not be created as the path %s does not match the pattern %s", filePath, pattern)).log();
            return Optional.empty();
        }
    }
}