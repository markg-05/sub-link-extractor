package io.github.revfactory;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;

public class SeleniumLinkExtractor implements LinkExtractorStrategy {
    private final int requestDelayMs;
    private final WebDriver driver;
    private final boolean manageDriver;

    public SeleniumLinkExtractor() {
        this(createDriver(), 1000, true);
    }

    public SeleniumLinkExtractor(int requestDelayMs) {
        this(createDriver(), requestDelayMs, true);
    }

    public SeleniumLinkExtractor(WebDriver driver) {
        this(driver, 1000, false);
    }

    public SeleniumLinkExtractor(WebDriver driver, int requestDelayMs) {
        this(driver, requestDelayMs, false);
    }

    private SeleniumLinkExtractor(WebDriver driver, int requestDelayMs, boolean manageDriver) {
        this.requestDelayMs = requestDelayMs;
        this.driver = driver;
        this.manageDriver = manageDriver;
    }

    @Override
    public List<String> extractLinks(String sourceUrl) throws IOException, InterruptedException {
        Set<String> visitedLinks = new HashSet<>();
        Queue<String> toVisit = new LinkedList<>();
        Set<String> disallowedPaths = LinkUtils.getDisallowedPaths(sourceUrl);

        URL url = new URL(sourceUrl);
        String base = url.getProtocol() + "://" + url.getHost();
        String basePath = url.getPath();
        if (basePath.isEmpty()) {
            basePath = "/";
        }
        String basePathWithSlash = basePath.endsWith("/") ? basePath : basePath + "/";

        toVisit.add(sourceUrl);

        try {
            while (!toVisit.isEmpty()) {
                String currentUrl = toVisit.poll();

                String sanitizedUrl = LinkUtils.removeHashTag(currentUrl);

                if (visitedLinks.contains(sanitizedUrl) || LinkUtils.isDisallowed(new URL(sanitizedUrl).getPath(), disallowedPaths)) {
                    continue;
                }

                try {
                    List<WebElement> elements = fetchLinksWithDriver(driver, currentUrl);

                    for (WebElement element : elements) {
                        String link = element.getAttribute("href");
                        if (link != null && link.startsWith(base)) {
                            try {
                                String linkPath = new URL(link).getPath();
                                if ((linkPath.equals(basePath) || linkPath.startsWith(basePathWithSlash)) && !visitedLinks.contains(link)) {
                                    toVisit.add(link);
                                }
                            } catch (Exception ignored) {
                                // Handle error or choose to ignore
                            }
                        }
                    }

                    visitedLinks.add(sanitizedUrl);
                } catch (InterruptedException e) {
                    throw e;
                } catch (Exception ignored) {
                    // Handle error or choose to ignore
                }
            }
        } finally {
            if (manageDriver) {
                driver.quit();
            }
        }

        return new ArrayList<>(visitedLinks);
    }

    protected List<WebElement> fetchLinksWithDriver(WebDriver driver, String url) throws InterruptedException {
        Thread.sleep(requestDelayMs);
        driver.get(url);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        return wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(By.tagName("a"))
        );
    }

    private static WebDriver createDriver() {
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

    public static void main(String[] args) throws IOException, InterruptedException {
        LinkExtractorStrategy extractor = new SeleniumLinkExtractor(10);
        List<String> links = extractor.extractLinks("https://h2hggl.com");

        String outputFileName = "extracted_links.txt";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            for (String link : links) {
                writer.write(link);
                writer.newLine();
            }
        }

        System.out.println("Links have been written to " + outputFileName);
        System.out.println("Total links extracted: " + links.size());
    }
}

