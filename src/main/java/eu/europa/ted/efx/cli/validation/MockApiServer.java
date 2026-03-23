/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.cli.validation;

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
public class MockApiServer implements AutoCloseable {

    private final HttpServer server;
    private final int port;

    public MockApiServer(final String response) throws IOException {
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

    public String baseUrl() {
        return "http://localhost:" + this.port;
    }

    @Override
    public void close() {
        this.server.stop(0);
    }
}
