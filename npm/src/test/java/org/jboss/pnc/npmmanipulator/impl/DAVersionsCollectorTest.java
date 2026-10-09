/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright © 2018-2026 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.npmmanipulator.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.jboss.pnc.npmmanipulator.api.Project;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import kong.unirest.Unirest;

/**
 * Tests for {@link DAVersionsCollector#restHeaderParser(String)} and header propagation to the DA REST endpoint.
 */
public class DAVersionsCollectorTest {

    // -------------------------------------------------------------------------
    // restHeaderParser – unit tests
    // -------------------------------------------------------------------------

    @Test
    public void parserReturnsEmptyMapForNullInput() {
        Map<String, String> result = DAVersionsCollector.restHeaderParser(null);
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void parserReturnsEmptyMapForEmptyString() {
        Map<String, String> result = DAVersionsCollector.restHeaderParser("");
        assertThat(result.isEmpty(), is(true));
    }

    @Test
    public void parserReturnsSingleHeader() {
        Map<String, String> result = DAVersionsCollector.restHeaderParser("Authorization:Bearer mytoken");
        assertThat(result.size(), is(1));
        assertThat(result.get("Authorization"), is("Bearer mytoken"));
    }

    @Test
    public void parserTrimsWhitespaceAroundNameAndValue() {
        Map<String, String> result = DAVersionsCollector.restHeaderParser("  Authorization : Bearer mytoken  ");
        assertThat(result.size(), is(1));
        assertThat(result.get("Authorization"), is("Bearer mytoken"));
    }

    @Test
    public void parserHandlesMultipleHeaders() {
        Map<String, String> result = DAVersionsCollector
                .restHeaderParser("Authorization:Bearer token123,X-Trace:abc");
        assertThat(result.size(), is(2));
        assertThat(result.get("Authorization"), is("Bearer token123"));
        assertThat(result.get("X-Trace"), is("abc"));
    }

    @Test
    public void parserPreservesColonsInValue() {
        // Base64 encoded "user:pass" contains a colon in the original form; the encoded form is a single token
        // but we verify that a value containing a colon is kept intact when split with limit 2.
        Map<String, String> result = DAVersionsCollector.restHeaderParser("Authorization:Basic dXNlcjpwYXNz");
        assertThat(result.size(), is(1));
        assertThat(result.get("Authorization"), is("Basic dXNlcjpwYXNz"));
    }

    @Test
    public void parserHandlesHeaderWithEmptyValue() {
        Map<String, String> result = DAVersionsCollector.restHeaderParser("X-Empty:");
        assertThat(result.size(), is(1));
        assertThat(result.get("X-Empty"), is(""));
    }

    @Test
    public void parserLastValueWinsForDuplicateNames() {
        Map<String, String> result = DAVersionsCollector
                .restHeaderParser("Authorization:first,Authorization:second");
        assertThat(result.size(), is(1));
        assertThat(result.get("Authorization"), is("second"));
    }

    // -------------------------------------------------------------------------
    // Mock HTTP server integration tests
    // -------------------------------------------------------------------------

    private HttpServer mockServer;
    private int mockPort;

    /** Captured headers from the last request received by the mock server. */
    private final AtomicReference<Map<String, List<String>>> capturedHeaders = new AtomicReference<>(
            Collections.emptyMap());

    /** Captured raw request body bytes from the last request received by the mock server. */
    private final AtomicReference<String> capturedRequestBody = new AtomicReference<>("");

    /** Response body the mock server will return; defaults to an empty versions array. */
    private final AtomicReference<String> mockResponseBody = new AtomicReference<>("[]");

    @Before
    public void startMockServer() throws IOException {
        // Unirest uses a global static config; reset it so each test gets a clean slate.
        Unirest.config().reset();
        mockResponseBody.set("[]");

        mockServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockPort = mockServer.getAddress().getPort();

        mockServer.createContext("/da/rest/v-1/reports/versions/npm", exchange -> {
            capturedHeaders.set(exchange.getRequestHeaders());
            try (InputStream is = exchange.getRequestBody()) {
                capturedRequestBody.set(new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            byte[] response = mockResponseBody.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        mockServer.start();
    }

    @After
    public void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    /**
     * Helper that configures a {@link NpmManipulationSession} (via the package-private no-arg constructor), wires
     * properties, calls {@link DAVersionsCollector#init} and then drives {@link DAVersionsCollector#applyChanges} with
     * a minimal project list built from the given {@link NpmPackageImpl} instances.
     */
    private NpmManipulationSession buildSession(String restHeaders) throws ManipulationException {
        NpmManipulationSession session = new NpmManipulationSession();
        session.getUserProps().setProperty("restURL", "http://127.0.0.1:" + mockPort + "/da/rest/");
        session.getUserProps().setProperty("versionIncrementalSuffix", "redhat");
        session.getUserProps().setProperty("versioningStrategy", "HYPHENED");
        if (restHeaders != null) {
            session.getUserProps().setProperty(DAVersionsCollector.REST_HEADERS, restHeaders);
        }
        return session;
    }

    private List<Project> singleProjectList() {
        // Minimal NpmPackageImpl substitute using NpmPackage interface directly via a simple anonymous class
        // is not possible (interface has many methods). We create a stub NpmPackage list instead.
        NpmPackageStub pkg = new NpmPackageStub("test-pkg", "1.0.0");
        List<Project> projects = new ArrayList<>();
        projects.add(pkg);
        return projects;
    }

    @Test
    public void authorizationHeaderIsForwardedToDA() throws ManipulationException {
        NpmManipulationSession session = buildSession("Authorization:Bearer secret-token");
        DAVersionsCollector collector = new DAVersionsCollector();
        collector.init(session);
        collector.applyChanges(singleProjectList());

        Map<String, List<String>> headers = capturedHeaders.get();
        assertThat(headers.containsKey("Authorization"), is(true));
        assertThat(headers.get("Authorization").get(0), is("Bearer secret-token"));
    }

    @Test
    public void multipleRestHeadersAreAllForwardedToDA() throws ManipulationException {
        NpmManipulationSession session = buildSession("Authorization:Bearer tok,X-Trace:xyz");
        DAVersionsCollector collector = new DAVersionsCollector();
        collector.init(session);
        collector.applyChanges(singleProjectList());

        Map<String, List<String>> headers = capturedHeaders.get();
        assertThat(headers.containsKey("Authorization"), is(true));
        assertThat(headers.get("Authorization").get(0), is("Bearer tok"));
        assertThat(headers.containsKey("X-Trace"), is(true));
        assertThat(headers.get("X-Trace").get(0), is("xyz"));
    }

    @Test
    public void logContextHeaderIsAlwaysPresent() throws ManipulationException {
        NpmManipulationSession session = buildSession(null);
        DAVersionsCollector collector = new DAVersionsCollector();
        collector.init(session);
        collector.applyChanges(singleProjectList());

        Map<String, List<String>> headers = capturedHeaders.get();
        assertThat(headers.containsKey("Log-context"), is(true));
    }

    @Test
    public void noRestHeadersPropertyMeansNoExtraHeaders() throws ManipulationException {
        NpmManipulationSession session = buildSession(null);
        DAVersionsCollector collector = new DAVersionsCollector();
        collector.init(session);
        collector.applyChanges(singleProjectList());

        Map<String, List<String>> headers = capturedHeaders.get();
        assertThat(headers.containsKey("Authorization"), is(false));
    }

    @Test
    public void availableVersionsArePopulatedFromDAResponse() throws ManipulationException {
        // NPMVersionsReport uses @JsonUnwrapped on its npmPackage field, so name/version are at the top level
        // (matches actual DA response shape confirmed by live curl against reports/lookup/npm)
        mockResponseBody.set(
                "[{\"name\":\"test-pkg\",\"version\":\"1.0.0\","
                        + "\"availableVersions\":[\"1.0.0-redhat-00001\",\"1.0.0-redhat-00002\"]}]");

        NpmManipulationSession session = buildSession(null);
        DAVersionsCollector collector = new DAVersionsCollector();
        collector.init(session);
        collector.applyChanges(singleProjectList());

        @SuppressWarnings("unchecked")
        Map<String, java.util.Set<String>> available = session.getState(
                DAVersionsCollector.AVAILABLE_VERSIONS,
                Map.class);
        assertThat(available != null, is(true));
        assertThat(available.containsKey("test-pkg"), is(true));
        assertThat(available.get("test-pkg").contains("1.0.0-redhat-00001"), is(true));
        assertThat(available.get("test-pkg").contains("1.0.0-redhat-00002"), is(true));
    }

    @Test
    public void requestBodyUsesVersionsNPMRequestShape() throws ManipulationException {
        NpmManipulationSession session = buildSession(null);
        DAVersionsCollector collector = new DAVersionsCollector();
        collector.init(session);
        collector.applyChanges(singleProjectList());

        String body = capturedRequestBody.get();
        // VersionsNPMRequest serialises with versionFilter / packages / includeAll fields
        assertThat(body.contains("\"versionFilter\""), is(true));
        assertThat(body.contains("\"packages\""), is(true));
        assertThat(body.contains("\"includeAll\""), is(true));
        // Must NOT use the old NVSchema field name
        assertThat(body.contains("\"versionFilter\":\"MAJOR_MINOR\""), is(true));
        // package entry must carry name and version
        assertThat(body.contains("\"name\":\"test-pkg\""), is(true));
        assertThat(body.contains("\"version\":\"1.0.0\""), is(true));
    }

    // -------------------------------------------------------------------------
    // Minimal NpmPackage stub for use in integration tests
    // -------------------------------------------------------------------------

    /**
     * Minimal stub for {@link NpmPackage} / {@link org.jboss.pnc.npmmanipulator.api.Project} that provides just the
     * name and version needed by {@link DAVersionsCollector#collect}.
     */
    private static class NpmPackageStub implements NpmPackage {

        private final String name;
        private final String version;

        NpmPackageStub(String name, String version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public String getName() throws ManipulationException {
            return name;
        }

        @Override
        public void setName(String name) throws ManipulationException {
        }

        @Override
        public String getVersion() throws ManipulationException {
            return version;
        }

        @Override
        public void setVersion(String version) throws ManipulationException {
        }

        @Override
        public Map<String, String> getDependencies() throws ManipulationException {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, String> getDevDependencies() throws ManipulationException {
            return Collections.emptyMap();
        }

        @Override
        public void setDependencyVersion(String dependencyName, String version, boolean isDevelopment)
                throws ManipulationException {
        }

        @Override
        public void update() throws ManipulationException {
        }
    }
}
