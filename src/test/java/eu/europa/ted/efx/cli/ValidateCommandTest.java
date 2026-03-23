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
package eu.europa.ted.efx.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sun.net.httpserver.HttpServer;

import picocli.CommandLine;

/**
 * Integration test for the {@code validate} command.
 * <p>
 * Uses a JDK {@link HttpServer} as a mock API endpoint.
 * Two test groups:
 * <ul>
 *   <li>Pre-compiled Schematron tests ({@code --schematron}): use a hand-crafted
 *       {@code test-validation.sch} to bypass EFX translation.</li>
 *   <li>Full pipeline tests ({@code --input}): translate an EFX file to Schematron,
 *       then validate. Requires SDK on disk.</li>
 * </ul>
 * </p>
 */
class ValidateCommandTest {

    private static HttpServer server;
    private static int port;
    private static volatile String mockResponse;
    private static volatile int mockStatusCode;

    private static final Path TEST_SCH = testFilePath("test-validation.sch");
    private static final Path TEST_NOTICE = testFilePath("test-notice.xml");
    private static final Path TEST_EFX = testFilePath("test-rules.efx");
    private static final Path TEST_CN_NOTICE = testFilePath("test-cn-notice.xml");
    private static final String SDK_VERSION = "2.0.0-SNAPSHOT";
    private static final String SDK_PATH = "eforms-sdk";

    @BeforeAll
    static void startMockServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        server.createContext("/v1/check", exchange -> {
            final byte[] body = mockResponse.getBytes();
            exchange.sendResponseHeaders(mockStatusCode, body.length);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.start();
    }

    @AfterAll
    static void stopMockServer() {
        server.stop(0);
    }

    @ParameterizedTest(name = "{0}: response={1}, status={2}")
    @MethodSource("validationScenarios")
    void testValidation(final String scenario, final String response, final int statusCode,
            final int expectedExit, final String expectedAssertId, final String mode) {

        mockResponse = response;
        mockStatusCode = statusCode;

        final String endpointUrl = "http://localhost:" + port + "/v1";

        final ByteArrayOutputStream capture = new ByteArrayOutputStream();
        final PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capture));

        try {
            final int exitCode = new CommandLine(new CliCommand()).execute(
                    "validate",
                    "--schematron", TEST_SCH.toString(),
                    "--notice", TEST_NOTICE.toString(),
                    "--mode", mode,
                    "--endpoint", "default=" + endpointUrl);

            assertEquals(expectedExit, exitCode,
                    "Exit code mismatch for scenario: " + scenario);

            final String output = capture.toString();
            if (expectedAssertId != null) {
                assertTrue(output.contains(expectedAssertId),
                        "Expected assert id '" + expectedAssertId + "' in output: " + output);
            }
        } finally {
            System.setOut(originalOut);
        }
    }

    static Stream<Arguments> validationScenarios() {
        return Stream.of("schxslt", "phpure").flatMap(mode -> Stream.of(
                Arguments.of("check-fails-" + mode, "0", 200, 1, "R-TEST-001", mode),
                Arguments.of("check-passes-" + mode, "1", 200, 0, null, mode),
                Arguments.of("api-error-" + mode, "error", 500, 1, "R-TEST-ERR", mode)));
    }

    @ParameterizedTest(name = "pipeline-{0}: response={1}, status={2}")
    @MethodSource("pipelineScenarios")
    void testFullPipeline(final String scenario, final String response, final int statusCode,
            final int expectedExit, final String expectedAssertId, final String mode) {

        mockResponse = response;
        mockStatusCode = statusCode;

        final String endpointUrl = "http://localhost:" + port + "/v1";

        final ByteArrayOutputStream capture = new ByteArrayOutputStream();
        final PrintStream originalOut = System.out;
        System.setOut(new PrintStream(capture));

        try {
            final int exitCode = new CommandLine(new CliCommand()).execute(
                    "validate",
                    "--rules", TEST_EFX.toString(),
                    "--notice", TEST_CN_NOTICE.toString(),
                    "--sdk-version", SDK_VERSION,
                    "--sdk-path", SDK_PATH,
                    "--mode", mode,
                    "--endpoint", "default=" + endpointUrl);

            assertEquals(expectedExit, exitCode,
                    "Exit code mismatch for scenario: " + scenario);

            final String output = capture.toString();
            if (expectedAssertId != null) {
                assertTrue(output.contains(expectedAssertId),
                        "Expected assert id '" + expectedAssertId + "' in output: " + output);
            }
        } finally {
            System.setOut(originalOut);
        }
    }

    static Stream<Arguments> pipelineScenarios() {
        return Stream.of("schxslt", "phpure").flatMap(mode -> Stream.of(
                Arguments.of("check-fails-" + mode, "0", 200, 1, "R-TST-001", mode),
                Arguments.of("check-passes-" + mode, "1", 200, 0, null, mode)));
    }

    private static Path testFilePath(final String name) {
        return Paths.get("src/test/resources/eu/europa/ted/efx/cli/" + name);
    }
}
