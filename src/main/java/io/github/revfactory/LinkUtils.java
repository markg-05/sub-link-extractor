package io.github.revfactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class LinkUtils {
    private LinkUtils() {
    }

    public static boolean isDisallowed(String path, Set<String> disallowedPaths) {
        return disallowedPaths.stream().anyMatch(path::startsWith);
    }

    public static String removeHashTag(String url) {
        int hashIndex = url.indexOf("#");
        return (hashIndex > -1) ? url.substring(0, hashIndex) : url;
    }

    public static Set<String> getDisallowedPaths(String sourceUrl) throws IOException {
        Set<String> disallowedPaths = new HashSet<>();
        URL url = new URL(sourceUrl);
        String robotUrl = url.getProtocol() + "://" + url.getHost() + "/robots.txt";
        try {
            List<String> lines = readAllLinesFromUrl(robotUrl);
            boolean isUserAgentAll = false;
            for (String line : lines) {
                if (line.trim().equalsIgnoreCase("User-agent: *")) {
                    isUserAgentAll = true;
                } else if (isUserAgentAll && line.trim().startsWith("Disallow:")) {
                    disallowedPaths.add(line.split(":")[1].trim());
                }
            }
        } catch (Exception ignored) {
            // robots.txt may not exist or there might be other issues accessing it.
        }
        return disallowedPaths;
    }

    public static List<String> readAllLinesFromUrl(String targetUrl) throws IOException {
        URL url = new URL(targetUrl);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return reader.lines().collect(Collectors.toList());
        }
    }
}

