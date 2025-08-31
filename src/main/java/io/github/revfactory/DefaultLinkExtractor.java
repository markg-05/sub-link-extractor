package io.github.revfactory;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class DefaultLinkExtractor implements LinkExtractorStrategy {
    private final int requestDelayMs;

    public DefaultLinkExtractor() {
        this(1000);  // default 1 second delay
    }

    public DefaultLinkExtractor(int requestDelayMs) {
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

        while (!toVisit.isEmpty()) {
            String currentUrl = toVisit.poll();

            // Remove hashtags from the URL
            String sanitizedUrl = LinkUtils.removeHashTag(currentUrl);

            if (visitedLinks.contains(sanitizedUrl) || LinkUtils.isDisallowed(new URL(sanitizedUrl).getPath(), disallowedPaths)) {
                continue;
            }

            try {
                Thread.sleep(requestDelayMs);
                Document document = connectAndParse(currentUrl);
                Elements elements = document.select("a[href]");

                for (var element : elements) {
                    String link = element.attr("abs:href");  // Automatically converts relative paths to absolute paths
                    if (link.startsWith(sourceUrl) && !visitedLinks.contains(link)) {
                        toVisit.add(link);
                    }
                }

                visitedLinks.add(sanitizedUrl);
            } catch (IOException e) {
                // Handle error or choose to ignore
            }
        }

        return new ArrayList<>(visitedLinks);
    }

    // This method is added to simplify the testing process
    protected Document connectAndParse(String url) throws IOException {
        return Jsoup.connect(url).get();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        LinkExtractorStrategy extractor = new DefaultLinkExtractor(10);  // 0.01 second delay
        List<String> links = extractor.extractLinks("https://h2hggl.com");
        
        // Write links to a text file
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
