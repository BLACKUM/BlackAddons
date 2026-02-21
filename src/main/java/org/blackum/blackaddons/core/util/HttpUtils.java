package org.blackum.blackaddons.core.util;

import org.blackum.blackaddons.Blackaddons;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpUtils {
    private static final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
            .build();

    public static CompletableFuture<HttpResponse<String>> sendGetRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(Constants.HTTP_TIMEOUT_SECONDS))
                .header("User-Agent", Constants.BROWSER_USER_AGENT)
                .header("Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(res -> {
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        Blackaddons.LOGGER.info("Successfully fetched: " + url);
                    } else {
                        Blackaddons.LOGGER.warn("Fetch failed. Status: " + res.statusCode() + " URL: " + url);
                    }
                    return res;
                })
                .exceptionally(e -> {
                    Blackaddons.LOGGER.error("Error fetching " + url + ": " + e.getMessage());
                    return null;
                });
    }
}
