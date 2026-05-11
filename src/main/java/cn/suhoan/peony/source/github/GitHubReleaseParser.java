package cn.suhoan.peony.source.github;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GitHubReleaseParser {
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private GitHubReleaseParser() {
    }

    public static List<GitHubRelease> parseReleases(String responseBody) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(responseBody)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new IOException("invalid GitHub releases response");
            }

            List<GitHubRelease> releases = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                releases.add(parseRelease(parser));
            }
            return releases;
        }
    }

    public static List<GitHubReleaseAsset> parseAssets(String responseBody) throws IOException {
        try (JsonParser parser = JSON_FACTORY.createParser(responseBody)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("invalid GitHub release response");
            }

            List<GitHubReleaseAsset> assets = new ArrayList<>();
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.currentName();
                parser.nextToken();
                if (!"assets".equals(fieldName)) {
                    parser.skipChildren();
                    continue;
                }

                if (parser.currentToken() != JsonToken.START_ARRAY) {
                    throw new IOException("invalid assets array in GitHub release response");
                }
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    assets.add(parseAsset(parser));
                }
            }
            return assets;
        }
    }

    private static GitHubRelease parseRelease(JsonParser parser) throws IOException {
        Boolean prerelease = null;
        List<GitHubReleaseAsset> assets = List.of();

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IOException("invalid release object in GitHub releases response");
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken();
            switch (fieldName) {
                case "prerelease" -> prerelease = parser.getBooleanValue();
                case "assets" -> {
                    if (parser.currentToken() != JsonToken.START_ARRAY) {
                        throw new IOException("invalid assets array in GitHub releases response");
                    }
                    List<GitHubReleaseAsset> parsedAssets = new ArrayList<>();
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        parsedAssets.add(parseAsset(parser));
                    }
                    assets = parsedAssets;
                }
                default -> parser.skipChildren();
            }
        }

        return new GitHubRelease(Boolean.TRUE.equals(prerelease), assets);
    }

    private static GitHubReleaseAsset parseAsset(JsonParser parser) throws IOException {
        String name = null;
        String apiUrl = null;
        String browserDownloadUrl = null;

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            throw new IOException("invalid asset object in GitHub release response");
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.currentName();
            parser.nextToken();
            switch (fieldName) {
                case "name" -> name = parser.getValueAsString();
                case "url" -> apiUrl = parser.getValueAsString();
                case "browser_download_url" -> browserDownloadUrl = parser.getValueAsString();
                default -> parser.skipChildren();
            }
        }

        if (name == null) {
            throw new IOException("GitHub release asset missing name");
        }
        if (apiUrl == null && browserDownloadUrl == null) {
            throw new IOException("GitHub release asset missing download url");
        }

        return new GitHubReleaseAsset(name, apiUrl, browserDownloadUrl);
    }
}
