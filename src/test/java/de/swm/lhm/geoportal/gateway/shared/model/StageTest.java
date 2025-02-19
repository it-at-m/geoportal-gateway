package de.swm.lhm.geoportal.gateway.shared.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StageTest {

    @Test
    void testPreviousStage() {
        assertAll("Test previous stage for each stage",
                () -> assertFalse(Stage.UNSTAGED.previousStage().isPresent()),
                () -> assertFalse(Stage.CONFIGURATION.previousStage().isPresent()),
                () -> assertEquals(Stage.CONFIGURATION, Stage.QS.previousStage().orElse(null)),
                () -> assertEquals(Stage.QS, Stage.PROD.previousStage().orElse(null)),
                () -> assertFalse(Stage.CACHING.previousStage().isPresent()),
                () -> assertFalse(Stage.FILE_CONFIGURATION.previousStage().isPresent()),
                () -> assertFalse(Stage.FILE_QS.previousStage().isPresent()),
                () -> assertFalse(Stage.FILE_PROD.previousStage().isPresent())
        );
    }

    @Test
    void testNextStage() {
        assertAll("Test next stage for each stage",
                () -> assertEquals(Stage.CONFIGURATION, Stage.UNSTAGED.nextStage().orElse(null)),
                () -> assertEquals(Stage.QS, Stage.CONFIGURATION.nextStage().orElse(null)),
                () -> assertEquals(Stage.PROD, Stage.QS.nextStage().orElse(null)),
                () -> assertFalse(Stage.PROD.nextStage().isPresent()),
                () -> assertFalse(Stage.CACHING.nextStage().isPresent()),
                () -> assertFalse(Stage.FILE_CONFIGURATION.nextStage().isPresent()),
                () -> assertFalse(Stage.FILE_QS.nextStage().isPresent()),
                () -> assertFalse(Stage.FILE_PROD.nextStage().isPresent())
        );
    }

    @Test
    void testGetOrder() {
        assertAll("Test getOrder for each stage",
                () -> assertEquals(0, Stage.UNSTAGED.getOrder()),
                () -> assertEquals(1, Stage.CONFIGURATION.getOrder()),
                () -> assertEquals(2, Stage.QS.getOrder()),
                () -> assertEquals(3, Stage.PROD.getOrder()),
                () -> assertEquals(4, Stage.CACHING.getOrder()),
                () -> assertEquals(5, Stage.FILE_CONFIGURATION.getOrder()),
                () -> assertEquals(6, Stage.FILE_QS.getOrder()),
                () -> assertEquals(7, Stage.FILE_PROD.getOrder())
        );
    }

    @Test
    void testIsSame() {
        assertAll("Test isSame for various stage relations",
                () -> assertTrue(Stage.UNSTAGED.isSame(Stage.CONFIGURATION)),
                () -> assertTrue(Stage.CONFIGURATION.isSame(Stage.UNSTAGED)),
                () -> assertTrue(Stage.QS.isSame(Stage.QS)),
                () -> assertTrue(Stage.PROD.isSame(Stage.PROD)),
                () -> assertTrue(Stage.CACHING.isSame(Stage.CACHING)),
                () -> assertTrue(Stage.FILE_CONFIGURATION.isSame(Stage.FILE_CONFIGURATION)),
                () -> assertTrue(Stage.FILE_QS.isSame(Stage.FILE_QS)),
                () -> assertTrue(Stage.FILE_PROD.isSame(Stage.FILE_PROD)),
                // Test negative cases
                () -> assertFalse(Stage.FILE_PROD.isSame(Stage.PROD)),
                () -> assertFalse(Stage.QS.isSame(Stage.UNSTAGED)),
                () -> assertFalse(Stage.FILE_QS.isSame(Stage.UNSTAGED))
        );
    }

    @Test
    void testExists() {
        assertAll("Test if stage names exist correctly",
                () -> assertTrue(Stage.exists("UNSTAGED")),
                () -> assertTrue(Stage.exists("configuration")),
                () -> assertTrue(Stage.exists("qs")),
                () -> assertTrue(Stage.exists("prod")),
                () -> assertTrue(Stage.exists("CacHing")),
                () -> assertTrue(Stage.exists("file_configuration")),
                // Negative cases
                () -> assertFalse(Stage.exists("nonexistent")),
                () -> assertFalse(Stage.exists("unstage"))
        );
    }

    @Test
    void testGetNextStages() {
        assertAll("Test getNextStages for various stages",
                () -> assertEquals(List.of(Stage.CONFIGURATION, Stage.QS, Stage.PROD), Stage.getNextStages(Stage.UNSTAGED)),
                () -> assertEquals(List.of(Stage.QS, Stage.PROD), Stage.getNextStages(Stage.CONFIGURATION)),
                () -> assertEquals(List.of(Stage.PROD), Stage.getNextStages(Stage.QS)),
                () -> assertEquals(List.of(), Stage.getNextStages(Stage.PROD))
        );
    }

    @Test
    void testNonProdStages() {
        List<Stage> nonProdStages = Stage.nonProdStages();
        assertTrue(nonProdStages.contains(Stage.UNSTAGED));
        assertTrue(nonProdStages.contains(Stage.CONFIGURATION));
        assertTrue(nonProdStages.contains(Stage.QS));
        assertFalse(nonProdStages.contains(Stage.PROD));
        assertTrue(nonProdStages.contains(Stage.CACHING));
        assertTrue(nonProdStages.contains(Stage.FILE_CONFIGURATION));
        assertTrue(nonProdStages.contains(Stage.FILE_QS));
        assertTrue(nonProdStages.contains(Stage.FILE_PROD));
    }

    @Test
    void testRealStages() {
        List<Stage> realStages = Stage.realStages();
        assertEquals(List.of(Stage.CONFIGURATION, Stage.QS, Stage.PROD), realStages);
        assertFalse(realStages.contains(Stage.UNSTAGED));
        assertFalse(realStages.contains(Stage.CACHING));
        assertFalse(realStages.contains(Stage.FILE_CONFIGURATION));
        assertFalse(realStages.contains(Stage.FILE_QS));
        assertFalse(realStages.contains(Stage.FILE_PROD));
    }

    @Test
    void testRealStageAsFileStage() {

        assertEquals(Stage.FILE_CONFIGURATION, Stage.realStageAsFileStage(Stage.CONFIGURATION).orElse(null));
        assertEquals(Stage.FILE_QS, Stage.realStageAsFileStage(Stage.QS).orElse(null));
        assertEquals(Stage.FILE_PROD, Stage.realStageAsFileStage(Stage.PROD).orElse(null));

        assertAll("Convert real stages to file stages",
                () -> assertEquals(Stage.FILE_CONFIGURATION, Stage.realStageAsFileStage(Stage.CONFIGURATION).orElse(null)),
                () -> assertEquals(Stage.FILE_QS, Stage.realStageAsFileStage(Stage.QS).orElse(null)),
                () -> assertEquals(Stage.FILE_PROD, Stage.realStageAsFileStage(Stage.PROD).orElse(null)),
                // Negative cases
                () -> assertFalse(Stage.realStageAsFileStage(Stage.UNSTAGED).isPresent()),
                () -> assertFalse(Stage.realStageAsFileStage(Stage.CACHING).isPresent())
        );
    }

    @Test
    void testFileStageAsRealStage() {
        assertAll("Convert file stages to real stages",
                () -> assertEquals(Stage.CONFIGURATION, Stage.fileStageAsRealStage(Stage.FILE_CONFIGURATION).orElse(null)),
                () -> assertEquals(Stage.QS, Stage.fileStageAsRealStage(Stage.FILE_QS).orElse(null)),
                () -> assertEquals(Stage.PROD, Stage.fileStageAsRealStage(Stage.FILE_PROD).orElse(null)),
                // Negative cases
                () -> assertFalse(Stage.fileStageAsRealStage(Stage.UNSTAGED).isPresent()),
                () -> assertFalse(Stage.fileStageAsRealStage(Stage.CACHING).isPresent())
        );
    }
}