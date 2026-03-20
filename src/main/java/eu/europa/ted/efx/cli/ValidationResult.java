package eu.europa.ted.efx.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Self-aware model that constructs itself from an SVRL validation report.
 * Owns the extracted failures and answers questions about the validation outcome.
 */
class ValidationResult {

    private static final String SVRL_NS = "http://purl.oclc.org/dsdl/svrl";

    private final List<Failure> failures = new ArrayList<>();

    ValidationResult(final Document svrl) {
        this.extractFailures(svrl.getElementsByTagNameNS(SVRL_NS, "failed-assert"));
        this.extractFailures(svrl.getElementsByTagNameNS(SVRL_NS, "successful-report"));
    }

    boolean isValid() {
        return this.failures.isEmpty();
    }

    int failureCount() {
        return this.failures.size();
    }

    void print(final PrintStream out) {
        for (final Failure failure : this.failures) {
            out.printf("[%s] %s at %s: %s%n",
                    failure.role, failure.id, failure.location, failure.message);
        }
    }

    private void extractFailures(final NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            final Element element = (Element) nodes.item(i);
            this.failures.add(new Failure(
                    element.getAttribute("id"),
                    element.getAttribute("location"),
                    element.getAttribute("role"),
                    element.getTextContent().trim()));
        }
    }

    private static class Failure {
        private final String id;
        private final String location;
        private final String role;
        private final String message;

        private Failure(final String id, final String location,
                final String role, final String message) {
            this.id = id;
            this.location = location;
            this.role = role;
            this.message = message;
        }
    }
}
