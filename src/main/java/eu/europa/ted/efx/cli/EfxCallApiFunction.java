package eu.europa.ted.efx.cli;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathFunction;
import javax.xml.xpath.XPathFunctionException;

/**
 * JAXP {@link XPathFunction} that makes an HTTP GET call to an API endpoint
 * and returns the result as an integer (1 = true, 0 = false, -1 = error).
 *
 * <p>Called by ph-schematron-pure when evaluating
 * {@code efx:call-api($endpointUrl, $funcName, ($arg1, $arg2, ...))} in an assert test.</p>
 *
 * <p>The generated URL is: {@code {baseUrl}/{funcName}?arg1={val}&arg2={val}...}</p>
 */
class EfxCallApiFunction implements XPathFunction {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(EfxCallApiFunction.class);

    @Override
    @SuppressWarnings("rawtypes")
    public Object evaluate(final List args) throws XPathFunctionException {
        final String baseUrl = String.valueOf(args.get(0));
        final String funcName = String.valueOf(args.get(1));

        final List<String> apiArgs = this.extractApiArgs(args);
        final String url = this.buildUrl(baseUrl, funcName, apiArgs);

        try {
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            final HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("API call to {} returned HTTP {}", url, response.statusCode());
                return -1;
            }

            final String body = response.body().trim();
            try {
                return Integer.parseInt(body);
            } catch (final NumberFormatException e) {
                logger.warn("API call to {} returned non-integer response: {}", url, body);
                return -1;
            }
        } catch (final IOException | InterruptedException e) {
            logger.warn("API call to {} failed: {}", url, e.getMessage());
            return -1;
        }
    }

    @SuppressWarnings("rawtypes")
    private List<String> extractApiArgs(final List args) {
        final List<String> apiArgs = new ArrayList<>();
        if (args.size() > 2) {
            final Object argsValue = args.get(2);
            if (argsValue instanceof Iterable) {
                for (final Object item : (Iterable<?>) argsValue) {
                    apiArgs.add(String.valueOf(item));
                }
            } else {
                apiArgs.add(String.valueOf(argsValue));
            }
        }
        return apiArgs;
    }

    private String buildUrl(final String baseUrl, final String funcName,
            final List<String> apiArgs) {
        final StringBuilder url = new StringBuilder(baseUrl);
        if (!baseUrl.endsWith("/")) {
            url.append('/');
        }
        url.append(funcName);

        for (int i = 0; i < apiArgs.size(); i++) {
            url.append(i == 0 ? '?' : '&')
                    .append("arg").append(i + 1).append('=')
                    .append(URLEncoder.encode(apiArgs.get(i), StandardCharsets.UTF_8));
        }
        return url.toString();
    }
}
