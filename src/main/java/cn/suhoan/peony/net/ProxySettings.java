package cn.suhoan.peony.net;

public record ProxySettings(String proxyUrl, String prefixProxy) {
    public static ProxySettings empty() {
        return new ProxySettings(null, null);
    }

    public static ProxySettings merge(ProxySettings... candidates) {
        String proxyUrl = null;
        String prefixProxy = null;
        for (ProxySettings candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (notBlank(candidate.proxyUrl())) {
                proxyUrl = candidate.proxyUrl().trim();
            }
            if (notBlank(candidate.prefixProxy())) {
                prefixProxy = candidate.prefixProxy().trim();
            }
        }
        return new ProxySettings(proxyUrl, prefixProxy);
    }

    public boolean hasProxy() {
        return notBlank(proxyUrl);
    }

    public boolean hasPrefixProxy() {
        return notBlank(prefixProxy);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
