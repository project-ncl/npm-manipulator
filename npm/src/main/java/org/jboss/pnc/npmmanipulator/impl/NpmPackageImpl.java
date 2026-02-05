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
package org.jboss.pnc.npmmanipulator.impl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.jboss.pnc.npmmanipulator.api.ManipulationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class NpmPackageImpl implements NpmPackage {

    private static final Logger LOGGER = LoggerFactory.getLogger(NpmPackageImpl.class);

    private File packageFile;
    private File packageLockFile;

    private JsonNode packageJson;
    private JsonNode packageLockJson;
    private JsonNode dependencies;
    private JsonNode devDependencies;

    private ObjectMapper mapper;

    /**
     * Creates an NPM project by referencing project definition files.
     *
     * @param packageFile basic package file, cannot be null
     * @param packageLockFile a package-lock file, may be null if it does not exist for the project
     */
    public NpmPackageImpl(File packageFile, File packageLockFile) {
        super();
        this.packageFile = packageFile;
        this.packageLockFile = packageLockFile;
        this.mapper = new ObjectMapper();
    }

    /**
     * Provides the JsonNode tree parsed from package.json referenced by packageFile.
     *
     * @return read JsonNode, never {@code null}
     * @throws ManipulationException in case of an error when reading package file or the package file does not exist
     */
    public JsonNode getPackage() throws ManipulationException {
        if (packageJson == null) {
            if (packageFile.exists()) {
                String packageContents;
                try {
                    packageContents = FileUtils.readFileToString(packageFile, "utf-8");
                    packageJson = mapper.readTree(packageContents);
                } catch (IOException ex) {
                    throw new ManipulationException("Error reading file {}", packageFile, ex);
                }
            } else {
                throw new ManipulationException("Package file {} does not exist", packageFile.toString());
            }
        }
        return packageJson;
    }

    /**
     * Provides the JsonNode tree parsed from package-lock.json referenced by packageFile.
     *
     * @return read JsonNode or null in case of file does not exist
     * @throws ManipulationException in case of an error when reading package file
     */
    public JsonNode getPackageLock() throws ManipulationException {
        if ((packageLockFile != null) && (packageLockJson == null)) {
            if (packageLockFile.exists()) {
                String packageLockContents;
                try {
                    packageLockContents = FileUtils.readFileToString(packageLockFile, "utf-8");
                    packageLockJson = mapper.readTree(packageLockContents);
                } catch (IOException ex) {
                    throw new ManipulationException("Error reading file {}", packageLockFile, ex);
                }
            }
        }
        return packageLockJson;
    }

    @Override
    @SuppressWarnings("resource")
    public void update() throws ManipulationException {
        JsonFactory factory = new JsonFactory();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

        if (packageJson != null) {
            JsonGenerator generator = null;
            try {
                generator = factory.createGenerator(packageFile, JsonEncoding.UTF8);
                writer.writeValue(generator, packageJson);
            } catch (IOException ex) {
                throw new ManipulationException("Error writing the package file {}.", packageFile, ex);
            } finally {
                if (generator != null && !generator.isClosed()) {
                    try {
                        generator.close();
                    } catch (IOException e) {
                        LOGGER.warn("Was not able to close JsonGenerator.", e);
                    }
                }
            }
        }

        if (packageLockJson != null) {
            JsonGenerator generator = null;
            try {
                generator = factory.createGenerator(packageLockFile, JsonEncoding.UTF8);
                writer.writeValue(generator, packageLockJson);
            } catch (IOException ex) {
                throw new ManipulationException("Error writing the package lock file {}.", packageFile, ex);
            } finally {
                if (generator != null && !generator.isClosed()) {
                    try {
                        generator.close();
                    } catch (IOException e) {
                        LOGGER.warn("Was not able to close JsonGenerator.", e);
                    }
                }
            }
        }
    }

    @Override
    public String getName() throws ManipulationException {
        getPackage();
        JsonNode jsonName = packageJson.get("name");
        if (jsonName == null) {
            throw new ManipulationException("The loaded project file {} does not contain field 'name'.", packageFile);
        }
        return jsonName.asText();
    }

    @Override
    public String getVersion() throws ManipulationException {
        getPackage();
        JsonNode jsonVersion = packageJson.get("version");
        if (jsonVersion == null) {
            throw new ManipulationException(
                    "The loaded project file {} does not contain field 'version'.",
                    packageFile);
        }
        return jsonVersion.asText();
    }

    @Override
    public Map<String, String> getDependencies() throws ManipulationException {
        getPackage();
        dependencies = packageJson.get("dependencies");
        return createDependenciesMap(dependencies);
    }

    @Override
    public Map<String, String> getDevDependencies() throws ManipulationException {
        getPackage();
        devDependencies = packageJson.get("devDependencies");
        return createDependenciesMap(devDependencies);
    }

    @Override
    public void setName(String name) throws ManipulationException {
        getPackage();
        getPackageLock();
        if (packageJson instanceof ObjectNode) {
            ((ObjectNode) packageJson).replace("name", new TextNode(name));
        } else {
            throw new ManipulationException(
                    "The loaded project file {} does not seem to have correct structure.",
                    packageFile);
        }
        if (packageLockJson != null) {
            if (packageLockJson instanceof ObjectNode) {
                ((ObjectNode) packageLockJson).replace("name", new TextNode(name));
            } else {
                throw new ManipulationException(
                        "The loaded project file {} does not seem to have correct structure.",
                        packageLockFile);
            }
        }
    }

    @Override
    public void setVersion(String version) throws ManipulationException {
        getPackage();
        getPackageLock();
        if (packageJson instanceof ObjectNode) {
            ((ObjectNode) packageJson).replace("version", new TextNode(version));
        } else {
            throw new ManipulationException(
                    "The loaded project file {} does not seem to have correct structure.",
                    packageFile);
        }
        if (packageLockJson != null) {
            if (packageLockJson instanceof ObjectNode) {
                ((ObjectNode) packageLockJson).replace("version", new TextNode(version));
            } else {
                throw new ManipulationException(
                        "The loaded project file {} does not seem to have correct structure.",
                        packageLockFile);
            }
        }
    }

    @Override
    public void setDependencyVersion(String dependencyName, String version, boolean isDevelopment)
            throws ManipulationException {
        getPackage();

        if (isDevelopment) {
            devDependencies = packageJson.get("devDependencies");
            replaceDependency(devDependencies, dependencyName, version);
        } else {
            dependencies = packageJson.get("dependencies");
            replaceDependency(dependencies, dependencyName, version);
        }
    }

    private Map<String, String> createDependenciesMap(JsonNode dependenciesNode) {
        Map<String, String> dependenciesMap = new LinkedHashMap<>();
        if (dependenciesNode != null) {
            if (dependenciesNode instanceof ObjectNode) {
                Iterator<Entry<String, JsonNode>> iterator = ((ObjectNode) dependenciesNode).fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> devDependency = iterator.next();
                    dependenciesMap.put(devDependency.getKey().toString(), devDependency.getValue().asText());
                }
            }
        }
        return Collections.unmodifiableMap(dependenciesMap);
    }

    private void replaceDependency(JsonNode dependenciesNode, String dependencyName, String version) {
        if (dependenciesNode != null) {
            if (dependenciesNode instanceof ObjectNode) {
                Iterator<Entry<String, JsonNode>> iterator = ((ObjectNode) dependenciesNode).fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> dependency = iterator.next();
                    if (dependency.getKey().toString().equals(dependencyName)) {
                        dependency.setValue(new TextNode(version));
                    }
                }
            }
        }
    }

}
