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
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;

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
        Set<String> disallowedPaths = LinkUtils.getDisallowedPaths(sourceUrl);

        URL url = new URL(sourceUrl);
        String base = url.getProtocol() + "://" + url.getHost();

        toVisit.add(sourceUrl);

        WebDriver driver = createDriver();
        try {
            while (!toVisit.isEmpty()) {
                String currentUrl = toVisit.poll();

                String sanitizedUrl = LinkUtils.removeHashTag(currentUrl);

                if (visitedLinks.contains(sanitizedUrl) || LinkUtils.isDisallowed(new URL(sanitizedUrl).getPath(), disallowedPaths)) {
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

}

