package de.swm.lhm.geoportal.gateway.base_classes.hamcrest;

// https://github.com/sharfah/java-utils/blob/master/src/test/java/com/sharfah/util/hamcrest/IsEqualJSON.java

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A Matcher for comparing JSON.
 * Example usage:
 * <pre>
 * assertThat(new String[] {"foo", "bar"}, equalToJSON("[\"foo\", \"bar\"]"));
 * </pre>
 */
public class HamcrestCompareJsonMatcher extends DiagnosingMatcher<Object> {

    private final String expectedJSON;
    private JSONCompareMode jsonCompareMode;

    public HamcrestCompareJsonMatcher(final String expectedJSON) {
        this.expectedJSON = expectedJSON;
        this.jsonCompareMode = JSONCompareMode.STRICT;
    }

    /**
     * Changes this matcher's JSON compare mode to lenient.
     * The comparison mode LENIENT means that even if the actual JSON contains extended fields, the test will still pass.
     * @return this matcher
     */
    public HamcrestCompareJsonMatcher leniently() {
        jsonCompareMode = JSONCompareMode.LENIENT;
        return this;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText(expectedJSON);
    }

    @Override
    protected boolean matches(final Object actual, final Description mismatchDescription) {
        final String actualJSON = toJSONString(actual);

        final JSONCompareResult result;

        try {
            result = JSONCompare.compareJSON(
                    expectedJSON,
                    actualJSON,
                    jsonCompareMode
            );

            if (!result.passed()) {
                mismatchDescription.appendText(result.getMessage());
            }
            return result.passed();

        } catch (JSONException e) {
            mismatchDescription.appendText(e.toString());
            return false;
        }

    }

    /**
     * Converts the specified object into a JSON string.
     * @param o the object to convert
     * @return the JSON string
     */
    private static String toJSONString(final Object o) {
        try {
            return o instanceof String ?
                    (String) o : new ObjectMapper().writeValueAsString(o);
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the specified file into a string.
     * @param path the path to read
     * @return the contents of the file
     */
    private static String getFileContents(final Path path) {
        try {
            return Files.readString(path);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a matcher that matches when the examined object
     * is equal to the specified JSON string.
     * For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSON("[\"foo\", \"bar\"]"));
     * </pre>
     *
     * @param expectedJSON the expected JSON string
     * @return the JSON matcher
     */
    public static HamcrestCompareJsonMatcher equalToJSON(final String expectedJSON) {
        return new HamcrestCompareJsonMatcher(expectedJSON);
    }

    /**
     * Creates a matcher that matches when the examined object
     * is equal to the JSON in the specified file.
     * For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSONInFile(Paths.get("/tmp/foo.json"));
     * </pre>
     *
     * @param expectedPath the path containing the expected JSON
     * @return the JSON matcher
     */
    public static HamcrestCompareJsonMatcher equalToJSONInFile(final Path expectedPath) {
        return equalToJSON(getFileContents(expectedPath));
    }

    /**
     * Creates a matcher that matches when the examined object
     * is equal to the JSON contained in the file with the specified name.
     * For example:
     * <pre>
     * assertThat(new String[] {"foo", "bar"},
     *            equalToJSONInFile("/tmp/foo.json"));
     * </pre>
     *
     * @param expectedFileName the name of the file containing the expected JSON
     * @return the JSON matcher
     */
    public static HamcrestCompareJsonMatcher equalToJSONInFile(final String expectedFileName) {
        return equalToJSONInFile(Paths.get(expectedFileName));
    }
}