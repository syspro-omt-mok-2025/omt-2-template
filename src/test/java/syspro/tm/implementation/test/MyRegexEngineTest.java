package syspro.tm.implementation.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import syspro.tm.Configuration;
import syspro.tm.TestData;
import syspro.tm.Utils;
import syspro.tm.implementation.MyRegexEngine;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class MyRegexEngineTest {
    private final MyRegexEngine myRegexEngine = new MyRegexEngine();

    public static Stream<TestData> singleConfiguredTest() {
        return Configuration.testData().stream().filter(data -> data.slidingWindowSize == null);
    }

    private boolean matches(String regex, String input) {
        return myRegexEngine.matches(Utils.toBytes(regex), Utils.toBytes(input));
    }

    @Test
    public void basicTest() {
        assertTrue(matches("a[bB][0-9]\\d\\w?.\\s", "aB42_\n\f"));
        assertFalse(matches("a[bB][0-9]\\d\\w?", "aB42-"));
    }

    @ParameterizedTest
    @MethodSource
    public void singleConfiguredTest(TestData data) {
        assertEquals(data.expected, myRegexEngine.matches(data.regex, data.input));
    }
}
