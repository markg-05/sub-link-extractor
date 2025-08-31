package io.github.revfactory;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LinkUtilsTest {

    @Test
    public void testGetDisallowedPathsIgnoresOtherAgents() throws IOException {
        List<String> lines = Arrays.asList(
                "User-agent: FooBot",
                "Disallow: /foo",
                "",
                "User-agent: *",
                "Disallow: /all",
                "",
                "User-agent: BarBot",
                "Disallow: /bar"
        );

        try (MockedStatic<LinkUtils> utilities = Mockito.mockStatic(LinkUtils.class, Mockito.CALLS_REAL_METHODS)) {
            utilities.when(() -> LinkUtils.readAllLinesFromUrl("http://example.com/robots.txt"))
                     .thenReturn(lines);
            Set<String> disallowed = LinkUtils.getDisallowedPaths("http://example.com");
            assertTrue(disallowed.contains("/all"));
            assertFalse(disallowed.contains("/foo"));
            assertFalse(disallowed.contains("/bar"));
        }
    }
}
