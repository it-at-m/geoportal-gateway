package de.swm.lhm.geoportal.gateway.geoservice.inspect;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * workspace-qualified layername
 *
 * 
 * 
 */
public record QualifiedLayerName(String workspaceName, String layerName) {

    public static final String WORKSPACE_LAYER_SEPARATOR = ":";

    public QualifiedLayerName {
        workspaceName = workspaceName.toLowerCase(Locale.ROOT).strip();
        layerName = layerName.toLowerCase(Locale.ROOT).strip();
    }

    @Override
    public String toString() {
        return String.format("%s%s%s",
                workspaceName, WORKSPACE_LAYER_SEPARATOR, layerName);
    }

    public static Optional<QualifiedLayerName> fromStringWithWorkspaceFallback(Optional<String> defaultWorkspace, String layerName) {
        if (StringUtils.isBlank(layerName)) {
            return Optional.empty();
        }
        if (!layerName.contains(WORKSPACE_LAYER_SEPARATOR) && defaultWorkspace.isPresent()) {
            return Optional.of(new QualifiedLayerName(defaultWorkspace.get(), layerName));
        }
        return Optional.of(QualifiedLayerName.fromString(layerName));
    }

    public static QualifiedLayerName fromString(String layerName) {
        if (StringUtils.isBlank(layerName)) {
            throw new IllegalLayerNameSyntaxException("layerName cannot be empty");
        }
        List<String> parts = Splitter.onPattern(WORKSPACE_LAYER_SEPARATOR).splitToList(layerName);
        if (parts.size() != 2) {
            throw new IllegalLayerNameSyntaxException("invalid syntax for layer name: " + layerName);
        }
        return new QualifiedLayerName(parts.get(0), parts.get(1));
    }

    public static class IllegalLayerNameSyntaxException extends IllegalArgumentException {
        public IllegalLayerNameSyntaxException(String s) {
            super(s);
        }
    }
}
