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
package org.jboss.pnc.npmmanipulator.impl.da;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonjava.atlas.npm.ident.ref.NpmPackageRef;
import org.jboss.da.model.rest.ErrorMessage;
import org.jboss.da.model.rest.NPMPackage;
import org.jboss.da.reports.model.request.VersionsNPMRequest;
import org.jboss.da.reports.model.response.NPMVersionsReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.zafarkhaja.semver.Version;

public class ReportMapper implements ReportObjectMapper {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private final boolean includeAll;

    private final String mode;

    private String errorString;

    public ReportMapper(boolean includeAll, String mode) {
        this.includeAll = includeAll;
        this.mode = mode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(String s, Class<T> valueType) {
        if (valueType == Map.class) {
            Map<NpmPackageRef, List<String>> result = new HashMap<>();

            // Workaround for https://github.com/Mashape/unirest-java/issues/122
            // Rather than throwing an exception we return an empty body which allows
            // DefaultTranslator to examine the status codes.

            if (s.length() == 0) {
                errorString = "No content to read.";
                return (T) result;
            } else if (s.startsWith("<")) {
                // Read an HTML string.
                String stripped = s.replaceAll("<.*?>", "").replaceAll("\n", " ").trim();
                logger.debug("Read HTML string '{}' rather than a JSON stream; stripping message to '{}'", s, stripped);
                errorString = stripped;
                return (T) result;
            }

            try {
                if (s.startsWith("{\"")) {
                    errorString = objectMapper.readValue(s, ErrorMessage.class).toString();

                    logger.debug("Read message string {}, processed to {} ", s, errorString);

                    return (T) result;
                }

                List<NPMVersionsReport> responseBody = objectMapper.readValue(
                        s,
                        new TypeReference<List<NPMVersionsReport>>() {
                        });

                for (NPMVersionsReport report : responseBody) {
                    NPMPackage npmPackage = report.getNpmPackage();
                    List<String> availableVersions = report.getAvailableVersions();
                    if (availableVersions != null) {
                        NpmPackageRef project = new NpmPackageRef(
                                npmPackage.getName(),
                                Version.parse(npmPackage.getVersion()));
                        result.put(project, availableVersions);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to decode map when reading string {}", s);
                throw new RuntimeException(
                        "Failed to read list-of-maps response from version server: " + e.getMessage(),
                        e);
            }

            return (T) result;
        } else {
            throw new RuntimeException("Unsupported value type: " + valueType);
        }
    }

    @Override
    public String writeValue(Object value) {
        @SuppressWarnings("unchecked")
        List<NpmPackageRef> projects = (List<NpmPackageRef>) value;

        List<NPMPackage> packages = new ArrayList<>();
        for (NpmPackageRef project : projects) {
            packages.add(new NPMPackage(project.getName(), project.getVersion().toString()));
        }

        VersionsNPMRequest request = VersionsNPMRequest.builder()
                .versionFilter(VersionsNPMRequest.VersionFilter.MAJOR_MINOR)
                .mode(mode)
                .includeAll(includeAll)
                .packages(packages)
                .build();

        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize version request: " + e.getMessage(), e);
        }
    }

    @Override
    public String getErrorString() {
        return errorString;
    }
}
