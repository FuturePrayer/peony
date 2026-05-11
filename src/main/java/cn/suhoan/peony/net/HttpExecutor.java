package cn.suhoan.peony.net;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class HttpExecutor implements AutoCloseable {
    private static final int MAX_REDIRECTS = 10;
    private static final int MAX_ATTEMPTS_PER_MODE = 3;
    private static final int REQUEST_TIMEOUT_MILLIS = Math.toIntExact(Duration.ofSeconds(30).toMillis());

    private final ProxySettings proxySettings;

    public HttpExecutor(ProxySettings proxySettings) {
        this.proxySettings = proxySettings == null ? ProxySettings.empty() : proxySettings;
    }

    public String getText(URI sourceUri, Map<String, String> headers) throws IOException {
        try (HttpResponse response = send(sourceUri, headers)) {
            return response.body();
        }
    }

    public void download(URI sourceUri, Map<String, String> headers, Path targetFile) throws IOException {
        Files.createDirectories(targetFile.getParent());
        try (HttpResponse response = send(sourceUri, headers);
             InputStream inputStream = response.bodyStream();
             OutputStream outputStream = Files.newOutputStream(targetFile)) {
            if (inputStream == null) {
                throw new IOException("response body is empty for " + sourceUri);
            }
            inputStream.transferTo(outputStream);
        }
    }

    private HttpResponse send(URI sourceUri, Map<String, String> headers) throws IOException {
        IOException proxiedFailure = null;
        if (proxySettings.hasProxy() || proxySettings.hasPrefixProxy()) {
            try {
                return sendWithPolicy(sourceUri, headers, proxySettings);
            } catch (IOException e) {
                proxiedFailure = e;
            }
        }

        try {
            return sendWithPolicy(sourceUri, headers, ProxySettings.empty());
        } catch (IOException directFailure) {
            if (proxiedFailure != null) {
                directFailure.addSuppressed(proxiedFailure);
            }
            throw directFailure;
        }
    }

    private HttpResponse sendWithPolicy(URI sourceUri, Map<String, String> headers, ProxySettings activeProxySettings) throws IOException {
        URI currentUri = sourceUri;
        Map<String, String> currentHeaders = new LinkedHashMap<>(headers);
        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            HttpResponse response = executeWithRetry(currentUri, currentHeaders, activeProxySettings);
            if (!isRedirect(response.getStatus())) {
                ensureSuccess(response.getStatus(), currentUri);
                return response;
            }

            String location = response.header("Location");
            response.close();
            if (location == null || location.isBlank()) {
                throw new IOException("redirect response missing location header");
            }
            URI redirectedUri = currentUri.resolve(location);
            if (!sameAuthority(currentUri, redirectedUri)) {
                currentHeaders.remove("Authorization");
            }
            currentUri = redirectedUri;
        }
        throw new IOException("too many redirects while requesting " + sourceUri);
    }

    private HttpResponse executeWithRetry(URI sourceUri, Map<String, String> headers, ProxySettings activeProxySettings) throws IOException {
        IOException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS_PER_MODE; attempt++) {
            URI effectiveUri = applyPrefixProxy(sourceUri, activeProxySettings);
            HttpRequest request = buildRequest(effectiveUri, headers, activeProxySettings);
            try {
                HttpResponse response = request.execute();
                if (isSuccessfulOrRedirect(response.getStatus())) {
                    return response;
                }
                response.close();
                lastFailure = new IOException("request failed with status " + response.getStatus() + " for " + sourceUri);
            } catch (Exception e) {
                lastFailure = e instanceof IOException ioException ? ioException : new IOException(e.getMessage(), e);
            }
        }
        throw lastFailure == null ? new IOException("request failed for " + sourceUri) : lastFailure;
    }

    private HttpRequest buildRequest(URI effectiveUri, Map<String, String> headers, ProxySettings activeProxySettings) {
        HttpRequest request = HttpRequest.get(effectiveUri.toString())
                .timeout(REQUEST_TIMEOUT_MILLIS)
                .setFollowRedirects(false);
        ProxyDefinition proxy = parseProxy(activeProxySettings.proxyUrl());
        if (proxy != null) {
            request.setProxy(new Proxy(proxy.type().javaNetType(), InetSocketAddress.createUnresolved(proxy.host(), proxy.port())));
        }
        headers.forEach(request::header);
        return request;
    }

    private URI applyPrefixProxy(URI sourceUri, ProxySettings activeProxySettings) {
        if (!activeProxySettings.hasPrefixProxy()) {
            return sourceUri;
        }
        return URI.create(activeProxySettings.prefixProxy() + sourceUri);
    }

    private void ensureSuccess(int statusCode, URI sourceUri) throws IOException {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        throw new IOException("request failed with status " + statusCode + " for " + sourceUri);
    }

    private static boolean isRedirect(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
    }

    private static boolean isSuccessfulOrRedirect(int statusCode) {
        return (statusCode >= 200 && statusCode < 300) || isRedirect(statusCode);
    }

    private boolean sameAuthority(URI left, URI right) {
        return Optional.ofNullable(left.getScheme()).equals(Optional.ofNullable(right.getScheme()))
                && Optional.ofNullable(left.getHost()).equals(Optional.ofNullable(right.getHost()))
                && portOf(left) == portOf(right);
    }

    private int portOf(URI uri) {
        return uri.getPort() == -1 ? switch (uri.getScheme()) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        } : uri.getPort();
    }

    static ProxyDefinition parseProxy(String rawProxyUrl) {
        if (rawProxyUrl == null || rawProxyUrl.isBlank()) {
            return null;
        }
        URI uri = URI.create(rawProxyUrl.trim());
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("proxy url must include scheme: " + rawProxyUrl);
        }
        if (uri.getHost() == null || uri.getPort() < 0) {
            throw new IllegalArgumentException("proxy url must include host and port: " + rawProxyUrl);
        }
        ProxyType proxyType = switch (scheme.toLowerCase()) {
            case "http", "https" -> ProxyType.HTTP;
            case "socks5" -> ProxyType.SOCKS5;
            default -> throw new IllegalArgumentException("unsupported proxy scheme: " + scheme);
        };
        return new ProxyDefinition(proxyType, uri.getHost(), uri.getPort());
    }

    @Override
    public void close() {
    }

    record ProxyDefinition(ProxyType type, String host, int port) {
    }

    enum ProxyType {
        HTTP(Proxy.Type.HTTP),
        SOCKS5(Proxy.Type.SOCKS);

        private final Proxy.Type javaNetType;

        ProxyType(Proxy.Type javaNetType) {
            this.javaNetType = javaNetType;
        }

        Proxy.Type javaNetType() {
            return javaNetType;
        }
    }
}
