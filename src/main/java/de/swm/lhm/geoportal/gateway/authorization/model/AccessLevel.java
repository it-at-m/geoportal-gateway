package de.swm.lhm.geoportal.gateway.authorization.model;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Enum for the access level of a component
 */
public enum AccessLevel {
    PUBLIC {
        @Override
        public int getOrder() {
            return 0;
        }

        @Override
        public String asMetadataClassificationCode() {
            return CLASSIFICATION_UNCLASSIFIED;
        }
    },
    PROTECTED {
        @Override
        public int getOrder() {
            return 1;
        }

        @Override
        public String asMetadataClassificationCode() {
            return CLASSIFICATION_RESTRICTED;
        }
    },
    RESTRICTED { //aka hidden

        @Override
        public int getOrder() {
            return 2;
        }

        @Override
        public String asMetadataClassificationCode() {
            return CLASSIFICATION_SECRET;
        }
    };

    private static final String CLASSIFICATION_UNCLASSIFIED = "unclassified";
    private static final String CLASSIFICATION_RESTRICTED = "restricted";
    private static final String CLASSIFICATION_CONFIDENTIAL = "confidential";
    private static final String CLASSIFICATION_SECRET = "secret";
    private static final String CLASSIFICATION_TOPSECRET = "topsecret";

    public static Optional<AccessLevel> fromMetadataClassificationCode(String metadataClassificationCode) {
        return switch (metadataClassificationCode.toLowerCase(Locale.ROOT)) {
            case CLASSIFICATION_UNCLASSIFIED -> Optional.of(AccessLevel.PUBLIC);
            case CLASSIFICATION_RESTRICTED, CLASSIFICATION_CONFIDENTIAL -> Optional.of(AccessLevel.PROTECTED);
            case CLASSIFICATION_SECRET, CLASSIFICATION_TOPSECRET -> Optional.of(AccessLevel.RESTRICTED);
            default -> Optional.empty();
        };
    }

    public abstract int getOrder();

    public abstract String asMetadataClassificationCode();

    public AccessLevel getHighestAccessLevel(AccessLevel otherAccessLevel) {
        Set<AccessLevel> accessLevels = Set.of(this, otherAccessLevel);
        return findHighestAccessLevel(accessLevels);
    }

    public static AccessLevel findHighestAccessLevel(Set<AccessLevel> accessLevels) {
        if (accessLevels.contains(AccessLevel.RESTRICTED)) {
            return AccessLevel.RESTRICTED;
        } else if (accessLevels.contains(AccessLevel.PROTECTED)) {
            return AccessLevel.PROTECTED;
        } else {
            return AccessLevel.PUBLIC;
        }
    }
}
