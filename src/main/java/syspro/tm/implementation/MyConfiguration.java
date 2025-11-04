package syspro.tm.implementation;

import syspro.tm.*;

public final class MyConfiguration implements ConfigurationProvider {
    @Override
    public void provideTestData(TestDataListBuilder builder) {
        builder.add("a?b", "ab", true);
        builder.add("a?b", "b", true);
        builder.add("a?b", "abc", false);
        try (var _ = builder.benchmarkGroup()) {
            builder.add("(a+)+b", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaab", true);
        }
    }

    @Override
    public boolean shouldSkipTestCase(ConfigurationProvider provider, byte[] regex, byte[] input, boolean benchmark) {
        // Convenient way to disable default test cases (for debugging)
        if (false && provider instanceof DefaultTestCases) {
            return true;
        }
        return false;
    }
}
