package de.swm.lhm.geoportal.gateway.resource.model;

import de.swm.lhm.geoportal.gateway.geoservice.inspect.QualifiedLayerName;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Data
@Builder
@ToString
public class FileResourcePath {
    // The file path of a resource is /unit/{documentsFolder}/{layer_name?}/{column_name?}/{file_name}
    String unit;
    String filePathWithinDocumentsFolder;

    public Optional<QualifiedLayerName> extractQualifiedLayerName() {

        if (unit == null || filePathWithinDocumentsFolder == null) {
            return Optional.empty();
        }

        Path filePath = Paths.get(filePathWithinDocumentsFolder.replace("\\", "/"));

        if (filePath.getNameCount() <= 1)
            return Optional.empty();

        return Optional.of(new QualifiedLayerName(unit, filePath.getName(0).toString()));

    }
}
