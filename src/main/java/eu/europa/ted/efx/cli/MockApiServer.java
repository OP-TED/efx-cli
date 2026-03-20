package eu.europa.ted.efx.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;

/**
 * Lightweight mock API server for testing dynamic EFX rules without an external service.
 * Uses the JDK built-in {@link HttpServer} — no extra dependencies.
 *
 * <p>Responds to all requests with a configurable value:
 * {@code "1"} (true), {@code "0"} (false), or {@code "-1"} (error).</p>
 */
class MockApiServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;

    MockApiServer(final String response) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(0), 0);
        this.port = this.server.getAddress().getPort();

        final byte[] body = response.getBytes(StandardCharsets.UTF_8);
        this.server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });

        this.server.start();
    }

    String baseUrl() {
        return "http://localhost:" + this.port;
    }

    @Override
    public void close() {
        this.server.stop(0);
    }
}
