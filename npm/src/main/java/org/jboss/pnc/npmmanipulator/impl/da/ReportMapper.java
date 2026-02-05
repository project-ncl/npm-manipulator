/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018-2026 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
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

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> responseBody = objectMapper.readValue(s, List.class);

                for (Map<String, Object> npmPackage : responseBody) {
                    String name = (String) npmPackage.get("name");
                    String version = (String) npmPackage.get("version");
                    Version semverVersion = Version.valueOf(version);
                    // String bestMatchVersion = (String) npmPackage.get("bestMatchVersion");
                    @SuppressWarnings("unchecked")
                    List<String> availableVersions = (List<String>) npmPackage.get("availableVersions");

                    if (availableVersions != null) {
                        NpmPackageRef project = new NpmPackageRef(name, semverVersion);
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
        Object request;

        List<Map<String, Object>> requestBody = new ArrayList<>();

        for (NpmPackageRef project : projects) {
            Map<String, Object> gav = new HashMap<>();
            gav.put("name", project.getName());
            gav.put("version", project.getVersion().toString());

            requestBody.add(gav);
        }

        request = new NVSchema("MAJOR_MINOR", mode, includeAll, requestBody);

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
