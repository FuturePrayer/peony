package cn.suhoan.peony.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpExecutorTest {
    @Test
    void parsesHttpProxy() {
        HttpExecutor.ProxyDefinition proxy = HttpExecutor.parseProxy("https://proxy.example:8443");
        assertEquals(HttpExecutor.ProxyType.HTTP, proxy.type());
        assertEquals("proxy.example", proxy.host());
        assertEquals(8443, proxy.port());
    }

    @Test
    void parsesSocksProxy() {
        HttpExecutor.ProxyDefinition proxy = HttpExecutor.parseProxy("socks5://127.0.0.1:1080");
        assertEquals(HttpExecutor.ProxyType.SOCKS5, proxy.type());
        assertEquals("127.0.0.1", proxy.host());
        assertEquals(1080, proxy.port());
    }

    @Test
    void emptyProxyReturnsNull() {
        assertNull(HttpExecutor.parseProxy(null));
    }
}
