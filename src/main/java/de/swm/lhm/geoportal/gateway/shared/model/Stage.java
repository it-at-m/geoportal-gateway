package de.swm.lhm.geoportal.gateway.shared.model;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


/**
 * Enum for the Stage of a Portal
 */
public enum Stage {

    // only valid for products and portals
    UNSTAGED {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.empty();
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.of(CONFIGURATION);
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_UNSTAGED;
        }

        @Override
        public boolean isSame(Stage stage) {
            return stage.equals(this) || stage.equals(Stage.CONFIGURATION);
        }
    },
    CONFIGURATION {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.empty();
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.of(QS);
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_CONFIGURATION;
        }

        @Override
        public boolean isSame(Stage stage) {
            return stage.equals(this) || stage.equals(Stage.UNSTAGED);
        }
    },
    QS {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.of(CONFIGURATION);
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.of(PROD);
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_QS;
        }
    },
    PROD {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.of(QS);
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.empty();
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_PROD;
        }
    },
    // only valid for geo-services
    CACHING {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.empty();
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.empty();
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_CACHING;
        }
    },

    FILE_CONFIGURATION {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.empty();
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.empty();
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_FILE_CONFIGURATION;
        }
    },

    FILE_QS {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.empty();
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.empty();
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_FILE_QS;
        }
    },

    FILE_PROD {
        @Override
        public Optional<Stage> previousStage() {
            return Optional.empty();
        }

        @Override
        public Optional<Stage> nextStage() {
            return Optional.empty();
        }

        @Override
        public int getOrder() {
            return OrderConstants.ORDER_FILE_PROD;
        }
    }
    //@formatter: off
    ;
    //@formatter: on

    public static boolean exists(String stageName) {
        return Stream.of(Stage.values()).anyMatch(stage -> stage.name().equalsIgnoreCase(stageName));
    }

    public static List<Stage> getNextStages(final Stage stage) {
        List<Stage> nextStages = new ArrayList<>();
        Stage nextStage = stage;
        while (true) {
            Optional<Stage> nextStageOptional = nextStage.nextStage();
            if (nextStageOptional.isPresent()) {
                nextStage = nextStageOptional.get();
                nextStages.add(nextStage);
            } else {
                break;
            }
        }
        return nextStages;
    }

    public abstract Optional<Stage> nextStage();

    public static List<Stage> nonProdStages() {
        return Arrays.stream(Stage.values()).filter(stage -> stage != Stage.PROD).toList();
    }

    public static List<Stage> realStages() {
        /*
        Some stages are not real stages, but were created as helpers for specific cases.
        It would be useful to check why and where these stages like CACHING or FILE_... are used and whether they cannot be deleted.
        In many places in the application it is necessary to loop over all real/relevant stages.
        But such a plain loop would also provide these non-relevant stages. Therefore this method was created.
        */
        return List.of(Stage.CONFIGURATION, Stage.QS, Stage.PROD);
    }

    public static Optional<Stage> realStageAsFileStage(Stage stage) {
        final Stage fileStage = switch (stage) {
            case CONFIGURATION, FILE_CONFIGURATION -> Stage.FILE_CONFIGURATION;
            case QS, FILE_QS -> Stage.FILE_QS;
            case PROD, FILE_PROD -> Stage.FILE_PROD;
            default -> null;
        };
        return Optional.ofNullable(fileStage);
    }

    public static Optional<Stage> fileStageAsRealStage(Stage stage) {
        final Stage fileStage = switch (stage) {
            case CONFIGURATION, FILE_CONFIGURATION -> Stage.CONFIGURATION;
            case QS, FILE_QS -> Stage.QS;
            case PROD, FILE_PROD -> Stage.PROD;
            default -> null;
        };
        return Optional.ofNullable(fileStage);
    }

    public boolean isSame(Stage stage) {
        return this.equals(stage);
    }

    public abstract Optional<Stage> previousStage();

    public abstract int getOrder();

    private static final class OrderConstants {
        private static final int ORDER_UNSTAGED = 0;
        private static final int ORDER_CONFIGURATION = 1;
        private static final int ORDER_QS = 2;
        private static final int ORDER_PROD = 3;
        private static final int ORDER_CACHING = 4;
        private static final int ORDER_FILE_CONFIGURATION = 5;
        private static final int ORDER_FILE_QS = 6;
        private static final int ORDER_FILE_PROD = 7;
    }
}
