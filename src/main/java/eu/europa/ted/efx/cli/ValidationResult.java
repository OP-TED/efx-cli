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

import java.util.ArrayList;
import java.util.Collections;
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

    private final int rulesChecked;
    private final List<Failure> failures = new ArrayList<>();

    ValidationResult(final Document svrl) {
        this.rulesChecked = svrl.getElementsByTagNameNS(SVRL_NS, "fired-rule").getLength();
        this.extractFailures(svrl.getElementsByTagNameNS(SVRL_NS, "failed-assert"));
        this.extractFailures(svrl.getElementsByTagNameNS(SVRL_NS, "successful-report"));
    }

    boolean isValid() {
        return this.failures.isEmpty();
    }

    int rulesChecked() {
        return this.rulesChecked;
    }

    int failureCount() {
        return this.failures.size();
    }

    List<Failure> failures() {
        return Collections.unmodifiableList(this.failures);
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

    static class Failure {
        private final String id;
        private final String location;
        private final String role;
        private final String message;

        Failure(final String id, final String location,
                final String role, final String message) {
            this.id = id;
            this.location = location;
            this.role = role;
            this.message = message;
        }

        String id() { return this.id; }
        String location() { return this.location; }
        String role() { return this.role; }
        String message() { return this.message; }
    }
}
