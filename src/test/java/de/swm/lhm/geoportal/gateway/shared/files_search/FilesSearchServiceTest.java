package de.swm.lhm.geoportal.gateway.shared.files_search;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesSearchServiceTest {

    @Test
    void testIcons() throws IOException {

        String basePath = new File("src/test/resources/de/swm/lhm/geoportal/gateway/shared/test_file_search")
                .getAbsolutePath();
        String globPattern = "*/icons/**/*.{jpg,jpeg,png,svg};*/icons/*.{jpg,jpeg,png,svg}";
        FilesSearchService reader = new FilesSearchService();

        List<String> result = reader.getFilenamesFromDirectory("", basePath, globPattern);
        assertEquals(145, result.size());
        for (String file : result) {
            MatcherAssert.assertThat(file, CoreMatchers.startsWith("./"));
            String fileName = file.substring(file.lastIndexOf('/') + 1);
            MatcherAssert.assertThat(fileName, CoreMatchers.startsWith("icon"));
            assertEquals("icons", Arrays.asList(file.split("/")).get(2));
        }

    }

    @Test
    void testLegends() throws IOException {

        String basePath = new File("src/test/resources/de/swm/lhm/geoportal/gateway/shared/test_file_search")
                .getAbsolutePath();
        String globPattern = "*/legends/**/*.{jpg,jpeg,png};*/legends/*.{jpg,jpeg,png}";
        FilesSearchService reader = new FilesSearchService();
        List<String> result = reader.getFilenamesFromDirectory("", basePath, globPattern);
        assertEquals(106, result.size());
        for (String file : result) {
            MatcherAssert.assertThat(file, CoreMatchers.startsWith("./"));
            String fileName = file.substring(file.lastIndexOf('/') + 1);
            MatcherAssert.assertThat(fileName, CoreMatchers.startsWith("legend"));
            assertEquals("legends", Arrays.asList(file.split("/")).get(2));
        }

    }
}