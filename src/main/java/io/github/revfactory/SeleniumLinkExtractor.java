package io.github.revfactory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class SeleniumLinkExtractor implements LinkExtractorStrategy {
    private final int requestDelayMs;

    public SeleniumLinkExtractor() {
        this(1000);  // default 1 second delay
    }

    public SeleniumLinkExtractor(int requestDelayMs) {
        this.requestDelayMs = requestDelayMs;
    }

    @Override
    public List<String> extractLinks(String sourceUrl) throws IOException, InterruptedException {
        Set<String> visitedLinks = new HashSet<>();
        Queue<String> toVisit = new LinkedList<>();
        Set<String> disallowedPaths = getDisallowedPaths(sourceUrl);

        URL url = new URL(sourceUrl);
        String base = url.getProtocol() + "://" + url.getHost();

        toVisit.add(sourceUrl);

        WebDriver driver = createDriver();
        try {
            while (!toVisit.isEmpty()) {
                String currentUrl = toVisit.poll();

                String sanitizedUrl = removeHashTag(currentUrl);

                if (visitedLinks.contains(sanitizedUrl) || isDisallowed(new URL(sanitizedUrl).getPath(), disallowedPaths)) {
                    continue;
                }

                try {
                    Thread.sleep(requestDelayMs);
                    driver.get(currentUrl);

                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                    List<WebElement> elements = wait.until(
                        ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("a"))
                    );

                    for (WebElement element : elements) {
                        String link = element.getAttribute("href");
                        if (link != null && link.startsWith(base) && !visitedLinks.contains(link)) {
                            toVisit.add(link);
                        }
                    }

                    visitedLinks.add(sanitizedUrl);
                } catch (Exception ignored) {
                    // Handle error or choose to ignore
                }
            }
        } finally {
            driver.quit();
        }

        return new ArrayList<>(visitedLinks);
    }

    private WebDriver createDriver() {
        try {
            WebDriverManager.chromedriver().setup();
        } catch (Throwable ignored) {
            // WebDriverManager might not be available
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-dev-shm-usage");
        return new ChromeDriver(options);
    }

    private boolean isDisallowed(String path, Set<String> disallowedPaths) {
        return disallowedPaths.stream().anyMatch(path::startsWith);
    }

    private String removeHashTag(String url) {
        int hashIndex = url.indexOf("#");
        return (hashIndex > -1) ? url.substring(0, hashIndex) : url;
    }

    private Set<String> getDisallowedPaths(String sourceUrl) throws IOException {
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

    private List<String> readAllLinesFromUrl(String targetUrl) throws IOException {
        URL url = new URL(targetUrl);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return reader.lines().collect(Collectors.toList());
        }
    }
}

